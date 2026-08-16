// API base URL — uses Vite proxy in dev, direct URL in prod
const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

// ─── Auth ─────────────────────────────────────────────────────────────────────
export const authApi = {
  register: (username, password, role = 'USER') =>
    request('POST', '/auth/register', { username, password, role }),

  login: (username, password) =>
    request('POST', '/auth/login', { username, password }),
};

// ─── Products ─────────────────────────────────────────────────────────────────
export const productApi = {
  getAll: (token) =>
    request('GET', '/products', null, token),

  getById: (id, token) =>
    request('GET', `/products/${id}`, null, token),

  create: (product, token) =>
    request('POST', '/products', product, token),

  update: (id, product, token) =>
    request('PUT', `/products/${id}`, product, token),

  delete: (id, token) =>
    request('DELETE', `/products/${id}`, null, token),
};

// ─── Orders ───────────────────────────────────────────────────────────────────
export const orderApi = {
  getAll: (token) =>
    request('GET', '/orders', null, token),

  getByUser: (username, token) =>
    request('GET', `/orders/user/${username}`, null, token),

  checkout: (orderData, token) =>
    request('POST', '/orders/checkout', orderData, token),
};

// ─── Core Fetcher ─────────────────────────────────────────────────────────────
async function request(method, path, body = null, token = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, options);
  const text = await res.text();

  let data;
  try { data = JSON.parse(text); } catch { data = text; }

  if (!res.ok) {
    const message = (typeof data === 'object' && data?.message) || text || `HTTP ${res.status}`;
    throw new Error(message);
  }

  return data;
}
