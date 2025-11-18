

-- TẮT KIỂM TRA KHÓA NGOẠI
EXEC sp_MSforeachtable "ALTER TABLE ? NOCHECK CONSTRAINT all"

-- =============================================
-- PHẦN 1: XÓA DỮ LIỆU CŨ (Theo thứ tự ngược)
-- LƯU Ý: KHÔNG XÓA BẢNG [users]
-- =============================================
DELETE FROM [carts];
DELETE FROM [variant_option_values];
DELETE FROM [order_items];
DELETE FROM [order_audit_logs];
DELETE FROM [payments];
DELETE FROM [variants];
DELETE FROM [product_option_values];
DELETE FROM [product_options];
DELETE FROM [product_images]; -- (Bảng mới)
DELETE FROM [orders]; -- (Bảng mới)
DELETE FROM [products];
DELETE FROM [coupons];
DELETE FROM [promotions];
DELETE FROM [categories];
DELETE FROM [brands];

-- =============================================
-- PHẦN 2: RESET ID TỰ TĂNG VỀ 0
-- LƯU Ý: KHÔNG RESET BẢNG [users]
-- =============================================
DBCC CHECKIDENT ('[variant_option_values]', RESEED, 0);
DBCC CHECKIDENT ('[order_items]', RESEED, 0);
DBCC CHECKIDENT ('[order_audit_logs]', RESEED, 0);
DBCC CHECKIDENT ('[payments]', RESEED, 0);
DBCC CHECKIDENT ('[variants]', RESEED, 0);
DBCC CHECKIDENT ('[product_option_values]', RESEED, 0);
DBCC CHECKIDENT ('[product_options]', RESEED, 0);
DBCC CHECKIDENT ('[product_images]', RESEED, 0); -- (Bảng mới)
DBCC CHECKIDENT ('[orders]', RESEED, 0); -- (Bảng mới)
DBCC CHECKIDENT ('[products]', RESEED, 0);
DBCC CHECKIDENT ('[coupons]', RESEED, 0);
DBCC CHECKIDENT ('[promotions]', RESEED, 0);
DBCC CHECKIDENT ('[categories]', RESEED, 0);
DBCC CHECKIDENT ('[brands]', RESEED, 0);


-- =============================================
-- PHẦN 3: CHÈN DỮ LIỆU MỚI
-- =============================================

-- 1. Brands (Thương hiệu) - (Giữ nguyên)
SET IDENTITY_INSERT [brands] ON;
INSERT INTO [brands] (id, name, description, image_url, active) VALUES
    (1, N'Gucci', N'Thương hiệu thời trang cao cấp Ý', 'https://storage.googleapis.com/images-storage-bezbe/brands/gucci.png', 1),
    (2, N'Louis Vuitton', N'Hãng thời trang xa xỉ của Pháp', 'https://storage.googleapis.com/images-storage-bezbe/brands/lv.png', 1),
    (3, N'Nike', N'Thương hiệu thể thao hàng đầu', 'https://storage.googleapis.com/images-storage-bezbe/brands/nike.png', 1),
    (4, N'Adidas', N'Đối thủ cạnh tranh của Nike', 'https://storage.googleapis.com/images-storage-bezbe/brands/adidas.png', 1),
    (5, N'Uniqlo', N'Thời trang hàng ngày LifeWear từ Nhật Bản', 'https://storage.googleapis.com/images-storage-bezbe/brands/uniqlo.png', 1),
    (6, N'Zara', N'Thời trang nhanh từ Tây Ban Nha', 'https://storage.googleapis.com/images-storage-bezbe/brands/zara.png', 1),
    (7, N'H&M', N'Thời trang bình dân từ Thụy Điển', 'https://storage.googleapis.com/images-storage-bezbe/brands/hm.png', 1),
    (8, N'Chanel', N'Biểu tượng thời trang Pháp', 'https://storage.googleapis.com/images-storage-bezbe/brands/chanel.png', 1),
    (9, N'Dior', N'Thương hiệu cao cấp', 'https://storage.googleapis.com/images-storage-bezbe/brands/dior.png', 1),
    (10, N'Puma', N'Thương hiệu thể thao Đức', 'https://storage.googleapis.com/images-storage-bezbe/brands/puma.png', 1),
    (11, N'Balenciaga', N'Thời trang đường phố cao cấp', 'https://storage.googleapis.com/images-storage-bezbe/brands/balenciaga.png', 1);
SET IDENTITY_INSERT [brands] OFF;

-- 2. Categories (Danh mục) - (Giữ nguyên)
SET IDENTITY_INSERT [categories] ON;
INSERT INTO [categories] (id, name, description, image_url, active) VALUES
    (1, N'Áo Sơ Mi', N'Các loại áo sơ mi nam nữ', 'https://storage.googleapis.com/images-storage-bezbe/categories/ao-somi.png', 1),
    (2, N'Áo Phông', N'Áo phông, áo thun các loại', 'https://storage.googleapis.com/images-storage-bezbe/categories/ao-phong.png', 1),
    (3, N'Quần Jeans', N'Quần bò nam nữ', 'https://storage.googleapis.com/images-storage-bezbe/categories/quan-jeans.png', 1),
    (4, N'Quần Tây', N'Quần âu công sở', 'https://storage.googleapis.com/images-storage-bezbe/categories/quan-tay.png', 1),
    (5, N'Váy/Đầm', N'Váy đầm dự tiệc, công sở', 'https://storage.googleapis.com/images-storage-bezbe/categories/vay-dam.png', 1),
    (6, N'Áo Khoác', N'Áo khoác gió, áo khoác dạ', 'https://storage.googleapis.com/images-storage-bezbe/categories/ao-khoac.png', 1),
    (7, N'Giày Thể Thao', N'Sneakers cho nam và nữ', 'https://storage.googleapis.com/images-storage-bezbe/categories/giay-the-thao.png', 1),
    (8, N'Giày Cao Gót', N'Giày cao gót nữ', 'https://storage.googleapis.com/images-storage-bezbe/categories/giay-cao-got.png', 1),
    (9, N'Túi Xách', N'Túi xách thời trang', 'https://storage.googleapis.com/images-storage-bezbe/categories/tui-xach.png', 1),
    (10, N'Phụ Kiện', N'Kính mắt, thắt lưng, đồng hồ', 'https://storage.googleapis.com/images-storage-bezbe/categories/phu-kien.png', 1),
    (11, N'Đồ Thể Thao', N'Quần áo tập gym, yoga', 'https://storage.googleapis.com/images-storage-bezbe/categories/do-the-thao.png', 1);
SET IDENTITY_INSERT [categories] OFF;

-- 3. Promotions (Khuyến mãi - Giảm theo %) - (Giữ nguyên, vốn đã là %)
SET IDENTITY_INSERT [promotions] ON;
INSERT INTO [promotions] (id, name, description, discount_value, start_date, end_date, active, created_at, updated_at) VALUES
    (1, N'Giảm giá Hè 2025', N'Sale 20% toàn bộ sản phẩm hè', 20.00, '2025-06-01', '2025-06-30', 1, GETDATE(), GETDATE()),
    (2, N'Black Friday 2025', N'Siêu sale Black Friday', 50.00, '2025-11-28', '2025-11-30', 1, GETDATE(), GETDATE()),
    (3, N'Giáng Sinh An Lành', N'Giảm 15% mừng Giáng Sinh', 15.00, '2025-12-20', '2025-12-25', 1, GETDATE(), GETDATE()),
    (4, N'Sale Xả Hàng Cuối Năm', N'Giảm giá kịch sàn', 30.00, '2025-12-26', '2026-01-05', 1, GETDATE(), GETDATE()),
    (5, N'Chào Năm Học Mới', N'Giảm 10% cho đồ đi học', 10.00, '2025-08-15', '2025-09-05', 1, GETDATE(), GETDATE()),
    (6, N'Mừng Quốc Khánh 2/9', N'Giảm giá 10%', 10.00, '2025-09-01', '2025-09-03', 1, GETDATE(), GETDATE()),
    (7, N'Ngày Nhà Giáo 20/11', N'Tri ân thầy cô, giảm 20%', 20.00, '2025-11-15', '2025-11-20', 1, GETDATE(), GETDATE()),
    (8, N'Sale 11/11', N'Siêu sale 11/11', 25.00, '2025-11-11', '2025-11-11', 1, GETDATE(), GETDATE()),
    (9, N'Khuyến mãi tháng 10', N'Giảm 10% các sản phẩm mới', 10.00, '2025-10-01', '2025-10-31', 1, GETDATE(), GETDATE()),
    (10, N'Khuyến mãi Áo Khoác', N'Giảm 15% cho Áo Khoác', 15.00, '2025-11-01', '2025-11-30', 1, GETDATE(), GETDATE()),
    (11, N'Khuyến mãi đã hết hạn', N'Chương trình cũ', 10.00, '2024-01-01', '2024-01-31', 0, GETDATE(), GETDATE());
SET IDENTITY_INSERT [promotions] OFF;

-- 4. Coupons (Mã giảm giá - Giảm theo %)
-- ⭐ ĐÃ SỬA: Bỏ FREESHIP, thay bằng BIGSALE (ID 2). Tất cả đều là %.
SET IDENTITY_INSERT [coupons] ON;
INSERT INTO [coupons] (id, code, description, discount_value, max_discount_amount, min_order_amount, usage_limit, used_count, start_date, end_date, active, created_at, updated_at) VALUES
    (1, N'WELCOME10', N'Giảm 10% cho đơn hàng đầu tiên, tối đa 50.000đ', 10.00, 50000, 0, 1000, 0, '2025-01-01', '2026-12-31', 1, GETDATE(), GETDATE()),
    (2, N'BIGSALE', N'Giảm 12% cho đơn từ 500k, tối đa 80k', 12.00, 80000, 500000, 500, 10, '2025-11-01', '2025-11-30', 1, GETDATE(), GETDATE()), -- <-- Đã sửa
    (3, N'SALE100K', N'Giảm 100.000đ cho đơn từ 1.000.000đ', 10.00, 100000, 1000000, 100, 5, '2025-11-15', '2025-11-30', 1, GETDATE(), GETDATE()),
    (4, N'HIHEHE', N'Mã bí mật giảm 50%, tối đa 100.000đ', 50.00, 100000, 0, 10, 2, '2025-11-01', '2025-11-30', 1, GETDATE(), GETDATE()),
    (5, N'TET2026', N'Mừng Tết, giảm 15% cho đơn từ 800.000đ', 15.00, 150000, 800000, 200, 0, '2026-01-15', '2026-02-01', 1, GETDATE(), GETDATE()),
    (6, N'VOUCHER5', N'Giảm 5% cho mọi đơn hàng, tối đa 20.000đ', 5.00, 20000, 0, 1000, 50, '2025-01-01', '2025-12-31', 1, GETDATE(), GETDATE()),
    (7, N'STUDENT10', N'Giảm 10% cho HSSV, tối đa 40.000đ', 10.00, 40000, 300000, 300, 15, '2025-01-01', '2025-12-31', 1, GETDATE(), GETDATE()),
    (8, N'HAPPYMONDAY', N'Giảm 20% (tối đa 50k) cho đơn từ 250k', 20.00, 50000, 250000, 50, 0, '2025-11-17', '2025-11-17', 1, GETDATE(), GETDATE()),
    (9, N'DEALNGON', N'Giảm 10% cho đơn từ 100.000đ, tối đa 10.000đ', 10.00, 10000, 100000, 1000, 120, '2025-11-01', '2025-11-30', 1, GETDATE(), GETDATE()),
    (10, N'HETHAN', N'Voucher đã hết hạn', 10.00, 50000, 0, 100, 10, '2024-01-01', '2024-01-31', 0, GETDATE(), GETDATE()),
    (11, N'HETLUOT', N'Voucher đã hết lượt sử dụng', 15.00, 50000, 0, 100, 100, '2025-01-01', '2025-12-31', 0, GETDATE(), GETDATE());
SET IDENTITY_INSERT [coupons] OFF;

-- 5. Products (Sản phẩm) - (Giữ nguyên)
SET IDENTITY_INSERT [products] ON;
INSERT INTO [products] (id, category_id, brand_id, promotion_id, name, description, price, image_url, active, created_at, updated_at) VALUES
    (1, 1, 5, NULL, N'Áo Sơ Mi Vải Oxford', N'Áo sơ mi nam vải Oxford dày dặn, phom dáng regular fit. <br>Chất liệu 100% Cotton. <br>Mặc đi làm, đi chơi đều đẹp.', 599000, 'https://storage.googleapis.com/images-storage-bezbe/products/somi-oxford-trang-1.jpg', 1, GETDATE(), GETDATE()),
    (2, 2, 3, 5, N'Áo Phông Nike Sportswear', N'Áo phông thể thao Nike chất liệu Dri-FIT, thoáng mát, co giãn 4 chiều.', 750000, 'https://storage.googleapis.com/images-storage-bezbe/products/nike-drifit-den-1.jpg', 1, GETDATE(), GETDATE()),
    (3, 3, 6, 1, N'Quần Jeans Nữ Zara Skinny', N'Quần jeans nữ dáng skinny, cạp cao, tôn dáng. Chất liệu co giãn thoải mái.', 899000, 'https://storage.googleapis.com/images-storage-bezbe/products/jean-zara-skinny-1.jpg', 1, GETDATE(), GETDATE()),
    (4, 7, 4, 2, N'Giày Adidas Ultraboost 22', N'Mẫu giày chạy bộ êm ái nhất của Adidas. Công nghệ đệm Boost. Sale Black Friday.', 4500000, 'https://storage.googleapis.com/images-storage-bezbe/products/ultraboost-22-1.jpg', 1, GETDATE(), GETDATE()),
    (5, 5, 6, NULL, N'Váy Lụa Họa Tiết Zara', N'Váy lụa dáng suông, họa tiết hoa nhí. Chất liệu lụa satin mềm mại.', 1299000, 'https://storage.googleapis.com/images-storage-bezbe/products/vay-lua-zara-1.jpg', 1, GETDATE(), GETDATE()),
    (6, 6, 5, 10, N'Áo Khoác Phao Lông Vũ Uniqlo', N'Áo khoác phao siêu nhẹ, siêu ấm. Chống nước nhẹ.', 1799000, 'https://storage.googleapis.com/images-storage-bezbe/products/ao-phao-uniqlo-1.jpg', 1, GETDATE(), GETDATE()),
    (7, 9, 2, NULL, N'Túi Louis Vuitton Neverfull MM', N'Mẫu túi tote kinh điển của LV, họa tiết Monogram.', 45000000, 'https://storage.googleapis.com/images-storage-bezbe/products/lv-neverfull-1.jpg', 1, GETDATE(), GETDATE()),
    (8, 7, 3, NULL, N'Giày Nike Air Force 1', N'Mẫu sneaker huyền thoại, phối màu trắng classic.', 2800000, 'https://storage.googleapis.com/images-storage-bezbe/products/nike-af1-1.jpg', 1, GETDATE(), GETDATE()),
    (9, 4, 5, NULL, N'Quần Tây Nam Uniqlo Smart Ankle', N'Quần tây nam dáng Smart Ankle, co giãn 2 chiều. Mặc công sở rất thoải mái.', 999000, 'https://storage.googleapis.com/images-storage-bezbe/products/quan-tay-uniqlo-1.jpg', 1, GETDATE(), GETDATE()),
    (10, 11, 4, NULL, N'Quần Legging Tập Gym Adidas', N'Quần legging cạp cao, che bụng, chất vải co giãn 4 chiều.', 1100000, 'https://storage.googleapis.com/images-storage-bezbe/products/quan-legging-adidas-1.jpg', 1, GETDATE(), GETDATE()),
    (11, 1, 1, NULL, N'Áo Sơ Mi Lụa Gucci', N'Áo sơ mi lụa cao cấp họa tiết GG. Sản phẩm đã ngừng kinh doanh.', 15000000, 'https://storage.googleapis.com/images-storage-bezbe/products/ao-lua-gucci-1.jpg', 0, GETDATE(), GETDATE());
SET IDENTITY_INSERT [products] OFF;

-- 6. Product Images (Ảnh chi tiết sản phẩm)
-- ⭐ ĐÃ THÊM 11 ẢNH MẪU
SET IDENTITY_INSERT [product_images] ON;
INSERT INTO [product_images] (id, product_id, image_url) VALUES
    (1, 1, 'https://storage.googleapis.com/images-storage-bezbe/products/somi-oxford-trang-1.jpg'),
    (2, 1, 'https://storage.googleapis.com/images-storage-bezbe/products/somi-oxford-trang-2.jpg'),
    (3, 1, 'https://storage.googleapis.com/images-storage-bezbe/products/somi-oxford-trang-3.jpg'),
    (4, 2, 'https://storage.googleapis.com/images-storage-bezbe/products/nike-drifit-den-1.jpg'),
    (5, 2, 'https://storage.googleapis.com/images-storage-bezbe/products/nike-drifit-den-2.jpg'),
    (6, 3, 'https://storage.googleapis.com/images-storage-bezbe/products/jean-zara-skinny-1.jpg'),
    (7, 3, 'https://storage.googleapis.com/images-storage-bezbe/products/jean-zara-skinny-2.jpg'),
    (8, 4, 'https://storage.googleapis.com/images-storage-bezbe/products/ultraboost-22-1.jpg'),
    (9, 4, 'https://storage.googleapis.com/images-storage-bezbe/products/ultraboost-22-2.jpg'),
    (10, 5, 'https://storage.googleapis.com/images-storage-bezbe/products/vay-lua-zara-1.jpg'),
    (11, 5, 'https://storage.googleapis.com/images-storage-bezbe/products/vay-lua-zara-2.jpg');
SET IDENTITY_INSERT [product_images] OFF;

-- 7. Product Options (Thuộc tính của sản phẩm) - (Giữ nguyên)
SET IDENTITY_INSERT [product_options] ON;
INSERT INTO [product_options] (id, product_id, name, position) VALUES
    (1, 1, N'Màu sắc', 1), (2, 1, N'Kích cỡ', 2),
    (3, 2, N'Màu sắc', 1), (4, 2, N'Kích cỡ', 2),
    (5, 3, N'Kích cỡ', 1),
    (6, 8, N'Kích cỡ', 1);
SET IDENTITY_INSERT [product_options] OFF;

-- 8. Product Option Values (Giá trị của thuộc tính) - (Giữ nguyên)
SET IDENTITY_INSERT [product_option_values] ON;
INSERT INTO [product_option_values] (id, option_id, value, position) VALUES
    (1, 1, N'Trắng', 1), (2, 1, N'Đen', 2), (3, 1, N'Xanh Navy', 3),
    (4, 2, N'S', 1), (5, 2, N'M', 2), (6, 2, N'L', 3), (7, 2, N'XL', 4),
    (8, 3, N'Đen', 1), (9, 3, N'Trắng', 2),
    (10, 4, N'M', 1), (11, 4, N'L', 2),
    (12, 5, N'28', 1), (13, 5, N'29', 2), (14, 5, N'30', 3),
    (15, 6, N'40', 1), (16, 6, N'41', 2), (17, 6, N'42', 3);
SET IDENTITY_INSERT [product_option_values] OFF;

-- 9. Variants (Biến thể sản phẩm) - (Giữ nguyên)
SET IDENTITY_INSERT [variants] ON;
INSERT INTO [variants] (id, product_id, sku, name, price, stock_quantity, image_url, active, created_at, updated_at) VALUES
    (1, 1, N'SMOXFORD-TRANG-S', N'Áo Sơ Mi Oxford - Trắng, S', 599000, 100, 'https://storage.googleapis.com/images-storage-bezbe/variants/somi-oxford-trang.jpg', 1, GETDATE(), GETDATE()),
    (2, 1, N'SMOXFORD-TRANG-M', N'Áo Sơ Mi Oxford - Trắng, M', 599000, 150, 'https://storage.googleapis.com/images-storage-bezbe/variants/somi-oxford-trang.jpg', 1, GETDATE(), GETDATE()),
    (3, 1, N'SMOXFORD-DEN-M', N'Áo Sơ Mi Oxford - Đen, M', 599000, 50, 'https://storage.googleapis.com/images-storage-bezbe/variants/somi-oxford-den.jpg', 1, GETDATE(), GETDATE()),
    (4, 1, N'SMOXFORD-XANH-L', N'Áo Sơ Mi Oxford - Xanh Navy, L', 599000, 70, 'https://storage.googleapis.com/images-storage-bezbe/variants/somi-oxford-xanh.jpg', 1, GETDATE(), GETDATE()),
    (5, 2, N'NIKE-DEN-M', N'Áo Phông Nike - Đen, M', 750000, 20, 'https://storage.googleapis.com/images-storage-bezbe/variants/nike-drifit-den.jpg', 1, GETDATE(), GETDATE()),
    (6, 2, N'NIKE-TRANG-L', N'Áo Phông Nike - Trắng, L', 750000, 30, 'https://storage.googleapis.com/images-storage-bezbe/variants/nike-drifit-trang.jpg', 1, GETDATE(), GETDATE()),
    (7, 3, N'JEAN-ZARA-28', N'Quần Jeans Nữ Zara Skinny - 28', 899000, 10, 'https://storage.googleapis.com/images-storage-bezbe/variants/jean-zara-skinny.jpg', 1, GETDATE(), GETDATE()),
    (8, 3, N'JEAN-ZARA-29', N'Quần Jeans Nữ Zara Skinny - 29', 899000, 15, 'https://storage.googleapis.com/images-storage-bezbe/variants/jean-zara-skinny.jpg', 1, GETDATE(), GETDATE()),
    (9, 8, N'AF1-TRANG-40', N'Giày Nike Air Force 1 - 40', 2800000, 5, 'https://storage.googleapis.com/images-storage-bezbe/variants/nike-af1-trang.jpg', 1, GETDATE(), GETDATE()),
    (10, 8, N'AF1-TRANG-41', N'Giày Nike Air Force 1 - 41', 2800000, 7, 'https://storage.googleapis.com/images-storage-bezbe/variants/nike-af1-trang.jpg', 1, GETDATE(), GETDATE());
SET IDENTITY_INSERT [variants] OFF;

-- 10. Variant Option Values (Kết nối Biến thể với Giá trị) - (Giữ nguyên)
SET IDENTITY_INSERT [variant_option_values] ON;
INSERT INTO [variant_option_values] (id, variant_id, option_value_id, option_id) VALUES
    (1, 1, 1, 1), (2, 1, 4, 2),
    (3, 2, 1, 1), (4, 2, 5, 2),
    (5, 3, 2, 1), (6, 3, 5, 2),
    (7, 4, 3, 1), (8, 4, 6, 2),
    (9, 5, 8, 3), (10, 5, 10, 4),
    (11, 6, 9, 3), (12, 6, 11, 4),
    (13, 7, 12, 5),
    (14, 8, 13, 5),
    (15, 9, 15, 6),
    (16, 10, 16, 6);
SET IDENTITY_INSERT [variant_option_values] OFF;


-- BẬT LẠI KIỂM TRA KHÓA NGOẠI
EXEC sp_MSforeachtable "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT all"

PRINT N'Hoàn thành chèn dữ liệu mẫu! Bảng [users] đã được giữ nguyên.';