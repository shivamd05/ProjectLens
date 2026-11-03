const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8000'
const TOKEN_KEY = 'projectlense_token'

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  if (response.status === 204) return null
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    const detail = Array.isArray(body.detail) ? body.detail.join(', ') : body.detail
    throw new Error(detail || body.message || `Request failed (${response.status})`)
  }
  return body
}
const json = (value) => ({ headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(value) })

export const apiLogin = (email, password) => request('/api/auth/login', { method: 'POST', ...json({ email, password }) })
export const apiAdminLogin = (token) => request('/api/auth/admin-login', { method: 'POST', ...json({ token }) })
export const apiRegister = (name, email, password, department) => request('/api/auth/register', { method: 'POST', ...json({ name, email, password, department }) })
export const fetchCurrentUser = () => request('/api/auth/me')
export const revokeSession = () => request('/api/auth/logout', { method: 'POST' })

function normalizeProject(project) {
  if (!project) return project
  const ai = project.ai_evaluation || project.aiEvaluation
  const faculty = project.faculty_evaluation || project.facultyEvaluation
  const statusMap = { pending_reviews: 'submitted', under_review: 'under-review', evaluated: 'evaluated', approved: 'approved' }
  return { ...project, submittedAt: project.submitted_at || project.submittedAt,
    studentName: project.student_name || project.studentName,
    academicYear: project.academic_year || project.academicYear,
    facultyGuide: project.faculty_guide || project.facultyGuide,
    aiEvaluation: ai ? { ...ai, overallScore: ai.overall_score ?? ai.overallScore,
      criterionScores: ai.criterion_scores || ai.criterionScores } : undefined,
    facultyEvaluation: faculty ? { ...faculty, finalScore: faculty.final_score ?? faculty.finalScore } : undefined,
    status: statusMap[project.status] || project.status }
}
export async function uploadProjectReport(formData) { return request('/api/projects/upload', { method: 'POST', body: formData }) }
export async function fetchProjects() { return (await request('/api/projects')).map(normalizeProject) }
export async function fetchProjectById(id) { return normalizeProject(await request(`/api/projects/${encodeURIComponent(id)}`)) }
export async function submitFacultyReview(id, reviewData) { return request(`/api/projects/${encodeURIComponent(id)}/faculty-review`, { method: 'POST', ...json(reviewData) }) }
export async function approveProjectEvaluation(id, approvalData) { return request(`/api/projects/${encodeURIComponent(id)}/approve`, { method: 'POST', ...json(approvalData) }) }

export const fetchFacultyGuides = () => request('/api/faculty-guides')
export const createProject = (project) => request('/api/projects', { method: 'POST', ...json(project) })
export const uploadProjectSubmission = (projectId, file) => {
  const data = new FormData(); data.append('file', file)
  return request(`/api/projects/${encodeURIComponent(projectId)}/submissions`, { method: 'POST', body: data })
}
export const fetchProjectSubmissions = (projectId) => request(`/api/projects/${encodeURIComponent(projectId)}/submissions`)
export const fetchProjectReviews = (projectId) => request(`/api/projects/${encodeURIComponent(projectId)}/reviews`)
export const saveProjectReview = (projectId, review, recordId) => request(
  `/api/projects/${encodeURIComponent(projectId)}/reviews${recordId ? `/${recordId}` : ''}`,
  { method: recordId ? 'PUT' : 'POST', ...json(review) },
)
export const requestAiEvaluation = (projectId) => request(`/api/projects/${encodeURIComponent(projectId)}/evaluate`, { method: 'POST' })
export const fetchProjectEvaluation = (projectId) => request(`/api/projects/${encodeURIComponent(projectId)}/evaluation`)
export const approveAiEvaluation = (evaluationId, feedback = '') => request(`/api/evaluations/${evaluationId}/approve`, { method: 'POST', ...json({ feedback }) })
export const rejectAiEvaluation = (evaluationId, feedback = '') => request(`/api/evaluations/${evaluationId}/reject`, { method: 'POST', ...json({ feedback }) })

export const fetchRubrics = () => request('/api/rubrics')
export const saveRubric = (rubric, id) => request(id ? `/api/rubrics/${id}` : '/api/rubrics', {
  method: id ? 'PUT' : 'POST', ...json(rubric),
})
export const fetchReviewStages = () => request('/api/review-stages')
export const updateReviewStage = (id, maxMarks) => request(`/api/admin/review-stages/${id}`, {
  method: 'PUT', ...json({ maxMarks: Number(maxMarks) }),
})

async function adminRequest(path, token, options = {}) {
  const headers = new Headers(options.headers || {})
  headers.set('X-Admin-Token', token)
  return request(path, { ...options, headers })
}
export async function createExcelImport(file, adminToken) {
  const data = new FormData(); data.append('file', file)
  return adminRequest('/api/v1/admin/excel-imports', adminToken, { method: 'POST', body: data })
}
export const fetchExcelImportPreview = (jobId, token) => adminRequest(`/api/v1/admin/excel-imports/${jobId}/preview`, token)
export const approveExcelImport = (jobId, token, corrections = {}) => adminRequest(`/api/v1/admin/excel-imports/${jobId}/approve`, token, { method: 'POST', ...json({ corrections }) })
export const cancelExcelImport = (jobId, token) => adminRequest(`/api/v1/admin/excel-imports/${jobId}/cancel`, token, { method: 'POST' })
export const createFacultyAccount = (faculty, token) => adminRequest('/api/v1/admin/users/faculty', token, { method: 'POST', ...json(faculty) })
export const checkProjectSimilarity = (project) => request('/api/v1/projects/similarity-checks', { method: 'POST', ...json(project) })
