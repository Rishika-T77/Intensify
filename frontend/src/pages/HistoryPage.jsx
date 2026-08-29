import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionsApi } from '../api'

export default function HistoryPage() {
  const [sessions, setSessions] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const navigate = useNavigate()

  useEffect(() => {
    setLoading(true)
    sessionsApi.list({ page, size: 15 }).then(res => {
      setSessions(res.data.data.content)
      setTotalPages(res.data.data.totalPages)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [page])

  const statusClass = (s) => {
    const m = { COMPLETED: 'status-completed', FAILED: 'status-failed', ANALYZING: 'status-analyzing', ABANDONED: 'status-started' }
    return m[s] || 'status-started'
  }

  const handleClick = (s) => {
    if (s.status === 'COMPLETED' || s.status === 'ANALYZED' || s.status === 'FOLLOWUP_PENDING' || s.status === 'FOLLOWUP_ANSWERED') navigate(`/results/${s.id}`)
    else if (s.status === 'STARTED' || s.status === 'FAILED') navigate(`/session/${s.id}`)
  }

  return (
    <div className="page">
      <div className="mb-3">
        <h1>Session history</h1>
        <p className="text-secondary mt-1">All your past practice sessions. Click any to view full feedback.</p>
      </div>

      {loading ? (
        <div className="loading-page"><div className="spinner" /></div>
      ) : sessions.length === 0 ? (
        <div className="empty-state">
          <div className="empty-state-icon">📚</div>
          <h3>No sessions yet</h3>
          <p>Start practicing to build your history.</p>
          <button className="btn btn-primary" onClick={() => navigate('/questions')}>Browse questions</button>
        </div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {sessions.map((s, i) => (
            <div key={s.id}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '1rem 1.5rem',
                borderBottom: i < sessions.length - 1 ? '1px solid var(--border)' : 'none',
                cursor: s.status === 'ABANDONED' || s.status === 'ANALYZING' ? 'default' : 'pointer',
                transition: 'background 0.2s',
              }}
              onMouseEnter={e => { if (s.status !== 'ABANDONED' && s.status !== 'ANALYZING') e.currentTarget.style.background = 'var(--bg-hover)' }}
              onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
              onClick={() => handleClick(s)}
            >
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 600, fontSize: '0.925rem' }}>{s.questionTitle}</div>
                <div className="flex gap-1 mt-1" style={{ alignItems: 'center' }}>
                  <span className={`badge badge-${s.category.toLowerCase()}`}>{s.category.replace('_', ' ')}</span>
                  <span className="text-muted text-xs">{new Date(s.startedAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}</span>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <span className={`status-pill ${statusClass(s.status)}`}>
                  <span className="status-dot" />
                  {s.status === 'ABANDONED' ? 'Incomplete' : s.status.replace(/_/g, ' ')}
                </span>
                {s.failureReason && <span className="text-muted text-xs">{s.failureReason}</span>}
              </div>
            </div>
          ))}
        </div>
      )}

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
