import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CheckCircle2, ClipboardCheck, FileText, LoaderCircle, Sparkles, XCircle } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { useAuth } from '../../contexts/AuthContext'
import { useToast } from '../../contexts/ToastContext'
import {
  approveAiEvaluation, fetchProjectById, fetchProjectEvaluation, fetchProjectReviews,
  fetchProjectSubmissions, rejectAiEvaluation, requestAiEvaluation, saveProjectReview,
} from '../../services/api'

const sidebarItems = [
  { icon: ClipboardCheck, label: 'Review Workbench', path: '/faculty/dashboard' },
]

export default function ProjectWorkflow() {
  const { id } = useParams()
  const { user } = useAuth()
  const { addToast } = useToast()
  const [project, setProject] = useState(null)
  const [reviews, setReviews] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [evaluation, setEvaluation] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [decisionFeedback, setDecisionFeedback] = useState('')

  const refresh = useCallback(async () => {
    const [projectResult, reviewResult, submissionResult, evaluationResult] = await Promise.allSettled([
      fetchProjectById(id), fetchProjectReviews(id), fetchProjectSubmissions(id), fetchProjectEvaluation(id),
    ])
    if (projectResult.status === 'rejected') throw projectResult.reason
    setProject(projectResult.value)
    setReviews(reviewResult.status === 'fulfilled' ? reviewResult.value : [])
    setSubmissions(submissionResult.status === 'fulfilled' ? submissionResult.value : [])
    setEvaluation(evaluationResult.status === 'fulfilled' ? evaluationResult.value : null)
  }, [id])

  useEffect(() => {
    refresh().catch(error => addToast(error.message, 'error')).finally(() => setLoading(false))
  }, [refresh, addToast])

  const saveReview = async (stage) => {
    const marks = stage.inputMarks
    if (marks === '' || !Number.isFinite(Number(marks)) || Number(marks) < 0 || Number(marks) > Number(stage.maxMarks)) {
      addToast(`Enter marks from 0 to ${stage.maxMarks}.`, 'error')
      return
    }
    setBusy(`review-${stage.id}`)
    try {
      await saveProjectReview(id, {
        reviewId: stage.id, marksObtained: Number(marks), feedback: stage.inputFeedback || '',
      }, stage.recordId)
      await refresh()
      addToast(`${stage.name} saved.`, 'success')
    } catch (error) { addToast(error.message, 'error') }
    finally { setBusy('') }
  }

  const runEvaluation = async () => {
    setBusy('evaluation')
    try {
      setEvaluation(await requestAiEvaluation(id))
      addToast('AI rubric recommendation is ready for faculty approval.', 'success')
    } catch (error) { addToast(error.message, 'error'); await refresh().catch(() => {}) }
    finally { setBusy('') }
  }

  const decide = async (approve) => {
    if (!evaluation?.evaluationId) return
    setBusy(approve ? 'approve' : 'reject')
    try {
      setEvaluation(approve
        ? await approveAiEvaluation(evaluation.evaluationId, decisionFeedback)
        : await rejectAiEvaluation(evaluation.evaluationId, decisionFeedback))
      addToast(approve ? 'AI recommendation approved.' : 'AI recommendation rejected.', 'success')
    } catch (error) { addToast(error.message, 'error') }
    finally { setBusy('') }
  }

  const dashboardPath = user?.role === 'admin' ? '/admin/dashboard' : '/faculty/dashboard'
  const workflowSidebar = [{ icon: ClipboardCheck, label: user?.role === 'admin' ? 'Admin Dashboard' : 'Review Workbench', path: dashboardPath }]
  if (loading) return <DashboardLayout sidebarItems={workflowSidebar}><div className="py-20 text-center text-gray-500">Loading project workflow…</div></DashboardLayout>
  if (!project) return <DashboardLayout sidebarItems={workflowSidebar}><div className="py-20 text-center"><p className="mb-4">Project could not be loaded.</p><Link className="text-blue-600" to={dashboardPath}>Back to projects</Link></div></DashboardLayout>

  const evaluationForLatestSubmission = Boolean(evaluation && submissions[0] && evaluation.submissionId === submissions[0].id)
  const canEvaluate = reviews.length > 0 && reviews.every(item => item.completed) && submissions.length > 0
  const currentEvaluationCanBeDecided = evaluationForLatestSubmission && evaluation?.status === 'COMPLETED'

  return (
    <DashboardLayout sidebarItems={workflowSidebar}>
      <Link to={dashboardPath} className="mb-5 inline-flex items-center gap-2 text-sm text-gray-500 hover:text-navy-900"><ArrowLeft className="h-4 w-4" />Back to projects</Link>
      <section className="card mb-6 p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><p className="text-xs font-semibold uppercase tracking-wide text-indigo-600">Faculty review workflow</p><h1 className="mt-1 text-2xl font-bold text-navy-900">{project.title}</h1></div>
          <span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">{project.academic_year || project.academicYear || 'Academic year not set'}</span>
        </div>
        <p className="mt-3 text-sm text-gray-600">Student: {project.student_name || project.studentName || '—'} · Guide: {project.faculty_guide || project.facultyGuide || '—'}</p>
        {project.description && <p className="mt-3 text-sm leading-6 text-gray-600">{project.description}</p>}
      </section>

      <section className="card mb-6 p-6">
        <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-navy-900"><FileText className="h-5 w-5 text-indigo-600" />Project submissions</h2>
        {submissions.length === 0 ? <p className="text-sm text-gray-500">The student has not uploaded a final PDF or ZIP submission.</p> : <div className="space-y-2">
          {submissions.map(file => <div key={file.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl bg-gray-50 p-3 text-sm"><span className="font-medium text-gray-800">{file.fileName} <span className="text-gray-500">· {file.submissionType}</span></span><span className="text-xs text-gray-500">{file.status} · {file.submissionDate ? new Date(file.submissionDate).toLocaleString() : ''}</span></div>)}
        </div>}
      </section>

      <section className="card mb-6 p-6">
        <div className="mb-4"><h2 className="flex items-center gap-2 text-lg font-semibold text-navy-900"><ClipboardCheck className="h-5 w-5 text-indigo-600" />Progress reviews</h2><p className="mt-1 text-xs text-gray-500">These stage marks stay separate from the final AI rubric score.</p></div>
        <div className="space-y-4">
          {reviews.map(stage => <ReviewCard key={stage.id} stage={stage} busy={busy === `review-${stage.id}`} onSave={saveReview} />)}
        </div>
      </section>

      <section className="card p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div><h2 className="flex items-center gap-2 text-lg font-semibold text-navy-900"><Sparkles className="h-5 w-5 text-violet-600" />Final AI evaluation</h2><p className="mt-1 text-sm text-gray-500">Requires a finalized project title, all three saved reviews, and an uploaded submission.</p></div>
          <button type="button" onClick={runEvaluation} disabled={!canEvaluate || busy !== '' || (evaluationForLatestSubmission && ['COMPLETED', 'APPROVED'].includes(evaluation?.status))} className="rounded-xl bg-indigo-600 px-4 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40">
            {busy === 'evaluation' ? 'Evaluating…' : !evaluationForLatestSubmission && evaluation ? 'Evaluate latest submission' : evaluation?.status === 'REJECTED' ? 'Run evaluation again' : 'Generate AI recommendation'}
          </button>
        </div>
        {evaluation && !evaluationForLatestSubmission && <p className="mt-4 text-sm text-amber-700">The displayed recommendation belongs to an earlier submission. Generate a new one for the latest file.</p>}
        {evaluation ? <EvaluationCard evaluation={evaluation} feedback={decisionFeedback} setFeedback={setDecisionFeedback} onDecision={decide} busy={busy} canDecide={currentEvaluationCanBeDecided} /> : <p className="mt-4 text-sm text-gray-500">No AI recommendation has been generated.</p>}
      </section>
    </DashboardLayout>
  )
}

function ReviewCard({ stage, busy, onSave }) {
  const [marks, setMarks] = useState(stage.marksObtained ?? '')
  const [feedback, setFeedback] = useState(stage.feedback || '')
  useEffect(() => { setMarks(stage.marksObtained ?? ''); setFeedback(stage.feedback || '') }, [stage.marksObtained, stage.feedback])
  return <article className="rounded-xl border border-gray-200 p-4">
    <div className="flex flex-wrap items-start justify-between gap-2"><div><h3 className="font-semibold text-navy-900">{stage.name}</h3><p className="mt-0.5 text-xs text-gray-500">Maximum {stage.maxMarks} marks {stage.reviewDate ? `· Saved ${new Date(stage.reviewDate).toLocaleDateString()}` : ''}</p></div><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${stage.completed ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>{stage.completed ? 'Complete' : 'Pending'}</span></div>
    <div className="mt-4 grid gap-3 sm:grid-cols-[150px_1fr]"><label className="text-xs font-semibold text-gray-600">Marks<input type="number" min="0" max={stage.maxMarks} step="0.01" value={marks} onChange={event => setMarks(event.target.value)} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm" /></label><label className="text-xs font-semibold text-gray-600">Faculty feedback<textarea rows="2" value={feedback} onChange={event => setFeedback(event.target.value)} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm" placeholder="Progress, risks, and next steps" /></label></div>
    <div className="mt-3 flex flex-wrap items-center justify-between gap-2"><span className="text-xs text-gray-500">{stage.facultyName ? `Entered by ${stage.facultyName}` : 'Not yet recorded'}</span><button type="button" disabled={busy} onClick={() => onSave({ ...stage, inputMarks: marks, inputFeedback: feedback })} className="rounded-lg border border-indigo-200 px-3 py-2 text-xs font-semibold text-indigo-700 disabled:opacity-50">{busy ? 'Saving…' : stage.completed ? 'Update review' : 'Save review'}</button></div>
  </article>
}

function EvaluationCard({ evaluation, feedback, setFeedback, onDecision, busy, canDecide }) {
  return <div className="mt-5 border-t border-gray-100 pt-5">
    <div className="flex flex-wrap items-center gap-3"><span className="text-2xl font-bold text-navy-900">{evaluation.totalMarks} / {evaluation.maximumMarks}</span><span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-700">{evaluation.status}</span><span className="text-xs text-gray-500">{evaluation.rubricName} · {evaluation.submissionType}</span></div>
    {evaluation.overallFeedback && <p className="mt-3 rounded-xl bg-indigo-50 p-4 text-sm leading-6 text-indigo-950">{evaluation.overallFeedback}</p>}
    <div className="mt-4 grid gap-3 md:grid-cols-2">{(evaluation.details || []).map(item => <article key={item.criterionId} className="rounded-xl border border-gray-100 p-4"><div className="flex justify-between gap-3"><h3 className="text-sm font-semibold text-navy-900">{item.criterionName}</h3><span className="whitespace-nowrap text-sm font-bold text-indigo-700">{item.awardedMarks} / {item.maximumMarks}</span></div><p className="mt-2 text-xs leading-5 text-gray-600">{item.justification}</p>{item.improvementSuggestions && <p className="mt-2 text-xs leading-5 text-gray-500"><strong>Next step:</strong> {item.improvementSuggestions}</p>}</article>)}</div>
    {evaluation.decisionFeedback && <p className="mt-4 text-sm text-gray-600">Faculty decision note: {evaluation.decisionFeedback}</p>}
    {canDecide && <div className="mt-5"><label className="block text-xs font-semibold text-gray-600">Faculty decision feedback<textarea value={feedback} onChange={event => setFeedback(event.target.value)} rows="2" className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm" placeholder="Optional approval or rejection note" /></label><div className="mt-3 flex flex-wrap gap-2"><button type="button" disabled={busy !== ''} onClick={() => onDecision(true)} className="inline-flex items-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"><CheckCircle2 className="h-4 w-4" />{busy === 'approve' ? 'Approving…' : 'Approve recommendation'}</button><button type="button" disabled={busy !== ''} onClick={() => onDecision(false)} className="inline-flex items-center gap-2 rounded-lg border border-red-200 px-4 py-2 text-sm font-semibold text-red-700 disabled:opacity-50"><XCircle className="h-4 w-4" />{busy === 'reject' ? 'Rejecting…' : 'Reject recommendation'}</button></div></div>}
  </div>
}
