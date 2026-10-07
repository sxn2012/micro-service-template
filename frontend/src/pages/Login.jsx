import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../AuthContext'

export default function Login() {
  const [isRegister, setIsRegister] = useState(false)
  const [form, setForm] = useState({ username: '', password: '', email: '', phone: '', address: '' })
  const [error, setError] = useState('')
  const { login } = useAuth()
  const navigate = useNavigate()

  const validate = () => {
    if (!form.username.trim()) return '请输入用户名'
    if (form.password.length < 6) return '密码至少6位'
    if (isRegister) {
      if (!form.email.trim()) return '请输入邮箱'
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return '邮箱格式不正确'
    }
    return ''
  }

  const submit = async (e) => {
    e.preventDefault()
    const msg = validate()
    if (msg) { setError(msg); return }
    setError('')
    try {
      const res = isRegister
        ? await api.register(form)
        : await api.login({ username: form.username, password: form.password })
      login(res.data)
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <div className="card" style={{ width: 400, padding: 32 }}>
        <h2 style={{ textAlign: 'center', marginBottom: 24, fontSize: 22 }}>{isRegister ? '注册' : '登录'}</h2>

        {error && <p style={{ color: '#d93025', marginBottom: 12, textAlign: 'center', fontSize: 14 }}>{error}</p>}

        <form onSubmit={submit}>
          <div className="form-row">
            <label>用户名</label>
            <input required value={form.username} onChange={e => setForm({...form, username: e.target.value})} />
          </div>
          <div className="form-row">
            <label>密码</label>
            <input type="password" required value={form.password} onChange={e => setForm({...form, password: e.target.value})} />
          </div>
          {isRegister && (
            <>
              <div className="form-row">
                <label>邮箱</label>
                <input type="email" required value={form.email} onChange={e => setForm({...form, email: e.target.value})} />
              </div>
              <div className="form-row">
                <label>电话</label>
                <input value={form.phone} onChange={e => setForm({...form, phone: e.target.value})} />
              </div>
              <div className="form-row">
                <label>地址</label>
                <input value={form.address} onChange={e => setForm({...form, address: e.target.value})} />
              </div>
            </>
          )}
          <button className="btn btn-primary" type="submit" style={{ width: '100%', padding: 12, marginTop: 8 }}>
            {isRegister ? '注册' : '登录'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: 16, fontSize: 13, color: '#666' }}>
          {isRegister ? '已有账号？' : '没有账号？'}
          <button className="btn" style={{ background: 'none', color: '#1a73e8', textDecoration: 'underline' }}
            onClick={() => { setIsRegister(!isRegister); setError('') }}>
            {isRegister ? '去登录' : '去注册'}
          </button>
        </p>
      </div>
    </div>
  )
}
