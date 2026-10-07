const BASE = '/api'

async function request(url, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }

  const res = await fetch(`${BASE}${url}`, { ...options, headers, credentials: 'include' })
  let data
  try {
    data = JSON.parse(await res.text())
  } catch {
    data = { code: 500, message: `请求失败 (${res.status})` }
  }
  if (data.code === 401 && !url.startsWith('/auth/')) {
    localStorage.removeItem('user')
    if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }
  if (data.code !== 200) throw new Error(data.message || '请求失败')
  return data
}

export const api = {
  // Auth
  login: (body) => request('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => request('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  logout: () => request('/auth/logout', { method: 'POST' }),

  // Categories
  getCategories: () => request('/categories'),
  createCategory: (body) => request('/categories', { method: 'POST', body: JSON.stringify(body) }),
  updateCategory: (id, body) => request(`/categories/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteCategory: (id) => request(`/categories/${id}`, { method: 'DELETE' }),

  // Products
  getProducts: (params = '') => request(`/products${params}`),
  getProduct: (id) => request(`/products/${id}`),
  getProductsByCategory: (id) => request(`/products/category/${id}`),
  searchProducts: (keyword) => request(`/products/search?keyword=${encodeURIComponent(keyword)}`),
  createProduct: (body) => request('/products', { method: 'POST', body: JSON.stringify(body) }),
  updateProduct: (id, body) => request(`/products/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteProduct: (id) => request(`/products/${id}`, { method: 'DELETE' }),

  // Users
  getUsers: () => request('/users'),
  getUser: (id) => request(`/users/${id}`),
  createUser: (body) => request('/users', { method: 'POST', body: JSON.stringify(body) }),
  updateUser: (id, body) => request(`/users/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  deleteUser: (id) => request(`/users/${id}`, { method: 'DELETE' }),

  // Cart
  getCart: (userId) => request(`/cart/${userId}`),
  addCartItem: (userId, body) => request(`/cart/${userId}/items`, { method: 'POST', body: JSON.stringify(body) }),
  updateCartItem: (userId, itemId, quantity) => request(`/cart/${userId}/items/${itemId}?quantity=${quantity}`, { method: 'PUT' }),
  removeCartItem: (userId, itemId) => request(`/cart/${userId}/items/${itemId}`, { method: 'DELETE' }),
  clearCart: (userId) => request(`/cart/${userId}`, { method: 'DELETE' }),

  // Orders
  getAllOrders: (params = '') => request(`/orders${params}`),
  getOrders: () => request('/orders'),
  getOrder: (id) => request(`/orders/${id}`),
  createOrder: () => request('/orders/from-cart', { method: 'POST' }),
  cancelOrder: (id) => request(`/orders/${id}/cancel`, { method: 'POST' }),
  updateOrderStatus: (id, status) => request(`/orders/${id}/status?status=${status}`, { method: 'PUT' }),
}
