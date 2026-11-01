import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Mail, Lock, ArrowRight } from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import Logo from '../components/Logo'

export default function Login() {
  const [email, setEmail] = useState(''); const [password, setPassword] = useState(''); const [adminToken, setAdminToken] = useState(''); const [adminMode, setAdminMode] = useState(false); const [loading, setLoading] = useState(false)
  const { login, loginAdmin } = useAuth(); const { addToast } = useToast(); const navigate = useNavigate()
  const submit = async (event) => {
    event.preventDefault(); setLoading(true)
    const result = adminMode ? await loginAdmin(adminToken) : await login(email, password); setLoading(false)
    if (!result.success) return addToast(result.error, 'error')
    addToast(`Welcome, ${result.user.name}`, 'success')
    navigate(`/${result.user.role}/dashboard`)
  }
  return <div className="min-h-screen bg-gradient-to-b from-[#7fa3d1] via-[#a9c3e0] to-[#eef4fa] flex items-center justify-center p-4">
    <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md bg-white rounded-3xl shadow-xl p-8 sm:p-10">
      <Link to="/"><Logo /></Link><h1 className="text-2xl font-extrabold text-[#12161f] mt-8">Sign in</h1>
      <p className="text-sm text-gray-500 mt-1 mb-6">Use your ProjectLense account credentials.</p>
      <form onSubmit={submit} className="space-y-4">
        {adminMode ? <label className="block text-sm font-medium">Administrator token<div className="relative mt-1"><Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"/><input required type="password" autoComplete="current-password" value={adminToken} onChange={e=>setAdminToken(e.target.value)} className="input-field pl-10"/></div></label> : <><label className="block text-sm font-medium">Email<div className="relative mt-1"><Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"/><input required type="email" autoComplete="email" value={email} onChange={e=>setEmail(e.target.value)} className="input-field pl-10"/></div></label>
        <label className="block text-sm font-medium">Password<div className="relative mt-1"><Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400"/><input required type="password" autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} className="input-field pl-10"/></div></label></>}
        <button disabled={loading} className="w-full bg-[#12161f] text-white py-3 rounded-xl text-sm font-semibold flex items-center justify-center gap-2 disabled:opacity-60">{loading?'Signing in…':<>Sign In <ArrowRight className="w-4 h-4"/></>}</button>
      </form>
      <button type="button" onClick={()=>setAdminMode(!adminMode)} className="block mx-auto mt-4 text-xs font-semibold text-gray-500 hover:text-blue-600">{adminMode ? 'Sign in as student or faculty' : 'Administrator sign in'}</button>
      <p className="text-center text-sm text-gray-500 mt-5">No account? <Link to="/register" className="font-semibold text-blue-600">Create a student account</Link></p>
    </motion.div>
  </div>
}
