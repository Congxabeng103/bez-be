package com.poly.bezbe.dto.request.product;


import jakarta.validation.constraints.*; // Import validation annotations
import lombok.Data;
import java.math.BigDecimal;

@Data // Lombok: Tự tạo getter, setter, toString, equals, hashCode
public class ProductRequestDTO {

    @NotEmpty(message = "Tên sản phẩm không được để trống") // Ràng buộc: Không được rỗng
    private String name;

    private String description; // Mô tả có thể rỗng

    @NotNull(message = "Giá không được để trống") // Ràng buộc: Không được null
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0") // Ràng buộc: Giá trị thập phân tối thiểu (không bao gồm 0)
    private BigDecimal price; // Giá gốc

    private String imageUrl; // URL ảnh có thể rỗng

    // ID của Category và Brand, có thể null nếu không bắt buộc
    private Long categoryId;
    private Long brandId;

    // ID của Promotion, có thể null nếu không áp dụng khuyến mãi
    private Long promotionId;
}