package com.busanit501.shoppingweb_project.repository;

import com.busanit501.shoppingweb_project.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    Optional<ProductImage> findByProduct_ProductIdAndThumbnail(Long productId, boolean thumbnail);
    List<ProductImage> findByFileNameIn(List<String> fileNames);
}