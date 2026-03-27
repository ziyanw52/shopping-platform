import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { getUser, clearToken, getToken } from './services/api'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import Items from './pages/Items'
import Cart from './pages/Cart'
import Orders from './pages/Orders'
import Payment from './pages/Payment'
import Account from './pages/Account'

function Navbar() {
  const [user, setUser] = useState(getUser())
  const token = getToken()

  useEffect(() => {
    const interval = setInterval(() => setUser(getUser()), 500)
    return () => clearInterval(interval)
  }, [])

  const handleLogout = () => {
    clearToken()
    setUser(null)
    window.location.href = '/'
  }

  return (
    <div className="navbar">
      <Link to="/" className="logo" style={{ color: 'white', textDecoration: 'none' }}>
        🛒 ShopZone
      </Link>
      <nav>
        <Link to="/">Home</Link>
        <Link to="/items">Products</Link>
        {token && <Link to="/cart">🛍 Cart</Link>}
        {token && <Link to="/orders">Orders</Link>}
        {token && <Link to="/payment">Payment</Link>}
        {token && <Link to="/account">👤 Account</Link>}
        {!token ? (
          <>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </>
        ) : (
          <>
            <span style={{ color: '#94a3b8', fontSize: '.85rem' }}>👤 {user?.username}</span>
            <button className="btn-logout" onClick={handleLogout}>Logout</button>
          </>
        )}
      </nav>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <div className="container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/items" element={<Items />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/payment" element={<Payment />} />
          <Route path="/account" element={<Account />} />
        </Routes>
      </div>
    </BrowserRouter>
  )
}
