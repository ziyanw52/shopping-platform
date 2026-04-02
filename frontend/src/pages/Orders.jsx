import { useState, useEffect } from 'react'
import { getOrders, confirmOrder, markPaid, completeOrder, cancelOrder, refundPayment, getToken } from '../services/api'
import { useNavigate } from 'react-router-dom'

const STATUS_BADGE = {
  CREATED: 'badge-yellow',
  PENDING: 'badge-yellow',
  PENDING_PAYMENT: 'badge-yellow',
  CONFIRMED: 'badge-blue',
  COMPLETED: 'badge-green',
  CANCELLED: 'badge-red',
  PAYMENT_FAILED: 'badge-red',
  REFUNDED: 'badge-gray',
}

export default function Orders() {
  const [orders, setOrders] = useState([])
  const [error, setError] = useState('')
  const [msg, setMsg] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    if (!getToken()) { navigate('/login'); return }
    loadOrders()
  }, [])

  const loadOrders = async () => {
    try {
      const data = await getOrders()
      setOrders(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err.message)
    }
  }

  const doAction = async (fn, id, label) => {
    try {
      await fn(id)
      setMsg(`Order #${id} ${label}!`)
      setTimeout(() => setMsg(''), 2000)
      loadOrders()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1 className="page-title">📋 Orders</h1>
      {msg && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {orders.length === 0 ? (
        <div className="empty-state">
          <span>📋</span>
          <p>No orders yet</p>
          <button className="btn btn-primary mt-1" onClick={() => navigate('/items')}>Start Shopping</button>
        </div>
      ) : (
        <table>
          <thead>
            <tr><th>Order #</th><th>User</th><th>Item</th><th>Qty</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {orders.map(o => (
              <tr key={o.orderId}>
                <td><strong>#{o.orderId}</strong></td>
                <td>{o.userId}</td>
                <td>{o.itemId}</td>
                <td>{o.quantity}</td>
                <td><span className={`badge ${STATUS_BADGE[o.status] || 'badge-gray'}`}>{o.status}</span></td>
                <td style={{ display: 'flex', gap: '.4rem', flexWrap: 'wrap' }}>
                  {o.status === 'CREATED' && (
                    <>
                      <button className="btn btn-success btn-sm" onClick={() => navigate(`/payment?orderId=${o.orderId}`)}>💰 Pay Now</button>
                      <button className="btn btn-danger btn-sm" onClick={() => doAction(cancelOrder, o.orderId, 'cancelled')}>Cancel</button>
                    </>
                  )}
                  {o.status === 'PENDING' && (
                    <>
                      <button className="btn btn-success btn-sm" onClick={() => navigate(`/payment?orderId=${o.orderId}`)}>💰 Pay Now</button>
                      <button className="btn btn-danger btn-sm" onClick={() => doAction(cancelOrder, o.orderId, 'cancelled')}>Cancel</button>
                    </>
                  )}
                  {o.status === 'PAYMENT_FAILED' && (
                    <>
                      <button className="btn btn-success btn-sm" onClick={() => navigate(`/payment?orderId=${o.orderId}`)}>🔄 Retry Payment</button>
                      <button className="btn btn-danger btn-sm" onClick={() => doAction(cancelOrder, o.orderId, 'cancelled')}>Cancel</button>
                    </>
                  )}
                  {o.status === 'CONFIRMED' && (
                    <>
                      <button className="btn btn-success btn-sm" onClick={() => doAction(completeOrder, o.orderId, 'completed')}>📦 Mark Received</button>
                      <button className="btn btn-danger btn-sm" onClick={() => {
                        const paymentId = prompt('Enter Payment ID to refund:')
                        if (paymentId) doAction(refundPayment, paymentId, 'refunded')
                      }}>💸 Refund</button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
