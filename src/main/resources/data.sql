-- Dọn dẹp dữ liệu cũ (chạy theo thứ tự ngược lại để tránh lỗi khóa ngoại)
DELETE FROM variant_values;
DELETE FROM variants;
DELETE FROM products;
DELETE FROM attribute_values;
DELETE FROM attributes;
DELETE FROM coupons;
DELETE FROM promotions;
DELETE FROM brands;
DELETE FROM categories;
GO -- (GO là dấu ngắt lô, quan trọng trong SQL Server)

-- =================================================================
-- 1. Bảng categories
-- =================================================================
SET IDENTITY_INSERT categories ON; -- <-- BẬT cho phép chèn ID
GO

INSERT INTO categories (id, name, description, image_url, active) VALUES
(1, N'Áo Phông', N'Áo phông nam nữ, 100% cotton', 'https://example.com/images/cat_aophong.jpg', 1),
(2, N'Quần Jeans', N'Quần jeans co giãn, dáng suông và skinny', 'https://example.com/images/cat_jeans.jpg', 1),
(3, N'Áo Sơ Mi', N'Áo sơ mi công sở và dạo phố', 'https://example.com/images/cat_somi.jpg', 1),
(4, N'Váy Đầm', N'Váy đầm dự tiệc (danh mục tạm ẩn)', 'https://example.com/images/cat_vaydam.jpg', 0);
GO

SET IDENTITY_INSERT categories OFF; -- <-- TẮT, trả về mặc định
GO

-- =================================================================
-- 2. Bảng brands
-- =================================================================
SET IDENTITY_INSERT brands ON;
GO

INSERT INTO brands (id, name, description, image_url, active) VALUES
(1, N'Coolmate', N'Thương hiệu thời trang nam Việt Nam', 'https://example.com/images/brand_coolmate.jpg', 1),
(2, N'Uniqlo', N'Thương hiệu thời trang từ Nhật Bản', 'https://example.com/images/brand_uniqlo.jpg', 1),
(3, N'Levi''s', N'Thương hiệu quần jeans nổi tiếng', 'https://example.com/images/brand_levis.jpg', 1);
GO

SET IDENTITY_INSERT brands OFF;
GO

-- =================================================================
-- 3. Bảng promotions
-- =================================================================
SET IDENTITY_INSERT promotions ON;
GO

INSERT INTO promotions (id, name, description, discount_value, start_date, end_date, active) VALUES
(1, N'Giảm 10%', N'Giảm 10% cho toàn bộ áo phông', 10.00, '2025-10-01', '2025-11-30', 1),
(2, N'Sale Hè 50K', N'Giảm 50,000đ cho đơn hàng quần jeans', 50000.00, '2025-06-01', '2025-06-30', 0);
GO

SET IDENTITY_INSERT promotions OFF;
GO

-- =================================================================
-- 4. Bảng coupons
-- =================================================================
SET IDENTITY_INSERT coupons ON;
GO

INSERT INTO coupons (id, code, description, discount_value, max_discount_amount, min_order_amount, usage_limit, used_count, start_date, end_date, active) VALUES
(1, 'SALE11', N'Giảm 11% tối đa 50K cho đơn từ 200K', 11.00, 50000.00, 200000.00, 100, 0, '2025-11-01', '2025-11-11', 1),
(2, 'FREESHIP', N'Giảm 30K (phí ship) cho đơn từ 150K', 30000.00, 30000.00, 150000.00, 500, 45, '2025-10-01', '2025-11-30', 1),
(3, 'HETLUOT', N'Mã giảm giá đã hết lượt sử dụng', 100000.00, 100000.00, 500000.00, 10, 10, '2025-10-01', '2025-11-30', 0);
GO

SET IDENTITY_INSERT coupons OFF;
GO

-- =================================================================
-- 5. Bảng attributes
-- =================================================================
SET IDENTITY_INSERT attributes ON;
GO

INSERT INTO attributes (id, name) VALUES
(1, N'Màu sắc'),
(2, N'Kích thước');
GO

SET IDENTITY_INSERT attributes OFF;
GO

-- =================================================================
-- 6. Bảng attribute_values
-- =================================================================
SET IDENTITY_INSERT attribute_values ON;
GO

INSERT INTO attribute_values (id, attribute_id, value) VALUES
-- Màu sắc (attribute_id = 1)
(1, 1, N'Trắng'),
(2, 1, N'Đen'),
(3, 1, N'Xanh Navy'),
(4, 1, N'Xám'),
-- Kích thước (attribute_id = 2)
(5, 2, 'S'),
(6, 2, 'M'),
(7, 2, 'L'),
(8, 2, 'XL');
GO

SET IDENTITY_INSERT attribute_values OFF;
GO

-- =================================================================
-- 7. Bảng products
-- =================================================================
SET IDENTITY_INSERT products ON;
GO

INSERT INTO products (id, category_id, brand_id, promotion_id, name, description, image_url, active, created_at, updated_at) VALUES
(1, 1, 1, 2, N'Áo Phông Nam Thể Thao Pro', N'Vải 100% Polyester, co giãn 4 chiều, thoáng mát.', 'https://example.com/images/prod_coolmate_pro.jpg', 1, '2025-10-31 09:00:00', '2025-10-31 09:00:00'),
(2, 1, 2, 1, N'Áo Phông Uniqlo Supima Cotton', N'Vải Supima Cotton cao cấp, mềm mịn, bền màu.', 'https://example.com/images/prod_uniqlo_supima.jpg', 1, '2025-10-31 09:10:00', '2025-10-31 09:10:00'),
(3, 2, 3, 1, N'Quần Jeans Levi''s 501 Original Fit', N'Dáng suông cổ điển, vải denim 100% cotton.', 'https://example.com/images/prod_levis_501.jpg', 1, '2025-10-31 09:20:00', '2025-10-31 09:20:00'),
(4, 3, 2, 1, N'Áo Sơ Mi Oxford Uniqlo Dài Tay', N'Vải Oxford dày dặn, form regular fit.', 'https://example.com/images/prod_uniqlo_oxford.jpg', 1, '2025-10-31 09:30:00', '2025-10-31 09:30:00');
GO

SET IDENTITY_INSERT products OFF;
GO

-- =================================================================
-- 8. Bảng variants
-- =================================================================
SET IDENTITY_INSERT variants ON;
GO

INSERT INTO variants (id, product_id, sku, name, price, stock_quantity, image_url, active, created_at, updated_at) VALUES
-- Các biến thể cho Áo Phông Coolmate (product_id = 1)
(1, 1, 'CM-PRO-BLK-M', N'Đen, M', 249000.00, 100, 'https://example.com/images/var_coolmate_pro_blk.jpg', 1, '2025-10-31 09:05:00', '2025-10-31 09:05:00'),
(2, 1, 'CM-PRO-BLK-L', N'Đen, L', 249000.00, 80, 'https://example.com/images/var_coolmate_pro_blk.jpg', 1, '2025-10-31 09:05:00', '2025-10-31 09:05:00'),
(3, 1, 'CM-PRO-NAV-M', N'Xanh Navy, M', 249000.00, 90, 'https://example.com/images/var_coolmate_pro_nav.jpg', 1, '2025-10-31 09:05:00', '2025-10-31 09:05:00'),
-- Các biến thể cho Áo Phông Uniqlo (product_id = 2)
(4, 2, 'UQ-SUP-WHT-S', N'Trắng, S', 399000.00, 50, 'https://example.com/images/var_uniqlo_sup_wht.jpg', 1, '2025-10-31 09:15:00', '2025-10-31 09:15:00'),
(5, 2, 'UQ-SUP-WHT-M', N'Trắng, M', 399000.00, 50, 'https://example.com/images/var_uniqlo_sup_wht.jpg', 1, '2025-10-31 09:15:00', '2025-10-31 09:15:00'),
-- Các biến thể cho Quần Jeans Levi's (product_id = 3)
(6, 3, 'LV-501-NAV-M', N'Xanh Navy, M', 1899000.00, 30, 'https://example.com/images/var_levis_501_nav.jpg', 1, '2025-10-31 09:25:00', '2025-10-31 09:25:00'),
(7, 3, 'LV-501-NAV-L', N'Xanh Navy, L', 1899000.00, 25, 'https://example.com/images/var_levis_501_nav.jpg', 1, '2025-10-31 09:25:00', '2025-10-31 09:25:00'),
-- Các biến thể cho Áo Sơ Mi Uniqlo (product_id = 4)
(8, 4, 'UQ-OXF-WHT-L', N'Trắng, L', 799000.00, 40, 'https://example.com/images/var_uniqlo_oxf_wht.jpg', 1, '2025-10-31 09:35:00', '2025-10-31 09:35:00'),
(9, 4, 'UQ-OXF-GRY-L', N'Xám, L', 799000.00, 0, 'https://example.com/images/var_uniqlo_oxf_gry.jpg', 0, '2025-10-31 09:35:00', '2025-10-31 09:35:00');
GO

SET IDENTITY_INSERT variants OFF;
GO

-- =================================================================
-- 9. Bảng variant_values
-- =================================================================
SET IDENTITY_INSERT variant_values ON;
GO

INSERT INTO variant_values (id, variant_id, attribute_value_id) VALUES
-- Variant 1 (CM-PRO-BLK-M): Đen, M
(1, 1, 2), -- 2 = Đen
(2, 1, 6), -- 6 = M
-- Variant 2 (CM-PRO-BLK-L): Đen, L
(3, 2, 2), -- 2 = Đen
(4, 2, 7), -- 7 = L
-- Variant 3 (CM-PRO-NAV-M): Xanh Navy, M
(5, 3, 3), -- 3 = Xanh Navy
(6, 3, 6), -- 6 = M
-- Variant 4 (UQ-SUP-WHT-S): Trắng, S
(7, 4, 1), -- 1 = Trắng
(8, 4, 5), -- 5 = S
-- Variant 5 (UQ-SUP-WHT-M): Trắng, M
(9, 5, 1), -- 1 = Trắng
(10, 5, 6), -- 6 = M
-- Variant 6 (LV-501-NAV-M): Xanh Navy, M
(11, 6, 3), -- 3 = Xanh Navy
(12, 6, 6), -- 6 = M
-- Variant 7 (LV-501-NAV-L): Xanh Navy, L
(13, 7, 3), -- 3 = Xanh Navy
(14, 7, 7), -- 7 = L
-- Variant 8 (UQ-OXF-WHT-L): Trắng, L
(15, 8, 1), -- 1 = Trắng
(16, 8, 7), -- 7 = L
-- Variant 9 (UQ-OXF-GRY-L): Xám, L
(17, 9, 4), -- 4 = Xám
(18, 9, 7); -- 7 = L
GO

SET IDENTITY_INSERT variant_values OFF;
GO