package com.busanit501.shoppingweb_project.service;

import com.busanit501.shoppingweb_project.domain.Product;
import com.busanit501.shoppingweb_project.domain.ProductImage;
import com.busanit501.shoppingweb_project.domain.enums.ProductCategory;
import com.busanit501.shoppingweb_project.domain.enums.ProductStatus;
import com.busanit501.shoppingweb_project.dto.ProductDTO;
import com.busanit501.shoppingweb_project.repository.ProductRepository;
import com.busanit501.shoppingweb_project.repository.ProductImageRepository;
import com.busanit501.shoppingweb_project.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional()
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ModelMapper modelMapper;
    private final FileUploadService fileUploadService;
    private final ReviewRepository reviewRepository;

    @Override
    public ProductDTO getProductById(Long productId) {
        log.info("ProductService - getProductById: " + productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다. id=" + productId));

        ProductDTO productDTO =  Product.entityToDTO(product);
        log.info("ProductService 에서 작업중 productDTO.thumbnailFileName : " + productDTO.getThumbnailFileName());
        log.info("ProductService 에서 작업중 productDTO.FileName : " + productDTO.getFileNames());
        // 썸네일 이미지 설정
        productImageRepository.findByProduct_ProductIdAndThumbnail(product.getProductId(), true)
                .ifPresent(productImage -> productDTO.setImageFileName(productImage.getFileName()));
        log.info("ProductService 에서 작업중 productDTO.thumbnailFileName : " + productDTO.getThumbnailFileName());
        log.info("ProductService 에서 작업중 productDTO.FileName : " + productDTO.getFileNames());

        return productDTO;
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        log.info("ProductService - getAllProducts 호출 (ACTIVE 상품만)");
        List<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE);
        return products.stream()
                .map(this::mapProductToDtoWithImage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getAllProductsForAdmin() {
        log.info("ProductService - getAllProductsForAdmin 호출 (모든 상품)");
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::mapProductToDtoWithImage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getProductsByCategory(String category) {
        log.info("ProductService - getProductsByCategory: " + category);
        ProductCategory productCategory = ProductCategory.fromKoreanName(category);
        List<Product> products = productRepository.findByProductTagAndStatus(productCategory, ProductStatus.ACTIVE);
        return products.stream()
                .map(this::mapProductToDtoWithImage)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        log.info("ProductService - searchProducts: " + keyword);
        List<Product> products = productRepository.searchByKeywordAndStatus(keyword, ProductStatus.ACTIVE);
        return products.stream()
                .map(this::mapProductToDtoWithImage)
                .collect(Collectors.toList());
    }

    @Override
    public void saveProduct(Product product) {
        productRepository.save(product);
    }

    // 새 상품 등록 메서드 구현
    @Override
    public ProductDTO createProduct(ProductDTO productDTO) {
        if (productDTO.getProductTag() == null) {
            productDTO.setProductTag(ProductCategory.UNKNOWN);
        }
        Product product = Product.builder()
                .productName(productDTO.getProductName())
                .price(productDTO.getPrice())
                .stock(productDTO.getStock())
                .productTag(productDTO.getProductTag())
                .build();
        Product savedProduct = productRepository.save(product);
        return Product.entityToDTO(savedProduct);
    }

    // 상품 수정 메서드 구현
    @Override
    public void updateProductWithImages(Long productId, String productName, BigDecimal price, int stock, ProductCategory productTag,
                                        MultipartFile thumbnail, List<MultipartFile> detailImages, String deleteImagesJson) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다. id=" + productId));

        // 1. 상품 기본 정보 업데이트
        ProductDTO productDTO = ProductDTO.builder()
                .productName(productName).price(price).stock(stock).productTag(productTag).build();
        product.changeTitleContent(productDTO);

        // 2. 삭제할 이미지 처리
        if (deleteImagesJson != null && !deleteImagesJson.isEmpty()) {
            try {
                List<String> deleteImageFileNames = new ObjectMapper().readValue(deleteImagesJson, new TypeReference<List<String>>() {});
                productImageRepository.findByFileNameIn(deleteImageFileNames).forEach(image -> {
                    fileUploadService.deleteFile(image.getFileName());
                    product.getImageSet().remove(image);
                    productImageRepository.delete(image);
                });
            } catch (IOException e) {
                log.error("이미지 파일명 JSON 파싱 오류", e);
            }
        }

        // 3. 새 썸네일 이미지 처리
        if (thumbnail != null && !thumbnail.isEmpty()) {
            // 기존 썸네일 삭제
            product.getThumbnailImage().ifPresent(existingThumbnail -> {
                fileUploadService.deleteFile(existingThumbnail.getFileName());
                product.getImageSet().remove(existingThumbnail);
                productImageRepository.delete(existingThumbnail);
            });
            // 새 썸네일 저장
            try {
                String savedFileName = fileUploadService.saveFile(thumbnail);
                ProductImage thumbnailEntity = ProductImage.builder()
                        .fileName(savedFileName).ord(0).thumbnail(true).build();
                product.addImage(thumbnailEntity);
            } catch (IOException e) {
                log.error("썸네일 업로드 실패", e);
            }
        }

        // 4. 새 상세 이미지 처리
        if (detailImages != null && !detailImages.isEmpty()) {
            int order = product.getImageSet().stream()
                            .mapToInt(ProductImage::getOrd).max().orElse(0) + 1;
            for (MultipartFile file : detailImages) {
                if (!file.isEmpty()) {
                    try {
                        String savedFileName = fileUploadService.saveFile(file);
                        ProductImage detailImageEntity = ProductImage.builder()
                                .fileName(savedFileName).ord(order++).thumbnail(false).build();
                        product.addImage(detailImageEntity);
                    } catch (IOException e) {
                        log.error("상세 이미지 업로드 실패", e);
                    }
                }
            }
        }
        productRepository.save(product);
    }

    // 상품 삭제 메서드 구현 (Soft Delete)
    @Override
    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다. id=" + productId));
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    @Override
    public void createProductWithImages(String productName, BigDecimal price, int stock, ProductCategory productTag, MultipartFile thumbnail, List<MultipartFile> detailImages) {
        Product product = Product.builder()
                .productName(productName)
                .price(price)
                .stock(stock)
                .productTag(productTag)
                .build();
        log.info("ProductService에서 작업중 관리자가 생성한 Product : "+ product.getProductName());
        productRepository.save(product);

        // 2. 섬네일 이미지 저장 thumnail = true로 저장
        if(thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String savedFileName = fileUploadService.saveFile(thumbnail);
                ProductImage thumbnailEntity = ProductImage.builder()
                        .fileName(savedFileName)
                        .ord(0)
                        .thumbnail(true)
                        .build();
                product.addImage(thumbnailEntity);
                log.info("ProductService에서 작업중 썸네일 이미지인지 확인 : " + thumbnailEntity.getFileName());
                productImageRepository.save(thumbnailEntity);
            }catch(IOException e) {
                e.printStackTrace();
            }

            //상세 이미지 DB에 저장 thumnail = false로 표시해주기
            if(detailImages != null && !detailImages.isEmpty()) {
                int order=1;
                for(MultipartFile file : detailImages){
                    try {
                        if(!file.isEmpty()) {
                            String savedFileName = fileUploadService.saveFile(file);
                            ProductImage detailImageEntity = ProductImage.builder()
                                    .fileName(savedFileName)
                                    .ord(order++)
                                    .thumbnail(false)
                                    .build();
                            product.addImage(detailImageEntity);
                            log.info("ProductService에서 작업중 썸네일 이미지인지 확인 : " + detailImageEntity.getFileName());
                            productImageRepository.save(detailImageEntity);
                        }
                    }catch(IOException e) {}
                }
            }
        }
    }

    // Helper method to map Product to ProductDTO and attach image filename
    @Override
    public ProductDTO mapProductToDtoWithImage(Product product) {
        ProductDTO dto = Product.entityToDTO(product);
        productImageRepository.findByProduct_ProductIdAndThumbnail(product.getProductId(), true)
                .ifPresent(productImage -> dto.setImageFileName(productImage.getFileName()));
        return dto;
    }

}
