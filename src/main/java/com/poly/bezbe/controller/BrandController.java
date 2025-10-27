package com.poly.bezbe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.poly.bezbe.service.BrandService;
import com.poly.bezbe.entity.Brand;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin("*")
public class BrandController {

    @Autowired
    private BrandService brandService;

    // Lấy tất cả brand
    @GetMapping
    public List<Brand> getAllBrands() {
        return brandService.getAllBrands();
    }

    // Lấy brand theo id
    @GetMapping("/{id}")
    public Brand getBrandById(@PathVariable Long id) {
        return brandService.getBrandById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id " + id));
    }

    // Tạo brand mới
    @PostMapping
    public Brand createBrand(@RequestBody Brand brand) {
        return brandService.createBrand(brand);
    }

    // Cập nhật brand
    @PutMapping("/{id}")
    public Brand updateBrand(@PathVariable Long id, @RequestBody Brand brandDetails) {
        return brandService.updateBrand(id, brandDetails);
    }

    // Xóa brand
    @DeleteMapping("/{id}")
    public String deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return "Brand deleted with id: " + id;
    }
}
