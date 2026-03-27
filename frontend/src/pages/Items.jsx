import { useState, useEffect } from 'react'
import { getItems, createItem, addToCart, getUser, getToken } from '../services/api'
import { useNavigate } from 'react-router-dom'

const PLACEHOLDER_IMAGES = [
  'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400',
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400',
  'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400',
  'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=400',
  'https://images.unsplash.com/photo-1560343090-f0409e92791a?w=400',
  'https://images.unsplash.com/photo-1585386959984-a4155224a1ad?w=400',
]

export default function Items() {
  const [items, setItems] = useState([])
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ name: '', price: '', upc: '', stock: '' })
  const [msg, setMsg] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const token = getToken()
  const user = getUser()

  useEffect(() => {
    if (!token) { navigate('/login'); return }
    loadItems()
  }, [])

  const loadItems = async () => {
    try {
      const data = await getItems()
      setItems(Array.isArray(data) ? data : [])
    } catch (err) {
      setError('Failed to load items')
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await createItem({ name: form.name, price: parseFloat(form.price), upc: form.upc, stock: parseInt(form.stock) })
      setForm({ name: '', price: '', upc: '', stock: '' })
      setShowForm(false)
      setMsg('Item created!')
      setTimeout(() => setMsg(''), 2000)
      loadItems()
    } catch (err) {
      setError(err.message)
    }
  }

  const handleAddToCart = async (item) => {
    if (!user) { navigate('/login'); return }
    try {
      await addToCart(user.userId, {
        itemId: item.id,
        itemName: item.name,
        itemPrice: item.price,
        quantity: 1
      })
      setMsg(`${item.name} added to cart!`)
      setTimeout(() => setMsg(''), 2000)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <div className="flex-between">
        <h1 className="page-title">📦 Products</h1>
        <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
          {showForm ? '✕ Cancel' : '+ Add Product'}
        </button>
      </div>

      {msg && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {showForm && (
        <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', marginBottom: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)' }}>
          <h3 style={{ marginBottom: '1rem' }}>New Product</h3>
          <form onSubmit={handleCreate} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div className="form-group"><label>Name</label><input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required /></div>
            <div className="form-group"><label>Price</label><input type="number" step="0.01" value={form.price} onChange={e => setForm({ ...form, price: e.target.value })} required /></div>
            <div className="form-group"><label>UPC</label><input value={form.upc} onChange={e => setForm({ ...form, upc: e.target.value })} required /></div>
            <div className="form-group"><label>Stock</label><input type="number" value={form.stock} onChange={e => setForm({ ...form, stock: e.target.value })} required /></div>
            <button className="btn btn-success" type="submit">Create Item</button>
          </form>
        </div>
      )}

      {items.length === 0 ? (
        <div className="empty-state"><span>📦</span>No products yet. Add one above!</div>
      ) : (
        <div className="grid">
          {items.map((item, i) => (
            <div key={item.id} className="card">
              <img src={PLACEHOLDER_IMAGES[i % PLACEHOLDER_IMAGES.length]} alt={item.name} />
              <div className="card-body">
                <h3>{item.name}</h3>
                <p className="price">${item.price?.toFixed(2)}</p>
                <p className="stock">Stock: {item.stock} | UPC: {item.upc}</p>
                <button className="btn btn-primary btn-sm mt-1" onClick={() => handleAddToCart(item)}>
                  🛒 Add to Cart
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  )
}
