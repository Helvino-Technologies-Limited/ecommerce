package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Product;
import com.helvino.ecommerce.entity.Review;
import com.helvino.ecommerce.entity.User;
import com.helvino.ecommerce.repository.ProductRepository;
import com.helvino.ecommerce.repository.ReviewRepository;
import com.helvino.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    /** Public: paginated reviews for a product */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<Page<Review>> getReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Review> reviews = reviewRepo.findByProductId(
                productId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(reviews);
    }

    /** Authenticated customer: create a review */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<?> createReview(
            @PathVariable UUID productId,
            @RequestBody Map<String, Object> body,
            Authentication auth) {

        User user = userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // One review per customer per product
        if (reviewRepo.existsByProductIdAndUserId(productId, user.getId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "You have already reviewed this product"));
        }

        int rating = ((Number) body.get("rating")).intValue();
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Rating must be between 1 and 5"));
        }

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(rating)
                .comment((String) body.get("comment"))
                .verified(false)
                .build();

        reviewRepo.save(review);

        // Recalculate averageRating and reviewCount
        List<Review> allReviews = reviewRepo.findByProductId(productId);
        OptionalDouble avg = allReviews.stream().mapToInt(Review::getRating).average();
        product.setAverageRating(avg.orElse(0.0));
        product.setReviewCount(allReviews.size());
        productRepo.save(product);

        return ResponseEntity.ok(review);
    }
}
