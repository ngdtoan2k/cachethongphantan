const API_BASE = 'http://localhost:8080/api';

// --- Auth Guard: redirect non-admin ---
document.addEventListener('DOMContentLoaded', () => {
    const savedUser = localStorage.getItem('user');
    if (!savedUser) {
        window.location.href = 'index.html';
        return;
    }
    const user = JSON.parse(savedUser);
    if (user.role !== 'ROLE_ADMIN') {
        alert('Access denied. Admins only.');
        window.location.href = 'index.html';
        return;
    }
    document.getElementById('admin-name').innerText = user.fullName;
    loadAdminProducts();
    loadAdminUsers();
    loadAdminOrders();
    updateStats();
});

function logoutAdmin() {
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}

// --- Toast ---
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

// --- Modal Helpers ---
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

// --- Switch Tabs ---
function switchAdminTab(tabName) {
    document.getElementById('tab-content-products').classList.add('hidden');
    document.getElementById('tab-content-users').classList.add('hidden');
    document.getElementById('tab-content-orders').classList.add('hidden');
    
    document.getElementById('tab-products').classList.remove('active');
    document.getElementById('tab-users').classList.remove('active');
    document.getElementById('tab-orders').classList.remove('active');
    
    document.getElementById(`tab-content-${tabName}`).classList.remove('hidden');
    document.getElementById(`tab-${tabName}`).classList.add('active');
    
    if (tabName === 'products') loadAdminProducts();
    if (tabName === 'users') loadAdminUsers();
    if (tabName === 'orders') loadAdminOrders();
    updateStats();
}

// --- Update Stats ---
async function updateStats() {
    try {
        const prodRes = await fetch(`${API_BASE}/products`);
        if (prodRes.ok) {
            const products = await prodRes.json();
            document.getElementById('stat-products').innerText = products.length;
        }
        const userRes = await fetch(`${API_BASE}/users`);
        if (userRes.ok) {
            const users = await userRes.json();
            document.getElementById('stat-users').innerText = users.length;
        }
        const orderRes = await fetch(`${API_BASE}/orders`);
        if (orderRes.ok) {
            const orders = await orderRes.json();
            document.getElementById('stat-orders').innerText = orders.length;
        }
    } catch (err) {
        console.error('Failed to update stats', err);
    }
}

// --- Add Product ---
async function addProduct(e) {
    e.preventDefault();
    const name = document.getElementById('prod-name').value.trim();
    const description = document.getElementById('prod-desc').value.trim();
    const price = parseFloat(document.getElementById('prod-price').value);
    const stockQuantity = parseInt(document.getElementById('prod-stock').value);
    const btn = document.getElementById('btn-add-product');
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Adding...';
    btn.disabled = true;

    try {
        const res = await fetch(`${API_BASE}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description, price, stockQuantity })
        });
        if (res.ok) {
            showToast(`Product "${name}" added successfully!`);
            document.getElementById('add-product-form').reset();
            loadAdminProducts();
            updateStats();
        } else {
            showToast('Failed to add product', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    } finally {
        btn.innerHTML = '<i class="fa-solid fa-plus"></i> Add Product';
        btn.disabled = false;
    }
}

// --- Load Products Table ---
async function loadAdminProducts() {
    const container = document.getElementById('admin-products-container');
    container.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const res = await fetch(`${API_BASE}/products`);
        const products = await res.json();
        renderAdminTable(products, container);
    } catch (err) {
        container.innerHTML = '<p style="color:var(--danger); text-align:center;">Failed to load products.</p>';
    }
}

function renderAdminTable(products, container) {
    if (products.length === 0) {
        container.innerHTML = '<p style="text-align:center; color:var(--text-muted); padding:2rem;">No products yet.</p>';
        return;
    }
    const table = document.createElement('table');
    table.className = 'admin-table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>#ID</th>
                <th>Name</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>
            ${products.map(p => `
                <tr>
                    <td style="color:var(--text-muted); font-size:0.8rem;">#${p.id}</td>
                    <td>
                        <a href="#" onclick="showProductDetails(${p.id}); return false;" style="font-weight:600; color:var(--primary); text-decoration:underline;">
                            ${p.name}
                        </a>
                    </td>
                    <td><b style="color:var(--primary);">$${p.price.toFixed(2)}</b></td>
                    <td>
                        <span class="badge-stock ${p.stockQuantity <= 0 ? 'low' : 'ok'}">
                            ${p.stockQuantity <= 0 ? 'Out of Stock' : p.stockQuantity + ' units'}
                        </span>
                    </td>
                    <td>
                        <button class="btn-delete" onclick="deleteProduct(${p.id}, '${p.name.replace(/'/g, "\\'")}')">
                            <i class="fa-solid fa-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

// --- Delete Product ---
async function deleteProduct(id, name) {
    if (!confirm(`Are you sure you want to delete "${name}"?`)) return;
    try {
        const res = await fetch(`${API_BASE}/products/${id}`, { method: 'DELETE' });
        if (res.ok) {
            showToast(`Product "${name}" deleted`);
            loadAdminProducts();
            updateStats();
        } else {
            showToast('Failed to delete product', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    }
}

// --- Load Users Table ---
async function loadAdminUsers() {
    const container = document.getElementById('admin-users-container');
    container.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const res = await fetch(`${API_BASE}/users`);
        const users = await res.json();
        renderUsersTable(users, container);
    } catch (err) {
        container.innerHTML = '<p style="color:var(--danger); text-align:center;">Failed to load users.</p>';
    }
}

function renderUsersTable(users, container) {
    if (users.length === 0) {
        container.innerHTML = '<p style="text-align:center; color:var(--text-muted); padding:2rem;">No users found.</p>';
        return;
    }
    const table = document.createElement('table');
    table.className = 'admin-table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>#ID</th>
                <th>Full Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Created At</th>
            </tr>
        </thead>
        <tbody>
            ${users.map(u => `
                <tr>
                    <td style="color:var(--text-muted); font-size:0.8rem;">#${u.id}</td>
                    <td><b>${u.fullName}</b></td>
                    <td>${u.email}</td>
                    <td>
                        <span class="badge-stock ${u.role === 'ROLE_ADMIN' ? 'ok' : 'low'}" style="background: ${u.role === 'ROLE_ADMIN' ? 'rgba(34,197,94,0.15)' : 'rgba(99,102,241,0.15)'}; color: ${u.role === 'ROLE_ADMIN' ? '#22c55e' : '#6366f1'};">
                            ${u.role}
                        </span>
                    </td>
                    <td style="color:var(--text-muted); font-size:0.85rem;">
                        ${new Date(u.createdAt).toLocaleString()}
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

// --- Load Orders Table ---
async function loadAdminOrders() {
    const container = document.getElementById('admin-orders-container');
    container.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const res = await fetch(`${API_BASE}/orders`);
        const orders = await res.json();
        renderOrdersTable(orders, container);
    } catch (err) {
        container.innerHTML = '<p style="color:var(--danger); text-align:center;">Failed to load orders.</p>';
    }
}

function renderOrdersTable(orders, container) {
    if (orders.length === 0) {
        container.innerHTML = '<p style="text-align:center; color:var(--text-muted); padding:2rem;">No orders yet.</p>';
        return;
    }
    const table = document.createElement('table');
    table.className = 'admin-table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>#ID</th>
                <th>User ID</th>
                <th>Date</th>
                <th>Items</th>
                <th>Total</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody>
            ${orders.map(o => `
                <tr>
                    <td style="color:var(--text-muted); font-size:0.8rem;">#${o.id}</td>
                    <td style="font-size:0.85rem;">User #${o.userId}</td>
                    <td style="color:var(--text-muted); font-size:0.85rem;">
                        ${new Date(o.createdAt).toLocaleString()}
                    </td>
                    <td style="font-size:0.85rem; max-width: 250px;">
                        ${o.items.map(item => `
                            <div style="margin-bottom: 0.2rem;"><i class="fa-solid fa-box" style="color:var(--primary); margin-right:0.3rem;"></i>Product #${item.productId} (x${item.quantity}) - $${(item.price * item.quantity).toFixed(2)}</div>
                        `).join('')}
                    </td>
                    <td><b style="color:var(--primary);">$${o.totalAmount.toFixed(2)}</b></td>
                    <td>
                        <span class="badge-stock ok">
                            ${o.status}
                        </span>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

// --- Product Details Helper ---
function getProductIcon(name) {
    const n = name.toLowerCase();
    if (n.includes('macbook') || n.includes('laptop')) return 'fa-laptop';
    if (n.includes('iphone') || n.includes('phone') || n.includes('samsung') || n.includes('galaxy')) return 'fa-mobile-screen';
    if (n.includes('watch')) return 'fa-clock';
    if (n.includes('headphone') || n.includes('airpod') || n.includes('sony') || n.includes('wh-')) return 'fa-headphones';
    if (n.includes('ipad') || n.includes('tablet')) return 'fa-tablet-screen-button';
    return 'fa-box-open';
}

async function showProductDetails(id) {
    showModal('product-detail-modal');
    
    const nameEl = document.getElementById('detail-product-name');
    const descEl = document.getElementById('detail-product-desc');
    const priceEl = document.getElementById('detail-product-price');
    const stockEl = document.getElementById('detail-product-stock');
    const iconEl = document.getElementById('detail-product-icon');
    
    nameEl.innerText = 'Loading...';
    descEl.innerText = '';
    priceEl.innerText = '';
    stockEl.className = 'badge-stock';
    stockEl.innerText = '';
    
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
    } catch (err) {
        nameEl.innerText = 'Error';
        descEl.innerText = 'Failed to load product details.';
        showToast('Failed to load product details', 'error');
    }
}
