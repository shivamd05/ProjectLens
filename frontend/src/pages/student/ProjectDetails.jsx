import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { 
  LayoutDashboard, FolderOpen, Upload, ClipboardList, MessageSquare, 
  Bot, FileText, ArrowLeft, CheckCircle, AlertCircle, Lightbulb, 
  Shield, Sparkles, Info, BookOpen, Layers, Clock, FileArchive, Code2 
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { fetchProjectById } from '../../services/api'

const sidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/student/dashboard' },
  { icon: FolderOpen, label: 'My Projects', path: '/student/projects' },
  { icon: Upload, label: 'Submit Project', path: '/student/submit' },
  { icon: ClipboardList, label: 'Evaluations', path: '/student/projects' },
  { icon: MessageSquare, label: 'Feedback', path: '/student/projects' },
]

export default function ProjectDetails() {
  const { id } = useParams()
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function loadData() {
      try { const remote = await fetchProjectById(id); setProject(remote) } catch { setProject(null) } finally { setLoading(false) }
    }
    loadData()
  }, [id])

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
          <Link to="/student/projects" className="btn-primary inline-flex items-center gap-2 text-sm">
            <ArrowLeft className="w-4 h-4" /> Back to My Projects
          </Link>
        </div>
      </DashboardLayout>
    )
  }

  const ai = project.ai_evaluation || project.aiEvaluation || {}
  const faculty = project.faculty_evaluation || project.facultyEvaluation
  const stats = project.document_stats || {}
  const sections = project.detected_sections || []
  const criteriaList = ai.criterion_scores || []
  const codebase = project.codebase_metrics || ai.codebase_metrics

  return (
    <DashboardLayout sidebarItems={sidebarItems}>
      <Link to="/student/projects" className="inline-flex items-center gap-2 text-xs font-semibold text-gray-500 hover:text-navy-900 mb-6 transition">
        <ArrowLeft className="w-4 h-4" /> Back to My Projects
      </Link>

      {/* Project Header Card */}
      <div className="card p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-3 mb-2 flex-wrap">
              <h1 className="text-2xl font-extrabold text-navy-900">{project.title}</h1>
              <span className={`badge ${project.status === 'approved' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'}`}>
                {project.status === 'approved' ? 'Approved by Faculty' : 'Evaluated by AI (Awaiting Review)'}
              </span>
              {project.file_type === 'zip' && (
                <span className="badge bg-purple-100 text-purple-700">ZIP Codebase Archive</span>
              )}
            </div>
            <p className="text-xs text-gray-500">
              Department: <strong>{project.department || '—'}</strong> • Academic Year: <strong>{project.academic_year || project.academicYear || '—'}</strong> • Faculty Guide: <strong>{project.faculty_guide || project.facultyGuide || '—'}</strong>
            </p>
          </div>
        </div>
        <p className="text-gray-600 text-xs sm:text-sm mt-3 leading-relaxed">{project.description}</p>
      </div>

      {/* APPROVED FACULTY OFFICIAL SCORE BANNER */}
      {faculty && (project.status === 'approved' || faculty.is_approved) && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-green-50/90 border border-green-200 rounded-2xl p-6 mb-6 shadow-sm"
        >
          <div className="flex items-center justify-between flex-wrap gap-4 mb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-green-600 text-white flex items-center justify-center font-bold">
                <CheckCircle className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-base font-bold text-green-950">Official Faculty Final Evaluation</h2>
                <p className="text-xs text-green-700">Verified & Approved by {faculty.reviewed_by || faculty.approvedBy || 'Faculty Reviewer'}</p>
              </div>
            </div>
            <div className="bg-white px-5 py-2.5 rounded-xl border border-green-200 text-center shadow-sm">
              <span className="text-[10px] uppercase font-bold text-gray-400 block">Final Official Score</span>
              <span className="text-2xl font-extrabold text-green-600">{faculty.final_score || faculty.finalScore}</span>
              <span className="text-xs text-gray-400 font-semibold"> / 100</span>
            </div>
          </div>
          {faculty.comment && (
            <div className="bg-white p-4 rounded-xl border border-green-100 text-xs text-navy-900 leading-relaxed">
              <span className="font-bold text-gray-700 block mb-1">Faculty Remarks:</span>
              {faculty.comment}
            </div>
          )}
        </motion.div>
      )}

      {/* =========================================================================
          PROJECT ANALYSIS SECTION
          ========================================================================= */}
      <div className="space-y-6">
        
        {/* Core Product Principle Banner */}
        <div className="flex items-center gap-2.5 px-4 py-3 bg-blue-50/80 border border-blue-200 rounded-xl text-xs text-blue-900 leading-relaxed">
          <Info className="w-4 h-4 text-blue-600 shrink-0" />
          <div>
            <strong>Evaluation Principle:</strong> The AI provides a structured recommendation based on document text, code repository metrics, and academic rubrics. The faculty guide makes the final decision.
          </div>
        </div>

        {/* Codebase Inspection Card (If submitted as ZIP) */}
        {codebase && (
          <div className="card p-5 border-purple-200 bg-purple-50/20">
            <h3 className="text-xs font-bold text-purple-950 uppercase tracking-wider mb-3 flex items-center gap-2">
              <Code2 className="w-4 h-4 text-purple-600" />
              <span>ZIP Codebase Inspection & Repository Metrics</span>
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
              <div className="bg-white p-3 rounded-xl border border-purple-100">
                <span className="text-[10px] text-gray-500 block">Total Files</span>
                <span className="text-base font-extrabold text-navy-900">{codebase.total_files} files</span>
              </div>
              <div className="bg-white p-3 rounded-xl border border-purple-100">
                <span className="text-[10px] text-gray-500 block">Lines of Code (LOC)</span>
                <span className="text-base font-extrabold text-purple-700">{codebase.total_loc?.toLocaleString()} LOC</span>
              </div>
              <div className="bg-white p-3 rounded-xl border border-purple-100 sm:col-span-2">
                <span className="text-[10px] text-gray-500 block">Detected Tech Stack</span>
                <div className="flex flex-wrap gap-1 mt-1">
                  {(codebase.detected_stack || []).map((tech, idx) => (
                    <span key={idx} className="bg-purple-100 text-purple-800 text-[10px] font-bold px-2 py-0.5 rounded">
                      {tech}
                    </span>
                  ))}
                </div>
              </div>
            </div>
            {codebase.found_report_pdf && (
              <div className="text-xs text-purple-900 bg-white p-2.5 rounded-lg border border-purple-100 flex items-center gap-2">
                <FileText className="w-4 h-4 text-red-500" />
                <span>Extracted embedded documentation: <strong>{codebase.found_report_pdf}</strong></span>
              </div>
            )}
          </div>
        )}

        {/* 1. Document Statistics & Detected Sections Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          <div className="card p-5 lg:col-span-4">
            <h3 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-3 flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-blue-600" />
              <span>Document Statistics</span>
            </h3>
            <div className="grid grid-cols-2 gap-3 text-left">
              <div className="bg-gray-50 p-3 rounded-xl border border-gray-100">
                <span className="text-[10px] text-gray-500 block">Pages</span>
                <span className="text-base font-extrabold text-navy-900">{stats.page_count} Pages</span>
              </div>
              <div className="bg-gray-50 p-3 rounded-xl border border-gray-100">
                <span className="text-[10px] text-gray-500 block">Words</span>
                <span className="text-base font-extrabold text-navy-900">{stats.word_count.toLocaleString()} Words</span>
              </div>
              <div className="bg-gray-50 p-3 rounded-xl border border-gray-100">
                <span className="text-[10px] text-gray-500 block">Characters</span>
                <span className="text-base font-extrabold text-navy-900">{stats.char_count.toLocaleString()} Chars</span>
              </div>
              <div className="bg-gray-50 p-3 rounded-xl border border-gray-100">
                <span className="text-[10px] text-gray-500 block">Reading Time</span>
                <span className="text-base font-extrabold text-navy-900">~{stats.reading_time_min} Mins</span>
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
              <span>File: <strong>{project.filename}</strong></span>
              <span className="text-green-700 font-semibold">✓ {sections.length} Sections Verified</span>
            </div>
          </div>
        </div>

        {/* 2. Overall AI Recommended Score */}
        <div className="card p-6 bg-gradient-to-br from-white to-blue-50/30 border-blue-100">
          <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-5">
              <div className="relative w-24 h-24 shrink-0">
                <svg className="w-24 h-24 -rotate-90" viewBox="0 0 100 100">
                  <circle cx="50" cy="50" r="42" fill="none" stroke="#e5e7eb" strokeWidth="8" />
                  <circle 
                    cx="50" cy="50" r="42" fill="none" stroke="#2563eb" strokeWidth="8" 
                    strokeDasharray={`${(ai.overall_score ?? ai.overallScore ?? 0) * 2.64} 264`} 
                    strokeLinecap="round" 
                  />
                </svg>
                <div className="absolute inset-0 flex items-center justify-center text-center">
                  <div>
                    <span className="text-2xl font-extrabold text-navy-900">{ai.overall_score ?? ai.overallScore ?? '—'}</span>
                    <span className="text-[10px] text-gray-400 block -mt-1">/ 100</span>
                  </div>
                </div>
              </div>
              <div>
                <span className="text-xs font-bold text-blue-700 bg-blue-100 px-2.5 py-0.5 rounded-full uppercase tracking-wider">
                  AI Recommended Score
                </span>
                <h3 className="text-lg font-bold text-navy-900 mt-1">Standardized Academic Rubric Synthesis</h3>
                <p className="text-xs text-gray-500 mt-0.5">
                  {ai.model_provider || '—'}
                </p>
              </div>
            </div>

            <div className="bg-white p-4 rounded-xl border border-gray-200 text-center sm:text-right">
              <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Similarity Index</span>
              <span className="text-xl font-extrabold text-navy-900">{ai.similarity_score ?? ai.similarity ?? '—'}{ai.similarity_score != null || ai.similarity != null ? '%' : ''}</span>
              {ai.similarity_score != null || ai.similarity != null ? <span className="text-[11px] text-green-600 font-semibold block mt-0.5">Similarity analysis complete</span> : null}
            </div>
          </div>
        </div>

        {/* 3. Criterion-Wise Scores & AI Reasoning */}
        <div className="card p-6">
          <h2 className="text-base font-bold text-navy-900 mb-4 flex items-center gap-2">
            <ClipboardList className="w-5 h-5 text-blue-600" />
            <span>Criterion-Wise Rubric Breakdown & AI Reasoning</span>
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {criteriaList.map((crit, idx) => (
              <div key={idx} className="bg-gray-50/70 border border-gray-100 rounded-xl p-4 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-bold text-navy-900">{crit.criterion}</span>
                    <span className="text-xs font-extrabold bg-blue-50 text-blue-700 border border-blue-200 px-2.5 py-0.5 rounded-md">
                      {crit.score} / {crit.maximum_score}
                    </span>
                  </div>
                  <div className="h-1.5 bg-gray-200 rounded-full overflow-hidden mb-3">
                    <div 
                      className="h-full bg-blue-600 rounded-full" 
                      style={{ width: `${(crit.score / crit.maximum_score) * 100}%` }} 
                    />
                  </div>
                </div>
                <p className="text-xs text-gray-600 leading-relaxed bg-white p-2.5 rounded-lg border border-gray-100">
                  <strong className="text-gray-800">Reasoning:</strong> {crit.reasoning}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* 4. Strengths, Weaknesses & Improvement Suggestions */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="card p-5 bg-green-50/20 border-green-100">
            <h3 className="text-xs font-bold text-green-900 uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <CheckCircle className="w-4 h-4 text-green-600" />
              <span>Project Strengths</span>
            </h3>
            <ul className="space-y-2">
              {(ai.strengths || []).map((s, i) => (
                <li key={i} className="text-xs text-gray-700 flex items-start gap-2 leading-relaxed">
                  <span className="w-1.5 h-1.5 rounded-full bg-green-500 mt-1.5 shrink-0" />
                  <span>{s}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="card p-5 bg-amber-50/20 border-amber-100">
            <h3 className="text-xs font-bold text-amber-900 uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <AlertCircle className="w-4 h-4 text-amber-600" />
              <span>Identified Gaps</span>
            </h3>
            <ul className="space-y-2">
              {(ai.weaknesses || []).map((w, i) => (
                <li key={i} className="text-xs text-gray-700 flex items-start gap-2 leading-relaxed">
                  <span className="w-1.5 h-1.5 rounded-full bg-amber-500 mt-1.5 shrink-0" />
                  <span>{w}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="card p-5 bg-blue-50/20 border-blue-100">
            <h3 className="text-xs font-bold text-blue-900 uppercase tracking-wider mb-3 flex items-center gap-1.5">
              <Lightbulb className="w-4 h-4 text-blue-600" />
              <span>Actionable Suggestions</span>
            </h3>
            <ul className="space-y-2">
              {(ai.suggestions || []).map((sug, i) => (
                <li key={i} className="text-xs text-gray-700 flex items-start gap-2 leading-relaxed">
                  <span className="w-1.5 h-1.5 rounded-full bg-blue-500 mt-1.5 shrink-0" />
                  <span>{sug}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

      </div>
    </DashboardLayout>
  )
}
