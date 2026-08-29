import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sessionsApi } from '../api'

export default function ResultsPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [analysis, setAnalysis] = useState(null)
  const [session, setSession] = useState(null)
  const [followUp, setFollowUp] = useState(null)
  const [followUpAnswer, setFollowUpAnswer] = useState('')
  const [followUpAnalysis, setFollowUpAnalysis] = useState(null)
  const [tab, setTab] = useState('main')
  const [loading, setLoading] = useState(true)
  const [submittingFU, setSubmittingFU] = useState(false)
  const [actionError, setActionError] = useState('')
  const [pageError, setPageError] = useState('')

  useEffect(() => {
    Promise.all([
      sessionsApi.get(id),
      sessionsApi.getAnalysis(id, 'MAIN'),
    ]).then(([sessRes, analysisRes]) => {
      setSession(sessRes.data.data)
      setAnalysis(analysisRes.data.data)

      // Try fetching follow-up analysis if session is COMPLETED
      if (sessRes.data.data.status === 'COMPLETED') {
        return sessionsApi.getAnalysis(id, 'FOLLOWUP').then(r => setFollowUpAnalysis(r.data.data)).catch(() => {})
      }
    }).catch(() => setPageError('Failed to load results for this session.')).finally(() => setLoading(false))
  }, [id])

  const handleGetFollowUp = async () => {
    setActionError('')
    try {
      const res = await sessionsApi.getFollowUp(id)
      setFollowUp(res.data.data)
    } catch (err) {
      setActionError(err.response?.data?.error || 'Failed to load follow-up question.')
    }
  }

  const handleSubmitFollowUp = async () => {
    if (!followUpAnswer.trim()) return
    setActionError('')
    setSubmittingFU(true)
    try {
      await sessionsApi.submitFollowUp(id, followUpAnswer)
      const res = await sessionsApi.getAnalysis(id, 'FOLLOWUP')
      setFollowUpAnalysis(res.data.data)
      setTab('followup')
    } catch (err) {
      setActionError(err.response?.data?.error || 'Failed to submit follow-up.')
    } finally {
      setSubmittingFU(false)
    }
  }

  const scoreColor = (s) => s >= 70 ? 'var(--green)' : s >= 40 ? 'var(--yellow)' : 'var(--red)'
  const barClass = (s) => s >= 70 ? 'bar-fill-green' : s >= 40 ? '' : 'bar-fill-red'
  // Normalize any score the AI may have returned on 0-10 scale to 0-100
  const normalizeScore = (s) => (s != null && s > 0 && s <= 10) ? s * 10 : (s ?? 0)

  if (loading) return <div className="loading-page"><div className="spinner" /><p>Loading results…</p></div>
  if (pageError) return <div className="page"><div className="alert alert-error">{pageError}</div></div>

  const activeAnalysis = tab === 'followup' ? followUpAnalysis : analysis

  return (
    <div className="page">
      <div className="flex-between mb-3">
        <div>
          <h1>Session results</h1>
          <p className="text-secondary mt-1">Here's your evidence-based reasoning feedback.</p>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/history')}>← History</button>
      </div>

      {actionError && <div className="alert alert-error mb-3">{actionError}</div>}

      {/* Overall score */}
      {activeAnalysis && (
        <div className="card card-glow mb-3" style={{ textAlign: 'center', padding: '2rem' }}>
          <div style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.07em', marginBottom: '1rem' }}>
            Overall reasoning score
          </div>
          {(() => {
            const overall = normalizeScore(activeAnalysis.overallScore)
            return (
              <div className="score-ring" style={{ '--score-pct': `${overall}%`, margin: '0 auto' }}>
                <div className="score-ring-inner" style={{ color: scoreColor(overall) }}>
                  {overall}
                </div>
              </div>
            )
          })()}
        </div>
      )}

      {/* Tabs */}
      <div className="tabs">
        <button className={`tab-btn${tab === 'main' ? ' active' : ''}`} onClick={() => setTab('main')}>Main evaluation</button>
        {(followUpAnalysis || session?.status === 'ANALYZED' || session?.status === 'FOLLOWUP_PENDING') && (
          <button className={`tab-btn${tab === 'followup' ? ' active' : ''}`} onClick={() => setTab('followup')}>Follow-up</button>
        )}
      </div>

      {/* Dimension scores */}
      {activeAnalysis?.categoryScores?.filter(s => s.applicable).length > 0 && (
        <div className="card mb-3">
          <h3 className="mb-2">Dimension scores</h3>
          <div className="flex-col gap-2">
            {activeAnalysis.categoryScores.filter(s => s.applicable && s.score != null).map(s => {
              const normScore = s.score <= 10 ? s.score * 10 : s.score
              return (
                <div className="dimension-bar" key={s.dimension}>
                  <div className="dimension-label">
                    <span>{s.dimension.replace(/_/g, ' ')}</span>
                    <span style={{ color: scoreColor(normScore) }}>{normScore}/100</span>
                  </div>
                  <div className="bar-track">
                    <div className={`bar-fill ${barClass(normScore)}`} style={{ width: `${normScore}%` }} />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Strengths */}
      {activeAnalysis?.strengths?.length > 0 && (
        <div className="card mb-2">
          <h3 className="mb-2" style={{ color: 'var(--green)' }}>✓ Strengths</h3>
          <div className="flex-col gap-2">
            {activeAnalysis.strengths.map((s, i) => (
              <div key={i} className="feedback-item strength">
                <div>{s.point}</div>
                {s.evidence && <code className="evidence">"{s.evidence}"</code>}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Weaknesses */}
      {activeAnalysis?.weaknesses?.length > 0 && (
        <div className="card mb-2">
          <h3 className="mb-2" style={{ color: 'var(--red)' }}>✗ Areas to improve</h3>
          <div className="flex-col gap-2">
            {activeAnalysis.weaknesses.map((w, i) => (
              <div key={i} className="feedback-item weakness">
                <div>{w.point}</div>
                {w.evidence && <code className="evidence">"{w.evidence}"</code>}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Missing concepts */}
      {activeAnalysis?.missingConcepts?.length > 0 && (
        <div className="card mb-2">
          <h3 className="mb-2">Missing concepts</h3>
          <ul style={{ paddingLeft: '1.25rem', color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.8 }}>
            {activeAnalysis.missingConcepts.map((c, i) => <li key={i}>{c}</li>)}
          </ul>
        </div>
      )}

      {/* Recommendations */}
      {activeAnalysis?.recommendations?.length > 0 && (
        <div className="card mb-3">
          <h3 className="mb-2">Recommendations</h3>
          <div className="flex-col gap-1">
            {activeAnalysis.recommendations.map((r, i) => (
              <div key={i} style={{ padding: '0.75rem', background: 'var(--accent-dim)', borderRadius: '8px', fontSize: '0.875rem', color: 'var(--accent-light)', borderLeft: '3px solid var(--accent)' }}>
                {r}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Follow-up section */}
      {tab === 'main' && (session?.status === 'ANALYZED' || session?.status === 'FOLLOWUP_PENDING') && !followUp && !followUpAnalysis && (
        <div className="card" style={{ borderColor: 'var(--accent-border)' }}>
          <h3 className="mb-1">Ready for the follow-up?</h3>
          <p className="text-secondary mb-2">The AI generated a probing question based on your specific response. Answer it to complete the session.</p>
          <button id="get-followup" className="btn btn-primary" onClick={handleGetFollowUp}>Get follow-up question →</button>
        </div>
      )}

      {followUp && !followUpAnalysis && (
        <div className="card" style={{ borderColor: 'var(--accent-border)' }}>
          <div className="session-panel-title">🤔 Follow-up question</div>
          <p style={{ fontSize: '1rem', fontWeight: 600, marginBottom: '1rem', color: 'var(--text-primary)' }}>{followUp.questionText}</p>
          <textarea
            id="followup-answer"
            className="textarea"
            placeholder="Answer the follow-up question…"
            value={followUpAnswer}
            onChange={e => setFollowUpAnswer(e.target.value.slice(0, 2000))}
            style={{ minHeight: 140 }}
          />
          <div className="char-count mb-2">{followUpAnswer.length} / 2000</div>
          <button id="submit-followup" className="btn btn-primary" onClick={handleSubmitFollowUp} disabled={submittingFU || followUpAnswer.trim().length < 10}>
            {submittingFU ? 'Evaluating…' : 'Submit follow-up →'}
          </button>
        </div>
      )}

      <div className="flex gap-2 mt-3">
        <button className="btn btn-ghost" onClick={() => navigate('/questions')}>Practice another →</button>
        <button className="btn btn-ghost" onClick={() => navigate('/progress')}>View progress →</button>
      </div>
    </div>
  )
}
