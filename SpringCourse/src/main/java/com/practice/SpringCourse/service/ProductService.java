package com.practice.SpringCourse.service;

import com.practice.SpringCourse.model.Products;
import com.practice.SpringCourse.repository.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ProductService {

    @Autowired
    ProductRepo productRepo;

    public List<Products> getAllProducts() {
        return productRepo.findAll();
    }

    public Products addProduct(Products product, MultipartFile image) throws IOException {

        product.setImageName(image.getOriginalFilename());
        product.setImageType(image.getContentType());
        product.setImageData(image.getBytes());

        return productRepo.save(product);
    }

    public Products getProductById(int id) {

        return productRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id:" + id));
    }

    public void deleteItem(int id) {

        try{
            productRepo.deleteById(id);
        }catch (Exception e){
            throw new IllegalArgumentException("product not found");
        }
    }
}
