import { useEffect, useMemo, useState } from 'react'
import { Award, Plus, Save, Settings } from 'lucide-react'
import DashboardLayout from '../../components/layout/DashboardLayout'
import { useToast } from '../../contexts/ToastContext'
import { fetchRubrics, fetchReviewStages, saveRubric, updateReviewStage } from '../../services/api'

const sidebarItems = [
  { icon: Settings, label: 'Dashboard', path: '/admin/dashboard' },
  { icon: Award, label: 'Evaluation Rubrics', path: '/admin/rubrics' },
]

const emptyForm = (submissionType = 'PDF') => ({
  name: '', submissionType, description: '', totalMarks: '100', active: true, criteria: [],
})

function fromRubric(rubric) {
  return {
    name: rubric.name, submissionType: rubric.submissionType, description: rubric.description,
    totalMarks: String(rubric.totalMarks), active: rubric.active,
    criteria: (rubric.criteria || []).map(item => ({ ...item, maxMarks: String(item.maxMarks), subcriteria: item.subcriteria || [] })),
  }
}

export default function Rubrics() {
  const { addToast } = useToast()
  const [rubrics, setRubrics] = useState([])
  const [stages, setStages] = useState([])
  const [stageMarks, setStageMarks] = useState({})
  const [stageBusy, setStageBusy] = useState('')
  const [typeFilter, setTypeFilter] = useState('PDF')
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const refresh = async (preferredId = null) => {
    const [items, stageItems] = await Promise.all([fetchRubrics(), fetchReviewStages()])
    setRubrics(items)
    setStages(stageItems)
    setStageMarks(Object.fromEntries(stageItems.map(stage => [stage.id, String(stage.maxMarks)])))
    const pick = items.find(item => item.id === preferredId) || items.find(item => item.submissionType === typeFilter && item.active) || items.find(item => item.submissionType === typeFilter)
    if (pick) { setEditingId(pick.id); setForm(fromRubric(pick)) }
    else { setEditingId(null); setForm(emptyForm(typeFilter)) }
  }

  useEffect(() => { refresh().catch(error => addToast(error.message, 'error')).finally(() => setLoading(false)) }, [])
  const shownRubrics = useMemo(() => rubrics.filter(item => item.submissionType === typeFilter), [rubrics, typeFilter])
  const criteriaTotal = form.criteria.reduce((sum, item) => sum + (Number(item.maxMarks) || 0), 0)

  const selectType = (type) => {
    setTypeFilter(type)
    const pick = rubrics.find(item => item.submissionType === type && item.active) || rubrics.find(item => item.submissionType === type)
    if (pick) { setEditingId(pick.id); setForm(fromRubric(pick)) }
    else { setEditingId(null); setForm(emptyForm(type)) }
  }

  const addCriterion = () => setForm(current => ({ ...current, criteria: [...current.criteria, { name: '', description: '', maxMarks: '', subcriteria: [] }] }))
  const updateCriterion = (index, key, value) => setForm(current => ({
    ...current, criteria: current.criteria.map((item, i) => i === index ? { ...item, [key]: value } : item),
  }))
  const removeCriterion = (index) => setForm(current => ({ ...current, criteria: current.criteria.filter((_, i) => i !== index) }))

  const startNewVersion = () => {
    setEditingId(null)
    setForm(current => ({ ...current, active: true }))
  }

  const startNewRubric = () => { setEditingId(null); setForm(emptyForm(typeFilter)) }

  const saveStage = async (stage) => {
    setStageBusy(String(stage.id))
    try {
      const updated = await updateReviewStage(stage.id, stageMarks[stage.id])
      setStages(current => current.map(item => item.id === updated.id ? updated : item))
      setStageMarks(current => ({ ...current, [updated.id]: String(updated.maxMarks) }))
      addToast(`${stage.name} maximum updated.`, 'success')
    } catch (error) { addToast(error.message, 'error') }
    finally { setStageBusy('') }
  }

  const persist = async (event) => {
    event.preventDefault()
    if (Math.abs(criteriaTotal - Number(form.totalMarks)) > 0.0001) {
      addToast(`Criterion maximums total ${criteriaTotal}; they must equal ${form.totalMarks}.`, 'error')
      return
    }
    setSaving(true)
    try {
      const payload = {
        ...form, totalMarks: Number(form.totalMarks),
        criteria: form.criteria.map(({ name, description, maxMarks, subcriteria = [] }) => ({
          name: name.trim(), description: description.trim(), maxMarks: Number(maxMarks), subcriteria,
        })),
      }
      const saved = await saveRubric(payload, editingId)
      await refresh(saved.id)
      addToast(`Rubric ${editingId ? 'updated' : 'version created'}.`, 'success')
    } catch (error) { addToast(error.message, 'error') }
    finally { setSaving(false) }
  }

  return <DashboardLayout sidebarItems={sidebarItems}>
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3"><div><h1 className="text-2xl font-bold text-navy-900">Evaluation rubrics</h1><p className="mt-1 text-sm text-gray-500">Manage separate PDF and ZIP scoring criteria. Used versions keep their evaluation history.</p></div><div className="flex gap-2"><button type="button" onClick={startNewVersion} disabled={!editingId} className="rounded-lg border border-indigo-200 px-3 py-2 text-sm font-semibold text-indigo-700 disabled:opacity-40">Create next version</button><button type="button" onClick={startNewRubric} className="inline-flex items-center gap-1.5 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white"><Plus className="h-4 w-4" />New rubric</button></div></div>

    <div className="mb-5 flex gap-2"><button type="button" onClick={() => selectType('PDF')} className={`rounded-full px-4 py-2 text-sm font-semibold ${typeFilter === 'PDF' ? 'bg-indigo-600 text-white' : 'bg-white text-gray-600 ring-1 ring-gray-200'}`}>PDF rubrics</button><button type="button" onClick={() => selectType('ZIP')} className={`rounded-full px-4 py-2 text-sm font-semibold ${typeFilter === 'ZIP' ? 'bg-indigo-600 text-white' : 'bg-white text-gray-600 ring-1 ring-gray-200'}`}>ZIP rubrics</button></div>

    <section className="card mb-6 p-5"><h2 className="mb-3 text-sm font-semibold text-navy-900">Saved {typeFilter} versions</h2>{loading ? <p className="text-sm text-gray-500">Loading rubrics…</p> : shownRubrics.length === 0 ? <p className="text-sm text-gray-500">No {typeFilter} rubric has been created.</p> : <div className="flex flex-wrap gap-2">{shownRubrics.map(item => <button type="button" key={item.id} onClick={() => { setEditingId(item.id); setForm(fromRubric(item)) }} className={`rounded-lg border px-3 py-2 text-left text-xs ${editingId === item.id ? 'border-indigo-400 bg-indigo-50' : 'border-gray-200 bg-white'}`}><span className="block font-semibold text-gray-800">{item.name} · v{item.version}</span><span className={`${item.active ? 'text-emerald-700' : 'text-gray-500'}`}>{item.active ? 'Active' : 'Inactive'} · {item.totalMarks} marks</span></button>)}</div>}</section>

    <section className="card mb-6 p-5"><div className="mb-3"><h2 className="text-sm font-semibold text-navy-900">Progress review score limits</h2><p className="mt-1 text-xs text-gray-500">These provisional stage limits are independent of the final 100-mark AI rubric.</p></div><div className="grid gap-3 md:grid-cols-3">{stages.map(stage => <div key={stage.id} className="rounded-xl border border-gray-100 p-3"><label className="block text-xs font-semibold text-gray-700">{stage.name}<span className="mt-2 flex gap-2"><input type="number" min="0.01" step="0.01" value={stageMarks[stage.id] ?? ''} onChange={event => setStageMarks(current => ({ ...current, [stage.id]: event.target.value }))} className="min-w-0 flex-1 rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /><button type="button" disabled={stageBusy !== '' || !stageMarks[stage.id]} onClick={() => saveStage(stage)} className="rounded-lg bg-gray-100 px-3 text-xs font-semibold text-gray-700 disabled:opacity-50">{stageBusy === String(stage.id) ? 'Saving…' : 'Save'}</button></span></label></div>)}</div></section>

    <form onSubmit={persist} className="card space-y-5 p-6">
      <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-lg font-semibold text-navy-900">{editingId ? `Edit version ${rubrics.find(item => item.id === editingId)?.version || ''}` : 'Create rubric version'}</h2>{editingId && <label className="flex items-center gap-2 text-sm text-gray-600"><input type="checkbox" checked={form.active} onChange={event => setForm(current => ({ ...current, active: event.target.checked }))} />Active version</label>}</div>
      {editingId && <p className="-mt-3 text-xs text-gray-500">If this version has been used for an evaluation, its criteria are locked. Create the next version to make changes.</p>}
      <div className="grid gap-4 md:grid-cols-2"><label className="text-xs font-semibold text-gray-600">Rubric name<input required maxLength="150" value={form.name} onChange={event => setForm(current => ({ ...current, name: event.target.value }))} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /></label><label className="text-xs font-semibold text-gray-600">Submission type<select value={form.submissionType} disabled={Boolean(editingId)} onChange={event => { setTypeFilter(event.target.value); setForm(current => ({ ...current, submissionType: event.target.value })) }} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal"><option value="PDF">PDF</option><option value="ZIP">ZIP</option></select></label></div>
      <label className="block text-xs font-semibold text-gray-600">Description<textarea required maxLength="10000" rows="2" value={form.description} onChange={event => setForm(current => ({ ...current, description: event.target.value }))} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /></label>
      <div className="grid gap-4 md:grid-cols-[180px_1fr] md:items-end"><label className="text-xs font-semibold text-gray-600">Total marks<input type="number" min="0.01" step="0.01" value={form.totalMarks} onChange={event => setForm(current => ({ ...current, totalMarks: event.target.value }))} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /></label><p className={`text-sm ${Math.abs(criteriaTotal - Number(form.totalMarks)) < 0.0001 ? 'text-emerald-700' : 'text-amber-700'}`}>Criteria total: {criteriaTotal} / {form.totalMarks || 0}</p></div>

      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-gray-100 pt-4"><h3 className="font-semibold text-navy-900">Criteria</h3><button type="button" onClick={addCriterion} className="inline-flex items-center gap-1 rounded-lg border border-gray-200 px-3 py-2 text-xs font-semibold text-gray-700"><Plus className="h-3.5 w-3.5" />Add criterion</button></div>
      {form.criteria.length === 0 && <p className="rounded-xl bg-amber-50 p-4 text-sm text-amber-800">Add at least one criterion. Its maximum mark must be positive.</p>}
      <div className="space-y-3">{form.criteria.map((criterion, index) => <article key={`${editingId || 'new'}-${index}`} className="rounded-xl border border-gray-200 p-4"><div className="mb-3 flex items-center justify-between"><span className="text-xs font-semibold uppercase tracking-wide text-indigo-600">Criterion {index + 1}</span><button type="button" onClick={() => removeCriterion(index)} className="text-xs font-semibold text-red-600">Remove</button></div><div className="grid gap-3 md:grid-cols-[1fr_150px]"><label className="text-xs font-semibold text-gray-600">Name<input required maxLength="150" value={criterion.name} onChange={event => updateCriterion(index, 'name', event.target.value)} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /></label><label className="text-xs font-semibold text-gray-600">Maximum marks<input required type="number" min="0.01" step="0.01" value={criterion.maxMarks} onChange={event => updateCriterion(index, 'maxMarks', event.target.value)} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /></label></div><label className="mt-3 block text-xs font-semibold text-gray-600">Evaluation description<textarea required maxLength="10000" rows="2" value={criterion.description} onChange={event => updateCriterion(index, 'description', event.target.value)} className="mt-1 block w-full rounded-lg border border-gray-200 px-3 py-2 text-sm font-normal" /></label>{criterion.subcriteria?.length > 0 && <p className="mt-2 text-[11px] text-gray-500">{criterion.subcriteria.length} subcriteria will be preserved.</p>}</article>)}</div>

      <div className="flex justify-end border-t border-gray-100 pt-4"><button type="submit" disabled={saving || form.criteria.length === 0 || Math.abs(criteriaTotal - Number(form.totalMarks)) > 0.0001} className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-5 py-2.5 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40"><Save className="h-4 w-4" />{saving ? 'Saving…' : editingId ? 'Save rubric' : 'Create rubric version'}</button></div>
    </form>
  </DashboardLayout>
}
