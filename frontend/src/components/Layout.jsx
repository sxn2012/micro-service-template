import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

const userNav = [
  { to: '/', label: '概览', icon: '📊' },
  { to: '/cart', label: '购物车', icon: '🛒' },
  { to: '/orders', label: '订单管理', icon: '📋' },
]

const adminNav = [
  { to: '/', label: '概览', icon: '📊' },
  { to: '/products', label: '商品管理', icon: '📦' },
  { to: '/categories', label: '分类管理', icon: '📁' },
  { to: '/orders', label: '订单管理', icon: '📋' },
  { to: '/users', label: '用户管理', icon: '👤' },
]

export default function Layout() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const items = isAdmin ? adminNav : userNav

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div>
      <div style={{ marginBottom: 20, display: 'flex', gap: 12, alignItems: 'center' }}>
        <h2 style={{ fontSize: 20 }}>🛍️ 电商管理系统</h2>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 12, alignItems: 'center' }}>
          <span style={{ fontSize: 13, color: '#666' }}>
            {user?.username}
            {isAdmin && <span className="badge badge-red" style={{ marginLeft: 8 }}>管理员</span>}
          </span>
          <button className="btn btn-sm" onClick={handleLogout} style={{ border: '1px solid #ddd' }}>退出</button>
        </div>
      </div>

      <div className="layout">
        <nav className="sidebar">
          {items.map(item => (
            <NavLink key={item.to} to={item.to} end={item.to === '/'}
              className={({ isActive }) => isActive ? 'active' : ''}>
              {item.icon} {item.label}
            </NavLink>
          ))}
        </nav>
        <main className="content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
