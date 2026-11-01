import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useToast } from '../contexts/ToastContext'
import Logo from '../components/Logo'

export default function Register() {
  const [name,setName]=useState(''); const [email,setEmail]=useState(''); const [department,setDepartment]=useState(''); const [password,setPassword]=useState(''); const [confirm,setConfirm]=useState(''); const [loading,setLoading]=useState(false)
  const {register}=useAuth(); const {addToast}=useToast(); const navigate=useNavigate()
  const submit=async (event)=>{event.preventDefault();if(password!==confirm)return addToast('Passwords do not match','error');if(password.length<10)return addToast('Password must be at least 10 characters','error');setLoading(true);const result=await register(name,email,password,department);setLoading(false);if(!result.success)return addToast(result.error,'error');addToast('Account created','success');navigate('/student/dashboard')}
  return <div className="min-h-screen bg-gradient-to-b from-[#7fa3d1] via-[#a9c3e0] to-[#eef4fa] flex items-center justify-center p-4"><motion.div initial={{opacity:0,y:10}} animate={{opacity:1,y:0}} className="w-full max-w-md bg-white rounded-3xl shadow-xl p-8 sm:p-10">
    <Link to="/"><Logo/></Link><h1 className="text-2xl font-extrabold text-[#12161f] mt-8">Create a student account</h1><p className="text-sm text-gray-500 mt-1 mb-6">Faculty accounts are provisioned by an administrator.</p>
    <form onSubmit={submit} className="space-y-3"><label className="block text-sm font-medium">Full name<input required maxLength="255" value={name} onChange={e=>setName(e.target.value)} className="input-field mt-1" autoComplete="name"/></label>
      <label className="block text-sm font-medium">Email<input required type="email" maxLength="320" value={email} onChange={e=>setEmail(e.target.value)} className="input-field mt-1" autoComplete="email"/></label>
      <label className="block text-sm font-medium">Department <span className="font-normal text-gray-400">(optional)</span><input maxLength="255" value={department} onChange={e=>setDepartment(e.target.value)} className="input-field mt-1"/></label>
      <label className="block text-sm font-medium">Password<input required minLength="10" maxLength="128" type="password" value={password} onChange={e=>setPassword(e.target.value)} className="input-field mt-1" autoComplete="new-password"/><span className="text-xs text-gray-400">At least 10 characters</span></label>
      <label className="block text-sm font-medium">Confirm password<input required type="password" value={confirm} onChange={e=>setConfirm(e.target.value)} className="input-field mt-1" autoComplete="new-password"/></label>
      <button disabled={loading} className="w-full bg-[#12161f] text-white py-3 rounded-xl text-sm font-semibold disabled:opacity-60">{loading?'Creating account…':'Create Account'}</button>
    </form><p className="text-center text-sm text-gray-500 mt-5">Already registered? <Link to="/login" className="font-semibold text-blue-600">Sign in</Link></p>
  </motion.div></div>
}
