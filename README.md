# 🛒 E-commerce Microservices Platform

Hệ thống Bán hàng Trực tuyến được thiết kế theo kiến trúc **Microservices** sử dụng **Spring Boot 3**, **Java 21**, **PostgreSQL**, **RabbitMQ** và **Frontend thuần HTML/CSS/JS**.

---

## 🏗️ Kiến trúc hệ thống

```
                  ┌─────────────────────────────────┐
     Browser ─────►  Frontend (Nginx)  :3000         │
                  └────────────┬────────────────────┘
                               │ HTTP
                  ┌────────────▼────────────────────┐
                  │  API Gateway (Spring Cloud) :8080 │
                  └──┬──────┬──────┬──────┬─────────┘
                     │      │      │      │
         ┌───────────┘  ┌───┘  ┌───┘      └────────────┐
         ▼              ▼      ▼                        ▼
   user-service   product-  cart-service    order-service
      :8081       service    :8083              :8084
                   :8082                          │
                                                  └──── RabbitMQ (Event Bus)
```

### Services

| Service | Port | Chức năng |
|---|---|---|
| `frontend` | 3000 | Giao diện web người dùng & Admin |
| `api-gateway` | 8080 | Điều hướng request (Spring Cloud Gateway) |
| `user-service` | 8081 | Đăng ký, đăng nhập, quản lý người dùng |
| `product-service` | 8082 | Quản lý sản phẩm (CRUD) |
| `cart-service` | 8083 | Quản lý giỏ hàng |
| `order-service` | 8084 | Xử lý đặt hàng, phát event |
| `rabbitmq` | 5672 / 15672 | Message Broker |

### Databases

| DB | Port | Dùng cho |
|---|---|---|
| `user-db` | 5432 | User Service |
| `product-db` | 5433 | Product Service |
| `cart-db` | 5434 | Cart Service |
| `order-db` | 5435 | Order Service |

---

## 🚀 Cách chạy dự án

### Yêu cầu
- **Docker** & **Docker Compose** đã cài đặt.

### Khởi động lần đầu (build + run)

```bash
docker compose up -d --build
```

> ⏳ Lần đầu build có thể mất 5–10 phút do cần download dependencies Maven.  
> Sau khi lệnh chạy xong, hãy **đợi thêm 1–2 phút** để các Spring Boot services khởi động hoàn toàn.

### Các lần sau (chạy lại nhanh)

```bash
docker compose up -d
```

### Dừng hệ thống

```bash
docker compose down
```

---

## 🌐 Truy cập ứng dụng

| URL | Mô tả |
|---|---|
| http://localhost:3000 | 🛒 **Web Frontend** (Trang chính) |
| http://localhost:3000/admin.html | 🔧 **Admin Dashboard** (Quản lý sản phẩm) |
| http://localhost:8080/api/... | 🔌 **API Gateway** |
| http://localhost:15672 | 🐇 **RabbitMQ Management UI** |

---

## 👤 Tài khoản mặc định

### Admin (tự động tạo khi khởi động)
| Field | Giá trị |
|---|---|
| Email | `admin@shop.com` |
| Password | `admin123` |
| Role | `ROLE_ADMIN` |

### User mẫu
| Field | Giá trị |
|---|---|
| Email | `john@example.com` |
| Password | `password123` |
| Role | `ROLE_USER` |

> 💡 Bạn cũng có thể tự **đăng ký tài khoản mới** ngay trên giao diện web.

### RabbitMQ
- Username: `guest` / Password: `guest`

---

## ✨ Tính năng

### Người dùng thường
- ✅ Đăng ký tài khoản mới
- ✅ Đăng nhập / Đăng xuất
- ✅ Xem danh sách sản phẩm
- ✅ Thêm sản phẩm vào giỏ hàng
- ✅ Đặt hàng (Checkout)
- ✅ Xem lịch sử đơn hàng

### Admin
- ✅ Tất cả tính năng của User
- ✅ Truy cập trang Admin Dashboard
- ✅ Thêm sản phẩm mới
- ✅ Xóa sản phẩm
- ✅ Xem toàn bộ danh sách sản phẩm
- ✅ Xem toàn bộ danh sách user
- ✅ Xem toàn bộ danh sách order

---

## 📡 API Endpoints (qua API Gateway :8080)

### User Service
```
POST /api/users/register    - Đăng ký tài khoản
POST /api/users/login       - Đăng nhập
GET  /api/users/{id}        - Lấy thông tin user
GET  /api/users             - Lấy tất cả users
```

### Product Service
```
GET    /api/products         - Lấy tất cả sản phẩm
GET    /api/products/{id}    - Lấy 1 sản phẩm
POST   /api/products         - Tạo sản phẩm mới
PUT    /api/products/{id}    - Cập nhật sản phẩm
DELETE /api/products/{id}    - Xóa sản phẩm
```

### Cart Service
```
POST   /api/cart                   - Thêm vào giỏ hàng
GET    /api/cart/user/{userId}     - Lấy giỏ hàng của user
DELETE /api/cart/{id}              - Xóa item khỏi giỏ hàng
```

### Order Service
```
POST /api/orders                    - Đặt hàng
GET  /api/orders                    - Lấy tất cả đơn hàng
GET  /api/orders/{id}               - Lấy 1 đơn hàng
GET  /api/orders/user/{userId}      - Lịch sử đơn hàng của user
```

---

## 🔁 Luồng xử lý đặt hàng (Event-Driven)

```
User clicks Checkout
    │
    ▼
POST /api/orders  →  Order Service
    │
    ├── GET cart từ Cart Service (sync)
    ├── GET products từ Product Service (sync)
    ├── Lưu Order vào DB
    │
    └── Publish OrderCreatedEvent → RabbitMQ
            │
            ├──► Product Service (giảm stockQuantity)
            ├──► User Service (ghi log lịch sử mua của người dùng)
            └──► Cart Service (xóa giỏ hàng)
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Cloud Gateway |
| Database | PostgreSQL 15 |
| Messaging | RabbitMQ 3 |
| Frontend | HTML5, CSS3, Vanilla JS, Nginx |
| Container | Docker, Docker Compose |
