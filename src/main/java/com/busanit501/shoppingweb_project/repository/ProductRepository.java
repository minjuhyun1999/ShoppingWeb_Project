package com.busanit501.shoppingweb_project.repository;

import com.busanit501.shoppingweb_project.domain.Product;
import com.busanit501.shoppingweb_project.domain.enums.ProductCategory;
import com.busanit501.shoppingweb_project.domain.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // 기본적인 CRUD
    @Query("select b from Product b where b.productId = :productId")
    Product findByProductId(@Param("productId")Long productId);
    //SELECT * FROM PRODUCT WHERE PRODUCTID = porductId
    List<Product> findByProductTagAndStatus(ProductCategory productTag, ProductStatus status);
    @Query("SELECT p FROM Product p WHERE p.productName LIKE %:keyword% AND p.status = :status")
    List<Product> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") ProductStatus status);

    // 관리자용: 모든 상태의 상품 조회
    List<Product> findAll();

    // 사용자용: 활성 상태의 상품만 조회
    List<Product> findByStatus(ProductStatus status);
}