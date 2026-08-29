import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { sessionsApi, progressApi } from '../api'

export default function DashboardPage() {
  const [recentSessions, setRecentSessions] = useState([])
  const [progressData, setProgressData] = useState(null)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    Promise.all([
      sessionsApi.list({ page: 0, size: 5 }),
      progressApi.summary('DSA'),
    ]).then(([sessRes, progRes]) => {
      setRecentSessions(sessRes.data.data.content || [])
      setProgressData(progRes.data.data)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const statusClass = (s) => {
    const m = { COMPLETED: 'status-completed', FAILED: 'status-failed', ANALYZING: 'status-analyzing' }
    return m[s] || 'status-started'
  }

  if (loading) return <div className="loading-page"><div className="spinner" /><p>Loading your dashboard…</p></div>

  return (
    <div className="page">
      {/* Hero */}
      <div style={{ marginBottom: '2.5rem' }}>
        <h1>Dashboard</h1>
        <p className="text-secondary mt-1">Track your reasoning practice and improve over time.</p>
      </div>

      {/* Quick stats */}
      <div className="grid-3 mb-3">
        <div className="card">
          <div className="text-muted text-sm mb-1">Sessions practiced</div>
          <div style={{ fontSize: '2rem', fontWeight: 800 }}>
            {recentSessions.filter(s => ['COMPLETED', 'ANALYZED', 'FOLLOWUP_PENDING', 'FOLLOWUP_ANSWERED'].includes(s.status)).length}
          </div>
        </div>
        <div className="card">
          <div className="text-muted text-sm mb-1">Dimensions tracked</div>
          <div style={{ fontSize: '2rem', fontWeight: 800 }}>
            {(progressData?.trends?.length || 0) + (progressData?.lockedDimensions?.length || 0)}
          </div>
        </div>
        <div className="card card-glow" style={{ cursor: 'pointer' }} onClick={() => navigate('/questions')}>
          <div className="text-muted text-sm mb-1">Ready to practice?</div>
          <div className="text-accent" style={{ fontWeight: 700 }}>Start new session →</div>
        </div>
      </div>

      {/* Recent sessions */}
      <div className="card mb-3">
        <div className="flex-between mb-2">
          <h3>Recent sessions</h3>
          <button className="btn btn-ghost btn-sm" onClick={() => navigate('/history')}>View all</button>
        </div>
        {recentSessions.length === 0 ? (
          <div className="empty-state" style={{ padding: '2rem' }}>
            <div className="empty-state-icon">🎯</div>
            <h3>No sessions yet</h3>
            <p>Pick a question and start your first practice session.</p>
            <button className="btn btn-primary" onClick={() => navigate('/questions')}>Browse questions</button>
          </div>
        ) : (
          <div className="flex-col gap-1">
            {recentSessions.map(s => (
              <div key={s.id}
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.75rem', borderRadius: '8px', cursor: 'pointer', transition: 'background 0.2s' }}
                onMouseEnter={e => e.currentTarget.style.background = 'var(--bg-hover)'}
                onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
                onClick={() => s.status === 'COMPLETED' || s.status === 'ANALYZED' ? navigate(`/results/${s.id}`) : navigate(`/session/${s.id}`)}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{s.questionTitle}</div>
                  <div className="text-muted text-xs mt-1">{s.category} · {new Date(s.startedAt).toLocaleDateString()}</div>
                </div>
                <span className={`status-pill ${statusClass(s.status)}`}>
                  <span className="status-dot" />
                  {s.status.replace(/_/g, ' ')}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Progress preview */}
      {progressData && progressData.trends.length > 0 && (
        <div className="card">
          <div className="flex-between mb-2">
            <h3>DSA progress</h3>
            <button className="btn btn-ghost btn-sm" onClick={() => navigate('/progress')}>Full view</button>
          </div>
          <div className="flex-col gap-2">
            {progressData.trends.slice(0, 4).map(t => {
              const rawLast = t.dataPoints[t.dataPoints.length - 1]?.score ?? 0
              const last = rawLast <= 10 ? rawLast * 10 : rawLast
              const color = last >= 70 ? 'bar-fill-green' : last >= 40 ? '' : 'bar-fill-red'
              return (
                <div className="dimension-bar" key={t.dimension}>
                  <div className="dimension-label">
                    <span>{t.dimension.replace(/_/g, ' ')}</span>
                    <span>{last}/100</span>
                  </div>
                  <div className="bar-track"><div className={`bar-fill ${color}`} style={{ width: `${last}%` }} /></div>
                </div>
              )
            })}
          </div>
        </div>
      )}
    </div>
  )
}
