const API_BASE = 'http://localhost:8080/api';

let currentUser = null;
let products = [];
let cart = [];

// --- Init ---
document.addEventListener('DOMContentLoaded', () => {
    fetchProducts();
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
        currentUser = JSON.parse(savedUser);
        updateUIForUser();
        fetchCart();
    }
});

// --- Auth Tab Switching ---
function switchTab(tab) {
    const isLogin = tab === 'login';
    document.getElementById('form-login').style.display = isLogin ? 'block' : 'none';
    document.getElementById('form-register').style.display = isLogin ? 'none' : 'block';
    document.getElementById('tab-login').classList.toggle('active', isLogin);
    document.getElementById('tab-register').classList.toggle('active', !isLogin);
}

// --- UI Helpers ---
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success' ? 'fa-check-circle' : 'fa-circle-exclamation';
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function showModal(id) {
    document.getElementById(id).classList.add('open');
    document.getElementById('modal-overlay').classList.add('open');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('open');
    document.getElementById('modal-overlay').classList.remove('open');
}

function closeAllModals() {
    document.querySelectorAll('.modal.open').forEach(m => m.classList.remove('open'));
    document.getElementById('modal-overlay').classList.remove('open');
}

function updateUIForUser() {
    document.getElementById('btn-login').style.display = 'none';
    document.getElementById('user-greeting').style.display = 'inline-block';
    document.getElementById('username-display').innerText = currentUser.fullName;
    document.getElementById('btn-logout').style.display = 'inline-flex';
    document.getElementById('btn-cart').style.display = 'flex';
    document.getElementById('btn-orders').style.display = 'inline-flex';

    // Show Admin button if admin
    if (currentUser.role === 'ROLE_ADMIN') {
        document.getElementById('btn-admin').style.display = 'inline-flex';
    }
}

function resetUI() {
    document.getElementById('btn-login').style.display = 'inline-flex';
    document.getElementById('user-greeting').style.display = 'none';
    document.getElementById('btn-logout').style.display = 'none';
    document.getElementById('btn-cart').style.display = 'none';
    document.getElementById('btn-orders').style.display = 'none';
    document.getElementById('btn-admin').style.display = 'none';
    document.getElementById('cart-count').innerText = '0';
}

// --- Auth ---
async function loginUser(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    const btn = document.getElementById('btn-submit-login');
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Logging in...';
    btn.disabled = true;

    try {
        const res = await fetch(`${API_BASE}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (res.ok) {
            currentUser = await res.json();
            localStorage.setItem('user', JSON.stringify(currentUser));
            showToast(`Welcome back, ${currentUser.fullName}!`);
            closeModal('auth-modal');
            updateUIForUser();
            fetchCart();
        } else {
            showToast('Invalid email or password', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    } finally {
        btn.innerHTML = '<i class="fa-solid fa-right-to-bracket"></i> Login';
        btn.disabled = false;
    }
}

async function registerUser(e) {
    e.preventDefault();
    const fullName = document.getElementById('reg-fullName').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    const btn = document.getElementById('btn-submit-register');
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Registering...';
    btn.disabled = true;

    try {
        const res = await fetch(`${API_BASE}/users/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, password })
        });

        if (res.ok) {
            currentUser = await res.json();
            localStorage.setItem('user', JSON.stringify(currentUser));
            showToast('Account created successfully!');
            closeModal('auth-modal');
            updateUIForUser();
            fetchCart();
        } else {
            const errText = await res.text();
            showToast(errText || 'Registration failed', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    } finally {
        btn.innerHTML = '<i class="fa-solid fa-user-plus"></i> Register & Login';
        btn.disabled = false;
    }
}

function logoutUser() {
    currentUser = null;
    localStorage.removeItem('user');
    cart = [];
    resetUI();
    renderCart();
    showToast('Logged out successfully');
}

// --- Products ---
async function fetchProducts(name = '') {
    const grid = document.getElementById('products-grid');
    grid.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const url = name ? `${API_BASE}/products?name=${encodeURIComponent(name)}` : `${API_BASE}/products`;
        const res = await fetch(url);
        products = await res.json();
        renderProducts(products);
    } catch (err) {
        grid.innerHTML = '<p style="text-align:center; color: var(--danger);">Failed to load products.</p>';
        showToast('Failed to connect to API Gateway', 'error');
    }
}

let searchTimeout = null;
function handleSearch(e) {
    const query = e.target.value;
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        fetchProducts(query);
    }, 400);
}

function getProductIcon(name) {
    const n = name.toLowerCase();
    if (n.includes('macbook') || n.includes('laptop')) return 'fa-laptop';
    if (n.includes('iphone') || n.includes('phone') || n.includes('samsung') || n.includes('galaxy')) return 'fa-mobile-screen';
    if (n.includes('watch')) return 'fa-clock';
    if (n.includes('headphone') || n.includes('airpod') || n.includes('sony') || n.includes('wh-')) return 'fa-headphones';
    if (n.includes('ipad') || n.includes('tablet')) return 'fa-tablet-screen-button';
    return 'fa-box-open';
}

function renderProducts(prods) {
    const grid = document.getElementById('products-grid');
    grid.innerHTML = '';
    if (prods.length === 0) {
        grid.innerHTML = '<p style="text-align:center; grid-column: 1/-1;">No products found.</p>';
        return;
    }
    prods.forEach(p => {
        const icon = getProductIcon(p.name);
        const outOfStock = p.stockQuantity <= 0;
        const card = document.createElement('div');
        card.className = 'product-card';
        card.innerHTML = `
            <div style="cursor: pointer;" onclick="showProductDetails(${p.id})">
                <div class="product-icon"><i class="fa-solid ${icon}"></i></div>
                <div class="product-info">
                    <h3 style="transition: color 0.2s;">${p.name}</h3>
                    <p style="margin-bottom: 0.5rem; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; height: 3.2em;">${p.description || 'Premium quality product.'}</p>
                    <span style="font-size: 0.85rem; color: var(--primary); font-weight: 500;">
                        <i class="fa-solid fa-circle-info"></i> View Details
                    </span>
                </div>
            </div>
            <div class="product-meta" style="margin-top: 1rem;">
                <span class="product-price">$${p.price.toFixed(2)}</span>
                <span class="stock ${outOfStock ? 'low' : ''}">${outOfStock ? 'Out of stock' : 'Stock: ' + p.stockQuantity}</span>
            </div>
            <button class="btn btn-primary btn-block" ${outOfStock ? 'disabled' : ''} onclick="addToCart(${p.id})">
                <i class="fa-solid fa-cart-plus"></i> Add to Cart
            </button>
        `;
        grid.appendChild(card);
    });
}

// --- Product Details ---
async function showProductDetails(id) {
    showModal('product-detail-modal');
    
    const nameEl = document.getElementById('detail-product-name');
    const descEl = document.getElementById('detail-product-desc');
    const priceEl = document.getElementById('detail-product-price');
    const stockEl = document.getElementById('detail-product-stock');
    const iconEl = document.getElementById('detail-product-icon');
    const btnContainer = document.getElementById('detail-add-to-cart-container');
    
    nameEl.innerText = 'Loading...';
    descEl.innerText = '';
    priceEl.innerText = '';
    stockEl.className = 'badge-stock';
    stockEl.innerText = '';
    btnContainer.innerHTML = '';
    
    try {
        const res = await fetch(`${API_BASE}/products/${id}`);
        if (!res.ok) throw new Error('Product not found');
        const p = await res.json();
        
        nameEl.innerText = p.name;
        descEl.innerText = p.description || 'No description available for this premium quality product.';
        priceEl.innerText = `$${p.price.toFixed(2)}`;
        
        const outOfStock = p.stockQuantity <= 0;
        stockEl.className = `badge-stock ${outOfStock ? 'low' : 'ok'}`;
        stockEl.innerText = outOfStock ? 'Out of Stock' : `Stock: ${p.stockQuantity} units`;
        
        const icon = getProductIcon(p.name);
        iconEl.className = `fa-solid ${icon}`;
        
        btnContainer.innerHTML = `
            <button class="btn btn-primary btn-block" ${outOfStock ? 'disabled' : ''} onclick="addToCart(${p.id}); closeModal('product-detail-modal');">
                <i class="fa-solid fa-cart-plus"></i> Add to Cart
            </button>
        `;
    } catch (err) {
        nameEl.innerText = 'Error';
        descEl.innerText = 'Failed to load product details.';
        showToast('Failed to load product details', 'error');
    }
}


// --- Cart ---
function toggleCart() {
    if (!currentUser) {
        showToast('Please login first', 'error');
        showModal('auth-modal');
        return;
    }
    const sidebar = document.getElementById('cart-sidebar');
    const overlay = document.getElementById('cart-overlay');
    sidebar.classList.toggle('open');
    overlay.classList.toggle('open');
    if (sidebar.classList.contains('open')) fetchCart();
}

async function addToCart(productId) {
    if (!currentUser) {
        showToast('Please login to add to cart', 'error');
        showModal('auth-modal');
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/cart`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.id, productId, quantity: 1 })
        });
        if (res.ok) {
            showToast('Added to cart');
            fetchCart();
        } else {
            showToast('Failed to add to cart', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    }
}

async function fetchCart() {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/cart/user/${currentUser.id}`);
        if (res.ok) {
            cart = await res.json();
            renderCart();
        }
    } catch (err) {
        console.error(err);
    }
}

function renderCart() {
    const countEl = document.getElementById('cart-count');
    const container = document.getElementById('cart-items');
    const totalEl = document.getElementById('cart-total-price');
    const totalCount = cart.reduce((sum, item) => sum + item.quantity, 0);
    countEl.innerText = totalCount;
    container.innerHTML = '';
    let totalPrice = 0;

    if (cart.length === 0) {
        container.innerHTML = '<p style="text-align:center; color: var(--text-muted); margin-top: 2rem;">Your cart is empty.</p>';
        totalEl.innerText = '$0.00';
        return;
    }
    cart.forEach(item => {
        const prod = products.find(p => p.id === item.productId);
        if (prod) {
            totalPrice += prod.price * item.quantity;
            const el = document.createElement('div');
            el.className = 'cart-item';
            el.innerHTML = `
                <div class="cart-item-info">
                    <h4>${prod.name}</h4>
                    <p>$${prod.price.toFixed(2)}</p>
                </div>
                <div class="cart-item-qty">Qty: <b>${item.quantity}</b></div>
            `;
            container.appendChild(el);
        }
    });
    totalEl.innerText = `$${totalPrice.toFixed(2)}`;
}

async function placeOrder() {
    if (cart.length === 0) {
        showToast('Your cart is empty', 'error');
        return;
    }
    const btn = document.querySelector('.cart-footer button');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...';
    btn.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.id })
        });
        if (res.ok) {
            const order = await res.json();
            showToast(`Order placed! Total: $${order.totalAmount.toFixed(2)}`);
            toggleCart();
            cart = [];
            renderCart();
            setTimeout(fetchProducts, 1000);
        } else {
            const err = await res.json();
            showToast(err.message || 'Order failed', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    } finally {
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
}

// --- Order History ---
async function showOrderHistory() {
    showModal('orders-modal');
    const container = document.getElementById('orders-list');
    container.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const res = await fetch(`${API_BASE}/orders/user/${currentUser.id}`);
        if (res.ok) {
            const orders = await res.json();
            renderOrders(orders, container);
        } else {
            container.innerHTML = '<p style="color:var(--danger); text-align:center;">Failed to load orders.</p>';
        }
    } catch (err) {
        container.innerHTML = '<p style="color:var(--danger); text-align:center;">Network error.</p>';
    }
}

function renderOrders(orders, container) {
    // Hiển thị tổng số đơn
    const summaryEl = document.getElementById('orders-summary');
    const countEl = document.getElementById('orders-total-count');
    if (summaryEl && countEl) {
        countEl.innerText = orders.length;
        summaryEl.style.display = orders.length > 0 ? 'inline' : 'none';
    }

    if (orders.length === 0) {
        container.innerHTML = '<p style="text-align:center; color:var(--text-muted); padding:2rem 0;">You have no orders yet.</p>';
        return;
    }
    container.innerHTML = '';
    orders.slice().reverse().forEach(order => {
        const date = new Date(order.createdAt).toLocaleString();
        const card = document.createElement('div');
        card.className = 'order-card';
        card.innerHTML = `
            <div class="order-card-header">
                <div>
                    <span class="order-id">#${order.id}</span>
                    <span class="order-date">${date}</span>
                </div>
                <div>
                    <span class="order-status">${order.status}</span>
                    <span class="order-total">$${order.totalAmount.toFixed(2)}</span>
                </div>
            </div>
            <div class="order-items-list">
                ${order.items.map(item => `
                    <div class="order-item-row">
                        <span><i class="fa-solid fa-box"></i> Product #${item.productId}</span>
                        <span>x${item.quantity} &nbsp; <b>$${(item.price * item.quantity).toFixed(2)}</b></span>
                    </div>
                `).join('')}
            </div>
        `;
        container.appendChild(card);
    });
}

