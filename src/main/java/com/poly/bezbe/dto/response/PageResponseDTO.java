package com.poly.bezbe.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponseDTO<T> {
    private List<T> content;      // Nội dung của trang hiện tại
    private int pageNo;           // Số trang hiện tại
    private int pageSize;         // Kích thước của trang
    private long totalElements;   // Tổng số phần tử
    private int totalPages;       // Tổng số trang
}