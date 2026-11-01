import { useEffect, useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { 
  LayoutDashboard, FolderOpen, Brain, ClipboardCheck, BarChart3, 
  Bot, FileText, ArrowLeft, CheckCircle, AlertCircle, Lightbulb, 
  Shield, Sparkles, Info, Save, BookOpen, Layers 
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../contexts/ToastContext'
import { fetchProjectById, submitFacultyReview, approveProjectEvaluation } from '../../services/api'

const sidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/faculty/dashboard' },
  { icon: FolderOpen, label: 'Projects Queue', path: '/faculty/dashboard' },
  { icon: Brain, label: 'AI Evaluations', path: '/faculty/dashboard' },
  { icon: ClipboardCheck, label: 'Final Evaluations', path: '/faculty/dashboard' },
  { icon: BarChart3, label: 'Analytics', path: '/faculty/dashboard' },
]

export default function FacultyProjectReview() {
  const { id } = useParams()
  const { user } = useAuth()
  const { addToast } = useToast()
  const navigate = useNavigate()
  
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [finalScore, setFinalScore] = useState('')
  const [comment, setComment] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    async function loadData() {
      let p = null
      try { p = await fetchProjectById(id) } catch { p = null }
      setProject(p)
      if (p) {
        const aiScore = p.ai_evaluation?.overall_score || p.aiEvaluation?.overallScore
        const facScore = p.faculty_evaluation?.final_score || p.facultyEvaluation?.finalScore
        setFinalScore(facScore ? facScore.toString() : (aiScore ? aiScore.toString() : ''))
        setComment(p.faculty_evaluation?.comment || p.facultyEvaluation?.comment || '')
      }
      setLoading(false)
    }
    loadData()
  }, [id])

  const handleSave = async () => {
    if (!finalScore) {
      addToast('Please enter a faculty score', 'error')
      return
    }
    const scoreNum = parseInt(finalScore)
    if (isNaN(scoreNum) || scoreNum < 0 || scoreNum > 100) {
      addToast('Score must be a number between 0 and 100', 'error')
      return
    }

    setSaving(true)
    const reviewPayload = {
      final_score: scoreNum,
      comment: comment,
      faculty_name: user?.name
    }

    try { const result = await submitFacultyReview(id, reviewPayload); setProject(result.project) }
    catch (error) { setSaving(false); addToast(error.message, 'error'); return }
    setSaving(false)
    addToast('Evaluation draft saved successfully', 'success')
  }

  const handleApprove = async () => {
    if (!finalScore) {
      addToast('Please enter a faculty score before approving', 'error')
      return
    }
    const scoreNum = parseInt(finalScore)
    if (isNaN(scoreNum) || scoreNum < 0 || scoreNum > 100) {
      addToast('Score must be a number between 0 and 100', 'error')
      return
    }

    setSaving(true)
    const approvalPayload = {
      final_score: scoreNum,
      comment: comment,
      faculty_name: user?.name
    }

    try { const result = await approveProjectEvaluation(id, approvalPayload); setProject(result.project) }
    catch (error) { setSaving(false); addToast(error.message, 'error'); return }
    setSaving(false)
    addToast('Project evaluation officially approved!', 'success')
  }

  if (loading) {
    return (
      <DashboardLayout sidebarItems={sidebarItems}>
        <div className="flex items-center justify-center py-20">
          <div className="w-8 h-8 border-4 border-blue-200 border-t-blue-600 rounded-full animate-spin" />
        </div>
      </DashboardLayout>
    )
  }

  if (!project) {
    return (
      <DashboardLayout sidebarItems={sidebarItems}>
        <div className="text-center py-20">
          <FileText className="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-navy-900 mb-2">Project Not Found</h2>
          <Link to="/faculty/dashboard" className="btn-primary inline-flex items-center gap-2 text-sm">
            <ArrowLeft className="w-4 h-4" /> Back to Faculty Dashboard
          </Link>
        </div>
      </DashboardLayout>
    )
  }

  const ai = project.ai_evaluation || project.aiEvaluation || {}
  const faculty = project.faculty_evaluation || project.facultyEvaluation
  const isApproved = project.status === 'approved' || faculty?.is_approved
  const stats = project.document_stats || {}
  const sections = project.detected_sections || []
  const criteriaList = ai.criterion_scores || []

  return (
    <DashboardLayout sidebarItems={sidebarItems}>
      <Link to="/faculty/dashboard" className="inline-flex items-center gap-2 text-xs font-semibold text-gray-500 hover:text-navy-900 mb-6 transition">
        <ArrowLeft className="w-4 h-4" /> Back to Faculty Dashboard
      </Link>

      {/* Project Header */}
      <div className="card p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <h1 className="text-2xl font-extrabold text-navy-900">{project.title}</h1>
              <span className={`badge ${isApproved ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'}`}>
                {isApproved ? 'Status: Approved' : 'Status: Pending Faculty Verification'}
              </span>
            </div>
            <p className="text-xs text-gray-500">
              Student: <strong>{project.student_name || project.studentName || '—'}</strong> • Department: <strong>{project.department || '—'}</strong> • Academic Year: <strong>{project.academic_year || project.academicYear || '—'}</strong>
            </p>
          </div>
        </div>
        <p className="text-gray-600 text-xs sm:text-sm mt-3 leading-relaxed">{project.description}</p>
      </div>

      {/* Document Stats & PyMuPDF Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6">
        <div className="card p-5 lg:col-span-4">
          <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-3 flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-blue-600" />
            <span>Extracted Document Stats</span>
          </h3>
          <div className="grid grid-cols-2 gap-2.5 text-left">
            <div className="bg-gray-50 p-2.5 rounded-lg border border-gray-100">
              <span className="text-[10px] text-gray-500 block">Pages</span>
              <span className="text-sm font-extrabold text-navy-900">{stats.page_count ?? '—'}</span>
            </div>
            <div className="bg-gray-50 p-2.5 rounded-lg border border-gray-100">
              <span className="text-[10px] text-gray-500 block">Words</span>
              <span className="text-sm font-extrabold text-navy-900">{stats.word_count?.toLocaleString() ?? '—'}</span>
            </div>
            <div className="bg-gray-50 p-2.5 rounded-lg border border-gray-100">
              <span className="text-[10px] text-gray-500 block">Chars</span>
              <span className="text-sm font-extrabold text-navy-900">{stats.char_count?.toLocaleString() ?? '—'}</span>
            </div>
            <div className="bg-gray-50 p-2.5 rounded-lg border border-gray-100">
              <span className="text-[10px] text-gray-500 block">Est. Reading</span>
              <span className="text-sm font-extrabold text-navy-900">{stats.reading_time_min ? `~${stats.reading_time_min} mins` : '—'}</span>
            </div>
          </div>
        </div>

        <div className="card p-5 lg:col-span-8 flex flex-col justify-between">
          <div>
            <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-3 flex items-center gap-2">
              <Layers className="w-4 h-4 text-blue-600" />
              <span>Detected Academic Sections</span>
            </h3>
            <div className="flex flex-wrap gap-2">
              {sections.map((sec, idx) => (
                <span key={idx} className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-50/80 border border-blue-200 text-blue-900 text-xs font-semibold rounded-lg">
                  <CheckCircle className="w-3.5 h-3.5 text-blue-600" />
                  <span>{sec}</span>
                </span>
              ))}
            </div>
          </div>
          <div className="mt-4 pt-3 border-t border-gray-100 flex items-center justify-between text-xs text-gray-500">
            <span>Report Filename: <strong>{project.filename || '—'}</strong></span>
            <span className="text-blue-700 font-semibold">Similarity Scan: {ai.similarity_score ?? ai.similarity ?? '—'}{ai.similarity_score != null || ai.similarity != null ? '%' : ''}</span>
          </div>
        </div>
      </div>

      {/* AI Rubric Recommendation vs Faculty Review Workbench */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6">
        
        {/* Left Side: AI Rubric Breakdown */}
        <div className="lg:col-span-7 space-y-4">
          <div className="card p-5 bg-blue-50/30 border-blue-100">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Sparkles className="w-5 h-5 text-blue-600" />
                <h3 className="text-sm font-bold text-navy-900">AI Recommended Scores</h3>
              </div>
              <div className="bg-blue-600 text-white text-xs font-extrabold px-3 py-1 rounded-full shadow-sm">
                AI Score: {ai.overall_score ?? ai.overallScore ?? '—'} / 100
              </div>
            </div>

            <div className="space-y-3">
              {criteriaList.map((crit, idx) => (
                <div key={idx} className="bg-white p-3 rounded-xl border border-blue-100">
                  <div className="flex justify-between items-center mb-1">
                    <span className="text-xs font-bold text-navy-900">{crit.criterion}</span>
                    <span className="text-xs font-bold text-blue-700">{crit.score} / {crit.maximum_score}</span>
                  </div>
                  <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden mb-2">
                    <div className="h-full bg-blue-600 rounded-full" style={{ width: `${(crit.score / crit.maximum_score) * 100}%` }} />
                  </div>
                  <p className="text-[11px] text-gray-600 leading-relaxed">
                    <strong>Reasoning:</strong> {crit.reasoning}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Side: Faculty Review & Approval Workbench */}
        <div className="lg:col-span-5">
          <div className="card p-6 sticky top-24 border-blue-200 shadow-md">
            <h2 className="text-base font-extrabold text-navy-900 mb-1 flex items-center gap-2">
              <ClipboardCheck className="w-5 h-5 text-blue-600" />
              <span>FACULTY REVIEW WORKBENCH</span>
            </h2>
            <p className="text-xs text-gray-500 mb-5">
              Review AI recommendation, adjust marks if needed, and issue official grade approval.
            </p>

            {isApproved ? (
              <div className="bg-green-50 border border-green-200 rounded-xl p-5 space-y-4">
                <div className="flex items-center gap-3">
                  <CheckCircle className="w-6 h-6 text-green-600 shrink-0" />
                  <div>
                    <h4 className="text-sm font-bold text-green-950">Official Approval Locked</h4>
                    <p className="text-xs text-green-700">Verified by {faculty?.reviewed_by || faculty?.approvedBy || user.name}</p>
                  </div>
                </div>

                <div className="bg-white p-4 rounded-xl border border-green-200 text-center shadow-sm">
                  <span className="text-xs font-bold text-gray-500 uppercase tracking-wider block">Final Approved Score</span>
                  <span className="text-3xl font-extrabold text-green-600">{faculty?.final_score || faculty?.finalScore}</span>
                  <span className="text-sm text-gray-400 font-semibold"> / 100</span>
                </div>

                <div className="bg-white p-3 rounded-lg border border-green-100 text-xs text-navy-900 leading-relaxed">
                  <span className="font-bold text-gray-700 block mb-1">Approved Remarks:</span>
                  {faculty?.comment}
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {/* AI Recommended Score Reference */}
                <div className="p-3 bg-blue-50/70 border border-blue-100 rounded-xl flex items-center justify-between">
                  <span className="text-xs font-medium text-blue-950">AI Recommended Score:</span>
                  <span className="text-sm font-extrabold text-blue-700">{ai.overall_score ?? ai.overallScore ?? '—'} / 100</span>
                </div>

                {/* Faculty Final Score Input */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Faculty Final Score (0–100) *
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="100"
                    value={finalScore}
                    onChange={e => setFinalScore(e.target.value)}
                    className="w-full text-xl font-extrabold text-navy-900 px-4 py-2.5 rounded-xl border border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition bg-white text-center"
                    placeholder="85"
                  />
                  <p className="text-[11px] text-gray-400 mt-1 text-center">Faculty has full authority to adjust marks.</p>
                </div>

                {/* Faculty Comments Textarea */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1.5">
                    Faculty Comments & Feedback *
                  </label>
                  <textarea
                    value={comment}
                    onChange={e => setComment(e.target.value)}
                    rows={4}
                    className="w-full text-xs sm:text-sm px-3.5 py-2.5 rounded-xl border border-gray-300 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 outline-none transition bg-white resize-none"
                    placeholder="Enter academic review remarks, viva evaluation notes, and final recommendation..."
                  />
                </div>

                {/* Action Buttons */}
                <div className="space-y-2 pt-2">
                  <button
                    type="button"
                    onClick={handleApprove}
                    disabled={saving}
                    className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-xl text-sm font-bold transition flex items-center justify-center gap-2 shadow-sm disabled:opacity-70"
                  >
                    {saving ? (
                      <div className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                    ) : (
                      <>
                        <CheckCircle className="w-4 h-4" />
                        <span>Approve Final Evaluation</span>
                      </>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={handleSave}
                    disabled={saving}
                    className="w-full bg-gray-100 hover:bg-gray-200 text-gray-800 py-2.5 rounded-xl text-xs font-semibold transition flex items-center justify-center gap-2"
                  >
                    <Save className="w-4 h-4 text-gray-500" />
                    <span>Save Draft Evaluation</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

      </div>
    </DashboardLayout>
  )
}
