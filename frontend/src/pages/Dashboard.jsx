import { useState, useEffect } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'

export default function Dashboard() {
  const { user, isAdmin } = useAuth()
  const userId = user?.userId
  const [products, setProducts] = useState([])
  const [orders, setOrders] = useState([])
  const [qty, setQty] = useState({})
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  useEffect(() => {
    api.getProducts('?size=1000')
      .then(r => setProducts(r.data.content || []))
      .catch(e => setErr(e.message))
  }, [])

  useEffect(() => {
    if (user && !isAdmin) {
      api.getOrders().then(r => setOrders(r.data || [])).catch(e => setErr(e.message))
    } else if (isAdmin) {
      api.getAllOrders('?size=1000').then(r => setOrders(r.data.content || [])).catch(e => setErr(e.message))
    }
  }, [user, isAdmin])

  const totalAmount = orders.reduce((s, o) => s + Number(o.totalAmount ?? 0), 0)
  const pendingOrders = orders.filter(o => o.status === 'PENDING').length

  const stats = [
    { label: '商品总数', value: products.length },
    { label: isAdmin ? '订单总数' : '我的订单', value: orders.length },
    { label: '待处理', value: pendingOrders },
    { label: '总金额', value: `¥${totalAmount.toFixed(2)}` },
  ]

  const addToCart = async (productId) => {
    setMsg('')
    const quantity = qty[productId] || 1
    try {
      await api.addCartItem(userId, { productId, quantity })
      setMsg('已加入购物车')
    } catch (e) {
      setMsg(e.message)
    }
  }

  return (
    <div>
      <div className="header">
        <h1>概览</h1>
        <p style={{ color: '#666', fontSize: 14, marginTop: 4 }}>欢迎回来, {user?.username}</p>
      </div>
      {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
      <div className="stats">
        {stats.map(s => (
          <div key={s.label} className="stat-card">
            <h3>{s.label}</h3>
            <div className="value">{s.value}</div>
          </div>
        ))}
      </div>

      {!isAdmin && (
        <div className="card">
          <h3 style={{ marginBottom: 12 }}>商品列表</h3>
          {msg && <p style={{ color: '#1a73e8', marginBottom: 12, fontSize: 14 }}>{msg}</p>}
          <div className="product-grid">
            {products.map(p => (
              <div key={p.id} className="product-card">
                <h4>{p.name}</h4>
                <p style={{ color: '#666', fontSize: 13 }}>{p.description}</p>
                <p style={{ fontWeight: 600, margin: '8px 0' }}>¥{Number(p.price).toFixed(2)}</p>
                <p style={{ fontSize: 12, color: p.stock > 0 ? '#666' : '#d93025' }}>
                  {p.stock > 0 ? `库存: ${p.stock}` : '缺货'}
                </p>
                {p.stock > 0 && (
                  <div style={{ display: 'flex', gap: 8, marginTop: 8, alignItems: 'center' }}>
                    <input type="number" min="1" max={p.stock} value={qty[p.id] || 1}
                      onChange={e => setQty({...qty, [p.id]: Math.max(1, Math.min(p.stock, parseInt(e.target.value) || 1))})}
                      style={{ width: 50, padding: 4, textAlign: 'center' }} />
                    <button className="btn btn-primary btn-sm" onClick={() => addToCart(p.id)}>加入购物车</button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <h3 style={{ marginBottom: 12 }}>最近订单</h3>
        {orders.slice(0, 5).map(o => (
          <div key={o.id} style={{ padding: '8px 0', borderBottom: '1px solid #eee', fontSize: 14, display: 'flex', justifyContent: 'space-between' }}>
            <span>订单 #{o.id}</span>
            <span>¥{Number(o.totalAmount ?? 0).toFixed(2)}</span>
            <span className={`badge ${o.status === 'PENDING' ? 'badge-yellow' : o.status === 'CANCELLED' ? 'badge-red' : 'badge-green'}`}>
              {o.status}
            </span>
          </div>
        ))}
        {orders.length === 0 && <div className="empty">暂无订单</div>}
      </div>
    </div>
  )
}
