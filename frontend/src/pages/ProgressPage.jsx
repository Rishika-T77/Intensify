import { useEffect, useState } from 'react'
import { progressApi } from '../api'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts'

const CATEGORIES = ['DSA', 'SYSTEM_DESIGN', 'CONCEPTUAL']
const LINE_COLORS = ['#6c63ff', '#22d3a0', '#fbbf24', '#ff4d6d', '#60a5fa', '#f472b6', '#34d399']

const CustomTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null
  return (
    <div style={{ background: 'var(--bg-card)', border: '1px solid var(--border)', borderRadius: '8px', padding: '0.75rem 1rem', fontSize: '0.8rem' }}>
      <div style={{ color: 'var(--text-muted)', marginBottom: '0.4rem' }}>{new Date(label).toLocaleDateString()}</div>
      {payload.map((p, i) => (
        <div key={i} style={{ color: p.color, fontWeight: 600 }}>{p.name.replace(/_/g, ' ')}: {p.value}</div>
      ))}
    </div>
  )
}

export default function ProgressPage() {
  const [category, setCategory] = useState('DSA')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    progressApi.summary(category).then(res => setData(res.data.data)).catch(() => {}).finally(() => setLoading(false))
  }, [category])

  // Flatten trends into chart-friendly format
  const chartData = (() => {
    if (!data?.trends?.length) return []
    const allDates = [...new Set(data.trends.flatMap(t => t.dataPoints.map(p => p.recordedAt)))].sort()
    return allDates.map(date => {
      const point = { date }
      data.trends.forEach(t => {
        const dp = t.dataPoints.find(p => p.recordedAt === date)
        if (dp) point[t.dimension] = dp.score
      })
      return point
    })
  })()

  return (
    <div className="page">
      <div className="mb-3">
        <h1>Progress</h1>
        <p className="text-secondary mt-1">Track how your reasoning improves over sessions. Charts unlock after 3+ sessions per dimension.</p>
      </div>

      {/* Category selector */}
      <div className="tabs mb-3">
        {CATEGORIES.map(c => (
          <button key={c} className={`tab-btn${category === c ? ' active' : ''}`} onClick={() => setCategory(c)}>
            {c.replace('_', ' ')}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading-page"><div className="spinner" /></div>
      ) : (
        <>
          {/* Main trend chart */}
          {chartData.length > 0 ? (
            <div className="chart-container mb-3">
              <h3 className="mb-2">Reasoning trends — {category.replace('_', ' ')}</h3>
              <ResponsiveContainer width="100%" height={320}>
                <LineChart data={chartData} margin={{ top: 5, right: 20, bottom: 5, left: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                  <XAxis dataKey="date" tick={{ fill: 'var(--text-muted)', fontSize: 11 }}
                    tickFormatter={d => new Date(d).toLocaleDateString('en-IN', { month: 'short', day: 'numeric' })} />
                  <YAxis domain={[0, 100]} tick={{ fill: 'var(--text-muted)', fontSize: 11 }} />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend wrapperStyle={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}
                    formatter={v => v.replace(/_/g, ' ')} />
                  {data.trends.map((t, i) => (
                    <Line key={t.dimension} type="monotone" dataKey={t.dimension}
                      stroke={LINE_COLORS[i % LINE_COLORS.length]}
                      strokeWidth={2} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                  ))}
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="empty-state mb-3">
              <div className="empty-state-icon">📈</div>
              <h3>No data yet for {category.replace('_', ' ')}</h3>
              <p>Complete at least 3 sessions in this category to unlock your progress chart.</p>
            </div>
          )}

          {/* Latest scores */}
          {data?.trends?.length > 0 && (
            <div className="card mb-3">
              <h3 className="mb-2">Latest scores</h3>
              <div className="flex-col gap-2">
                {data.trends.map((t, i) => {
                  const last = t.dataPoints[t.dataPoints.length - 1]?.score ?? 0
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

          {/* Locked dimensions */}
          {data?.lockedDimensions?.length > 0 && (
            <div className="card">
              <h3 className="mb-2">🔒 Unlocking soon</h3>
              <p className="text-secondary text-sm mb-2">
                These dimensions need {data.minSessionsRequired} sessions in {category.replace('_', ' ')} to display a trend chart.
              </p>
              <div className="flex-col gap-1">
                {data.lockedDimensions.map(d => (
                  <div key={d} className="locked-dim">
                    <span>🔒</span>
                    <span>{d.replace(/_/g, ' ')}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
