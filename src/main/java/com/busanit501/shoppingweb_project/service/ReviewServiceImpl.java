package com.busanit501.shoppingweb_project.service;

import com.busanit501.shoppingweb_project.domain.Member;
import com.busanit501.shoppingweb_project.domain.Product;
import com.busanit501.shoppingweb_project.domain.Review;
import com.busanit501.shoppingweb_project.dto.PageRequestDTO;
import com.busanit501.shoppingweb_project.dto.PageResponseDTO;
import com.busanit501.shoppingweb_project.dto.ReviewDTO;
import com.busanit501.shoppingweb_project.repository.MemberRepository;
import com.busanit501.shoppingweb_project.repository.ProductRepository;
import com.busanit501.shoppingweb_project.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Override
    public void createReview(ReviewDTO dto, Long memberId) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));

                LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatted = now.format(formatter);

        Review review = Review.builder()
                .reviewContent(dto.getReviewContent())
                .rating(dto.getRating())
                .createdAt(formatted)
                .product(product)
                .member(member)
                .build();

        review.setProduct(product);
        review.setMember(member);
        product.addReview(review);

        reviewRepository.save(review);
    }

    @Override
    public PageResponseDTO<ReviewDTO> getList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable();

        Page<Review> result = reviewRepository.findByProduct_ProductId(requestDTO.getProductId(), pageable);
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String formatted = now.format(formatter);
        List<ReviewDTO> dtoList = result.getContent().stream().map(review -> ReviewDTO.builder()
                .reviewId(review.getReviewId())
                .reviewContent(review.getReviewContent())
                .rating(review.getRating())
                .memberName(review.getMember().getUserName())
                .memberId(review.getMember().getId())
                .createdAt(review.getCreatedAt())
                .productId(review.getProduct().getProductId())
                .build()).collect(Collectors.toList());
        dtoList.forEach(dto -> log.info("ReviewService에서 작업중 페이징 된 10개의 뎃글 표시하기 : " + dto.getReviewContent()));


        PageResponseDTO<ReviewDTO> responseDTO = PageResponseDTO.<ReviewDTO>builder()
                .dtoList(dtoList)
                .page(requestDTO.getPage())
                .size(requestDTO.getSize())
                .build();

        responseDTO.setTotal((int) result.getTotalElements());

        return responseDTO;
    }

    @Transactional
    public void updateReview(Long reviewId, String newContent, Long memberId) throws AccessDeniedException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰 없음"));

        if (!review.getMember().getId().equals(memberId)) {
            throw new AccessDeniedException("본인만 수정 가능");
        }

        review.updateContent(newContent); // 엔티티에 setter 또는 별도 메서드 필요
    }
}
