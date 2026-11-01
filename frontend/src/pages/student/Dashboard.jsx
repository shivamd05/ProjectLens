import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { LayoutDashboard, FolderOpen, Upload, ClipboardList, MessageSquare, User, FileText, TrendingUp, Clock, CheckCircle, Eye, BarChart3, Bot, Sparkles, ArrowRight } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { useAuth } from '../../contexts/AuthContext'
import { fetchProjects } from '../../services/api'

const sidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/student/dashboard' },
  { icon: FolderOpen, label: 'My Projects', path: '/student/projects' },
  { icon: Upload, label: 'Submit Project', path: '/student/submit' },
  { icon: ClipboardList, label: 'Evaluations', path: '/student/projects' },
  { icon: MessageSquare, label: 'Feedback', path: '/student/projects' },
]

const statusConfig = {
  'submitted': { label: 'Submitted', class: 'badge-submitted' },
  'under-review': { label: 'Under Review', class: 'badge-review' },
  'evaluated': { label: 'Evaluated', class: 'badge-evaluated' },
  'approved': { label: 'Approved', class: 'badge-approved' },
}

export default function StudentDashboard() {
  const { user } = useAuth()
  const [projects, setProjects] = useState([])

  useEffect(() => {
    fetchProjects().then(setProjects).catch(() => setProjects([]))
  }, [user.id])

  const stats = [
    { label: 'Projects Submitted', value: projects.length, icon: FileText, color: 'from-blue-500 to-indigo-600' },
    { label: 'Under Evaluation', value: projects.filter(p => ['submitted', 'under-review', 'evaluated'].includes(p.status)).length, icon: Clock, color: 'from-yellow-500 to-orange-500' },
    { label: 'Completed', value: projects.filter(p => p.status === 'approved').length, icon: CheckCircle, color: 'from-green-500 to-emerald-600' },
    { label: 'Average Score', value: projects.length > 0 ? Math.round(projects.reduce((sum, p) => sum + (p.facultyEvaluation?.finalScore || p.aiEvaluation?.overallScore || 0), 0) / projects.length) + '%' : 'N/A', icon: BarChart3, color: 'from-purple-500 to-indigo-600' },
  ]

  return (
    <DashboardLayout sidebarItems={sidebarItems}>
      <div className="mb-6 flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy-900">Welcome back, {user.name?.split(' ')[0]} 👋</h1>
        <p className="text-gray-500 text-sm mt-0.5">Track your submitted projects and evaluations.</p>
        </div>
      </div>


      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {stats.map((s, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.1 }}
            className="stat-card"
          >
            <div className="flex items-center justify-between mb-3">
              <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${s.color} flex items-center justify-center`}>
                <s.icon className="w-5 h-5 text-white" />
              </div>
              <TrendingUp className="w-4 h-4 text-green-500" />
            </div>
            <p className="text-2xl font-bold text-navy-900">{s.value}</p>
            <p className="text-sm text-gray-500 mt-1">{s.label}</p>
          </motion.div>
        ))}
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-8">
        <Link to="/student/submit" className="card p-6 group hover:border-indigo-200">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-50 flex items-center justify-center group-hover:bg-indigo-100 transition">
              <Upload className="w-6 h-6 text-indigo-600" />
            </div>
            <div>
              <h3 className="font-semibold text-navy-900">Submit New Project</h3>
              <p className="text-sm text-gray-500">Upload a new project for evaluation</p>
            </div>
          </div>
        </Link>
        <Link to="/student/projects" className="card p-6 group hover:border-indigo-200">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-purple-50 flex items-center justify-center group-hover:bg-purple-100 transition">
              <FolderOpen className="w-6 h-6 text-purple-600" />
            </div>
            <div>
              <h3 className="font-semibold text-navy-900">View Projects</h3>
              <p className="text-sm text-gray-500">Check your project statuses</p>
            </div>
          </div>
        </Link>
        <Link to="/student/projects" className="card p-6 group hover:border-indigo-200">
          <div className="flex items-center gap-4">
            <div className="w-12 h-12 rounded-xl bg-green-50 flex items-center justify-center group-hover:bg-green-100 transition">
              <Bot className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <h3 className="font-semibold text-navy-900">Review Feedback</h3>
              <p className="text-sm text-gray-500">View evaluation results and faculty feedback</p>
            </div>
          </div>
        </Link>
      </div>

      {/* Recent Projects */}
      <div className="card">
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-navy-900">Recent Projects</h2>
          <Link to="/student/projects" className="text-sm text-indigo-600 font-medium hover:text-indigo-800">View All</Link>
        </div>
        {projects.length === 0 ? (
          <div className="p-12 text-center">
            <FolderOpen className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500 font-medium">No projects yet</p>
            <p className="text-gray-400 text-sm mt-1">Submit your first project to get started</p>
            <Link to="/student/submit" className="btn-primary inline-flex items-center gap-2 mt-4 text-sm">
              <Upload className="w-4 h-4" /> Submit Project
            </Link>
          </div>
        ) : (
          <div className="divide-y divide-gray-50">
            {projects.map((project) => (
              <div key={project.id} className="px-6 py-4 flex items-center justify-between hover:bg-gray-50/50 transition">
                <div className="flex items-center gap-4 flex-1 min-w-0">
                  <div className="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center shrink-0">
                    <FileText className="w-5 h-5 text-indigo-600" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="font-medium text-navy-900 truncate">{project.title}</h3>
                    <p className="text-xs text-gray-500">{new Date(project.submittedAt).toLocaleDateString()}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <span className={statusConfig[project.status]?.class || 'badge-submitted'}>
                    {statusConfig[project.status]?.label || project.status}
                  </span>
                  <span className="text-sm font-semibold text-navy-900 hidden sm:block">
                    {project.facultyEvaluation?.finalScore || project.aiEvaluation?.overallScore || '-'}/100
                  </span>
                  <Link to={`/student/projects/${project.id}`} className="p-2 rounded-lg hover:bg-indigo-50 text-gray-400 hover:text-indigo-600 transition">
                    <Eye className="w-4 h-4" />
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
