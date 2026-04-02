import { useState, useEffect } from 'react'
import { getItems, getItem, createItem, addToCart, getUser, getToken } from '../services/api'
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
  const [searchId, setSearchId] = useState('')
  const [selectedItem, setSelectedItem] = useState(null)
  const navigate = useNavigate()
  const token = getToken()
  const user = getUser()

  useEffect(() => {
    if (!token) { navigate('/login'); return }
    loadItems()
  }, [])

  const loadItems = async () => {
    try {
      setSelectedItem(null)
      const data = await getItems()
      setItems(Array.isArray(data) ? data : [])
    } catch (err) {
      setError('Failed to load items')
    }
  }

  const handleSearch = async () => {
    if (!searchId.trim()) return
    setError('')
    try {
      const item = await getItem(searchId.trim())
      if (item) {
        setSelectedItem(item)
        setItems([item])
        setMsg(`Found: ${item.name}`)
        setTimeout(() => setMsg(''), 2000)
      } else {
        setError('Item not found')
      }
    } catch (err) {
      setError('Item not found: ' + err.message)
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
        <div style={{ display: 'flex', gap: '.5rem' }}>
          <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
            {showForm ? '✕ Cancel' : '+ Add Product'}
          </button>
        </div>
      </div>

      {msg && <div className="alert alert-success">{msg}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Search & Filter Bar */}
      <div style={{ background: 'white', borderRadius: '12px', padding: '1rem 1.5rem', marginBottom: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)', display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', gap: '.5rem', flex: 1, minWidth: '250px' }}>
          <input
            value={searchId}
            onChange={e => setSearchId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleSearch()}
            placeholder="Search by Item ID..."
            style={{ flex: 1 }}
          />
          <button className="btn btn-primary btn-sm" onClick={handleSearch}>🔍 Search</button>
        </div>
        <button className="btn btn-sm" style={{ background: '#f1f5f9', border: '1px solid #e2e8f0' }} onClick={() => { setSearchId(''); setSelectedItem(null); loadItems() }}>
          🔄 Show All ({items.length})
        </button>
      </div>

      {/* Selected Item Detail */}
      {selectedItem && (
        <div style={{ background: 'white', borderRadius: '12px', padding: '1.5rem', marginBottom: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,.08)', border: '2px solid #3b82f6' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 style={{ margin: 0 }}>📋 Item Details</h3>
            <button className="btn btn-sm" style={{ background: '#f1f5f9' }} onClick={() => { setSelectedItem(null); loadItems() }}>✕ Close</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <div style={{ color: '#64748b', fontSize: '.85rem' }}>Item ID</div>
              <div style={{ fontFamily: 'monospace', fontSize: '.9rem', wordBreak: 'break-all' }}>{selectedItem.id}</div>
            </div>
            <div>
              <div style={{ color: '#64748b', fontSize: '.85rem' }}>Name</div>
              <div style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>{selectedItem.name}</div>
            </div>
            <div>
              <div style={{ color: '#64748b', fontSize: '.85rem' }}>Price</div>
              <div style={{ color: '#16a34a', fontWeight: 'bold', fontSize: '1.2rem' }}>${selectedItem.price?.toFixed(2)}</div>
            </div>
            <div>
              <div style={{ color: '#64748b', fontSize: '.85rem' }}>Stock</div>
              <div style={{ fontWeight: 'bold', fontSize: '1.2rem', color: selectedItem.stock > 0 ? '#16a34a' : '#dc2626' }}>
                {selectedItem.stock} {selectedItem.stock === 0 ? '(Out of Stock)' : 'available'}
              </div>
            </div>
            <div>
              <div style={{ color: '#64748b', fontSize: '.85rem' }}>UPC</div>
              <div>{selectedItem.upc || '—'}</div>
            </div>
            <div>
              <button className="btn btn-primary" onClick={() => handleAddToCart(selectedItem)} disabled={selectedItem.stock === 0}>
                🛒 Add to Cart
              </button>
            </div>
          </div>
        </div>
      )}

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
            <div key={item.id} className="card" style={{ cursor: 'pointer', transition: 'transform .15s', position: 'relative' }}
              onClick={() => setSelectedItem(item)}
              onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-4px)'}
              onMouseLeave={e => e.currentTarget.style.transform = 'none'}>
              <img src={PLACEHOLDER_IMAGES[i % PLACEHOLDER_IMAGES.length]} alt={item.name} />
              <div className="card-body">
                <h3>{item.name}</h3>
                <p className="price">${item.price?.toFixed(2)}</p>
                <p className="stock">Stock: {item.stock} | UPC: {item.upc}</p>
                <p style={{ fontFamily: 'monospace', fontSize: '.7rem', color: '#94a3b8', wordBreak: 'break-all' }}>ID: {item.id}</p>
                <button className="btn btn-primary btn-sm mt-1" onClick={(e) => { e.stopPropagation(); handleAddToCart(item) }}>
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
