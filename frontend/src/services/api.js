const API = {
  auth:     '/api/auth',
  accounts: '/api/accounts',
  items:    '/api/items',
  orders:   '/api/orders',
  payments: '/api/payments',
};

function getToken() {
  return localStorage.getItem('token');
}

function setToken(token) {
  localStorage.setItem('token', token);
}

function clearToken() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
}

function getUser() {
  const u = localStorage.getItem('user');
  return u ? JSON.parse(u) : null;
}

function setUser(user) {
  localStorage.setItem('user', JSON.stringify(user));
}

async function request(url, options = {}) {
  const token = getToken();
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(url, { ...options, headers });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `HTTP ${res.status}`);
  }
  const ct = res.headers.get('content-type');
  if (ct && ct.includes('application/json')) return res.json();
  return res.text();
}

// Auth
export const register = (data) => request(`${API.auth}/register`, { method: 'POST', body: JSON.stringify(data) });
export const login = (data) => request(`${API.auth}/login`, { method: 'POST', body: JSON.stringify(data) });

// Items
export const getItems = () => request(`${API.items}`);
export const getItem = (id) => request(`${API.items}/${id}`);
export const createItem = (data) => request(`${API.items}`, { method: 'POST', body: JSON.stringify(data) });

// Orders
export const getOrders = () => request(`${API.orders}`);
export const createOrder = (data) => request(`${API.orders}`, { method: 'POST', body: JSON.stringify(data) });
export const confirmOrder = (id) => request(`${API.orders}/${id}/confirm`, { method: 'POST' });
export const markPaid = (id) => request(`${API.orders}/${id}/paid`, { method: 'POST' });
export const completeOrder = (id) => request(`${API.orders}/${id}/complete`, { method: 'POST' });
export const cancelOrder = (id) => request(`${API.orders}/${id}/cancel`, { method: 'POST' });

// Cart
export const getCart = (userId) => request(`${API.orders}/carts/${userId}`);
export const addToCart = (userId, data) => request(`${API.orders}/carts/${userId}/items`, { method: 'POST', body: JSON.stringify(data) });
export const checkout = (userId) => request(`${API.orders}/carts/${userId}/checkout`, { method: 'POST' });

// Payments
export const createPayment = (data) => request(`${API.payments}`, { method: 'POST', body: JSON.stringify(data) });
export const getPayment = (id) => request(`${API.payments}/${id}`);

export { getToken, setToken, clearToken, getUser, setUser };
