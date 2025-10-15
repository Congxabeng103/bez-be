package com.poly.bezbe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.poly.bezbe.entity.Brand;
import com.poly.bezbe.repository.BrandRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    // Lấy tất cả brand
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    // Lấy brand theo id
    public Optional<Brand> getBrandById(Long id) {
        return brandRepository.findById(id);
    }

    // Tạo brand mới
    public Brand createBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    // Cập nhật brand
    public Brand updateBrand(Long id, Brand brandDetails) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id " + id));
        brand.setName(brandDetails.getName());
        brand.setImageUrl(brandDetails.getImageUrl());
        return brandRepository.save(brand);
    }

    // Xóa brand
    public void deleteBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id " + id));
        brandRepository.delete(brand);
    }
}
