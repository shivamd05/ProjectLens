import { createContext, useContext, useEffect, useState } from 'react'
import { apiLogin, apiAdminLogin, apiRegister, fetchCurrentUser, revokeSession } from '../services/api'

const AuthContext = createContext(null)
const TOKEN_KEY = 'projectlense_token'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // Remove data created by the former demo-only frontend.
    for (const key of ['projectlense_user', 'projectlense_registered_users', 'projectlense_projects', 'projectlense_evaluations']) localStorage.removeItem(key)
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) { setLoading(false); return }
    fetchCurrentUser().then(setUser).catch(() => localStorage.removeItem(TOKEN_KEY)).finally(() => setLoading(false))
  }, [])

  const login = async (email, password) => {
    try {
      const result = await apiLogin(email, password)
      localStorage.setItem(TOKEN_KEY, result.token)
      setUser(result.user)
      return { success: true, user: result.user }
    } catch (error) { return { success: false, error: error.message } }
  }

  const register = async (name, email, password, department) => {
    try {
      const result = await apiRegister(name, email, password, department)
      localStorage.setItem(TOKEN_KEY, result.token)
      setUser(result.user)
      return { success: true, user: result.user }
    } catch (error) { return { success: false, error: error.message } }
  }

  const loginAdmin = async (adminToken) => {
    try {
      const result = await apiAdminLogin(adminToken)
      localStorage.setItem(TOKEN_KEY, result.token)
      setUser(result.user)
      return { success: true, user: result.user }
    } catch (error) { return { success: false, error: error.message } }
  }

  const logout = async () => {
    try { await revokeSession() } finally { localStorage.removeItem(TOKEN_KEY); setUser(null) }
  }

  return <AuthContext.Provider value={{ user, login, loginAdmin, register, logout, loading }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
