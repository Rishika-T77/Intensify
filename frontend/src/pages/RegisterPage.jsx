import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import { authApi } from '../api'
import { setCredentials } from '../store/authSlice'

export default function RegisterPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '', targetRole: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const dispatch = useDispatch()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.password.length < 8) { setError('Password must be at least 8 characters.'); return }
    setLoading(true)
    try {
      const res = await authApi.register(form)
      dispatch(setCredentials(res.data.data.token))
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-title">Create account</div>
        <div className="auth-subtitle">Start practising your technical reasoning</div>

        {error && <div className="alert alert-error mb-2">{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="label">Full name</label>
            <input id="reg-name" className="input" type="text" placeholder="Rishi Sharma"
              value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label className="label">Email</label>
            <input id="reg-email" className="input" type="email" placeholder="you@example.com"
              value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label className="label">Password <span className="text-muted">(min 8 chars)</span></label>
            <input id="reg-password" className="input" type="password" placeholder="••••••••"
              value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} required />
          </div>
          <div className="form-group">
            <label className="label">Target role <span className="text-muted">(optional)</span></label>
            <input id="reg-role" className="input" type="text" placeholder="Backend Engineer, SDE-2…"
              value={form.targetRole} onChange={e => setForm(f => ({ ...f, targetRole: e.target.value }))} />
          </div>
          <button id="reg-submit" className="btn btn-primary btn-full btn-lg" type="submit" disabled={loading}>
            {loading ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <div className="auth-footer">
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  )
}
