import { Link } from 'react-router-dom'
import { getToken } from '../services/api'

const SAMPLE_IMAGES = [
  'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400',
  'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400',
  'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400',
]

export default function Home() {
  const token = getToken()

  return (
    <>
      <div className="hero">
        <h1>Welcome to ShopZone 🛒</h1>
        <p>Your one-stop microservices-powered shopping platform</p>
        <Link to="/items" className="btn btn-primary" style={{ padding: '.7rem 2rem', fontSize: '1rem' }}>
          Browse Products →
        </Link>
      </div>

      <h2 className="page-title">Featured Products</h2>
      <div className="grid">
        {['Smart Watch', 'Headphones', 'Camera Lens'].map((name, i) => (
          <div key={i} className="card">
            <img src={SAMPLE_IMAGES[i]} alt={name} />
            <div className="card-body">
              <h3>{name}</h3>
              <p className="price">${(49.99 + i * 50).toFixed(2)}</p>
              <p className="stock">In Stock</p>
              <Link to={token ? '/items' : '/login'} className="btn btn-primary btn-sm mt-1">
                {token ? 'Shop Now' : 'Login to Shop'}
              </Link>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-2" style={{ background: 'white', borderRadius: '12px', padding: '2rem', marginTop: '2rem' }}>
        <h2 className="page-title">Microservices Architecture</h2>
        <div className="grid" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))' }}>
          {[
            { icon: '🔐', name: 'Auth Service', port: '8085', desc: 'JWT Authentication' },
            { icon: '👤', name: 'Account Service', port: '8081', desc: 'User Management' },
            { icon: '📦', name: 'Item Service', port: '8082', desc: 'Product Catalog' },
            { icon: '🛒', name: 'Order Service', port: '8083', desc: 'Orders & Cart' },
            { icon: '💳', name: 'Payment Service', port: '8084', desc: 'Payments' },
          ].map((svc, i) => (
            <div key={i} style={{ textAlign: 'center', padding: '1rem' }}>
              <div style={{ fontSize: '2rem' }}>{svc.icon}</div>
              <h4 style={{ margin: '.3rem 0' }}>{svc.name}</h4>
              <span className="badge badge-blue">:{svc.port}</span>
              <p style={{ fontSize: '.8rem', color: '#64748b', marginTop: '.3rem' }}>{svc.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
