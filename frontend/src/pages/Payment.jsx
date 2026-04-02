import { useState, useEffect } from 'react'
import { createPayment, getPayment, getToken, getOrder, getItem } from '../services/api'
import { useNavigate, useSearchParams } from 'react-router-dom'

export default function Payment() {
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId') || ''
  const [orderInfo, setOrderInfo] = useState(null)
  const [itemInfo, setItemInfo] = useState(null)
  const [computedAmount, setComputedAmount] = useState(null)
  const [currency, setCurrency] = useState('USD')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [lookupId, setLookupId] = useState('')
  const [demoMode, setDemoMode] = useState('normal') // normal, validation, mock-failure
  const navigate = useNavigate()

  if (!getToken()) { navigate('/login'); return null }

  // Auto-fetch order and item details to compute amount
  useEffect(() => {
    if (!orderId) return
    const fetchDetails = async () => {
      try {
        const order = await getOrder(orderId)
        setOrderInfo(order)
        if (order.itemId) {
          const item = await getItem(order.itemId)
          setItemInfo(item)
          setComputedAmount((item.price * order.quantity).toFixed(2))
        }
      } catch (err) {
        setError('Failed to load order details: ' + err.message)
      }
    }
    fetchDetails()
  }, [orderId])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setResult(null)
    setLoading(true)
    try {
      const res = await createPayment({
        orderId: parseInt(orderId),
        amount: parseFloat(computedAmount),
        currency,
        requestId: `pay-${Date.now()}`
      })
      setResult(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  // Demo: Validation Errors
  const testValidationErrors = async (errorType) => {
    setError('')
    setResult(null)
    setLoading(true)
    
    let testPayload = {}
    let testDescription = ''
    
    switch(errorType) {
      case 'negative-amount':
        testPayload = {
          orderId: parseInt(orderId),
          amount: -10.00,
          currency,
          requestId: `test-negative-${Date.now()}`
        }
        testDescription = 'Negative Amount (-10.00)'
        break
      case 'missing-orderid':
        testPayload = {
          orderId: null,
          amount: parseFloat(computedAmount),
          currency,
          requestId: `test-no-order-${Date.now()}`
        }
        testDescription = 'Missing Order ID (null)'
        break
      case 'empty-requestid':
        testPayload = {
          orderId: parseInt(orderId),
          amount: parseFloat(computedAmount),
          currency,
          requestId: ''
        }
        testDescription = 'Empty Request ID'
        break
      case 'zero-amount':
        testPayload = {
          orderId: parseInt(orderId),
          amount: 0,
          currency,
          requestId: `test-zero-${Date.now()}`
        }
        testDescription = 'Zero Amount (0.00)'
        break
    }
    
    try {
      await createPayment(testPayload)
      setError(`❌ Validation should have failed for: ${testDescription}`)
    } catch (err) {
      setResult({
        testType: 'Validation Error Test',
        testCase: testDescription,
        expectedResult: '❌ Validation Error',
        actualResult: '✅ Validation Failed as Expected',
        errorMessage: err.message,
        status: 'VALIDATION_PASSED'
      })
    } finally {
      setLoading(false)
    }
  }

  // Demo: Mock Payment Failures
  const testMockFailure = async (failureType) => {
    setError('')
    setResult(null)
    setLoading(true)
    
    // Use correct amount but trigger failure via requestId
    const testAmount = parseFloat(computedAmount)
    let testRequestId = ''
    let testDescription = ''
    
    // Trigger mock failures using special requestId patterns
    switch(failureType) {
      case 'card-declined':
        testRequestId = `mock-card-declined-${Date.now()}`
        testDescription = 'Card Declined (triggered via requestId)'
        break
      case 'insufficient-funds':
        testRequestId = `mock-insufficient-funds-${Date.now()}`
        testDescription = 'Insufficient Funds (triggered via requestId)'
        break
      case 'amount-exceed':
        testRequestId = `mock-exceeds-limit-${Date.now()}`
        testDescription = 'Amount Exceeds Limit (triggered via requestId)'
        break
    }
    
    try {
      const res = await createPayment({
        orderId: parseInt(orderId),
        amount: testAmount,  // Use correct amount
        currency,
        requestId: testRequestId  // Trigger failure via requestId
      })
      setResult({
        ...res,
        testType: 'Mock Failure Test',
        testCase: testDescription,
        note: res.status === 'FAILED' ? '✅ Payment failed as expected' : '⚠️ Payment succeeded unexpectedly'
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  // Test idempotency by sending same requestId twice
  const testIdempotency = async () => {
    setError('')
    setResult(null)
    setLoading(true)
    const testRequestId = `idempotency-test-${Date.now()}`
    
    try {
      // First request
      const res1 = await createPayment({
        orderId: parseInt(orderId),
        amount: parseFloat(computedAmount),
        currency,
        requestId: testRequestId
      })
      
      // Wait a moment to ensure first request is processed
      await new Promise(resolve => setTimeout(resolve, 500))
      
      // Second request with SAME requestId
      const res2 = await createPayment({
        orderId: parseInt(orderId),
        amount: parseFloat(computedAmount),
        currency,
        requestId: testRequestId
      })
      
      // Check if both paymentIds are the same
      if (res1.paymentId === res2.paymentId) {
        setResult({
          testType: 'Idempotency Test',
          testCase: 'Retry Same Request (same requestId)',
          expectedResult: '✅ Same paymentId returned',
          actualResult: '✅ PASSED',
          requestId: testRequestId,
          firstPaymentId: res1.paymentId,
          secondPaymentId: res2.paymentId,
          status: res2.status,
          amount: res2.amount,
          currency: res2.currency,
          note: 'Both requests returned identical paymentId - no duplicate charge'
        })
      } else {
        setError('❌ Idempotency test FAILED - Different paymentIds returned')
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleLookup = async () => {
    if (!lookupId) return
    try {
      const res = await getPayment(lookupId)
      setResult(res)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1 className="page-title">💳 Payment Demo</h1>
      {error && <div className="alert alert-error">{error}</div>}

      {/* Demo Mode Selector */}
      <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f8fafc', borderRadius: '8px', border: '2px solid #e2e8f0' }}>
        <h3 style={{ margin: '0 0 0.5rem', color: '#0f172a' }}>🎯 Demo Mode</h3>
        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <button 
            className={`btn ${demoMode === 'normal' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setDemoMode('normal')}
          >
            💰 Normal Payment
          </button>
          <button 
            className={`btn ${demoMode === 'idempotency' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setDemoMode('idempotency')}
          >
            🔄 Idempotency Test
          </button>
          <button 
            className={`btn ${demoMode === 'validation' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setDemoMode('validation')}
          >
            ⚠️ Validation Errors
          </button>
          <button 
            className={`btn ${demoMode === 'mock-failure' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setDemoMode('mock-failure')}
          >
            ❌ Mock Failures
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem' }}>
        {/* Submit Payment */}
        <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)' }}>
          <h3 style={{ marginBottom: '1rem' }}>
            {demoMode === 'normal' && '💰 Submit Payment'}
            {demoMode === 'idempotency' && '🔄 Idempotency Test'}
            {demoMode === 'validation' && '⚠️ Validation Error Tests'}
            {demoMode === 'mock-failure' && '❌ Mock Failure Tests'}
          </h3>

          {/* Order Summary */}
          {orderInfo && itemInfo && (
            <div style={{ marginBottom: '1rem', padding: '1rem', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <h4 style={{ margin: '0 0 .5rem', color: '#475569' }}>📦 Order Summary</h4>
              <div style={{ fontSize: '.9rem', color: '#64748b' }}>
                <div><strong>Order:</strong> #{orderInfo.orderId}</div>
                <div><strong>Item:</strong> {itemInfo.name}</div>
                <div><strong>Price:</strong> ${itemInfo.price?.toFixed(2)} × {orderInfo.quantity}</div>
                <div style={{ marginTop: '.5rem', fontSize: '1.1rem', fontWeight: 'bold', color: '#0f172a' }}>
                  💰 Total: ${computedAmount}
                </div>
              </div>
            </div>
          )}

          {/* Normal Payment Mode */}
          {demoMode === 'normal' && (
            <form onSubmit={handleSubmit}>
              <div className="form-group">
                <label>Order ID</label>
                <input type="number" value={orderId} readOnly style={{ background: '#f1f5f9' }} />
              </div>
              <div className="form-group">
                <label>Amount</label>
                <input type="number" step="0.01" value={computedAmount || ''} readOnly style={{ background: '#f1f5f9', fontWeight: 'bold' }} />
                <small style={{ color: '#64748b' }}>Auto-calculated from item price × quantity</small>
              </div>
              <div className="form-group">
                <label>Currency</label>
                <select value={currency} onChange={e => setCurrency(e.target.value)}>
                  <option>USD</option><option>EUR</option><option>GBP</option>
                </select>
              </div>
              <button className="btn btn-success" style={{ width: '100%' }} disabled={loading || !computedAmount}>
                {loading ? 'Processing...' : `💰 Pay $${computedAmount || '...'}`}
              </button>
            </form>
          )}

          {/* Idempotency Test Mode */}
          {demoMode === 'idempotency' && (
            <div>
              <div style={{ padding: '1rem', background: '#dbeafe', borderRadius: '8px', marginBottom: '1rem' }}>
                <p style={{ margin: 0, fontSize: '0.9rem', color: '#1e40af' }}>
                  <strong>Test:</strong> Sends the same requestId twice to verify idempotency. 
                  Both requests should return the same paymentId without duplicate charges.
                </p>
              </div>
              <button 
                className="btn btn-primary" 
                style={{ width: '100%' }} 
                disabled={loading || !computedAmount}
                onClick={testIdempotency}
              >
                {loading ? 'Testing...' : '🔄 Run Idempotency Test'}
              </button>
            </div>
          )}

          {/* Validation Error Tests Mode */}
          {demoMode === 'validation' && (
            <div>
              <div style={{ padding: '1rem', background: '#fef3c7', borderRadius: '8px', marginBottom: '1rem' }}>
                <p style={{ margin: 0, fontSize: '0.9rem', color: '#92400e' }}>
                  <strong>Test:</strong> Trigger validation errors by sending invalid data
                </p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <button 
                  className="btn btn-secondary" 
                  disabled={loading}
                  onClick={() => testValidationErrors('negative-amount')}
                >
                  ⚠️ Negative Amount (-10.00)
                </button>
                <button 
                  className="btn btn-secondary" 
                  disabled={loading}
                  onClick={() => testValidationErrors('zero-amount')}
                >
                  ⚠️ Zero Amount (0.00)
                </button>
                <button 
                  className="btn btn-secondary" 
                  disabled={loading}
                  onClick={() => testValidationErrors('missing-orderid')}
                >
                  ⚠️ Missing Order ID (null)
                </button>
                <button 
                  className="btn btn-secondary" 
                  disabled={loading}
                  onClick={() => testValidationErrors('empty-requestid')}
                >
                  ⚠️ Empty Request ID
                </button>
              </div>
            </div>
          )}

          {/* Mock Failure Tests Mode */}
          {demoMode === 'mock-failure' && (
            <div>
              <div style={{ padding: '1rem', background: '#fee2e2', borderRadius: '8px', marginBottom: '1rem' }}>
                <p style={{ margin: 0, fontSize: '0.9rem', color: '#991b1b' }}>
                  <strong>Test:</strong> Simulate payment gateway failures using special amounts
                </p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <button 
                  className="btn btn-danger" 
                  disabled={loading}
                  onClick={() => testMockFailure('card-declined')}
                >
                  💳 Card Declined ($666.66)
                </button>
                <button 
                  className="btn btn-danger" 
                  disabled={loading}
                  onClick={() => testMockFailure('insufficient-funds')}
                >
                  💸 Insufficient Funds ($999.99)
                </button>
                <button 
                  className="btn btn-danger" 
                  disabled={loading}
                  onClick={() => testMockFailure('amount-exceed')}
                >
                  🚫 Amount Exceeds Limit ($10,000)
                </button>
              </div>
              <small style={{ display: 'block', marginTop: '0.5rem', color: '#64748b', fontSize: '0.85rem' }}>
                Note: Stock will be reserved then restored on failure
              </small>
            </div>
          )}
        </div>

        {/* Lookup / Result */}
        <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)' }}>
          <h3 style={{ marginBottom: '1rem' }}>Payment Status</h3>

          {!result && (
            <>
              <div className="form-group">
                <label>Lookup Payment ID</label>
                <input value={lookupId} onChange={e => setLookupId(e.target.value)} placeholder="Enter payment ID" />
              </div>
              <button className="btn btn-primary" onClick={handleLookup}>🔍 Lookup</button>
            </>
          )}

          {result && (
            <div style={{ marginTop: '1.5rem', padding: '1rem', background: result.status === 'SUCCESS' || result.status === 'VALIDATION_PASSED' ? '#f0fdf4' : '#fef2f2', borderRadius: '8px' }}>
              <h4 style={{ color: result.status === 'SUCCESS' || result.status === 'VALIDATION_PASSED' ? '#16a34a' : '#dc2626', marginBottom: '.5rem' }}>
                {result.status === 'SUCCESS' && '✅ Payment Successful!'}
                {result.status === 'FAILED' && '❌ Payment Failed'}
                {result.status === 'VALIDATION_PASSED' && '✅ Validation Test Passed'}
                {result.testType && ` - ${result.testType}`}
              </h4>
              
              {result.testCase && (
                <div style={{ padding: '0.75rem', background: '#e0e7ff', borderRadius: '4px', marginBottom: '0.75rem' }}>
                  <div style={{ color: '#3730a3', fontWeight: 'bold', marginBottom: '0.25rem' }}>
                    Test Case: {result.testCase}
                  </div>
                  {result.expectedResult && (
                    <div style={{ color: '#4338ca', fontSize: '0.9rem' }}>
                      Expected: {result.expectedResult}
                    </div>
                  )}
                  {result.actualResult && (
                    <div style={{ color: '#4338ca', fontSize: '0.9rem' }}>
                      Actual: {result.actualResult}
                    </div>
                  )}
                  {result.note && (
                    <div style={{ color: '#4338ca', fontSize: '0.9rem', marginTop: '0.25rem', fontStyle: 'italic' }}>
                      {result.note}
                    </div>
                  )}
                </div>
              )}
              
              <pre style={{ fontSize: '.8rem', whiteSpace: 'pre-wrap', wordBreak: 'break-all', background: '#f8fafc', padding: '0.75rem', borderRadius: '4px', maxHeight: '400px', overflow: 'auto' }}>
                {JSON.stringify(result, null, 2)}
              </pre>
              
              <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem' }}>
                <button className="btn btn-primary" onClick={() => { setResult(null); setError('') }}>
                  🔄 Run Another Test
                </button>
                <button className="btn btn-secondary" onClick={() => navigate('/orders')}>
                  📋 Back to Orders
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  )
}
