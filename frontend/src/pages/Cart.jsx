import { useState, useEffect } from 'react'
import { getCart, checkout, getUser, getToken } from '../services/api'
import { useNavigate } from 'react-router-dom'

export default function Cart() {
  const [cart, setCart] = useState(null)
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const token = getToken()
  const user = getUser()

  useEffect(() => {
    if (!token) { navigate('/login'); return }
    loadCart()
  }, [])

  const loadCart = async () => {
    try {
      const data = await getCart(user.userId)
      setCart(data)
    } catch (err) {
      if (err.message.includes('Cart not found')) {
        setCart({ items: [], totalPrice: 0, totalQuantity: 0 })
      } else {
        setError(err.message)
      }
    }
  }

  const handleCheckout = async () => {
    try {
      const order = await checkout(user.userId)
      setMsg(`Order #${order.orderId} created! Redirecting to orders...`)
      setTimeout(() => navigate('/orders'), 1500)
    } catch (err) {
      setError(err.message)
    }
  }

  if (!cart) return <div className="text-center mt-2">Loading cart...</div>

  return (
    <>
      <h1 className="page-title">🛍 Your Cart</h1>
      {msg && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {!cart.items || cart.items.length === 0 ? (
        <div className="empty-state">
          <span>🛒</span>
          <p>Your cart is empty</p>
          <button className="btn btn-primary mt-1" onClick={() => navigate('/items')}>Browse Products</button>
        </div>
      ) : (
        <>
          <table>
            <thead>
              <tr><th>Item</th><th>Price</th><th>Qty</th><th>Subtotal</th></tr>
            </thead>
            <tbody>
              {cart.items.map(item => (
                <tr key={item.cartItemId}>
                  <td><strong>{item.itemName}</strong></td>
                  <td>${item.itemPrice?.toFixed(2)}</td>
                  <td>{item.quantity}</td>
                  <td><strong>${item.subtotal?.toFixed(2)}</strong></td>
                </tr>
              ))}
            </tbody>
          </table>

          <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', marginTop: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <span style={{ fontSize: '.9rem', color: '#64748b' }}>Total ({cart.totalQuantity} items)</span>
              <h2 style={{ color: '#16a34a' }}>${cart.totalPrice?.toFixed(2)}</h2>
            </div>
            <button className="btn btn-success" style={{ padding: '.7rem 2rem' }} onClick={handleCheckout}>
              Checkout →
            </button>
          </div>
        </>
      )}
    </>
  )
}
