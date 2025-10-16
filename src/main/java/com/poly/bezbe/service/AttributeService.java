package com.poly.bezbe.service;

import com.poly.bezbe.dto.request.attribute.AttributeRequestDTO;
import com.poly.bezbe.dto.request.attribute.AttributeValueRequest;
import com.poly.bezbe.dto.response.attribute.AttributeResponseDTO;
import com.poly.bezbe.dto.response.attribute.AttributeValueResponseDTO;
import com.poly.bezbe.dto.response.PageResponseDTO;
import com.poly.bezbe.entity.ProductVariantAttribute;
import com.poly.bezbe.entity.ProductVariantAttributeValue;
import com.poly.bezbe.exception.ResourceNotFoundException;
import com.poly.bezbe.repository.ProductVariantAttributeRepository;
import com.poly.bezbe.repository.ProductVariantAttributeValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttributeService {

    private final ProductVariantAttributeRepository attributeRepository;
    private final ProductVariantAttributeValueRepository valueRepository;

    // --- LOGIC CHO THUỘC TÍNH (ATTRIBUTE) ---

    public AttributeResponseDTO createAttribute(AttributeRequestDTO request) {
        ProductVariantAttribute attribute = ProductVariantAttribute.builder().name(request.getName()).build();
        ProductVariantAttribute savedAttribute = attributeRepository.save(attribute);
        return convertToAttributeDTO(savedAttribute);
    }

    public PageResponseDTO<AttributeResponseDTO> getAllAttributes(Pageable pageable) {
        Page<ProductVariantAttribute> attributePage = attributeRepository.findAll(pageable);
        List<AttributeResponseDTO> content = attributePage.getContent().stream()
                .map(this::convertToAttributeDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<AttributeResponseDTO>builder()
                .content(content)
                .pageNo(attributePage.getNumber())
                .pageSize(attributePage.getSize())
                .totalElements(attributePage.getTotalElements())
                .totalPages(attributePage.getTotalPages())
                .build();
    }

    public AttributeResponseDTO getAttributeById(Long id) {
        ProductVariantAttribute attribute = attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + id));
        return convertToAttributeDTO(attribute);
    }

    public AttributeResponseDTO updateAttribute(Long id, AttributeRequestDTO request) {
        ProductVariantAttribute existingAttribute = attributeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + id));
        existingAttribute.setName(request.getName());
        ProductVariantAttribute updatedAttribute = attributeRepository.save(existingAttribute);
        return convertToAttributeDTO(updatedAttribute);
    }

    public void deleteAttribute(Long id) {
        if (!attributeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + id);
        }
        attributeRepository.deleteById(id);
    }

    // --- LOGIC CHO GIÁ TRỊ THUỘC TÍNH (ATTRIBUTE VALUE) ---

    public AttributeValueResponseDTO createAttributeValue(Long attributeId, AttributeValueRequest request) {
        ProductVariantAttribute attribute = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thuộc tính cha với ID: " + attributeId));
        ProductVariantAttributeValue newValue = ProductVariantAttributeValue.builder()
                .attribute(attribute)
                .value(request.getValue())
                .build();
        ProductVariantAttributeValue savedValue = valueRepository.save(newValue);
        return convertToValueDTO(savedValue);
    }

    public PageResponseDTO<AttributeValueResponseDTO> getValuesForAttribute(Long attributeId, Pageable pageable) {
        if (!attributeRepository.existsById(attributeId)) {
            throw new ResourceNotFoundException("Không tìm thấy thuộc tính với ID: " + attributeId);
        }
        Page<ProductVariantAttributeValue> valuePage = valueRepository.findAllByAttributeId(attributeId, pageable);
        List<AttributeValueResponseDTO> content = valuePage.getContent().stream()
                .map(this::convertToValueDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<AttributeValueResponseDTO>builder()
                .content(content)
                .pageNo(valuePage.getNumber())
                .pageSize(valuePage.getSize())
                .totalElements(valuePage.getTotalElements())
                .totalPages(valuePage.getTotalPages())
                .build();
    }

    public void deleteAttributeValue(Long valueId) {
        if (!valueRepository.existsById(valueId)) {
            throw new ResourceNotFoundException("Không tìm thấy giá trị thuộc tính với ID: " + valueId);
        }
        valueRepository.deleteById(valueId);
    }

    // --- Private Mapper Methods ---
    private AttributeResponseDTO convertToAttributeDTO(ProductVariantAttribute attribute) {
        return AttributeResponseDTO.builder().id(attribute.getId()).name(attribute.getName()).build();
    }

    private AttributeValueResponseDTO convertToValueDTO(ProductVariantAttributeValue value) {
        return AttributeValueResponseDTO.builder().id(value.getId()).value(value.getValue()).build();
    }
}