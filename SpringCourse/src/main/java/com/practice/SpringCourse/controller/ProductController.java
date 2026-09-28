package com.practice.SpringCourse.controller;

import com.practice.SpringCourse.model.Products;
import com.practice.SpringCourse.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
public class ProductController {

    @Autowired
    ProductService service;

    @GetMapping("/")
    public String home(HttpServletRequest request){
        return "Welcome to home page : " + request.getSession().getId();
    }

    @GetMapping("/ShowProducts")
    public List<Products> getProducts(){

        return service.getAllProducts();
    }

    @PostMapping("/product")
    public ResponseEntity<?> addProduct(@RequestPart Products product , @RequestPart MultipartFile image){

        try{
            Products product1 = service.addProduct(product,image);
            return new ResponseEntity<>(product1 , HttpStatus.CREATED);
        }catch (Exception e){

            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @GetMapping("/product/{id}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable int id) {

        Products product = service.getProductById(id);
        byte[] image = product.getImageData();

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(product.getImageType()))
                .body(image);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteItem(@PathVariable int id){
        service.deleteItem(id);

        return "Successfully deleted";
    }
}
