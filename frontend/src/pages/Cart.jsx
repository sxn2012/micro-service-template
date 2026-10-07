import { useState, useEffect } from 'react'
import { api } from '../api'
import { useAuth } from '../AuthContext'

export default function Cart() {
  const { user } = useAuth()
  const userId = user?.userId
  const [cart, setCart] = useState(null)
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')

  const load = async () => {
    setErr('')
    setLoading(true)
    try {
      const r = await api.getCart(userId)
      setCart(r.data)
    } catch (e) { setErr(e.message) }
    try {
      const r = await api.getProducts('?size=1000')
      setProducts(r.data.content || [])
    } catch (e) { setErr(e.message) }
    setLoading(false)
  }

  useEffect(() => { load() }, [userId])

  const updateQty = async (itemId, qty) => {
    setErr('')
    if (qty < 1) return removeItem(itemId)
    if (qty > 999) { setErr('单次购买数量不能超过999'); return }
    const r = await api.updateCartItem(userId, itemId, qty)
    setCart(r.data)
  }

  const removeItem = async (itemId) => {
    setErr('')
    const r = await api.removeCartItem(userId, itemId)
    setCart(r.data)
  }

  const clearCart = async () => {
    setErr('')
    await api.clearCart(userId)
    load()
  }

  const checkout = async () => {
    setErr('')
    setLoading(true)
    try {
      await api.createOrder(userId)
      load()
    } catch (e) { setErr(e.message)
    } finally { setLoading(false) }
  }

  const getProduct = (id) => products.find(p => p.id === id)
  const items = cart?.items || []
  const total = items.reduce((s, i) => s + Number(getProduct(i.productId)?.price || 0) * i.quantity, 0)

  return (
    <div>
      <div className="flex-between">
        <div className="header"><h1>购物车</h1></div>
        <div>
          {items.length > 0 && <button className="btn btn-danger btn-sm" onClick={clearCart}>清空</button>}
        </div>
      </div>

      <div className="card">
        {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
        {items.length === 0 ? (
          <div className="empty">购物车是空的</div>
        ) : (
          items.map(item => {
            const p = getProduct(item.productId)
            return (
              <div key={item.id} className="cart-item">
                <div className="cart-item-info">
                  <h4>{p?.name || `商品 #${item.productId}`}</h4>
                  <p>¥{Number(p?.price || 0).toFixed(2)} / 件</p>
                </div>
                <div className="quantity-controls">
                  <button onClick={() => updateQty(item.id, item.quantity - 1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateQty(item.id, item.quantity + 1)}>+</button>
                </div>
                <div style={{ width: 80, textAlign: 'right', fontWeight: 600 }}>
                  ¥{(Number(p?.price || 0) * item.quantity).toFixed(2)}
                </div>
                <button className="btn btn-danger btn-sm" onClick={() => removeItem(item.id)}>删除</button>
              </div>
            )
          })
        )}
        {items.length > 0 && (
          <>
            <div className="total">合计: ¥{total.toFixed(2)}</div>
            <button className="btn btn-success" onClick={checkout} disabled={loading}
              style={{ width: '100%', marginTop: 12, padding: 12 }}>
              {loading ? '处理中...' : '去结算'}
            </button>
          </>
        )}
      </div>
    </div>
  )
}
