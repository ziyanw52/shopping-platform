import { useState, useEffect } from 'react'
import { getUser, setUser as saveUser } from '../services/api'

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
  if (res.status === 404) {
    // Account doesn't exist yet - this is expected for new users
    return null
  }
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

async function createAccountApi(data) {
  const res = await fetch(API_BASE, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  })
  if (res.status === 409) throw new Error('Account already exists with this username or email')
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json()
}

async function updateAccountApi(id, data) {
  const res = await fetch(`${API_BASE}/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json()
}

export default function Account() {
  const user = getUser()
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  // Active tab
  const [tab, setTab] = useState('profile')

  // Address form
  const [showAddressForm, setShowAddressForm] = useState(false)
  const [addressForm, setAddressForm] = useState({
    street: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
    type: 'SHIPPING',
  })

  // Payment method form
  const [showPaymentForm, setShowPaymentForm] = useState(false)
  const [paymentForm, setPaymentForm] = useState({
    methodType: 'CREDIT_CARD',
    cardNumber: '',
    lastFour: '',
    expiryMonth: '',
    expiryYear: '',
    isDefault: false,
  })

  // Create form
  const [form, setForm] = useState({
    username: user?.username || '',
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
    street: '',
    city: '',
    state: '',
    postalCode: '',
    country: '',
  })

  // Edit mode for profile
  const [editing, setEditing] = useState(false)
  const [editForm, setEditForm] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
  })

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
    try {
      const data = await fetchAccount(id)
      if (data === null) {
        // Account doesn't exist - show profile completion form
        setAccount(null)
        setShowCreate(true)
      } else {
        setAccount(data)
        setShowCreate(false)
      }
    } catch (e) {
      setError(`Failed to load account: ${e.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      // Create account profile
      const accountData = {
        username: form.username,
        email: form.email,
        firstName: form.firstName,
        lastName: form.lastName,
        phone: form.phone,
      }
      const data = await createAccountApi(accountData)
      
      // If address fields are filled, create address
      if (form.street && form.city && form.state && form.postalCode && form.country) {
        try {
          const addressData = {
            street: form.street,
            city: form.city,
            state: form.state,
            postalCode: form.postalCode,
            country: form.country,
            type: 'SHIPPING',
          }
          const addressRes = await fetch(`${API_BASE}/${data.id}/addresses`, {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify(addressData),
          })
          if (addressRes.ok) {
            const updatedAccount = await fetchAccount(data.id)
            setAccount(updatedAccount)
          } else {
            setAccount(data)
          }
        } catch (addrErr) {
          console.warn('Address creation failed:', addrErr)
          setAccount(data)
        }
      } else {
        setAccount(data)
      }
      
      setShowCreate(false)
      setSuccess('Profile completed successfully!')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      setError(e.message)
    }
  }

  const startEditing = () => {
    setEditForm({
      username: account.username || '',
      email: account.email || '',
      firstName: account.firstName || '',
      lastName: account.lastName || '',
      phone: account.phone || '',
    })
    setEditing(true)
    setError('')
    setSuccess('')
  }

  const handleUpdate = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      const data = await updateAccountApi(account.id, editForm)
      setAccount(data)
      setEditing(false)
      // If username changed, update localStorage so nav shows new name
      if (data.username && data.username !== user?.username) {
        saveUser({ ...user, username: data.username })
      }
      setSuccess('Profile updated successfully!')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      setError(e.message)
    }
  }

  const handleAddAddress = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      const res = await fetch(`${API_BASE}/${account.id}/addresses`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(addressForm),
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `HTTP ${res.status}`)
      }
      const updatedAccount = await fetchAccount(account.id)
      setAccount(updatedAccount)
      setShowAddressForm(false)
      setAddressForm({ street: '', city: '', state: '', postalCode: '', country: '', type: 'SHIPPING' })
      setSuccess('Address added successfully!')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      setError(e.message)
    }
  }

  const handleDeleteAddress = async (addressId) => {
    if (!confirm('Delete this address?')) return
    setError('')
    try {
      const res = await fetch(`${API_BASE}/${account.id}/addresses/${addressId}`, {
        method: 'DELETE',
        headers: authHeaders(),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const updatedAccount = await fetchAccount(account.id)
      setAccount(updatedAccount)
      setSuccess('Address deleted!')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      setError(e.message)
    }
  }

  const handleAddPayment = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    try {
      const res = await fetch(`${API_BASE}/${account.id}/payment-methods`, {
        method: 'POST',
        headers: authHeaders(),
        body: JSON.stringify(paymentForm),
      })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `HTTP ${res.status}`)
      }
      const updatedAccount = await fetchAccount(account.id)
      setAccount(updatedAccount)
      setShowPaymentForm(false)
      setPaymentForm({ methodType: 'CREDIT_CARD', cardNumber: '', lastFour: '', expiryMonth: '', expiryYear: '', isDefault: false })
      setSuccess('Payment method added successfully!')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      setError(e.message)
    }
  }

  const handleDeletePayment = async (paymentId) => {
    if (!confirm('Delete this payment method?')) return
    setError('')
    try {
      const res = await fetch(`${API_BASE}/${account.id}/payment-methods/${paymentId}`, {
        method: 'DELETE',
        headers: authHeaders(),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const updatedAccount = await fetchAccount(account.id)
      setAccount(updatedAccount)
      setSuccess('Payment method deleted!')
      setTimeout(() => setSuccess(''), 3000)
    } catch (e) {
      setError(e.message)
    }
  }

  // ── Not logged in ──
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

  // ── Loading ──
  if (loading) {
    return <p className="text-center" style={{ marginTop: '3rem' }}>Loading account...</p>
  }

  // ── Create Account Profile ──
  if (showCreate && !account) {
    return (
      <div>
        <h1 className="page-title">✨ Complete Your Profile</h1>
        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <div className="card" style={{ padding: '2rem', maxWidth: 700, margin: '0 auto' }}>
          <p style={{ color: '#64748b', marginBottom: '1.5rem', fontSize: '.95rem' }}>
            Welcome <strong>{user.username}</strong>! Complete your profile to start shopping.
          </p>
          <form onSubmit={handleCreate}>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: '#1e293b' }}>👤 Personal Information</h3>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>Username *</label>
                <input
                  value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                  required
                />
                <small style={{ color: '#94a3b8' }}>Pre-filled from your login</small>
              </div>

              <div className="form-group">
                <label>Email *</label>
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  required
                  placeholder="you@example.com"
                />
              </div>

              <div className="form-group">
                <label>First Name</label>
                <input
                  value={form.firstName}
                  onChange={(e) => setForm({ ...form, firstName: e.target.value })}
                  placeholder="John"
                />
              </div>
              
              <div className="form-group">
                <label>Last Name</label>
                <input
                  value={form.lastName}
                  onChange={(e) => setForm({ ...form, lastName: e.target.value })}
                  placeholder="Doe"
                />
              </div>

              <div className="form-group">
                <label>Phone</label>
                <input
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                  placeholder="555-0123"
                />
              </div>
            </div>

            <hr style={{ margin: '1.5rem 0', border: 'none', borderTop: '1px solid #e5e7eb' }} />

            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: '#1e293b' }}>📍 Shipping Address (Optional)</h3>
            <p style={{ color: '#94a3b8', fontSize: '.85rem', marginBottom: '1rem' }}>
              Add your address now for faster checkout later
            </p>

            <div className="form-group">
              <label>Street Address</label>
              <input
                value={form.street}
                onChange={(e) => setForm({ ...form, street: e.target.value })}
                placeholder="123 Main St, Apt 4B"
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>City</label>
                <input
                  value={form.city}
                  onChange={(e) => setForm({ ...form, city: e.target.value })}
                  placeholder="New York"
                />
              </div>
              
              <div className="form-group">
                <label>State</label>
                <input
                  value={form.state}
                  onChange={(e) => setForm({ ...form, state: e.target.value })}
                  placeholder="NY"
                />
              </div>

              <div className="form-group">
                <label>Zip Code</label>
                <input
                  value={form.postalCode}
                  onChange={(e) => setForm({ ...form, postalCode: e.target.value })}
                  placeholder="10001"
                />
              </div>
            </div>

            <div className="form-group">
              <label>Country</label>
              <input
                value={form.country}
                onChange={(e) => setForm({ ...form, country: e.target.value })}
                placeholder="United States"
              />
            </div>

            <button type="submit" className="btn btn-success" style={{ width: '100%', marginTop: '1rem' }}>
              ✅ Complete Profile
            </button>
          </form>
        </div>
      </div>
    )
  }

  // ── Tab styles ──
  const tabStyle = (t) => ({
    padding: '.6rem 1.2rem',
    border: 'none',
    borderBottom: tab === t ? '3px solid #3b82f6' : '3px solid transparent',
    background: 'none',
    cursor: 'pointer',
    fontWeight: tab === t ? 'bold' : 'normal',
    color: tab === t ? '#3b82f6' : '#64748b',
    fontSize: '.95rem',
  })

  return (
    <div>
      <h1 className="page-title">👤 My Account</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* Account Header */}
      {account && (
        <div className="card" style={{ padding: '1.5rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
          <div style={{
            width: 64, height: 64, borderRadius: '50%', background: 'linear-gradient(135deg, #3b82f6, #8b5cf6)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '1.5rem', fontWeight: 'bold',
            flexShrink: 0
          }}>
            {(account.firstName?.[0] || account.username?.[0] || '?').toUpperCase()}
          </div>
          <div style={{ flex: 1 }}>
            <h2 style={{ margin: 0, fontSize: '1.3rem' }}>
              {account.firstName && account.lastName
                ? `${account.firstName} ${account.lastName}`
                : account.username}
            </h2>
            <p style={{ margin: '.2rem 0 0', color: '#64748b', fontSize: '.9rem' }}>
              @{account.username} · {account.email}
            </p>
            <p style={{ margin: '.2rem 0 0', color: '#94a3b8', fontSize: '.8rem' }}>
              Account #{account.id} · Member since {account.createdAt ? new Date(account.createdAt).toLocaleDateString() : '—'}
            </p>
          </div>
        </div>
      )}

      {/* Tabs */}
      {account && (
        <div style={{ borderBottom: '1px solid #e5e7eb', marginBottom: '1.5rem', display: 'flex' }}>
          <button style={tabStyle('profile')} onClick={() => { setTab('profile'); setEditing(false) }}>📝 Profile</button>
          <button style={tabStyle('addresses')} onClick={() => setTab('addresses')}>📍 Addresses</button>
          <button style={tabStyle('payment')} onClick={() => setTab('payment')}>💳 Payment Methods</button>
        </div>
      )}

      {/* ── PROFILE TAB (view) ── */}
      {account && tab === 'profile' && !editing && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <div className="flex-between" style={{ marginBottom: '1.2rem' }}>
            <h3 style={{ margin: 0 }}>Profile Information</h3>
            <button className="btn btn-primary btn-sm" onClick={startEditing}>✏️ Edit Profile</button>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.2rem' }}>
            <ProfileField label="Username" value={account.username} />
            <ProfileField label="Email" value={account.email} />
            <ProfileField label="First Name" value={account.firstName} />
            <ProfileField label="Last Name" value={account.lastName} />
            <ProfileField label="Phone" value={account.phone} />
            <ProfileField label="Last Updated" value={account.updatedAt ? new Date(account.updatedAt).toLocaleString() : '—'} />
          </div>
        </div>
      )}

      {/* ── PROFILE TAB (edit) ── */}
      {account && tab === 'profile' && editing && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <h3 style={{ margin: '0 0 1.2rem' }}>✏️ Edit Profile</h3>
          <form onSubmit={handleUpdate}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div className="form-group">
                <label>Username</label>
                <input value={editForm.username} onChange={(e) => setEditForm({ ...editForm, username: e.target.value })} required />
                <small style={{ color: '#94a3b8' }}>Changing username updates your display name</small>
              </div>
              <div className="form-group">
                <label>Email</label>
                <input type="email" value={editForm.email} onChange={(e) => setEditForm({ ...editForm, email: e.target.value })} required />
              </div>
              <div className="form-group">
                <label>First Name</label>
                <input value={editForm.firstName} onChange={(e) => setEditForm({ ...editForm, firstName: e.target.value })} placeholder="John" />
              </div>
              <div className="form-group">
                <label>Last Name</label>
                <input value={editForm.lastName} onChange={(e) => setEditForm({ ...editForm, lastName: e.target.value })} placeholder="Doe" />
              </div>
              <div className="form-group">
                <label>Phone</label>
                <input value={editForm.phone} onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })} placeholder="555-0123" />
              </div>
            </div>
            <div style={{ display: 'flex', gap: '.8rem', marginTop: '.5rem' }}>
              <button type="submit" className="btn btn-success">💾 Save Changes</button>
              <button type="button" className="btn" style={{ background: '#f1f5f9' }} onClick={() => setEditing(false)}>Cancel</button>
            </div>
          </form>
        </div>
      )}

      {/* ── ADDRESSES TAB ── */}
      {account && tab === 'addresses' && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <div className="flex-between" style={{ marginBottom: '1rem' }}>
            <h3 style={{ margin: 0 }}>📍 Addresses</h3>
            <button className="btn btn-primary btn-sm" onClick={() => setShowAddressForm(!showAddressForm)}>
              {showAddressForm ? '✖ Cancel' : '➕ Add Address'}
            </button>
          </div>

          {showAddressForm && (
            <div style={{ padding: '1.5rem', marginBottom: '1.5rem', borderRadius: '8px', background: '#f8fafc', border: '1px solid #e5e7eb' }}>
              <h4 style={{ margin: '0 0 1rem', fontSize: '1rem' }}>Add New Address</h4>
              <form onSubmit={handleAddAddress}>
                <div className="form-group">
                  <label>Address Type</label>
                  <select value={addressForm.type} onChange={(e) => setAddressForm({ ...addressForm, type: e.target.value })}>
                    <option value="SHIPPING">Shipping</option>
                    <option value="BILLING">Billing</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Street Address *</label>
                  <input
                    value={addressForm.street}
                    onChange={(e) => setAddressForm({ ...addressForm, street: e.target.value })}
                    placeholder="123 Main St, Apt 4B"
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '1rem' }}>
                  <div className="form-group">
                    <label>City *</label>
                    <input
                      value={addressForm.city}
                      onChange={(e) => setAddressForm({ ...addressForm, city: e.target.value })}
                      placeholder="New York"
                      required
                    />
                  </div>
                  
                  <div className="form-group">
                    <label>State *</label>
                    <input
                      value={addressForm.state}
                      onChange={(e) => setAddressForm({ ...addressForm, state: e.target.value })}
                      placeholder="NY"
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>Zip Code *</label>
                    <input
                      value={addressForm.postalCode}
                      onChange={(e) => setAddressForm({ ...addressForm, postalCode: e.target.value })}
                      placeholder="10001"
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Country *</label>
                  <input
                    value={addressForm.country}
                    onChange={(e) => setAddressForm({ ...addressForm, country: e.target.value })}
                    placeholder="United States"
                    required
                  />
                </div>

                <button type="submit" className="btn btn-success" style={{ width: '100%' }}>
                  ✅ Add Address
                </button>
              </form>
            </div>
          )}

          {(!account.addresses || account.addresses.length === 0) ? (
            <p style={{ color: '#94a3b8', textAlign: 'center', padding: '2rem 0' }}>
              No addresses saved yet. Click "Add Address" to create one.
            </p>
          ) : (
            account.addresses.map((addr) => (
              <div key={addr.id} style={{
                padding: '1rem', marginBottom: '.8rem', borderRadius: '8px',
                border: '1px solid #e5e7eb', background: '#fafafa', display: 'flex', justifyContent: 'space-between', alignItems: 'start'
              }}>
                <div>
                  <span style={{
                    display: 'inline-block', padding: '.15rem .5rem', borderRadius: '4px',
                    fontSize: '.75rem', fontWeight: 'bold', marginBottom: '.4rem',
                    background: addr.type === 'SHIPPING' ? '#dbeafe' : '#fef3c7',
                    color: addr.type === 'SHIPPING' ? '#1d4ed8' : '#92400e',
                  }}>{addr.type}</span>
                  <p style={{ margin: '.3rem 0 0', fontSize: '.95rem' }}>
                    {addr.street}<br />
                    {addr.city}, {addr.state} {addr.postalCode}<br />
                    {addr.country}
                  </p>
                </div>
                <button 
                  className="btn btn-sm" 
                  style={{ background: '#fee2e2', color: '#991b1b' }}
                  onClick={() => handleDeleteAddress(addr.id)}
                >
                  🗑️ Delete
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {/* ── PAYMENT METHODS TAB ── */}
      {account && tab === 'payment' && (
        <div className="card" style={{ padding: '1.5rem' }}>
          <div className="flex-between" style={{ marginBottom: '1rem' }}>
            <h3 style={{ margin: 0 }}>💳 Payment Methods</h3>
            <button className="btn btn-primary btn-sm" onClick={() => setShowPaymentForm(!showPaymentForm)}>
              {showPaymentForm ? '✖ Cancel' : '➕ Add Payment Method'}
            </button>
          </div>

          {showPaymentForm && (
            <div style={{ padding: '1.5rem', marginBottom: '1.5rem', borderRadius: '8px', background: '#f8fafc', border: '1px solid #e5e7eb' }}>
              <h4 style={{ margin: '0 0 1rem', fontSize: '1rem' }}>Add New Payment Method</h4>
              <form onSubmit={handleAddPayment}>
                <div className="form-group">
                  <label>Payment Type</label>
                  <select value={paymentForm.methodType} onChange={(e) => setPaymentForm({ ...paymentForm, methodType: e.target.value })}>
                    <option value="CREDIT_CARD">Credit Card</option>
                    <option value="DEBIT_CARD">Debit Card</option>
                    <option value="PAYPAL">PayPal</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Card Number *</label>
                  <input
                    value={paymentForm.cardNumber}
                    onChange={(e) => {
                      const val = e.target.value
                      setPaymentForm({ 
                        ...paymentForm, 
                        cardNumber: val,
                        lastFour: val.slice(-4)
                      })
                    }}
                    placeholder="1234 5678 9012 3456"
                    maxLength="19"
                    required
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div className="form-group">
                    <label>Expiry Month *</label>
                    <input
                      type="number"
                      min="1"
                      max="12"
                      value={paymentForm.expiryMonth}
                      onChange={(e) => setPaymentForm({ ...paymentForm, expiryMonth: e.target.value })}
                      placeholder="MM"
                      required
                    />
                  </div>
                  
                  <div className="form-group">
                    <label>Expiry Year *</label>
                    <input
                      type="number"
                      min="2026"
                      max="2050"
                      value={paymentForm.expiryYear}
                      onChange={(e) => setPaymentForm({ ...paymentForm, expiryYear: e.target.value })}
                      placeholder="YYYY"
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label style={{ display: 'flex', alignItems: 'center', gap: '.5rem', cursor: 'pointer' }}>
                    <input
                      type="checkbox"
                      checked={paymentForm.isDefault}
                      onChange={(e) => setPaymentForm({ ...paymentForm, isDefault: e.target.checked })}
                    />
                    Set as default payment method
                  </label>
                </div>

                <button type="submit" className="btn btn-success" style={{ width: '100%' }}>
                  ✅ Add Payment Method
                </button>
              </form>
            </div>
          )}

          {(!account.paymentMethods || account.paymentMethods.length === 0) ? (
            <p style={{ color: '#94a3b8', textAlign: 'center', padding: '2rem 0' }}>
              No payment methods saved yet. Click "Add Payment Method" to create one.
            </p>
          ) : (
            account.paymentMethods.map((pm) => (
              <div key={pm.id} style={{
                padding: '1rem', marginBottom: '.8rem', borderRadius: '8px',
                border: '1px solid #e5e7eb', background: '#fafafa',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center'
              }}>
                <div>
                  <span style={{
                    display: 'inline-block', padding: '.15rem .5rem', borderRadius: '4px',
                    fontSize: '.75rem', fontWeight: 'bold', background: '#dcfce7', color: '#166534',
                  }}>{pm.methodType}</span>
                  <span style={{ marginLeft: '.8rem', fontSize: '.95rem' }}>•••• {pm.lastFour}</span>
                </div>
                <div style={{ fontSize: '.85rem', color: '#64748b', display: 'flex', alignItems: 'center', gap: '.8rem' }}>
                  <span>Exp: {pm.expiryMonth}/{pm.expiryYear}</span>
                  {pm.isDefault && (
                    <span style={{
                      padding: '.15rem .5rem', borderRadius: '4px', fontSize: '.7rem',
                      fontWeight: 'bold', background: '#fef3c7', color: '#92400e',
                    }}>DEFAULT</span>
                  )}
                  <button 
                    className="btn btn-sm" 
                    style={{ background: '#fee2e2', color: '#991b1b', marginLeft: '.5rem' }}
                    onClick={() => handleDeletePayment(pm.id)}
                  >
                    🗑️
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  )
}

function ProfileField({ label, value }) {
  return (
    <div>
      <div style={{ fontSize: '.8rem', color: '#64748b', fontWeight: 600, marginBottom: '.2rem' }}>{label}</div>
      <div style={{ fontSize: '1rem' }}>{value || <span style={{ color: '#cbd5e1' }}>—</span>}</div>
    </div>
  )
}
