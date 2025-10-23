package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.product.AttributeRequestDTO;
import com.poly.bezbe.dto.request.product.AttributeValueRequestDTO;
import com.poly.bezbe.dto.response.product.AttributeResponseDTO;
import com.poly.bezbe.dto.response.product.AttributeValueResponseDTO;
import com.poly.bezbe.entity.Attribute;
import com.poly.bezbe.entity.AttributeValue;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.AttributeRepository;
import com.poly.bezbe.repository.AttributeValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections; // Import Collections
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttributeService {

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    // Hàm private để chuyển đổi Attribute Entity -> DTO (bao gồm cả values)
    private AttributeResponseDTO mapToAttributeDTO(Attribute attribute) {
        // Lấy danh sách các giá trị con từ DB
        List<AttributeValueResponseDTO> valueDTOs = attributeValueRepository.findByAttribute_Id(attribute.getId())
                .stream()
                .map(val -> AttributeValueResponseDTO.builder()
                        .id(val.getId())
                        .value(val.getValue())
                        .build())
                .collect(Collectors.toList());

        return AttributeResponseDTO.builder()
                .id(attribute.getId())
                .name(attribute.getName())
                .values(valueDTOs) // Gắn danh sách giá trị vào DTO
                .build();
    }

    /**
     * Lấy tất cả thuộc tính và giá trị của chúng.
     */
    @Transactional(readOnly = true)
    public List<AttributeResponseDTO> getAllAttributes() {
        return attributeRepository.findAll().stream()
                .map(this::mapToAttributeDTO) // Dùng hàm map đã có sẵn
                .collect(Collectors.toList());
    }

    /**
     * Tạo một thuộc tính mới (ví dụ: "Chất liệu").
     */
    @Transactional
    public AttributeResponseDTO createAttribute(AttributeRequestDTO request) {
        // (Bạn có thể thêm kiểm tra trùng tên ở đây nếu muốn)
        Attribute attribute = Attribute.builder()
                .name(request.getName().trim()) // Trim khoảng trắng
                .build();
        Attribute savedAttribute = attributeRepository.save(attribute);
        // Trả về DTO với danh sách values rỗng ban đầu
        return AttributeResponseDTO.builder()
                .id(savedAttribute.getId())
                .name(savedAttribute.getName())
                .values(Collections.emptyList())
                .build();
    }

    /**
     * Xóa một thuộc tính và tất cả giá trị con của nó (do Cascade).
     */
    @Transactional
    public void deleteAttribute(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId);
        }
        // Do có CascadeType.ALL (hoặc tương tự) trong mối quan hệ,
        // việc xóa Attribute sẽ tự động xóa các AttributeValue liên quan.
        // Tuy nhiên, bạn cần kiểm tra xem có biến thể nào đang dùng các giá trị này không
        // trước khi xóa để tránh lỗi khóa ngoại (Foreign Key constraint).
        // (Logic kiểm tra ràng buộc này phức tạp hơn, tạm thời bỏ qua)
        attributeRepository.deleteById(attributeId);
    }

    /**
     * Thêm một giá trị mới vào thuộc tính (ví dụ: thêm "Cotton" vào "Chất liệu").
     */
    @Transactional
    public AttributeValueResponseDTO addAttributeValue(Long attributeId, AttributeValueRequestDTO request) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId));

        // (Bạn có thể thêm kiểm tra trùng giá trị trong cùng 1 thuộc tính)
        AttributeValue newValue = AttributeValue.builder()
                .attribute(attribute)
                .value(request.getValue().trim()) // Trim khoảng trắng
                .build();
        AttributeValue savedValue = attributeValueRepository.save(newValue);

        // Trả về DTO của giá trị vừa tạo
        return AttributeValueResponseDTO.builder()
                .id(savedValue.getId())
                .value(savedValue.getValue())
                .build();
    }

    /**
     * Xóa một giá trị thuộc tính.
     */
    @Transactional
    public void deleteAttributeValue(Long valueId) {
        AttributeValue valueToDelete = attributeValueRepository.findById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá trị thuộc tính với ID: " + valueId));

        // Tương tự deleteAttribute, bạn cần kiểm tra xem có biến thể nào
        // đang sử dụng giá trị này không trước khi xóa.
        // (Tạm thời bỏ qua logic kiểm tra ràng buộc)
        attributeValueRepository.deleteById(valueId);
    }
}