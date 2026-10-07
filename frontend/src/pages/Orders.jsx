import { useState, useEffect } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'

const PAGE_SIZE = 10

const statusBadge = {
  PENDING: 'badge-yellow', PAID: 'badge-blue', SHIPPED: 'badge-blue',
  DELIVERED: 'badge-green', CANCELLED: 'badge-red',
}

export default function Orders() {
  const { user, isAdmin } = useAuth()
  const userId = user?.userId
  const [orders, setOrders] = useState([])
  const [detail, setDetail] = useState(null)
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const load = () => {
    setErr('')
    setLoading(true)
    if (isAdmin) {
      api.getAllOrders(`?page=${page}&size=${PAGE_SIZE}&sort=id,desc`)
        .then(r => { setOrders(r.data.content || []); setTotalPages(r.data.totalPages || 0) })
        .catch(e => setErr(e.message))
        .finally(() => setLoading(false))
    } else {
      api.getOrders()
        .then(r => { setOrders(r.data || []); setTotalPages(0) })
        .catch(e => setErr(e.message))
        .finally(() => setLoading(false))
    }
  }
  useEffect(() => { setPage(0) }, [isAdmin])
  useEffect(() => { load() }, [userId, isAdmin, page])

  const cancel = async (id) => {
    if (!confirm('确定取消此订单？')) return
    try { await api.cancelOrder(id); load() } catch (e) { setErr(e.message) }
  }

  const nextStatus = {
    PENDING: { label: '标记已付款', next: 'PAID' },
    PAID: { label: '标记已发货', next: 'SHIPPED' },
    SHIPPED: { label: '标记已完成', next: 'DELIVERED' },
  }

  const advance = async (id, status) => {
    try { await api.updateOrderStatus(id, status); load() } catch (e) { setErr(e.message) }
  }

  return (
    <div>
      <div className="header"><h1>订单管理</h1></div>
      {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}

      {loading ? (
        <div className="empty">加载中...</div>
      ) : (
        <table>
          <thead>
            <tr><th>订单号</th><th>总金额</th><th>状态</th><th>创建时间</th><th style={{width:240}}>操作</th></tr>
          </thead>
          <tbody>
            {orders.map(o => (
              <tr key={o.id}>
                <td>#{o.id}</td>
                <td>¥{Number(o.totalAmount).toFixed(2)}</td>
                <td><span className={`badge ${statusBadge[o.status] || 'badge-blue'}`}>{o.status}</span></td>
                <td>{o.createdAt?.replace('T', ' ')?.substring(0, 19) || '-'}</td>
                <td>
                  <button className="btn btn-primary btn-sm" onClick={() => setDetail(o)}>详情</button>
                  {o.status === 'PENDING' && <button className="btn btn-danger btn-sm" onClick={() => cancel(o.id)}>取消</button>}
                  {isAdmin && nextStatus[o.status] && (
                    <button className="btn btn-success btn-sm" onClick={() => advance(o.id, nextStatus[o.status].next)}>{nextStatus[o.status].label}</button>
                  )}
                </td>
              </tr>
            ))}
            {orders.length === 0 && <tr><td colSpan={5} className="empty">暂无订单</td></tr>}
          </tbody>
        </table>
      )}

      {isAdmin && totalPages > 1 && (
        <div className="btn-row" style={{ justifyContent: 'center', marginTop: 16 }}>
          <button className="btn" disabled={page <= 0} onClick={() => setPage(p => p - 1)}>上一页</button>
          <span style={{ fontSize: 14, color: '#666' }}>第 {page + 1} / {totalPages} 页</span>
          <button className="btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>下一页</button>
        </div>
      )}

      {detail && (
        <div className="overlay" onClick={() => setDetail(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>订单 #{detail.id} 详情</h2>
            <p style={{ marginBottom: 12, color: '#666' }}>
              状态: <span className={`badge ${statusBadge[detail.status] || ''}`}>{detail.status}</span>
              &nbsp;| 总金额: ¥{Number(detail.totalAmount).toFixed(2)}
              &nbsp;| {detail.createdAt?.replace('T', ' ')?.substring(0, 19) || ''}
            </p>
            <table>
              <thead>
                <tr><th>商品</th><th>单价</th><th>数量</th><th>小计</th></tr>
              </thead>
              <tbody>
                {(detail.items || []).map(item => (
                  <tr key={item.id}>
                    <td>{item.productName}</td>
                    <td>¥{Number(item.price).toFixed(2)}</td>
                    <td>{item.quantity}</td>
                    <td>¥{(Number(item.price) * item.quantity).toFixed(2)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="btn-row">
              <button className="btn" onClick={() => setDetail(null)}>关闭</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
