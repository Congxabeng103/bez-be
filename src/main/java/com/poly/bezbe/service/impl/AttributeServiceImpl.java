package com.poly.bezbe.service.impl; // <-- Chú ý package con 'impl'

import com.poly.bezbe.dto.request.product.AttributeRequestDTO;
import com.poly.bezbe.dto.request.product.AttributeValueRequestDTO;
import com.poly.bezbe.dto.response.product.AttributeResponseDTO;
import com.poly.bezbe.dto.response.product.AttributeValueResponseDTO;
import com.poly.bezbe.entity.Attribute;
import com.poly.bezbe.entity.AttributeValue;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.AttributeRepository;
import com.poly.bezbe.repository.AttributeValueRepository;
import com.poly.bezbe.service.AttributeService; // <-- Import interface
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service // <-- Annotation @Service được đặt ở đây
@RequiredArgsConstructor
public class AttributeServiceImpl implements AttributeService { // <-- Implement interface

    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;

    // Hàm private để chuyển đổi Attribute Entity -> DTO (bao gồm cả values)
    // Hàm private này nằm ở lớp Impl là đúng
    private AttributeResponseDTO mapToAttributeDTO(Attribute attribute) {
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
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional(readOnly = true)
    public List<AttributeResponseDTO> getAllAttributes() {
        return attributeRepository.findAll().stream()
                .map(this::mapToAttributeDTO) // Dùng hàm map đã có sẵn
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public AttributeResponseDTO createAttribute(AttributeRequestDTO request) {
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
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public void deleteAttribute(Long attributeId) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId);
        }
        attributeRepository.deleteById(attributeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public AttributeValueResponseDTO addAttributeValue(Long attributeId, AttributeValueRequestDTO request) {
        Attribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId));

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
     * {@inheritDoc}
     */
    @Override // <-- Thêm @Override
    @Transactional
    public void deleteAttributeValue(Long valueId) {
        AttributeValue valueToDelete = attributeValueRepository.findById(valueId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giá trị thuộc tính với ID: " + valueId));

        attributeValueRepository.deleteById(valueId);
    }
}