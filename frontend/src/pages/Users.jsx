import { useState, useEffect } from 'react'
import { api } from '../api'

export default function Users() {
  const [list, setList] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [edit, setEdit] = useState(null)
  const [form, setForm] = useState({ username: '', password: '', email: '', phone: '', address: '' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)

  const load = () => {
    setLoading(true)
    api.getUsers()
      .then(r => { setErr(''); setList(Array.from(r.data || [])) })
      .catch(e => setErr(e.message))
      .finally(() => setLoading(false))
  }
  useEffect(() => { load() }, [])

  const openCreate = () => { setEdit(null); setForm({ username: '', password: '', email: '', phone: '', address: '' }); setErr(''); setShowForm(true) }
  const openEdit = (u) => { setEdit(u); setForm({ username: u.username, password: '', email: u.email, phone: u.phone || '', address: u.address || '' }); setErr(''); setShowForm(true) }

  const save = async () => {
    setErr('')
    if (!form.username.trim()) { setErr('用户名不能为空'); return }
    if (!form.email.trim()) { setErr('邮箱不能为空'); return }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { setErr('邮箱格式不正确'); return }
    if (!edit && form.password.length < 6) { setErr('密码至少6位'); return }
    try {
      const body = edit ? { ...form } : form
      if (!body.password && edit) delete body.password
      if (edit) await api.updateUser(edit.id, body)
      else await api.createUser(body)
      setShowForm(false); load()
    } catch (e) { setErr(e.message) }
  }

  const remove = async (id) => {
    if (!confirm('确定删除？')) return
    try {
      await api.deleteUser(id); load()
    } catch (e) {
      setErr(e.message)
    }
  }

  return (
    <div>
      <div className="flex-between">
        <div className="header"><h1>用户管理</h1></div>
        <button className="btn btn-primary" onClick={openCreate}>+ 新建用户</button>
      </div>

      {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
      {loading ? (
        <div className="empty">加载中...</div>
      ) : (
      <table>
        <thead>
          <tr><th>ID</th><th>用户名</th><th>邮箱</th><th>电话</th><th>地址</th><th style={{width:120}}>操作</th></tr>
        </thead>
        <tbody>
          {list.map(u => (
            <tr key={u.id}>
              <td>{u.id}</td><td>{u.username}</td><td>{u.email}</td>
              <td>{u.phone || '-'}</td><td>{u.address || '-'}</td>
              <td>
                <button className="btn btn-primary btn-sm" onClick={() => openEdit(u)}>编辑</button>
                <button className="btn btn-danger btn-sm" onClick={() => remove(u.id)}>删除</button>
              </td>
            </tr>
          ))}
          {list.length === 0 && <tr><td colSpan={6} className="empty">暂无用户</td></tr>}
        </tbody>
      </table>
      )}

      {showForm && (
        <div className="overlay" onClick={() => setShowForm(false)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <h2>{edit ? '编辑用户' : '新建用户'}</h2>
            {err && <p style={{ color: '#d93025', marginBottom: 12, fontSize: 14 }}>{err}</p>}
            <div className="form-row"><label>用户名</label><input value={form.username} onChange={e => setForm({...form, username: e.target.value})} /></div>
            <div className="form-row"><label>密码{edit && ' (留空不修改)'}</label><input type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} /></div>
            <div className="form-row"><label>邮箱</label><input value={form.email} onChange={e => setForm({...form, email: e.target.value})} /></div>
            <div className="form-row"><label>电话</label><input value={form.phone} onChange={e => setForm({...form, phone: e.target.value})} /></div>
            <div className="form-row"><label>地址</label><textarea value={form.address} onChange={e => setForm({...form, address: e.target.value})} /></div>
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
