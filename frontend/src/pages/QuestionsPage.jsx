import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { questionsApi, sessionsApi } from '../api'

const CATEGORIES = ['ALL', 'DSA', 'SYSTEM_DESIGN', 'CONCEPTUAL']
const DIFFICULTIES = ['ALL', 'EASY', 'MEDIUM', 'HARD']

export default function QuestionsPage() {
  const [questions, setQuestions] = useState([])
  const [loading, setLoading] = useState(true)
  const [category, setCategory] = useState('ALL')
  const [difficulty, setDifficulty] = useState('ALL')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [starting, setStarting] = useState(null)
  const navigate = useNavigate()

  useEffect(() => {
    setLoading(true)
    const params = { page, size: 15 }
    if (category !== 'ALL') params.category = category
    if (difficulty !== 'ALL') params.difficulty = difficulty
    questionsApi.list(params).then(res => {
      setQuestions(res.data.data.content)
      setTotalPages(res.data.data.totalPages)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [category, difficulty, page])

  const handleStart = async (questionId) => {
    setStarting(questionId)
    try {
      const res = await sessionsApi.create(questionId)
      navigate(`/session/${res.data.data.id}`)
    } catch (err) {
      alert(err.response?.data?.error || 'Failed to start session.')
      setStarting(null)
    }
  }

  const categoryBadge = (cat) => `badge badge-${cat.toLowerCase()}`
  const diffBadge = (d) => `badge badge-${d.toLowerCase()}`

  return (
    <div className="page">
      <div className="mb-3">
        <h1>Question bank</h1>
        <p className="text-secondary mt-1">Select a question to start a practice session. Re-answering a question improves your communication, not just your solution.</p>
      </div>

      {/* Filters */}
      <div className="flex gap-2 mb-3" style={{ flexWrap: 'wrap' }}>
        <div className="form-group" style={{ minWidth: 160 }}>
          <label className="label">Category</label>
          <select id="filter-category" className="select" value={category} onChange={e => { setCategory(e.target.value); setPage(0) }}>
            {CATEGORIES.map(c => <option key={c} value={c}>{c.replace('_', ' ')}</option>)}
          </select>
        </div>
        <div className="form-group" style={{ minWidth: 140 }}>
          <label className="label">Difficulty</label>
          <select id="filter-difficulty" className="select" value={difficulty} onChange={e => { setDifficulty(e.target.value); setPage(0) }}>
            {DIFFICULTIES.map(d => <option key={d} value={d}>{d}</option>)}
          </select>
        </div>
      </div>

      {/* Question list */}
      {loading ? (
        <div className="loading-page"><div className="spinner" /></div>
      ) : questions.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📭</div>
          <h3>No questions found</h3>
          <p>Try a different category or difficulty filter.</p>
        </div>
      ) : (
        <div className="flex-col gap-2">
          {questions.map(q => (
            <div key={q.id} className="question-card" onClick={() => handleStart(q.id)}>
              <div className="question-card-meta">
                <span className={categoryBadge(q.category)}>{q.category.replace('_', ' ')}</span>
                <span className={diffBadge(q.difficulty)}>{q.difficulty}</span>
              </div>
              <div className="question-card-title">{q.title}</div>
              <div className="flex-between">
                <span className="text-muted text-xs">Click to start a practice session</span>
                <button
                  id={`start-q-${q.id}`}
                  className="btn btn-primary btn-sm"
                  onClick={(e) => { e.stopPropagation(); handleStart(q.id) }}
                  disabled={starting === q.id}
                >
                  {starting === q.id ? 'Starting…' : 'Practice →'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="pagination">
          <button className="page-btn" disabled={page === 0} onClick={() => setPage(p => p - 1)}>‹</button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button key={i} className={`page-btn${page === i ? ' active' : ''}`} onClick={() => setPage(i)}>{i + 1}</button>
          ))}
          <button className="page-btn" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>›</button>
        </div>
      )}
    </div>
  )
}
