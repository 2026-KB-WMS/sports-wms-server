package com.kb.wms.product.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kb.wms.product.application.port.in.BrandQueryUseCase;
import com.kb.wms.product.application.port.out.BrandRepository;
import com.kb.wms.product.domain.entity.Brand;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandService implements BrandQueryUseCase {

    private final BrandRepository brandRepository;

    @Override
    public List<Brand> getBrands() {
        return brandRepository.findAll();
    }
}
