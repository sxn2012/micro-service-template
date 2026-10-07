import { createContext, useContext, useState } from 'react'
import { api } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem('user')
      return saved ? JSON.parse(saved) : null
    } catch {
      localStorage.removeItem('user')
      return null
    }
  })

  const login = (userData) => {
    const { token, ...profile } = userData
    setUser(profile)
    localStorage.setItem('user', JSON.stringify(profile))
  }

  const logout = () => {
    setUser(null)
    localStorage.removeItem('user')
    api.logout().catch(() => {})
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin: user?.role === 'ADMIN' }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
