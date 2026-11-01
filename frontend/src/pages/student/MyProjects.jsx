import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { LayoutDashboard, FolderOpen, Upload, ClipboardList, MessageSquare, User, FileText, Eye, Search, Filter } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { useAuth } from '../../contexts/AuthContext'
import { fetchProjects } from '../../services/api'

const sidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/student/dashboard' },
  { icon: FolderOpen, label: 'My Projects', path: '/student/projects' },
  { icon: Upload, label: 'Submit Project', path: '/student/submit' },
  { icon: ClipboardList, label: 'Evaluations', path: '/student/projects' },
  { icon: MessageSquare, label: 'Feedback', path: '/student/projects' },
  { icon: User, label: 'Profile', path: '/student/dashboard' },
]

const statusConfig = {
  'submitted': { label: 'Submitted', class: 'badge-submitted' },
  'under-review': { label: 'Under Review', class: 'badge-review' },
  'evaluated': { label: 'Evaluated', class: 'badge-evaluated' },
  'approved': { label: 'Approved', class: 'badge-approved' },
}

export default function MyProjects() {
  const { user } = useAuth()
  const [projects, setProjects] = useState([])
  const [search, setSearch] = useState('')
  const [filterStatus, setFilterStatus] = useState('all')

  useEffect(() => {
    fetchProjects().then(setProjects).catch(() => setProjects([]))
  }, [user.id])

  const filtered = projects.filter(p => {
    const matchSearch = p.title.toLowerCase().includes(search.toLowerCase())
    const matchStatus = filterStatus === 'all' || p.status === filterStatus
    return matchSearch && matchStatus
  })

  return (
    <DashboardLayout sidebarItems={sidebarItems}>
      <div className="mb-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-navy-900">My Projects</h1>
          <p className="text-gray-500 mt-1">View and manage your submitted projects.</p>
        </div>
        <Link to="/student/submit" className="btn-primary inline-flex items-center gap-2 text-sm self-start">
          <Upload className="w-4 h-4" /> New Project
        </Link>
      </div>

      {/* Filters */}
      <div className="card p-4 mb-6">
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="input-field pl-10 !py-2.5 text-sm"
              placeholder="Search projects..."
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-gray-400" />
            <select
              value={filterStatus}
              onChange={e => setFilterStatus(e.target.value)}
              className="input-field !py-2.5 text-sm !w-auto"
            >
              <option value="all">All Status</option>
              <option value="submitted">Submitted</option>
              <option value="under-review">Under Review</option>
              <option value="evaluated">Evaluated</option>
              <option value="approved">Approved</option>
            </select>
          </div>
        </div>
      </div>

      {/* Projects Table */}
      <div className="card overflow-hidden">
        {filtered.length === 0 ? (
          <div className="p-12 text-center">
            <FolderOpen className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500 font-medium">No projects found</p>
            <p className="text-gray-400 text-sm mt-1">Try adjusting your search or filter</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Project</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider hidden sm:table-cell">Date</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Score</th>
                  <th className="text-left px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map((project, i) => (
                  <motion.tr
                    key={project.id}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: i * 0.05 }}
                    className="hover:bg-gray-50/50 transition"
                  >
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-lg bg-indigo-50 flex items-center justify-center shrink-0">
                          <FileText className="w-4 h-4 text-indigo-600" />
                        </div>
                        <span className="font-medium text-navy-900 text-sm truncate max-w-[200px]">{project.title}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500 hidden sm:table-cell">
                      {new Date(project.submittedAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4">
                      <span className={statusConfig[project.status]?.class || 'badge-submitted'}>
                        {statusConfig[project.status]?.label || project.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-sm font-semibold text-navy-900">
                        {project.facultyEvaluation?.finalScore || project.aiEvaluation?.overallScore || '-'}/100
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <Link
                        to={`/student/projects/${project.id}`}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-600 text-xs font-semibold hover:bg-indigo-100 transition"
                      >
                        <Eye className="w-3.5 h-3.5" /> View
                      </Link>
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </DashboardLayout>
  )
}
