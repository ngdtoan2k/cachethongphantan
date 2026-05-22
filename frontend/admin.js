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
                <th>Description</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Action</th>
            </tr>
        </thead>
        <tbody>
            ${products.map(p => `
                <tr>
                    <td style="color:var(--text-muted); font-size:0.8rem;">#${p.id}</td>
                    <td><b>${p.name}</b></td>
                    <td style="color:var(--text-muted); font-size:0.85rem; max-width:200px;">${p.description || '—'}</td>
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
        } else {
            showToast('Failed to delete product', 'error');
        }
    } catch (err) {
        showToast('Network error', 'error');
    }
}
