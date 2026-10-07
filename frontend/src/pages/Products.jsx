import { useState, useEffect } from 'react'
import { api } from '../api'

export default function Products() {
  const [list, setList] = useState([])
  const [categories, setCategories] = useState([])
  const [keyword, setKeyword] = useState('')
  const [catFilter, setCatFilter] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [edit, setEdit] = useState(null)
  const [form, setForm] = useState({ name: '', description: '', price: '', stock: '', imageUrl: '', categoryId: '' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)

  const load = async () => {
    setErr('')
    setLoading(true)
    try {
      const catRes = await api.getCategories()
      setCategories(Array.from(catRes.data || []))
      if (catFilter) {
        const r = await api.getProductsByCategory(catFilter)
        setList(r.data || [])
      } else {
        const r = await api.getProducts('?size=1000')
        setList(r.data.content || [])
      }
    } catch (e) { setErr(e.message) }
    finally { setLoading(false) }
  }

  useEffect(() => { load() }, [catFilter])

  const search = async () => {
    if (!keyword.trim()) return load()
    const r = await api.searchProducts(keyword)
    setList(r.data || [])
  }

  const openCreate = () => {
    setEdit(null)
    setForm({ name: '', description: '', price: '', stock: '', imageUrl: '', categoryId: '' })
    setErr(''); setShowForm(true)
  }
  const openEdit = (p) => {
    setEdit(p)
    setForm({ name: p.name, description: p.description || '', price: String(p.price), stock: String(p.stock), imageUrl: p.imageUrl || '', categoryId: String(p.categoryId || '') })
    setErr(''); setShowForm(true)
  }

  const save = async () => {
    setErr('')
    if (!form.name.trim()) { setErr('商品名称不能为空'); return }
    const price = parseFloat(form.price)
    const stock = parseInt(form.stock)
    if (isNaN(price) || price <= 0) { setErr('价格必须大于0'); return }
    if (isNaN(stock) || stock < 0) { setErr('库存不能为负数'); return }
    try {
      const body = { ...form, price, stock, categoryId: form.categoryId ? parseInt(form.categoryId) : null }
      if (edit) await api.updateProduct(edit.id, body)
      else await api.createProduct(body)
      setShowForm(false); load()
    } catch (e) { setErr(e.message) }
  }

  const remove = async (id) => {
    if (!confirm('确定删除？')) return
    try {
      await api.deleteProduct(id); load()
    } catch (e) { setErr(e.message) }
  }

  return (
    <div>
      <div className="flex-between">
        <div className="header"><h1>商品管理</h1></div>
        <button className="btn btn-primary" onClick={openCreate}>+ 新建商品</button>
      </div>

      <div className="card" style={{ padding: 12 }}>
        <div className="inline-form">
          <input placeholder="搜索商品..." value={keyword} onChange={e => setKeyword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && search()} />
          <button className="btn btn-primary btn-sm" onClick={search}>搜索</button>
          <select value={catFilter} onChange={e => setCatFilter(e.target.value)}>
            <option value="">全部分类</option>
            {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
      </div>

      {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
      {loading ? (
        <div className="empty">加载中...</div>
      ) : (
      <table>
        <thead>
          <tr><th>ID</th><th>名称</th><th>价格</th><th>库存</th><th>分类</th><th style={{width:160}}>操作</th></tr>
        </thead>
        <tbody>
          {list.map(p => (
            <tr key={p.id}>
              <td>{p.id}</td>
              <td>{p.name}</td>
              <td>¥{Number(p.price).toFixed(2)}</td>
              <td>
                <span className={`badge ${p.stock > 10 ? 'badge-green' : p.stock > 0 ? 'badge-yellow' : 'badge-red'}`}>
                  {p.stock}
                </span>
              </td>
              <td>{categories.find(c => c.id === p.categoryId)?.name || '-'}</td>
              <td>
                <button className="btn btn-primary btn-sm" onClick={() => openEdit(p)}>编辑</button>
                <button className="btn btn-danger btn-sm" onClick={() => remove(p.id)}>删除</button>
              </td>
            </tr>
          ))}
          {list.length === 0 && <tr><td colSpan={6} className="empty">暂无商品</td></tr>}
        </tbody>
      </table>
      )}

      {showForm && (
        <div className="overlay" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>{edit ? '编辑商品' : '新建商品'}</h2>
            {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
            <div className="form-row"><label>名称</label><input value={form.name} onChange={e => setForm({...form, name: e.target.value})} /></div>
            <div className="form-row"><label>描述</label><textarea value={form.description} onChange={e => setForm({...form, description: e.target.value})} /></div>
            <div style={{ display: 'flex', gap: 12 }}>
              <div className="form-row" style={{ flex: 1 }}><label>价格</label><input type="number" step="0.01" value={form.price} onChange={e => setForm({...form, price: e.target.value})} /></div>
              <div className="form-row" style={{ flex: 1 }}><label>库存</label><input type="number" value={form.stock} onChange={e => setForm({...form, stock: e.target.value})} /></div>
            </div>
            <div className="form-row"><label>图片 URL</label><input value={form.imageUrl} onChange={e => setForm({...form, imageUrl: e.target.value})} /></div>
            <div className="form-row">
              <label>分类</label>
              <select value={form.categoryId} onChange={e => setForm({...form, categoryId: e.target.value})}>
                <option value="">无</option>
                {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <div className="btn-row">
              <button className="btn" onClick={() => setShowForm(false)}>取消</button>
              <button className="btn btn-primary" onClick={save}>保存</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
