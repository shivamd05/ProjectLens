import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { 
  LayoutDashboard, FolderOpen, Upload, ClipboardList, MessageSquare, 
  Bot, FileText, X, CheckCircle, Sparkles, FileArchive, Code2, Layers 
} from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { useToast } from '../../contexts/ToastContext'
import { checkProjectSimilarity, createProject, fetchFacultyGuides, uploadProjectSubmission } from '../../services/api'

const sidebarItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/student/dashboard' },
  { icon: FolderOpen, label: 'My Projects', path: '/student/projects' },
  { icon: Upload, label: 'Submit Project', path: '/student/submit' },
  { icon: ClipboardList, label: 'Evaluations', path: '/student/projects' },
  { icon: MessageSquare, label: 'Feedback', path: '/student/projects' },
]

export default function SubmitProject() {
  const { addToast } = useToast()
  const navigate = useNavigate()
  
  const [uploadMode, setUploadMode] = useState('pdf') // 'pdf' or 'zip'
  const [selectedFile, setSelectedFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [pipelineStep, setPipelineStep] = useState('')
  const [similarityBusy, setSimilarityBusy] = useState(false)
  const [similarityResults, setSimilarityResults] = useState(null)
  const [similarityError, setSimilarityError] = useState('')
  const [facultyGuides, setFacultyGuides] = useState([])
  const [guidesError, setGuidesError] = useState('')
  const [draftProject, setDraftProject] = useState(null)
  
  const [form, setForm] = useState({
    title: '',
    description: '',
    department: '',
    academicYear: '',
    facultyGuide: '',
  })
  
  const [errors, setErrors] = useState({})

  useEffect(() => {
    fetchFacultyGuides().then(setFacultyGuides).catch(error => setGuidesError(error.message))
  }, [])

  const handleChange = (field, value) => {
    setForm(prev => ({ ...prev, [field]: value }))
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: null }))
  }

  const handleSimilarityCheck = async () => {
    if (!form.title.trim()) return setSimilarityError('Enter a project title first.')
    setSimilarityBusy(true); setSimilarityError(''); setSimilarityResults(null)
    try { setSimilarityResults(await checkProjectSimilarity({ title: form.title, description: form.description })) }
    catch (e) { setSimilarityError(e.message) } finally { setSimilarityBusy(false) }
  }

  const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (!file) return
    if (file.size > 100 * 1024 * 1024) {
      addToast('Files must be 100 MB or smaller.', 'error')
      e.target.value = ''
      return
    }

    const name = file.name.toLowerCase()
    if (uploadMode === 'pdf' && !name.endsWith('.pdf')) {
      addToast('Please select a valid PDF file (.pdf)', 'error')
      return
    }
    if (uploadMode === 'zip' && !name.endsWith('.zip')) {
      addToast('Please select a valid ZIP archive (.zip)', 'error')
      return
    }

    setSelectedFile(file)
    if (errors.file) setErrors(prev => ({ ...prev, file: null }))
  }

  const validate = () => {
    const errs = {}
    if (!form.title.trim()) errs.title = 'Project title is required'
    if (!form.description.trim()) errs.description = 'Description is required'
    if (!form.academicYear) errs.academicYear = 'Academic year is required'
    if (!form.facultyGuide) errs.facultyGuide = 'Select an assigned faculty guide'
    if (!selectedFile) errs.file = `Please upload your project ${uploadMode.toUpperCase()} file`
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!validate()) {
      addToast('Please fill in required fields and attach your file', 'error')
      return
    }

    setLoading(true)
    try {
      let project = draftProject
      if (!project) {
        setPipelineStep('Saving project details...')
        const created = await createProject({
          title: form.title.trim(), description: form.description.trim(), department: form.department,
          academicYear: form.academicYear, facultyGuide: form.facultyGuide,
        })
        project = created.project
        setDraftProject(project)
      }
      setPipelineStep(`Uploading ${uploadMode.toUpperCase()} submission...`)
      await uploadProjectSubmission(project.id, selectedFile)
      setDraftProject(null)
      setLoading(false)
      addToast('Submission received. Faculty progress reviews must be completed before AI evaluation.', 'success')
      navigate(`/student/projects/${project.id}`)
      return
    } catch (err) {
      console.error('[Submit] Project submission failed:', err)
      setLoading(false)
      setPipelineStep('')
      addToast(`Submission failed: ${err.message}`, 'error')
      return
    }

    setLoading(false)
    setPipelineStep('')
    addToast('The backend did not return a saved project. Check that Spring Boot and MySQL are running, then try again.', 'error')
  }

  return (
    <DashboardLayout sidebarItems={sidebarItems}>
      <div className="max-w-4xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-navy-900">Submit Project for AI Evaluation</h1>
          <p className="text-gray-500 text-sm mt-1">
            Choose a <strong>PDF Report</strong> or <strong>ZIP Project Archive</strong>. AI evaluation begins after all three faculty progress reviews.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          
          {/* UPLOAD TYPE SWITCHER TABS */}
          <div className="card p-2 bg-gray-50/80 border-gray-200">
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => {
                  setUploadMode('pdf')
                  setSelectedFile(null)
                }}
                className={`py-3 px-4 rounded-xl text-xs sm:text-sm font-bold transition flex items-center justify-center gap-2 ${
                  uploadMode === 'pdf'
                    ? 'bg-white text-navy-900 shadow-sm border border-gray-200/80'
                    : 'text-gray-500 hover:text-navy-900'
                }`}
              >
                <FileText className={`w-4 h-4 ${uploadMode === 'pdf' ? 'text-red-500' : 'text-gray-400'}`} />
                <span>Upload Report PDF (.pdf)</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  setUploadMode('zip')
                  setSelectedFile(null)
                }}
                className={`py-3 px-4 rounded-xl text-xs sm:text-sm font-bold transition flex items-center justify-center gap-2 ${
                  uploadMode === 'zip'
                    ? 'bg-white text-navy-900 shadow-sm border border-gray-200/80'
                    : 'text-gray-500 hover:text-navy-900'
                }`}
              >
                <FileArchive className={`w-4 h-4 ${uploadMode === 'zip' ? 'text-purple-600' : 'text-gray-400'}`} />
                <span>Upload Project ZIP Archive (.zip)</span>
              </button>
            </div>
          </div>

          {/* Project Details */}
          <div className="card p-6">
            <h2 className="text-base font-bold text-navy-900 mb-4 flex items-center gap-2">
              <Layers className="w-5 h-5 text-blue-600" />
              <span>Project Information</span>
            </h2>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1.5">Project Title *</label>
                <input
                  type="text"
                  value={form.title}
                  onChange={e => handleChange('title', e.target.value)}
                  className={`w-full text-sm px-4 py-2.5 rounded-xl border ${errors.title ? 'border-red-400 ring-2 ring-red-100' : 'border-gray-200'} focus:border-blue-500 outline-none transition bg-gray-50/50`}
                  placeholder="e.g. ResQ: Smart Crisis Coordination Platform"
                />
                {errors.title && <p className="text-red-500 text-xs mt-1">{errors.title}</p>}
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1.5">Abstract / Overview *</label>
                <textarea
                  value={form.description}
                  onChange={e => handleChange('description', e.target.value)}
                  rows={3}
                  className={`w-full text-sm px-4 py-2.5 rounded-xl border ${errors.description ? 'border-red-400 ring-2 ring-red-100' : 'border-gray-200'} focus:border-blue-500 outline-none transition bg-gray-50/50 resize-none`}
                  placeholder="Executive description of the project"
                />
                {errors.description && <p className="text-red-500 text-xs mt-1">{errors.description}</p>}
                <div className="mt-3 flex flex-wrap items-center gap-3">
                  <button type="button" disabled={similarityBusy} onClick={handleSimilarityCheck} className="rounded-lg border border-indigo-200 bg-indigo-50 px-3 py-2 text-sm font-semibold text-indigo-700 disabled:opacity-50">{similarityBusy ? 'Checking…' : 'Check similar projects'}</button>
                  {similarityError && <span role="alert" className="text-sm text-red-600">{similarityError}</span>}
                </div>
                {similarityResults && <div className="mt-3 rounded-lg bg-indigo-50 p-3 text-sm">
                  <p className="font-semibold text-indigo-900">Similarity check complete · {similarityResults.model}</p>
                  {similarityResults.results?.length ? <ul className="mt-2 space-y-1">{similarityResults.results.map(result => <li key={result.projectId}>{result.title} · {(result.similarity * 100).toFixed(1)}%</li>)}</ul> : <p className="mt-1 text-indigo-800">No matching projects were found at the configured threshold.</p>}
                </div>}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1.5">Department</label>
                  <select
                    value={form.department}
                    onChange={e => handleChange('department', e.target.value)}
                    className="w-full text-sm px-3 py-2.5 rounded-xl border border-gray-200 bg-gray-50/50 outline-none focus:border-blue-500"
                  >
                    <option>Computer Engineering</option>
                    <option>Information Technology</option>
                    <option>Electronics & Comm.</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1.5">Academic Year</label>
                  <select
                    required
                    value={form.academicYear}
                    onChange={e => handleChange('academicYear', e.target.value)}
                    className="w-full text-sm px-3 py-2.5 rounded-xl border border-gray-200 bg-gray-50/50 outline-none focus:border-blue-500"
                  >
                    <option value="">Select academic year</option>
                    {['2025-26', '2026-27', '2027-28'].map(year => <option key={year}>{year}</option>)}
                    <option>2023-24</option>
                  </select>
                  {errors.academicYear && <p className="text-red-500 text-xs mt-1">{errors.academicYear}</p>}
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-700 uppercase tracking-wider mb-1.5">Faculty Guide *</label>
                  <select
                    value={form.facultyGuide}
                    onChange={e => handleChange('facultyGuide', e.target.value)}
                    className={`w-full text-sm px-3 py-2.5 rounded-xl border ${errors.facultyGuide ? 'border-red-400' : 'border-gray-200'} bg-gray-50/50 outline-none focus:border-blue-500`}
                  >
                    <option value="">Select a faculty guide</option>
                    {facultyGuides.map(guide => <option key={guide.id} value={guide.id}>{guide.name}{guide.department ? ` · ${guide.department}` : ''}</option>)}
                  </select>
                  {errors.facultyGuide && <p className="text-red-500 text-xs mt-1">{errors.facultyGuide}</p>}
                  {guidesError && <p role="alert" className="text-red-600 text-xs mt-1">Could not load faculty guides: {guidesError}</p>}
                  {!guidesError && facultyGuides.length === 0 && <p className="text-amber-700 text-xs mt-1">No faculty accounts are available yet. Ask an administrator to provision a faculty guide.</p>}
                </div>
              </div>
            </div>
          </div>

          {/* DYNAMIC UPLOAD CARD (PDF OR ZIP) */}
          <div className="card p-6">
            <h2 className="text-base font-bold text-navy-900 mb-1 flex items-center gap-2">
              {uploadMode === 'pdf' ? (
                <>
                  <FileText className="w-5 h-5 text-red-500" />
                  <span>Attach Project Report PDF</span>
                </>
              ) : (
                <>
                  <FileArchive className="w-5 h-5 text-purple-600" />
                  <span>Attach Project Code & Report ZIP Archive</span>
                </>
              )}
            </h2>
            <p className="text-xs text-gray-500 mb-4">
              {uploadMode === 'pdf'
                ? 'Your report will be stored securely and evaluated after the three faculty progress reviews.'
                : 'Your archive will be stored securely and inspected after the three faculty progress reviews.'}
            </p>

            {selectedFile ? (
              <div className="flex items-center justify-between p-4 bg-blue-50 border border-blue-200 rounded-xl">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center font-bold text-xs ${
                    uploadMode === 'zip' ? 'bg-purple-100 text-purple-700' : 'bg-red-100 text-red-600'
                  }`}>
                    {uploadMode === 'zip' ? 'ZIP' : 'PDF'}
                  </div>
                  <div>
                    <p className="text-sm font-bold text-blue-950 truncate max-w-[280px] sm:max-w-md">{selectedFile.name}</p>
                    <p className="text-xs text-blue-700">{(selectedFile.size / 1024).toFixed(1)} KB · Ready for analysis</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setSelectedFile(null)}
                  className="text-blue-600 hover:text-red-500 p-1 rounded-lg transition"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            ) : (
              <label className={`flex flex-col items-center justify-center p-8 border-2 border-dashed rounded-2xl cursor-pointer transition hover:border-blue-400 hover:bg-blue-50/30 ${
                errors.file ? 'border-red-400 bg-red-50/20' : 'border-gray-300'
              }`}>
                {uploadMode === 'zip' ? (
                  <FileArchive className="w-10 h-10 text-purple-600 mb-2" />
                ) : (
                  <Upload className="w-10 h-10 text-blue-500 mb-2" />
                )}
                <p className="text-sm font-bold text-navy-900">
                  Click to select {uploadMode === 'zip' ? 'ZIP Archive (.zip)' : 'PDF Report (.pdf)'}
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  {uploadMode === 'zip' 
                    ? 'Pack your source code directory and report PDF into a single .zip' 
                    : 'Upload academic report, capstone draft, or IEEE conference paper'}
                </p>
                <input
                  type="file"
                  accept={uploadMode === 'zip' ? '.zip' : '.pdf'}
                  onChange={handleFileChange}
                  className="hidden"
                />
              </label>
            )}
            {errors.file && <p className="text-red-500 text-xs mt-1.5">{errors.file}</p>}
          </div>

          {/* Submit Button */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
            <div className="text-xs text-gray-500">
              💡 <strong>AI Principle:</strong> Produces structured recommendations; faculty makes final decision.
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full sm:w-auto bg-[#12161f] text-white px-8 py-3 rounded-xl text-sm font-semibold hover:bg-black transition flex items-center justify-center gap-2 shadow-sm disabled:opacity-70"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" />
                  <span>{pipelineStep || 'Processing...'}</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-blue-400" />
                  <span>Execute {uploadMode.toUpperCase()} AI Evaluation</span>
                </>
              )}
            </button>
          </div>

        </form>
      </div>
    </DashboardLayout>
  )
}
