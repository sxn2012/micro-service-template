import { useState, useEffect } from 'react'
import { api } from '../api'

export default function Categories() {
  const [list, setList] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [edit, setEdit] = useState(null)
  const [form, setForm] = useState({ name: '', description: '' })
  const [err, setErr] = useState('')

  const load = () => api.getCategories().then(r => setList(Array.from(r.data || [])))

  useEffect(() => { load() }, [])

  const openCreate = () => { setEdit(null); setForm({ name: '', description: '' }); setErr(''); setShowForm(true) }
  const openEdit = (c) => { setEdit(c); setForm({ name: c.name, description: c.description || '' }); setErr(''); setShowForm(true) }

  const save = async () => {
    setErr('')
    if (!form.name.trim()) { setErr('分类名称不能为空'); return }
    try {
      if (edit) await api.updateCategory(edit.id, form)
      else await api.createCategory(form)
      setShowForm(false); load()
    } catch (e) { setErr(e.message) }
  }

  const remove = async (id) => {
    if (!confirm('确定删除？')) return
    try {
      await api.deleteCategory(id); load()
    } catch (e) { setErr(e.message) }
  }

  return (
    <div>
      <div className="flex-between">
        <div className="header"><h1>分类管理</h1></div>
        <button className="btn btn-primary" onClick={openCreate}>+ 新建分类</button>
      </div>

      {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
      <table>
        <thead>
          <tr><th>ID</th><th>名称</th><th>描述</th><th style={{width:160}}>操作</th></tr>
        </thead>
        <tbody>
          {list.map(c => (
            <tr key={c.id}>
              <td>{c.id}</td><td>{c.name}</td><td>{c.description}</td>
              <td>
                <button className="btn btn-primary btn-sm" onClick={() => openEdit(c)}>编辑</button>
                <button className="btn btn-danger btn-sm" onClick={() => remove(c.id)}>删除</button>
              </td>
            </tr>
          ))}
          {list.length === 0 && <tr><td colSpan={4} className="empty">暂无分类</td></tr>}
        </tbody>
      </table>

      {showForm && (
        <div className="overlay" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>{edit ? '编辑分类' : '新建分类'}</h2>
            {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
            <div className="form-row">
              <label>名称</label>
              <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
            </div>
            <div className="form-row">
              <label>描述</label>
              <textarea value={form.description} onChange={e => setForm({ ...form, description: e.target.value })} />
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
