# microservice

一个电商演示系统的微服务重构版：原 `ideaTest` 单体重构为 Spring Cloud 微服务架构，完整保留原有技术栈与业务契约（接口路径、JWT Cookie 鉴权、统一响应体、MQ 事件、ES 搜索、Redis 缓存）。

## 技术栈

- 后端：Spring Boot 4.1（Java 25）+ Spring Cloud 2025.1.2（Oakwood），Maven 多模块
- 微服务：Eureka 注册中心、Config Server 配置中心、Spring Cloud Gateway 网关
- 前端：React 19 + Vite（SPA，构建产物在 `frontend/dist/`）
- 数据库：PostgreSQL 16（每服务独立库）
- 缓存：Redis 7
- 搜索引擎：Elasticsearch 7.17
- 消息队列：RabbitMQ（商品索引同步、订单/库存事件解耦）
- 网关/入口：Nginx（`8088` 提供前端页面 + `/api` 代理到网关）
- 鉴权：Spring Security + JWT（登录/注册后写入 `HttpOnly` Cookie，同时兼容 `Authorization: Bearer` 头；前端不持久化 token）

## 架构与模块

```
请求 → Nginx(:8088) ──/api──> Gateway(:8080) ──路由──> 各服务
                                              │
                Eureka(:8761) <─────注册──────┘
                Config Server(:8888) ←──── 拉取配置

postgres(:5432)  redis(:6379)  elasticsearch(:9200)  rabbitmq(:5672/15672)
```

| 模块 | 端口 | 职责 |
|---|---|---|
| `microservice-common` | - | 公共 DTO、实体、异常、MQ 消息载体、JWT/Security 工具、全局异常处理 |
| `microservice-registry` | 8761 | Eureka 注册中心 |
| `microservice-config` | 8888 | Config Server（native classpath 配置仓库，`src/main/resources/config/*.properties`） |
| `microservice-gateway` | 8080 | 网关，`/api/**` 按前缀路由到各服务（`StripPrefix=1`） |
| `user-service` | 8081 | 认证与用户（登录/注册/登出、用户管理、Feign 级联删除购物车与订单） |
| `product-service` | 8082 | 商品、分类、ES 搜索、库存扣减/回补、MQ 索引消费者、Redis 缓存 |
| `cart-service` | 8083 | 购物车（Feign 调用商品服务校验库存） |
| `order-service` | 8084 | 订单（Feign 编排购物车/商品/库存，MQ 订单事件） |
| `frontend` | 8088 | React SPA + Nginx（`/api` 反代网关） |

**服务间调用**：服务间通过 OpenFeign + Eureka 发现调用；内部调用使用 `X-Internal-Token` 头（由公共 `InternalAuthFilter` 映射为管理员身份），不再透传用户 JWT。

## 安全与一致性设计

- **鉴权**：JWT 存 `HttpOnly` + `SameSite=Lax` Cookie（`/auth/login`、`/auth/register` 下发，`/auth/logout` 清除），前端不再把 token 存进 `localStorage`，降低 XSS 窃取风险；`SameSite=Lax` 同时抑制跨站请求携带 Cookie。HTTPS 部署时设置 `JWT_COOKIE_SECURE=true` 为 Cookie 追加 `Secure` 标志。
- **登出吊销**：`/auth/logout` 会把当前 token 写入 Redis 黑名单（key 为 SHA-256(token)，TTL 至 token 自然过期），所有业务服务的 JWT 过滤器统一校验黑名单，登出后 token 立即失效；Redis 不可用时降级为 fail-open（仅失去主动吊销能力，不影响鉴权可用性）。
- **接口权限**：购物车与订单接口在控制器层做**属主校验**（`SecurityUtils.requireOwnerOrAdmin`），普通用户只能操作自己的数据；用户/商品/分类写操作与订单状态变更仅管理员可访问。
- **并发库存**：下单通过原子 `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?` 扣减（`ProductRepository.decrementStock`），0 行受影响即抛「库存不足」，避免高并发超卖；取消订单原子回补。
- **统一响应与异常**：所有接口统一返回 HTTP 200，状态由响应体中的数字 `code` 字段表达（枚举 `ApiStatus`：`SUCCESS=200`/`BAD_REQUEST=400`/`UNAUTHORIZED=401`/`FORBIDDEN=403`/`NOT_FOUND=404`/`CONFLICT=409`/`INTERNAL_ERROR=500`）；`@RestControllerAdvice` 全局异常处理，统一返回 `ApiResponse{code,message,data}`。
- **数据一致性**：Elasticsearch 商品索引通过 RabbitMQ 异步同步——业务事务提交后（`@TransactionalEventListener(AFTER_COMMIT)`）由 `MqEventForwarder` 把事件投递到 `ideatest.events` topic 交换机，`ProductIndexConsumer` 消费后写 ES（失败重试 3 次，仍失败则丢弃并在下次启动由 bootstrap 全量回填）；启动时自动全量回填存量商品（`ProductIndexBootstrap`）。
- **跨服务补偿**：下单逐项扣库存后若清空购物车失败、或取消订单回补库存中途失败，会对已完成步骤执行反向补偿（尽力而为，失败则记录 error 日志提示人工修数）；本地事务随异常回滚，保证订单数据与已扣库存最终对齐。
- **数据库迁移**：schema 由 Flyway 管理（各服务 `db/migration/V1__init.sql`），启动时自动应用增量迁移；`baseline-on-migrate=true` 使存量库平滑纳入版本管理。
- **事件解耦**：订单（创建/取消/状态变更）与库存（扣减/回补）变更以 MQ 事件发布（`order.*` / `stock.*` routing key），当前由消费者记录日志，为通知、统计等扩展预留订阅点。
- **订单取消一致性**：取消订单只能走 `/orders/{id}/cancel`（先原子翻转 PENDING→CANCELLED 再回补库存，防并发双倍回补）；本人或管理员可调用，管理员可代客取消；管理员状态接口 `PUT /orders/{id}/status` 不接受 `CANCELLED`，避免绕过库存回补。
- **端口收敛**：docker compose 中除前端 Nginx 外，所有端口（中间件、注册/配置中心、各业务服务）仅绑定宿主机回环地址 `127.0.0.1`——本机调试可达，但不向局域网/公网暴露；远程访问请用 SSH 隧道或自行调整映射。
- **生产安全**：`jwt.secret` 在 `prod`/`dev`/`test` profile 下均无默认值（`${JWT_SECRET}`），未设置直接启动失败；RabbitMQ 密码同理（`${RABBITMQ_PASSWORD}`），dev/test 数据库密码分别为 `${DEV_DB_PASSWORD}` / `${TEST_DB_PASSWORD}`（fail-fast）；初始管理员密码由 `ADMIN_PASSWORD` 控制。`local` profile 保留内置演示密钥，仅限本机回环环境使用。
- **测试与 CI**：JUnit 5 + Mockito + AssertJ 单元测试（无需中间件），GitHub Actions 在 push/PR 时自动执行 `./mvnw verify` 与前端 `npm run build`。

## 配置说明

配置由 Config Server 统一管理（`microservice-config/src/main/resources/config/`），按 `服务名` + `-<profile>` 分文件；各服务 `application.properties` 通过 `spring.config.import=configserver:http://${CONFIG_SERVER_HOST:localhost}:8888` 拉取。激活的 profile 由 Maven 资源过滤在构建时按根 `pom.xml` 的 `profiles.active` 属性（默认 `local`）烘焙进 `application.properties`（`spring.profiles.active=@profiles.active@`）。

| profile | 用途 | 连接对象 |
|---|---|---|
| `local` | 本机环境 | `localhost` 中间件（IDEA 里直接 Run 连 docker 中间件） |
| `dev` | 开发环境 | 远程 `dev-*` 主机 |
| `test` | 测试环境 | 远程 `test-*` 主机 |
| `prod` | 生产/容器环境 | 远程 `prod-*` 主机（docker compose 使用） |

切换环境：`SPRING_PROFILES_ACTIVE=prod ./mvnw clean package`（或 `-Pprod`）。配置优先级：环境变量 > Config Server 对应 profile 文件 > 服务本地 `application.properties`。

## 快速启动（Docker，推荐）

### 前提

- 已安装 Docker / Docker Compose（v2 以上），Docker daemon 正常运行。
- **首次启动前在根目录执行 `cp .env.example .env`，并修改其中的 `JWT_SECRET`、`INTERNAL_TOKEN` 等密钥**；未设置 `JWT_SECRET` 时业务服务会启动失败（fail-fast，属预期行为）。
- 构建前端镜像时会自动执行 `npm ci && npm run build`（无需本地构建 `dist/`）。
- 构建后端镜像时需要从 Maven Central 拉依赖。
- 网络不稳定时，镜像/依赖拉取可能偶发失败，重跑 `docker compose up -d --build` 即可。

### 启动

```bash
docker compose up -d --build
```

启动顺序：Compose 会等 PostgreSQL / Redis / Elasticsearch / RabbitMQ 全部 healthcheck 通过后，再启动各服务；网关健康后 Nginx 才启动。注册中心（8761）、配置中心（8888）先于业务服务。

### 访问

| 服务 | 地址 |
|---|---|
| 前端页面 | http://localhost:8088 |
| API 入口（经 Nginx → 网关） | http://localhost:8088/api/* |
| 网关健康检查 | http://localhost:8088/api/actuator/health |
| Eureka 控制台 | http://localhost:8761 |
| Config Server | http://localhost:8888 |
| RabbitMQ 管理台 | http://localhost:15672（默认 `ideatest_prod` / `ideatest2026`） |

### 默认账号

- 管理员：`admin` / `admin123`（user-service 启动时若账号不存在则自动创建，可用环境变量 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 覆盖，**上线前务必修改**）

### 环境变量（可覆盖，参考 `.env.example`）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `NGINX_PORT` | `8088` | 前端对外端口 |
| `POSTGRES_USER` / `PROD_DB_PASSWORD` | `ideatest_prod` / `ideatest2026` | 容器 PostgreSQL 账号/密码 |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `ideatest_prod` / `ideatest2026` | RabbitMQ 账号。prod 必填，未设置则启动失败（fail-fast） |
| `JWT_SECRET` | 内置默认（local 用） | JWT 签名密钥。**prod/dev/test 必填**，未设置则启动失败（fail-fast） |
| `JWT_EXPIRATION` | `86400000` | JWT 有效期(ms) |
| `JWT_COOKIE_SECURE` | `false` | HTTPS 部署时设为 `true`，Cookie 追加 `Secure` 标志 |
| `INTERNAL_TOKEN` | 无（prod 必填） | 服务间 Feign 内部调用令牌，**生产必须修改** |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin123` | 初始管理员账号 |
| `DEV_DB_PASSWORD` / `TEST_DB_PASSWORD` | 无（dev/test 必填） | dev/test 环境数据库密码，未设置则启动失败（fail-fast） |

可在项目根目录创建 `.env` 文件覆盖以上变量。

### 查看状态与日志

```bash
docker compose ps                    # 查看各服务健康状态
docker compose logs -f user-service  # 跟踪某服务日志
docker compose logs -f nginx         # 跟踪前端/网关日志
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
```

后端日志文件写在 Docker 数据卷 `service_logs`（容器内 `/app/logs`），不在项目目录里。导出：

```bash
docker compose exec user-service ls -la /app/logs
docker cp microservice-user-service:/app/logs/. ./logs/
```

### 停止 / 清理

```bash
docker compose down        # 停止并删除容器与网络（保留数据卷）
docker compose down -v     # 停止并删除，同时清空全部数据卷
```

## 本地开发（IDEA 中直接跑后端，不使用 Docker）

`local` profile 的连接地址指向宿主机的 `localhost:5432/6379/9200/5672`，schema 由各服务启动时通过 Flyway 自动创建/迁移（无需手工建表）。注意 compose 里 postgres 默认用户是 `ideatest_prod`，而 `local` profile 用的是 `ideatest/ideatest2026`，所以本地起 postgres 必须显式覆盖用户。

### 启动顺序

```bash
# 0. 打开工程：File → Open → 选根目录 pom.xml（以 Maven 项目导入）
#    首次先在 IDEA 里执行一次 Build（或 ./mvnw package），把 @profiles.active@ 过滤成 local

# 1. 起中间件（local profile 专用账号，必须覆盖 POSTGRES_USER）
docker compose up -d postgres redis elasticsearch rabbitmq
#    等 4 个容器 healthcheck 通过（docker compose ps 查看）
```

```text
2. 启动顺序（配置中心 → 注册中心 → 业务服务 → 网关）：

   ① microservice-config   ConfigServerApplication   :8888  （配置中心，必须最先启动）
   ② microservice-registry RegistryApplication      :8761  （注册中心）
   ③ user-service          UserServiceApplication   :8081
   ④ product-service       ProductServiceApplication :8082
   ⑤ cart-service          CartServiceApplication   :8083
   ⑥ order-service         OrderServiceApplication  :8084
   ⑦ microservice-gateway  GatewayApplication       :8080  （最后启动）
```

IDEA 左侧 Maven 面板展开对应模块，在 `src/main/java` 里找到主类右键 **Run** 即可；或命令行逐个启动：

```bash
./mvnw -pl microservice-config -am spring-boot:run
./mvnw -pl microservice-registry -am spring-boot:run
./mvnw -pl user-service -am spring-boot:run
./mvnw -pl product-service -am spring-boot:run
./mvnw -pl cart-service -am spring-boot:run
./mvnw -pl order-service -am spring-boot:run
./mvnw -pl microservice-gateway -am spring-boot:run
```

> 顺序要求：业务服务启动时会先连 Config Server 拉配置，再注册到 Eureka；网关依赖 Eureka 做服务发现。配置中心/注册中心没起来就 Run 业务服务会报 `Could not locate configserver` 或连不上 Eureka。全部启动后可在 `http://localhost:8761` 看到各服务注册列表。

### 访问方式

| 用途 | 地址 | 说明 |
|---|---|---|
| 前端开发模式 | http://localhost:3000 | `cd frontend && npm install && npm run dev`，Vite 代理 `/api` → `localhost:8080`（网关） |
| 前端生产形态 | http://localhost:8088 | `docker compose up -d --build nginx` 起 Nginx，`/api` 反代网关 |
| API 直连（经网关） | http://localhost:8080/api/* | 例：`/api/products`、`/api/auth/login` |
| 注册中心控制台 | http://localhost:8761 | 查看各服务注册状态 |
| RabbitMQ 管理台 | http://localhost:15672 | 默认 `ideatest` / `ideatest2026` |

前端登录默认账号：管理员 `admin` / `admin123`（user-service 启动时自动创建）。

## 运行测试

单元测试均为纯 JUnit/Mockito（不依赖数据库、Redis、ES），可直接在根目录执行：

```bash
./mvnw test
```

前端构建验证：`cd frontend && npm run build`。

CI（GitHub Actions，`.github/workflows/ci.yml`）在 push/PR 时并行执行两个 job：

| job | 内容 |
|---|---|
| `backend` | JDK 25 + `./mvnw verify`（编译 + 单元测试） |
| `frontend` | Node 22 + `npm ci` + `npm run lint` + `npm run build` |

## 常见问题

- **IDEA 启动报 `Connection refused: localhost:5432`**：IDEA 里的服务连接的是 docker 里的中间件（compose 已映射端口到宿主机）。先 `POSTGRES_USER=ideatest docker compose up -d postgres redis elasticsearch rabbitmq` 再 Run；如报用户名/密码错误，说明没覆盖 `POSTGRES_USER`，local profile 用的是 `ideatest/ideatest2026`。
- **服务启动报 `Could not locate configserver` 或拉不到配置**：先启动 `microservice-config`（8888），并确认服务环境变量 `CONFIG_SERVER_HOST` 指向它；IDEA 里默认 `localhost`。
- **网关路由 503 / 无法发现服务**：确认 `microservice-registry`（8761）已启动、各服务已注册（Eureka 控制台可见），且网关环境变量 `EUREKA_HOST` 正确。
- **远程机器访问不了 8761/8888 等端口**：compose 已将除 Nginx 外的端口限制为 `127.0.0.1` 回环绑定（安全收敛）；需要远程访问时用 SSH 隧道，或将对应端口映射改回 `0.0.0.0` 并自行做好认证防护。
- **Feign 调用返回 403**：确认各服务 `INTERNAL_TOKEN` 一致（生产环境与 `internal.token` 配置匹配），内部调用由 `InternalAuthFilter` 放行。
- **登录后请求提示「未登录或 token 已过期」**：JWT 有效期与 Cookie 一致（`JWT_EXPIRATION`，默认 1 天），过期后需重新登录；前端收到 401 会自动跳回登录页。
- **prod 启动报 `Could not resolve placeholder 'JWT_SECRET'`**：`prod` profile 强制要求设置环境变量 `JWT_SECRET`（无默认值），这是预期的 fail-fast；设置后即可启动。
- **后端启动报错 `NoClassDefFoundError: org/apache/http/HttpHost`**：`elasticsearch-java` 必须锁定 `7.17.29`（根 `pom.xml` 的 `dependencyManagement` 已处理），不要升级到 8/9.x，否则与 ES 7.17 不兼容。
- **容器一直 `unhealthy`**：确认 `/actuator/health` 可达且依赖的中间件健康；若返回 401，说明 SecurityConfig 未放行 `/actuator/health`。
- **前端改了不生效**：前端在镜像构建阶段自动执行 `npm run build`，`docker compose up -d --build` 即可生效；不要依赖旧的 `dist/`。
- **首次 `docker compose up` 报数据库已存在**：schema 由 Flyway 管理（增量迁移自动应用）；存量库首次升级会自动 baseline，无需清空数据卷。仅当需要彻底重置数据时执行 `docker compose down -v`。
