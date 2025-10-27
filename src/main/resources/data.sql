USE BEZAPP;

-- 1. Xóa dữ liệu cũ (đúng thứ tự tránh lỗi FK)
DELETE FROM carts;
DELETE FROM product_variants;
DELETE FROM products;
DELETE FROM users;

-- 2. Reset IDENTITY để bắt đầu từ 1
DBCC CHECKIDENT ('products', RESEED, 0);
DBCC CHECKIDENT ('product_variants', RESEED, 0);
DBCC CHECKIDENT ('users', RESEED, 0);
DBCC CHECKIDENT ('carts', RESEED, 0);

-- 3. Thêm 5 sản phẩm mới
INSERT INTO products (name, description, price) VALUES
('iPhone 13', '128GB - Blue', 18000000),
('iPhone 14', '128GB - Black', 22000000),
('iPhone 14 Pro', '256GB - Gold', 30000000),
('iPhone 15', '128GB - Pink', 25000000),
('iPhone 15 Pro', '512GB - Silver', 38000000);

-- 4. Thêm 5 biến thể sản phẩm
INSERT INTO product_variants (product_id, sku, name, price, stock_quantity) VALUES
(1, 'IP13-BLUE', 'iPhone 13 - Blue', 18000000, 50),
(2, 'IP14-BLACK', 'iPhone 14 - Black', 22000000, 30),
(3, 'IP14PRO-GOLD', 'iPhone 14 Pro - Gold', 30000000, 20),
(4, 'IP15-PINK', 'iPhone 15 - Pink', 25000000, 15),
(5, 'IP15PRO-SILVER', 'iPhone 15 Pro - Silver', 38000000, 10);

-- 5. Thêm 5 người dùng
INSERT INTO users (email, password, first_name, last_name, is_active) VALUES
('nguyenlengocquan@gmail.com', 'quan1425', 'Test', 'NgocQuan', 1),
('user2@example.com', '123456', 'User', 'Two', 1),
('trandaitu@gmail.com', 'tu12345', 'tu', 'tran', 1),
('quannlnps41384@gmail.com', 'quan1425', 'quan', 'nguyen', 0);


-- 6. Thêm 5 giỏ hàng — mỗi user có 1 sản phẩm variant khác nhau
INSERT INTO carts (user_id, variant_id, quantity, price) VALUES
(1, 1, 1, 18000000);


-- 7. Kiểm tra dữ liệu
SELECT * FROM products;
SELECT * FROM product_variants;
SELECT * FROM users;
SELECT * FROM carts;
