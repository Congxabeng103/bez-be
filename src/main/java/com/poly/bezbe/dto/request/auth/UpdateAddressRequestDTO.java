package com.poly.bezbe.dto.request.auth; // (Bạn có thể đặt ở package com.poly.bezbe.dto.request cũng được)

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAddressRequestDTO {

    @NotBlank(message = "Số nhà, tên đường là bắt buộc")
    private String streetAddress;

    @NotNull(message = "Mã Tỉnh/Thành là bắt buộc")
    private Integer provinceCode;

    @NotBlank(message = "Tên Tỉnh/Thành là bắt buộc")
    private String provinceName;

    @NotNull(message = "Mã Quận/Huyện là bắt buộc")
    private Integer districtCode;

    @NotBlank(message = "Tên Quận/Huyện là bắt buộc")
    private String districtName;

    @NotNull(message = "Mã Phường/Xã là bắt buộc")
    private Integer wardCode;

    @NotBlank(message = "Tên Phường/Xã là bắt buộc")
    private String wardName;
}