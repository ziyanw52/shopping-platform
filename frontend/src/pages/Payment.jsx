import { useState } from 'react'
import { createPayment, getPayment, getToken } from '../services/api'
import { useNavigate, useSearchParams } from 'react-router-dom'

export default function Payment() {
  const [searchParams] = useSearchParams()
  const [form, setForm] = useState({
    orderId: searchParams.get('orderId') || '',
    amount: '',
    currency: 'USD',
    requestId: `pay-${Date.now()}`
  })
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  if (!getToken()) { navigate('/login'); return null }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await createPayment({
        orderId: parseInt(form.orderId),
        amount: parseFloat(form.amount),
        currency: form.currency,
        requestId: form.requestId
      })
      setResult(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleLookup = async () => {
    if (!form.lookupId) return
    try {
      const res = await getPayment(form.lookupId)
      setResult(res)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1 className="page-title">💳 Payment</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        {/* Submit Payment */}
        <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)' }}>
          <h3 style={{ marginBottom: '1rem' }}>Submit Payment</h3>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Order ID</label>
              <input type="number" value={form.orderId} onChange={e => setForm({ ...form, orderId: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Amount</label>
              <input type="number" step="0.01" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Currency</label>
              <select value={form.currency} onChange={e => setForm({ ...form, currency: e.target.value })}>
                <option>USD</option><option>EUR</option><option>GBP</option>
              </select>
            </div>
            <button className="btn btn-success" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Processing...' : '💰 Pay Now'}
            </button>
          </form>
        </div>

        {/* Lookup / Result */}
        <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)' }}>
          <h3 style={{ marginBottom: '1rem' }}>Lookup Payment</h3>
          <div className="form-group">
            <label>Payment ID</label>
            <input value={form.lookupId || ''} onChange={e => setForm({ ...form, lookupId: e.target.value })} placeholder="Enter payment ID" />
          </div>
          <button className="btn btn-primary" onClick={handleLookup}>🔍 Lookup</button>

          {result && (
            <div style={{ marginTop: '1.5rem', padding: '1rem', background: '#f0fdf4', borderRadius: '8px' }}>
              <h4 style={{ color: '#16a34a', marginBottom: '.5rem' }}>✅ Payment Result</h4>
              <pre style={{ fontSize: '.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>
                {JSON.stringify(result, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
