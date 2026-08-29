import { useEffect, useState, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sessionsApi, questionsApi } from '../api'

const EXPLANATION_MAX = 2000
const CODE_MAX = 5000
const LANGUAGES = ['java', 'python', 'javascript', 'cpp', 'csharp', 'go']

const LOADING_TIMEOUT_MS = 35000
const POLL_INTERVAL_MS = 3000

export default function SessionPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [session, setSession] = useState(null)
  const [question, setQuestion] = useState(null)
  const [explanation, setExplanation] = useState('')
  const [code, setCode] = useState('')
  const [language, setLanguage] = useState('java')
  const [showCode, setShowCode] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const pollRef = useRef(null)
  const timeoutRef = useRef(null)

  useEffect(() => {
    sessionsApi.get(id).then(res => {
      const s = res.data.data
      setSession(s)
      if (s.status === 'ANALYZING') startPolling()
      if (s.status === 'ANALYZED' || s.status === 'COMPLETED') navigate(`/results/${id}`)
      return questionsApi.get(s.questionId)
    }).then(res => setQuestion(res.data.data)).catch(() => setError('Failed to load session.'))

    return () => { clearInterval(pollRef.current); clearTimeout(timeoutRef.current) }
  }, [id])

  const startPolling = () => {
    pollRef.current = setInterval(async () => {
      try {
        const res = await sessionsApi.get(id)
        const s = res.data.data
        setSession(s)
        if (s.status === 'ANALYZED' || s.status === 'COMPLETED') {
          clearInterval(pollRef.current)
          clearTimeout(timeoutRef.current)
          navigate(`/results/${id}`)
        } else if (s.status === 'FAILED') {
          clearInterval(pollRef.current)
          clearTimeout(timeoutRef.current)
          // Explicitly update local status so isAnalyzing becomes false (Audit §2.5)
          setSession(prev => ({ ...prev, status: 'FAILED' }))
          setError('Evaluation failed. Your response was saved — you can retry below.')
          setSubmitting(false)
        }
      } catch { /* network blip — keep polling */ }
    }, POLL_INTERVAL_MS)

    timeoutRef.current = setTimeout(() => {
      clearInterval(pollRef.current)
      // Explicitly mark FAILED locally so the spinner doesn't stay forever (Audit §2.5)
      setSession(prev => ({ ...prev, status: 'FAILED' }))
      setError('Evaluation is taking longer than expected. Your response was saved — you can retry below.')
      setSubmitting(false)
    }, LOADING_TIMEOUT_MS)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (explanation.trim().length < 30) { setError('Please write a more detailed explanation (at least 30 characters).'); return }
    setError('')
    setSubmitting(true)
    try {
      const payload = { explanationText: explanation }
      if (showCode && code.trim()) { payload.code = code; payload.language = language }
      await sessionsApi.submitResponse(id, payload)
      setSession(s => ({ ...s, status: 'ANALYZING' }))
      startPolling()
    } catch (err) {
      setError(err.response?.data?.error || 'Submission failed.')
      setSubmitting(false)
    }
  }

  const isAnalyzing = (session?.status === 'ANALYZING' || submitting) && session?.status !== 'FAILED'

  if (!session || !question) return <div className="loading-page"><div className="spinner" /><p>Loading session…</p></div>

  // Dedicated failure screen — replaces the stuck spinner (Audit §2.5)
  if (session?.status === 'FAILED') return (
    <div className="loading-page">
      <div style={{ fontSize: '3rem', marginBottom: '0.5rem' }}>⚠️</div>
      <h2 style={{ fontSize: '1.3rem', color: 'var(--error, #ef4444)' }}>Evaluation failed</h2>
      <p style={{ color: 'var(--text-secondary)', maxWidth: 400, textAlign: 'center', marginTop: '0.5rem' }}>
        {error || 'Your response was saved. You can retry the evaluation or go back to history.'}
      </p>
      <div style={{ display: 'flex', gap: '1rem', marginTop: '1.5rem', flexWrap: 'wrap', justifyContent: 'center' }}>
        <button
          id="goto-history-btn"
          className="btn btn-primary"
          onClick={() => navigate('/history')}
        >
          Go to History
        </button>
        <button
          id="retry-session-btn"
          className="btn btn-ghost"
          onClick={() => {
            setSession(s => ({ ...s, status: 'STARTED' }))
            setError('')
          }}
        >
          Try Again
        </button>
      </div>
    </div>
  )

  if (isAnalyzing) return (
    <div className="loading-page">
      <div className="spinner" style={{ width: 56, height: 56, borderWidth: 4 }} />
      <h2 style={{ fontSize: '1.3rem' }}>Evaluating your reasoning…</h2>
      <p>This usually takes up to 30 seconds. Please don't close this tab.</p>
      {error && <div className="alert alert-error mt-2">{error}</div>}
    </div>
  )

  return (
    <div className="page">
      <div className="flex-between mb-3">
        <div>
          <div className="flex gap-2 mb-1">
            <span className={`badge badge-${question.category.toLowerCase()}`}>{question.category.replace('_', ' ')}</span>
            <span className={`badge badge-${question.difficulty.toLowerCase()}`}>{question.difficulty}</span>
          </div>
          <h2>{question.title}</h2>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={() => navigate('/questions')}>← Back</button>
      </div>

      <div className="session-layout">
        {/* Left: question prompt */}
        <div className="session-panel">
          <div className="session-panel-title">📋 Question</div>
          <p className="question-prompt">{question.promptText}</p>
        </div>

        {/* Right: response form */}
        <div>
          <form onSubmit={handleSubmit} className="flex-col gap-3">
            <div className="session-panel">
              <div className="session-panel-title">✏️ Your approach</div>
              <div className="form-group">
                <textarea
                  id="explanation-input"
                  className="textarea"
                  placeholder={`Explain your approach clearly:\n• What problem pattern do you recognise?\n• What algorithm/data structure do you choose and WHY?\n• What are the time and space complexity tradeoffs?\n• Did you consider any alternatives?`}
                  value={explanation}
                  onChange={e => setExplanation(e.target.value.slice(0, EXPLANATION_MAX))}
                  style={{ minHeight: 200 }}
                  disabled={submitting}
                />
                <div className={`char-count ${explanation.length > EXPLANATION_MAX * 0.9 ? 'warn' : ''}`}>
                  {explanation.length} / {EXPLANATION_MAX}
                </div>
              </div>
            </div>

            {question.category === 'DSA' && (
              <div className="session-panel">
                <div className="session-panel-title flex-between" style={{ marginBottom: 0 }}>
                  <span>💻 Code <span className="text-muted" style={{ fontSize: '0.75rem', textTransform: 'none', letterSpacing: 0 }}>(optional)</span></span>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowCode(v => !v)}>
                    {showCode ? 'Remove code' : '+ Add code'}
                  </button>
                </div>
                {showCode && (
                  <div className="flex-col gap-2 mt-2">
                    <select id="code-language" className="select" value={language} onChange={e => setLanguage(e.target.value)}>
                      {LANGUAGES.map(l => <option key={l} value={l}>{l}</option>)}
                    </select>
                    <textarea
                      id="code-input"
                      className="textarea input-mono"
                      placeholder="// Paste your code here"
                      value={code}
                      onChange={e => setCode(e.target.value.slice(0, CODE_MAX))}
                      style={{ minHeight: 180 }}
                      disabled={submitting}
                    />
                    <div className={`char-count ${code.length > CODE_MAX * 0.9 ? 'warn' : ''}`}>
                      {code.length} / {CODE_MAX}
                    </div>
                  </div>
                )}
              </div>
            )}

            {error && <div className="alert alert-error">{error}</div>}

            <button id="submit-session" className="btn btn-primary btn-full btn-lg" type="submit" disabled={submitting || explanation.trim().length < 10}>
              Submit for evaluation →
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
