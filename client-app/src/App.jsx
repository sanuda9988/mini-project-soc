import { useState, useEffect, useCallback } from 'react';
import { authApi, productApi, orderApi } from './api.js';

// ─────────────────────────────────────────────────────────────────────────────
// Toast Hook
// ─────────────────────────────────────────────────────────────────────────────
function useToast() {
  const [toasts, setToasts] = useState([]);

  const addToast = useCallback((msg, type = 'default') => {
    const id = Date.now();
    setToasts(t => [...t, { id, msg, type }]);
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 3500);
  }, []);

  return { toasts, addToast };
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth Context (simple global state via prop drilling)
// ─────────────────────────────────────────────────────────────────────────────
function useAuth() {
  const [auth, setAuth] = useState(() => {
    try {
      const stored = localStorage.getItem('soc_auth');
      return stored ? JSON.parse(stored) : null;
    } catch { return null; }
  });

  const login = (data) => {
    localStorage.setItem('soc_auth', JSON.stringify(data));
    setAuth(data);
  };

  const logout = () => {
    localStorage.removeItem('soc_auth');
    setAuth(null);
  };

  return { auth, login, logout };
}

// ─────────────────────────────────────────────────────────────────────────────
// App Root
// ─────────────────────────────────────────────────────────────────────────────
export default function App() {
  const { auth, login, logout } = useAuth();
  const { toasts, addToast } = useToast();
  const [page, setPage] = useState('products');

  useEffect(() => {
    if (!auth) setPage('products');
  }, [auth]);

  const handleLogout = () => {
    logout();
    addToast('Logged out successfully', 'default');
    setPage('products');
  };

  return (
    <div className="layout">
      <Navbar auth={auth} page={page} setPage={setPage} onLogout={handleLogout} />

      <main>
        {!auth ? (
          page === 'auth' ? (
            <AuthPage onLogin={login} addToast={addToast} />
          ) : (
            <ProductsPage auth={auth} addToast={addToast} setPage={setPage} />
          )
        ) : (
          <>
            {page === 'products' && <ProductsPage auth={auth} addToast={addToast} setPage={setPage} />}
            {page === 'orders' && <OrdersPage auth={auth} addToast={addToast} />}
          </>
        )}
      </main>

      {/* Toast Notifications */}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast--${t.type}`}>{t.msg}</div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Navbar
// ─────────────────────────────────────────────────────────────────────────────
function Navbar({ auth, page, setPage, onLogout }) {
  return (
    <nav className="navbar">
      <div className="container">
        <div className="navbar__inner">
          <a className="navbar__logo" href="#" onClick={(e) => { e.preventDefault(); setPage('products'); }}>
            <div className="navbar__logo-icon">🛍</div>
            ShopFlow
          </a>

          <div className="navbar__nav">
            <button
              id="nav-products"
              className={`nav-btn nav-btn--ghost ${page === 'products' ? 'active' : ''}`}
              onClick={() => setPage('products')}
            >
              🏪 Products
            </button>

            {auth && (
              <button
                id="nav-orders"
                className={`nav-btn nav-btn--ghost ${page === 'orders' ? 'active' : ''}`}
                onClick={() => setPage('orders')}
              >
                📦 My Orders
              </button>
            )}
          </div>

          <div className="navbar__user">
            {auth ? (
              <>
                <div className="user-badge">
                  <div className="user-badge__avatar">{auth.username[0].toUpperCase()}</div>
                  {auth.username}
                  <span style={{ opacity: 0.6, fontSize: '0.75rem' }}>({auth.role})</span>
                </div>
                <button id="nav-logout" className="nav-btn nav-btn--ghost" onClick={onLogout}>
                  Sign out
                </button>
              </>
            ) : (
              <button id="nav-signin" className="nav-btn nav-btn--primary" onClick={() => setPage('auth')}>
                Sign in
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Auth Page (Login / Register)
// ─────────────────────────────────────────────────────────────────────────────
function AuthPage({ onLogin, addToast }) {
  const [tab, setTab] = useState('login');
  const [form, setForm] = useState({ username: '', password: '', role: 'USER' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      if (tab === 'login') {
        const data = await authApi.login(form.username, form.password);
        onLogin(data);
        addToast(`Welcome back, ${data.username}! 👋`, 'success');
      } else {
        await authApi.register(form.username, form.password, form.role);
        addToast('Account created! Please sign in.', 'success');
        setTab('login');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-card__logo">
          <h1>🛍 ShopFlow</h1>
          <p>Microservices E-Commerce Platform</p>
        </div>

        <div className="card">
          <div className="card__body">
            <div className="auth-tabs">
              <button
                id="tab-login"
                className={`auth-tab ${tab === 'login' ? 'active' : ''}`}
                onClick={() => { setTab('login'); setError(''); }}
              >
                Sign In
              </button>
              <button
                id="tab-register"
                className={`auth-tab ${tab === 'register' ? 'active' : ''}`}
                onClick={() => { setTab('register'); setError(''); }}
              >
                Register
              </button>
            </div>

            {error && <div className="alert alert--error">⚠️ {error}</div>}

            <form id="auth-form" onSubmit={handleSubmit}>
              <div className="form-group">
                <label className="form-label" htmlFor="username">Username</label>
                <input
                  id="username"
                  name="username"
                  type="text"
                  className="form-input"
                  placeholder="Enter your username"
                  value={form.username}
                  onChange={handleChange}
                  required
                  autoComplete="username"
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">Password</label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  className="form-input"
                  placeholder="Enter your password"
                  value={form.password}
                  onChange={handleChange}
                  required
                  autoComplete={tab === 'login' ? 'current-password' : 'new-password'}
                />
              </div>

              {tab === 'register' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="role">Role</label>
                  <select id="role" name="role" className="form-input" value={form.role} onChange={handleChange}>
                    <option value="USER">User</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
              )}

              <button id="auth-submit" type="submit" className="btn btn--primary" disabled={loading}>
                {loading ? '⏳ Please wait...' : tab === 'login' ? '🔑 Sign In' : '✨ Create Account'}
              </button>
            </form>

            <p className="text-muted mt-4" style={{ textAlign: 'center', fontSize: '0.8rem' }}>
              {tab === 'login'
                ? 'Auth requests go via API Gateway → Auth Service (JWT)'
                : 'Credentials stored with SHA-256 hashing'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Products Page
// ─────────────────────────────────────────────────────────────────────────────
const PRODUCT_EMOJIS = ['📱', '💻', '🎧', '📷', '🖥️', '⌨️', '🖱️', '🎮', '📺', '⌚'];

function ProductsPage({ auth, addToast, setPage }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [checkoutProduct, setCheckoutProduct] = useState(null);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await productApi.getAll(auth?.token);
      setProducts(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [auth?.token]);

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  const handleCheckout = (product) => {
    if (!auth) {
      addToast('Please sign in to purchase products', 'error');
      setPage('auth');
      return;
    }
    setCheckoutProduct(product);
  };

  return (
    <div className="page">
      <div className="container">
        <div className="flex-between page__header">
          <div>
            <h1 className="page__title">Product Catalogue</h1>
            <p className="page__subtitle">Browse available items — served by the Product Microservice</p>
          </div>
          <button id="refresh-products" className="btn btn--secondary btn--sm" onClick={fetchProducts}>
            🔄 Refresh
          </button>
        </div>

        {loading && (
          <div className="loading-center">
            <div className="spinner" />
            <span>Loading products from service...</span>
          </div>
        )}

        {error && !loading && (
          <div className="alert alert--error">
            <span>⚠️</span>
            <div>
              <strong>Could not load products:</strong> {error}
              <br /><small>Ensure the API Gateway and Product Service are running.</small>
            </div>
          </div>
        )}

        {!loading && !error && products.length === 0 && (
          <div className="empty-state">
            <div className="empty-state__icon">🏪</div>
            <div className="empty-state__title">No Products Found</div>
            <div className="empty-state__text">The product catalogue is empty. Seed some data via the API.</div>
          </div>
        )}

        {!loading && products.length > 0 && (
          <div className="products-grid">
            {products.map((p, i) => (
              <ProductCard
                key={p.id}
                product={p}
                emoji={PRODUCT_EMOJIS[i % PRODUCT_EMOJIS.length]}
                onBuy={handleCheckout}
                isLoggedIn={!!auth}
              />
            ))}
          </div>
        )}
      </div>

      {checkoutProduct && (
        <CheckoutModal
          product={checkoutProduct}
          auth={auth}
          onClose={() => setCheckoutProduct(null)}
          addToast={addToast}
          onSuccess={() => { setCheckoutProduct(null); setPage('orders'); }}
        />
      )}
    </div>
  );
}

function ProductCard({ product, emoji, onBuy, isLoggedIn }) {
  const stockStatus = product.stock > 20 ? 'success'
    : product.stock > 5 ? 'warning'
    : product.stock > 0 ? 'danger' : 'default';

  const stockLabel = product.stock > 20 ? 'In Stock'
    : product.stock > 5 ? 'Low Stock'
    : product.stock > 0 ? `Only ${product.stock} left`
    : 'Out of Stock';

  return (
    <div className="product-card">
      <div className="product-card__img">{emoji}</div>
      <div className="product-card__body">
        <div className="flex-between mb-4">
          <h3 className="product-card__name">{product.name}</h3>
          <span className={`badge badge--${stockStatus}`}>{stockLabel}</span>
        </div>
        <p className="product-card__desc">{product.description || 'Premium quality product available for purchase.'}</p>
        <div className="product-card__footer">
          <div>
            <div className="product-card__price">${Number(product.price).toFixed(2)}</div>
            <div className="product-card__stock">Stock: {product.stock}</div>
          </div>
          <button
            id={`buy-${product.id}`}
            className="btn btn--primary btn--sm"
            disabled={product.stock <= 0}
            onClick={() => onBuy(product)}
          >
            {isLoggedIn ? '🛒 Buy' : '🔑 Login to Buy'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Checkout Modal
// ─────────────────────────────────────────────────────────────────────────────
function CheckoutModal({ product, auth, onClose, addToast, onSuccess }) {
  const [form, setForm] = useState({
    quantity: 1,
    cardNumber: '4000-1234-5678-9010',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const totalAmount = (Number(product.price) * form.quantity).toFixed(2);

  const handleChange = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }));
    setError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const result = await orderApi.checkout({
        username: auth.username,
        productId: product.id,
        quantity: Number(form.quantity),
        cardNumber: form.cardNumber,
      }, auth.token);

      const status = result.paymentStatus;
      addToast(
        status === 'PAID'
          ? `✅ Order placed! Payment successful. Tx: ${result.transactionId}`
          : `⚠️ Order placed but payment ${status}. Check your order history.`,
        status === 'PAID' ? 'success' : 'error'
      );
      onSuccess();
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        <div className="modal__header">
          <div className="modal__title">🛒 Checkout</div>
        </div>
        <div className="modal__body">
          {error && <div className="alert alert--error">⚠️ {error}</div>}

          <div className="order-summary">
            <div className="order-summary__row">
              <span className="text-muted">Product</span>
              <strong>{product.name}</strong>
            </div>
            <div className="order-summary__row">
              <span className="text-muted">Unit Price</span>
              <span>${Number(product.price).toFixed(2)}</span>
            </div>
            <div className="order-summary__row">
              <span className="text-muted">Quantity</span>
              <strong>{form.quantity}</strong>
            </div>
            <div className="order-summary__row">
              <span className="text-muted">Total</span>
              <span className="order-summary__total">${totalAmount}</span>
            </div>
          </div>

          <form id="checkout-form" onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="checkout-qty">Quantity</label>
              <input
                id="checkout-qty"
                name="quantity"
                type="number"
                min="1"
                max={product.stock}
                className="form-input"
                value={form.quantity}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="checkout-card">Card Number</label>
              <input
                id="checkout-card"
                name="cardNumber"
                type="text"
                className="form-input"
                placeholder="4000-1234-5678-9010"
                value={form.cardNumber}
                onChange={handleChange}
                required
              />
              <p className="text-muted" style={{ marginTop: '4px', fontSize: '0.75rem' }}>
                Mock payment — use any valid-looking card number
              </p>
            </div>
          </form>
        </div>
        <div className="modal__footer">
          <button id="checkout-cancel" className="btn btn--secondary" onClick={onClose} disabled={loading}>
            Cancel
          </button>
          <button
            id="checkout-submit"
            type="submit"
            form="checkout-form"
            className="btn btn--primary"
            disabled={loading}
          >
            {loading ? '⏳ Processing...' : `💳 Pay $${totalAmount}`}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Orders Page
// ─────────────────────────────────────────────────────────────────────────────
function OrdersPage({ auth, addToast }) {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      // Admins see all orders; regular users see only their own
      const data = auth.role === 'ADMIN'
        ? await orderApi.getAll(auth.token)
        : await orderApi.getByUser(auth.username, auth.token);
      setOrders(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [auth]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  const statusBadge = (status) => {
    if (status === 'PAID') return <span className="badge badge--success">✅ Paid</span>;
    if (status === 'PENDING') return <span className="badge badge--warning">⏳ Pending</span>;
    return <span className="badge badge--danger">❌ {status}</span>;
  };

  return (
    <div className="page">
      <div className="container">
        <div className="flex-between page__header">
          <div>
            <h1 className="page__title">{auth.role === 'ADMIN' ? 'All Orders' : 'My Orders'}</h1>
            <p className="page__subtitle">
              {auth.role === 'ADMIN'
                ? 'Admin view — all orders across the platform'
                : `Order history for ${auth.username}`}
            </p>
          </div>
          <button id="refresh-orders" className="btn btn--secondary btn--sm" onClick={fetchOrders}>
            🔄 Refresh
          </button>
        </div>

        {loading && (
          <div className="loading-center">
            <div className="spinner" />
            <span>Fetching orders from service...</span>
          </div>
        )}

        {error && !loading && (
          <div className="alert alert--error">
            ⚠️ {error}
          </div>
        )}

        {!loading && !error && orders.length === 0 && (
          <div className="empty-state">
            <div className="empty-state__icon">📦</div>
            <div className="empty-state__title">No Orders Yet</div>
            <div className="empty-state__text">Your orders will appear here after checkout.</div>
          </div>
        )}

        {!loading && orders.length > 0 && (
          <div className="table-wrapper">
            <table id="orders-table">
              <thead>
                <tr>
                  <th>Order ID</th>
                  {auth.role === 'ADMIN' && <th>Customer</th>}
                  <th>Product</th>
                  <th>Qty</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {orders.map(order => (
                  <tr key={order.id}>
                    <td><strong>#{order.id}</strong></td>
                    {auth.role === 'ADMIN' && <td>{order.username}</td>}
                    <td>{order.productName}</td>
                    <td>{order.quantity}</td>
                    <td><strong>${Number(order.totalAmount).toFixed(2)}</strong></td>
                    <td>{statusBadge(order.status)}</td>
                    <td className="text-muted">{order.createdAt ? new Date(order.createdAt).toLocaleDateString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
