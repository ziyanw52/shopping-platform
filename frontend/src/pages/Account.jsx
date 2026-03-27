import { useState, useEffect } from 'react'
import { getUser } from '../services/api'

const API_BASE = '/api/accounts'

function authHeaders() {
  const token = localStorage.getItem('token')
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

async function fetchAccount(id) {
  const res = await fetch(`${API_BASE}/${id}`, { headers: authHeaders() })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

async function createAccount(data) {
  const res = await fetch(API_BASE, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  })
  if (res.status === 409) throw new Error('Account already exists')
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

async function updateAccount(id, data) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

export default function Account() {
  const user = getUser()
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [lookupId, setLookupId] = useState(user?.userId || '')
  const [showCreate, setShowCreate] = useState(false)

  // Create form
  const [form, setForm] = useState({
    username: user?.username || '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
  })

  // Edit form
  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({ firstName: '', lastName: '', phone: '' })

  useEffect(() => {
    if (user?.userId) {
      loadAccount(user.userId)
    } else {
      setLoading(false)
    }
  }, [])

  const loadAccount = async (id) => {
    setLoading(true)
    setError('')
    setSuccess('')
    try {
      const data = await fetchAccount(id)
      setAccount(data)
      setShowCreate(false)
    } catch (e) {
      if (e.message.includes('404')) {
        setAccount(null)
        setShowCreate(true)
      } else {
        setError(`Failed to load account: ${e.message}`)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleLookup = (e) => {
    e.preventDefault()
    if (lookupId) loadAccount(lookupId)
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      const data = await createAccount(form)
      setAccount(data)
      setShowCreate(false)
      setSuccess('Account created successfully!')
    } catch (e) {
      setError(e.message)
    }
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      const data = await updateAccount(account.id, editForm)
      setAccount(data)
      setEditing(false)
      setSuccess('Account updated successfully!')
    } catch (e) {
      setError(e.message)
    }
  }

  const startEditing = () => {
    setEditForm({
      firstName: account.firstName || '',
      lastName: account.lastName || '',
      phone: account.phone || '',
    })
    setEditing(true)
  }

  if (!user) {
    return (
      <div className="form-card">
        <h2>👤 Account</h2>
        <p className="text-center" style={{ color: '#94a3b8' }}>
          Please <a href="/login">login</a> to view your account.
        </p>
      </div>
    )
  }

  return (
    <div>
      <h1 className="page-title">👤 My Account</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Lookup by ID */}
      <div className="card" style={{ padding: '1rem', marginBottom: '1.2rem' }}>
        <form onSubmit={handleLookup} style={{ display: 'flex', gap: '.8rem', alignItems: 'end' }}>
          <div className="form-group" style={{ flex: 1, marginBottom: 0 }}>
            <label>Lookup Account by ID</label>
            <input
              type="number"
              value={lookupId}
              onChange={(e) => setLookupId(e.target.value)}
              placeholder="Account ID"
            />
          </div>
          <button type="submit" className="btn btn-primary">Search</button>
        </form>
      </div>

      {loading && <p className="text-center">Loading...</p>}

      {/* Account Details */}
      {account && !editing && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <div className="flex-between" style={{ marginBottom: '1rem' }}>
            <h2 style={{ fontSize: '1.2rem' }}>Account Details</h2>
            <button className="btn btn-primary btn-sm" onClick={startEditing}>✏️ Edit</button>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <strong style={{ fontSize: '.8rem', color: '#64748b' }}>ID</strong>
              <p>{account.id}</p>
            </div>
            <div>
              <strong style={{ fontSize: '.8rem', color: '#64748b' }}>Username</strong>
              <p>{account.username}</p>
            </div>
            <div>
              <strong style={{ fontSize: '.8rem', color: '#64748b' }}>Email</strong>
              <p>{account.email}</p>
            </div>
            <div>
              <strong style={{ fontSize: '.8rem', color: '#64748b' }}>Name</strong>
              <p>{account.firstName || '-'} {account.lastName || '-'}</p>
            </div>
            <div>
              <strong style={{ fontSize: '.8rem', color: '#64748b' }}>Phone</strong>
              <p>{account.phone || '-'}</p>
            </div>
            <div>
              <strong style={{ fontSize: '.8rem', color: '#64748b' }}>Created</strong>
              <p>{account.createdAt ? new Date(account.createdAt).toLocaleDateString() : '-'}</p>
            </div>
          </div>

          {/* Addresses */}
          {account.addresses && account.addresses.length > 0 && (
            <div style={{ marginTop: '1.5rem' }}>
              <h3 style={{ fontSize: '1rem', marginBottom: '.6rem' }}>📍 Addresses</h3>
              {account.addresses.map((addr) => (
                <div key={addr.id} className="card" style={{ padding: '.8rem', marginBottom: '.5rem', boxShadow: 'none', border: '1px solid #e5e7eb' }}>
                  <span className="badge badge-blue" style={{ marginBottom: '.3rem' }}>{addr.type}</span>
                  <p style={{ fontSize: '.9rem' }}>{addr.street}, {addr.city}, {addr.state} {addr.postalCode}, {addr.country}</p>
                </div>
              ))}
            </div>
          )}

          {/* Payment Methods */}
          {account.paymentMethods && account.paymentMethods.length > 0 && (
            <div style={{ marginTop: '1.5rem' }}>
              <h3 style={{ fontSize: '1rem', marginBottom: '.6rem' }}>💳 Payment Methods</h3>
              {account.paymentMethods.map((pm) => (
                <div key={pm.id} className="card" style={{ padding: '.8rem', marginBottom: '.5rem', boxShadow: 'none', border: '1px solid #e5e7eb', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <span className="badge badge-green">{pm.methodType}</span>
                    <span style={{ marginLeft: '.5rem' }}>•••• {pm.lastFour}</span>
                  </div>
                  <div style={{ fontSize: '.85rem', color: '#64748b' }}>
                    Exp: {pm.expiryMonth}/{pm.expiryYear}
                    {pm.isDefault && <span className="badge badge-yellow" style={{ marginLeft: '.5rem' }}>Default</span>}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Edit Form */}
      {account && editing && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Edit Account</h2>
          <form onSubmit={handleUpdate}>
            <div className="form-group">
              <label>First Name</label>
              <input value={editForm.firstName} onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Last Name</label>
              <input value={editForm.lastName} onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })} />
            </div>
            <div className="form-group">
              <label>Phone</label>
              <input value={editForm.phone} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} />
            </div>
            <div style={{ display: 'flex', gap: '.8rem' }}>
              <button type="submit" className="btn btn-success">Save</button>
              <button type="button" className="btn btn-danger" onClick={() => setEditing(false)}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {/* Create Account Form */}
      {showCreate && !account && !loading && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <h2 style={{ fontSize: '1.2rem', marginBottom: '1rem' }}>Create Account Profile</h2>
          <p style={{ color: '#64748b', marginBottom: '1rem', fontSize: '.9rem' }}>
            No account profile found. Create one to save your details.
          </p>
          <form onSubmit={handleCreate}>
            <div className="form-group">
              <label>Username</label>
              <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>Password</label>
              <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>First Name</label>
                <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
              </div>
              <div className="form-group">
                <label>Last Name</label>
                <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
              </div>
            </div>
            <div className="form-group">
              <label>Phone</label>
              <input value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            </div>
            <button type="submit" className="btn btn-success">Create Account</button>
          </form>
        </div>
      )}
    </div>
  )
}
