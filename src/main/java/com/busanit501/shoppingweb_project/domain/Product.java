package com.busanit501.shoppingweb_project.domain;

import com.busanit501.shoppingweb_project.domain.enums.ProductCategory;
import com.busanit501.shoppingweb_project.domain.enums.ProductStatus;
import com.busanit501.shoppingweb_project.dto.ProductDTO;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Builder
@Getter

@NoArgsConstructor
@AllArgsConstructor
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    private String productName;
    private BigDecimal price;
    private int stock;
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @OneToMany(mappedBy = "product" , cascade = {CascadeType.ALL}
    , fetch = FetchType.LAZY,
    orphanRemoval = true)
    @Builder.Default
    @JsonManagedReference
    @BatchSize(size = 20)
    private Set<ProductImage> imageSet = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private ProductCategory productTag;

    public void removeStock(int quantity) {
        int restStock = this.stock - quantity;
        if (restStock < 0) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + this.stock);
        }
        this.stock = restStock;
    }

    public void addStock(int quantity) {
        this.stock += quantity;
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    public void addCartItem(CartItem cartItem) {
        this.cartItems.add(cartItem);
        cartItem.setCartItems(this); // FK 설정
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    public void addReview(Review review) {
        this.reviews.add(review);
        review.setProduct(this);
    }

    public void removeReview(Review review) {
        this.reviews.remove(review);
        review.setProduct(null);
    }
    public void addImage(ProductImage image) {
        imageSet.add(image);
        image.setProduct(this);
    }

    public void changeTitleContent(ProductDTO productDTO) {
        this.productName = productDTO.getProductName();
        this.price = productDTO.getPrice();
        this.stock = productDTO.getStock();
        this.productTag = productDTO.getProductTag();
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Optional<ProductImage> getThumbnailImage(){
        return imageSet.stream()
                .filter(ProductImage::isThumbnail)
                .findFirst();
    }

    public static ProductDTO entityToDTO(Product product){
        String thumbnailFileName = product.getThumbnailImage()
                .map(ProductImage::getFileName)
                .orElse(null);

        List<String> detailFileNames = product.getImageSet().stream()
                .filter(img -> !img.isThumbnail())
                .map(ProductImage::getFileName)
                .toList();

        return ProductDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .price(product.getPrice())
                .stock(product.getStock())
                .productTag(product.getProductTag())
                .thumbnailFileName(thumbnailFileName)
                .fileNames(detailFileNames)
                .status(product.getStatus())
                .build();
    }



}
