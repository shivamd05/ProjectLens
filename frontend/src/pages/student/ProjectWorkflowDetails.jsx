import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, ClipboardCheck, FileArchive, FileText, LoaderCircle, Sparkles } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { fetchProjectById, fetchProjectEvaluation, fetchProjectReviews, fetchProjectSubmissions, uploadProjectSubmission } from '../../services/api'

const sidebarItems = [
  { icon: ClipboardCheck, label: 'Dashboard', path: '/student/dashboard' },
  { icon: FileText, label: 'My Projects', path: '/student/projects' },
]

export default function ProjectWorkflowDetails() {
  const { id } = useParams()
  const [project, setProject] = useState(null)
  const [reviews, setReviews] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [evaluation, setEvaluation] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    let alive = true
    async function load() {
      try {
        const p = await fetchProjectById(id)
        const [reviewResult, submissionResult, evaluationResult] = await Promise.allSettled([
          fetchProjectReviews(id), fetchProjectSubmissions(id), fetchProjectEvaluation(id),
        ])
        if (!alive) return
        setProject(p)
        setReviews(reviewResult.status === 'fulfilled' ? reviewResult.value : [])
        setSubmissions(submissionResult.status === 'fulfilled' ? submissionResult.value : [])
        setEvaluation(evaluationResult.status === 'fulfilled' ? evaluationResult.value : null)
      } catch (e) { if (alive) setError(e.message) }
      finally { if (alive) setLoading(false) }
    }
    load()
    return () => { alive = false }
  }, [id])

  const title = project?.title || 'Project'
  const completedReviews = reviews.filter(review => review.completed).length

  const submitUpdatedFile = async (event) => {
    event.preventDefault()
    if (!selectedFile || !project) return
    setUploading(true)
    try {
      await uploadProjectSubmission(id, selectedFile)
      setSelectedFile(null)
      const list = await fetchProjectSubmissions(id)
      setSubmissions(list)
      try { setEvaluation(await fetchProjectEvaluation(id)) } catch { setEvaluation(null) }
      setError('')
    } catch (e) { setError(e.message) }
    finally { setUploading(false) }
  }

  return (
    <DashboardLayout sidebarItems={sidebarItems}>
      <Link to="/student/projects" className="mb-5 inline-flex items-center gap-2 text-sm text-gray-500 hover:text-navy-900"><ArrowLeft className="h-4 w-4" />Back to My Projects</Link>
      {loading ? <div className="py-20 text-center text-gray-500"><LoaderCircle className="mx-auto mb-2 h-6 w-6 animate-spin" />Loading project status…</div> : error || !project ? <div role="alert" className="card p-8 text-center text-red-700">{error || 'Project not found'}</div> : <>
        <section className="card mb-6 p-6">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-wide text-indigo-600">Project workflow</p><h1 className="mt-1 text-2xl font-bold text-navy-900">{title}</h1></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${evaluation ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-800'}`}>{evaluation ? 'Faculty approved' : 'In progress'}</span></div>
          <p className="mt-3 text-sm text-gray-600">{project.description || 'No project overview added.'}</p>
          <p className="mt-3 text-xs text-gray-500">Faculty guide: {project.faculty_guide || project.facultyGuide || '—'} · Academic year: {project.academic_year || project.academicYear || '—'}</p>
        </section>

        <section className="card mb-6 p-6">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-2"><h2 className="flex items-center gap-2 text-lg font-semibold text-navy-900"><ClipboardCheck className="h-5 w-5 text-indigo-600" />Faculty progress reviews</h2><span className="text-xs text-gray-500">{completedReviews} of {reviews.length || 3} complete</span></div>
          <div className="grid gap-3 md:grid-cols-3">
            {reviews.map(review => <article key={review.id} className="rounded-xl border border-gray-100 p-4"><div className="flex items-center justify-between gap-2"><h3 className="text-sm font-semibold text-navy-900">{review.name}</h3>{review.completed ? <CheckCircle2 className="h-4 w-4 text-emerald-600" /> : <span className="text-xs text-amber-700">Pending</span>}</div>{review.completed ? <><p className="mt-2 text-lg font-bold text-indigo-700">{review.marksObtained} / {review.maxMarks}</p><p className="mt-2 text-xs leading-5 text-gray-600">{review.feedback || 'No written feedback.'}</p><p className="mt-2 text-[11px] text-gray-400">{review.facultyName || 'Faculty'} · {review.reviewDate || ''}</p></> : <p className="mt-2 text-xs text-gray-500">Your guide will enter the score and feedback for this stage.</p>}</article>)}
          </div>
        </section>

        <section className="card mb-6 p-6">
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3"><div><h2 className="text-lg font-semibold text-navy-900">Uploaded submissions</h2><p className="mt-1 text-xs text-gray-500">The latest upload is used when faculty start final evaluation.</p></div><form onSubmit={submitUpdatedFile} className="flex flex-wrap gap-2"><input type="file" accept=".pdf,.zip,application/pdf,application/zip" onChange={event => setSelectedFile(event.target.files?.[0] || null)} className="max-w-full text-xs" /><button type="submit" disabled={!selectedFile || uploading} className="rounded-lg bg-indigo-600 px-3 py-2 text-xs font-semibold text-white disabled:opacity-40">{uploading ? 'Uploading…' : 'Upload another version'}</button></form></div>
          {error && <p role="alert" className="mb-3 text-xs text-red-600">{error}</p>}
          {submissions.length === 0 ? <p className="text-sm text-gray-500">No files submitted yet.</p> : <div className="space-y-2">{submissions.map(file => { const Icon = file.submissionType === 'ZIP' ? FileArchive : FileText; return <article key={file.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-gray-50 p-3"><div className="flex items-center gap-2 text-sm"><Icon className="h-4 w-4 text-indigo-600" /><span className="font-medium text-gray-800">{file.fileName}</span><span className="text-gray-500">· {file.submissionType}</span></div><span className="text-xs text-gray-500">{file.status} · {file.submissionDate ? new Date(file.submissionDate).toLocaleString() : ''}</span></article> })}</div>}
        </section>

        <section className="card p-6">
          <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold text-navy-900"><Sparkles className="h-5 w-5 text-violet-600" />Final AI evaluation</h2>
          {!evaluation ? <p className="text-sm leading-6 text-gray-500">The final recommendation appears here after all three faculty reviews, the submission check, and faculty approval.</p> : <>
            <div className="flex flex-wrap items-center gap-3"><span className="text-2xl font-bold text-navy-900">{evaluation.totalMarks} / {evaluation.maximumMarks}</span><span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">Approved by faculty</span><span className="text-xs text-gray-500">{evaluation.rubricName} · {evaluation.submissionType}</span></div>
            <p className="mt-3 rounded-xl bg-indigo-50 p-4 text-sm leading-6 text-indigo-950">{evaluation.overallFeedback}</p>
            <div className="mt-4 grid gap-3 md:grid-cols-2">{(evaluation.details || []).map(item => <article key={item.criterionId} className="rounded-xl border border-gray-100 p-4"><div className="flex justify-between gap-3"><h3 className="text-sm font-semibold text-navy-900">{item.criterionName}</h3><span className="whitespace-nowrap text-sm font-bold text-indigo-700">{item.awardedMarks} / {item.maximumMarks}</span></div><p className="mt-2 text-xs leading-5 text-gray-600">{item.justification}</p>{item.improvementSuggestions && <p className="mt-2 text-xs leading-5 text-gray-500"><strong>Suggestion:</strong> {item.improvementSuggestions}</p>}</article>)}</div>
            {evaluation.decisionFeedback && <p className="mt-4 text-sm text-gray-600">Faculty note: {evaluation.decisionFeedback}</p>}
          </>}
        </section>
      </>}
    </DashboardLayout>
  )
}
