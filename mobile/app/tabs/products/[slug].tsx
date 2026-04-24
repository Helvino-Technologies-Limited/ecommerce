import React, { useState } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, Image, StyleSheet,
  Dimensions, ActivityIndicator, FlatList,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router, useLocalSearchParams } from "expo-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { SafeAreaView } from "react-native-safe-area-context";
import Toast from "react-native-toast-message";
import api from "@/src/lib/api";
import { Product, ProductVariant, Review, ApiPage } from "@/src/types";
import { formatPrice } from "@/src/lib/utils";
import { useCartStore } from "@/src/store/cart";
import { useAuthStore } from "@/src/store/auth";
import StarRating from "@/src/components/StarRating";
import ProductCard from "@/src/components/ProductCard";

const { width } = Dimensions.get("window");

export default function ProductDetailScreen() {
  const { slug } = useLocalSearchParams<{ slug: string }>();
  const { addItem } = useCartStore();
  const { isAuthenticated } = useAuthStore();
  const qc = useQueryClient();
  const [activeImage, setActiveImage] = useState(0);
  const [qty, setQty] = useState(1);
  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState("");

  const { data: product, isLoading } = useQuery<Product>({
    queryKey: ["product", slug],
    queryFn: () => api.get(`/products/${slug}`).then((r) => r.data),
  });

  const { data: variants = [] } = useQuery<ProductVariant[]>({
    queryKey: ["variants", product?.id],
    queryFn: () => api.get(`/products/${product!.id}/variants`).then((r) => r.data),
    enabled: !!product?.id,
  });

  const { data: reviewsPage } = useQuery<ApiPage<Review>>({
    queryKey: ["reviews", product?.id],
    queryFn: () => api.get(`/products/${product!.id}/reviews?size=10`).then((r) => r.data),
    enabled: !!product?.id,
  });

  const { data: relatedPage } = useQuery<ApiPage<Product>>({
    queryKey: ["related", product?.category?.id],
    queryFn: () => api.get("/products", { params: { categoryId: product!.category?.id, size: 6 } }).then((r) => r.data),
    enabled: !!product?.category?.id,
  });

  const reviewMutation = useMutation({
    mutationFn: () =>
      api.post(`/products/${product!.id}/reviews`, { rating: reviewRating, comment: reviewComment }),
    onSuccess: () => {
      Toast.show({ type: "success", text1: "Review submitted!" });
      setReviewComment("");
      qc.invalidateQueries({ queryKey: ["reviews", product?.id] });
      qc.invalidateQueries({ queryKey: ["product", slug] });
    },
    onError: (err: any) => {
      Toast.show({ type: "error", text1: err.response?.data?.message ?? "Failed to submit review" });
    },
  });

  if (isLoading || !product) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator color="#059669" size="large" />
      </View>
    );
  }

  const basePrice = product.flashSale && product.flashSaleDiscount
    ? product.price * (1 - product.flashSaleDiscount / 100)
    : product.price;
  const variantAdj = selectedVariant ? Number(selectedVariant.priceAdjustment) : 0;
  const currentPrice = basePrice + variantAdj;

  const handleAddToCart = () => {
    for (let i = 0; i < qty; i++) {
      addItem({
        id: product.id + (selectedVariant ? `-${selectedVariant.id}` : ""),
        name: product.name + (selectedVariant ? ` (${selectedVariant.variantValue})` : ""),
        price: product.price,
        discountedPrice: currentPrice !== product.price ? currentPrice : undefined,
        image: product.images[0] ?? "",
        currency: product.currency,
        slug: product.slug,
      });
    }
    Toast.show({ type: "success", text1: `${qty} × ${product.name} added` });
  };

  const handleBuyNow = () => {
    handleAddToCart();
    router.push("/checkout");
  };

  const reviews = reviewsPage?.content ?? [];
  const relatedProducts = (relatedPage?.content ?? []).filter((p) => p.id !== product.id);

  // Group variants by type
  const variantGroups = variants.reduce<Record<string, ProductVariant[]>>((acc, v) => {
    if (!acc[v.variantType]) acc[v.variantType] = [];
    acc[v.variantType].push(v);
    return acc;
  }, {});

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <ScrollView showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
            <Ionicons name="arrow-back" size={22} color="#111827" />
          </TouchableOpacity>
          <TouchableOpacity style={styles.cartBtn} onPress={() => router.push("/tabs/cart")}>
            <Ionicons name="cart-outline" size={22} color="#111827" />
          </TouchableOpacity>
        </View>

        {/* Images */}
        <View style={styles.imageContainer}>
          <Image
            source={{ uri: product.images[activeImage] }}
            style={styles.mainImage}
            resizeMode="contain"
          />
          {product.flashSale && product.flashSaleDiscount && (
            <View style={styles.flashBadge}>
              <Ionicons name="flash" size={12} color="#fff" />
              <Text style={styles.flashBadgeText}>-{product.flashSaleDiscount}%</Text>
            </View>
          )}
        </View>

        {product.images.length > 1 && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.thumbnails}>
            {product.images.map((img, i) => (
              <TouchableOpacity key={i} onPress={() => setActiveImage(i)}
                style={[styles.thumbnail, i === activeImage && styles.thumbnailActive]}>
                <Image source={{ uri: img }} style={styles.thumbnailImage} resizeMode="contain" />
              </TouchableOpacity>
            ))}
          </ScrollView>
        )}

        {/* Info */}
        <View style={styles.info}>
          <Text style={styles.category}>{product.category?.name}</Text>
          <Text style={styles.name}>{product.name}</Text>

          {product.reviewCount > 0 && (
            <View style={styles.ratingRow}>
              <StarRating rating={product.averageRating} size={14} />
              <Text style={styles.ratingText}>{product.averageRating.toFixed(1)} ({product.reviewCount})</Text>
            </View>
          )}

          {/* Price */}
          <View style={styles.priceContainer}>
            <Text style={styles.price}>{formatPrice(currentPrice, product.currency)}</Text>
            {currentPrice < product.price && (
              <Text style={styles.originalPrice}>{formatPrice(product.price, product.currency)}</Text>
            )}
          </View>

          {/* Variants */}
          {Object.entries(variantGroups).map(([type, typeVariants]) => (
            <View key={type} style={styles.variantSection}>
              <Text style={styles.variantTitle}>{type}</Text>
              <View style={styles.variantOptions}>
                {typeVariants.map((v) => {
                  const isSelected = selectedVariant?.id === v.id;
                  const unavailable = !v.available || v.stockQuantity === 0;
                  return (
                    <TouchableOpacity
                      key={v.id}
                      style={[
                        styles.variantBtn,
                        isSelected && styles.variantBtnActive,
                        unavailable && styles.variantBtnDisabled,
                      ]}
                      onPress={() => !unavailable && setSelectedVariant(isSelected ? null : v)}
                      disabled={unavailable}
                    >
                      <Text style={[styles.variantBtnText, isSelected && styles.variantBtnTextActive,
                        unavailable && styles.variantBtnTextDisabled]}>
                        {v.variantValue}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>
          ))}

          {/* Stock */}
          <Text style={[styles.stockText,
            product.stockQuantity === 0 ? { color: "#ef4444" } :
            product.stockQuantity < 5 ? { color: "#f97316" } : { color: "#059669" }]}>
            {product.stockQuantity === 0 ? "Out of Stock" :
             product.stockQuantity < 5 ? `Only ${product.stockQuantity} left!` :
             `In Stock (${product.stockQuantity} available)`}
          </Text>

          {/* Quantity */}
          {product.stockQuantity > 0 && (
            <View style={styles.qtyRow}>
              <Text style={styles.qtyLabel}>Quantity</Text>
              <View style={styles.qtyControl}>
                <TouchableOpacity style={styles.qtyBtn} onPress={() => setQty((q) => Math.max(1, q - 1))}>
                  <Ionicons name="remove" size={16} color="#374151" />
                </TouchableOpacity>
                <Text style={styles.qtyValue}>{qty}</Text>
                <TouchableOpacity style={styles.qtyBtn}
                  onPress={() => setQty((q) => Math.min(product.stockQuantity, q + 1))}>
                  <Ionicons name="add" size={16} color="#374151" />
                </TouchableOpacity>
              </View>
            </View>
          )}

          {/* Actions */}
          {product.stockQuantity > 0 ? (
            <View style={styles.actionRow}>
              <TouchableOpacity style={styles.addToCartBtn} onPress={handleAddToCart}>
                <Ionicons name="cart-outline" size={18} color="#059669" />
                <Text style={styles.addToCartText}>Add to Cart</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.buyNowBtn} onPress={handleBuyNow}>
                <Text style={styles.buyNowText}>Buy Now</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.outOfStockBtn}>
              <Text style={styles.outOfStockText}>Out of Stock</Text>
            </View>
          )}

          {/* Description */}
          {product.description && (
            <View style={styles.section}>
              <Text style={styles.sectionTitle}>Description</Text>
              <Text style={styles.description}>{product.description}</Text>
            </View>
          )}

          {/* Tags */}
          {product.tags?.length > 0 && (
            <View style={styles.tagsRow}>
              {product.tags.map((tag) => (
                <View key={tag} style={styles.tag}>
                  <Text style={styles.tagText}>{tag}</Text>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* Reviews */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Customer Reviews</Text>

          {isAuthenticated && (
            <View style={styles.reviewForm}>
              <Text style={styles.reviewFormTitle}>Write a Review</Text>
              <StarRating
                rating={reviewRating}
                size={28}
                interactive
                onChange={setReviewRating}
              />
              <View style={styles.commentInput}>
                <Text style={styles.commentInputPlaceholder}>Share your experience...</Text>
              </View>
              <TouchableOpacity
                style={styles.submitReviewBtn}
                onPress={() => reviewMutation.mutate()}
                disabled={reviewMutation.isPending}
              >
                {reviewMutation.isPending ? (
                  <ActivityIndicator color="#fff" size="small" />
                ) : (
                  <Text style={styles.submitReviewText}>Submit Review</Text>
                )}
              </TouchableOpacity>
            </View>
          )}

          {reviews.length === 0 ? (
            <View style={styles.noReviews}>
              <Ionicons name="star-outline" size={32} color="#d1d5db" />
              <Text style={styles.noReviewsText}>No reviews yet</Text>
            </View>
          ) : (
            reviews.map((review) => (
              <View key={review.id} style={styles.reviewCard}>
                <View style={styles.reviewHeader}>
                  <View style={styles.reviewAvatar}>
                    <Text style={styles.reviewAvatarText}>
                      {review.user?.firstName?.[0] ?? "U"}
                    </Text>
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={styles.reviewerName}>
                      {review.user ? `${review.user.firstName} ${review.user.lastName}` : "Anonymous"}
                    </Text>
                    <StarRating rating={review.rating} size={12} />
                  </View>
                  <Text style={styles.reviewDate}>
                    {new Date(review.createdAt).toLocaleDateString("en-KE", { month: "short", day: "numeric" })}
                  </Text>
                </View>
                {review.comment && <Text style={styles.reviewComment}>{review.comment}</Text>}
              </View>
            ))
          )}
        </View>

        {/* Related Products */}
        {relatedProducts.length > 0 && (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Related Products</Text>
            <FlatList
              data={relatedProducts.slice(0, 6)}
              keyExtractor={(p) => p.id}
              horizontal
              showsHorizontalScrollIndicator={false}
              renderItem={({ item }) => <ProductCard product={item} compact />}
              contentContainerStyle={{ paddingBottom: 8 }}
            />
          </View>
        )}

        <View style={{ height: 40 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#fff" },
  centered: { flex: 1, alignItems: "center", justifyContent: "center" },
  header: {
    flexDirection: "row", justifyContent: "space-between",
    paddingHorizontal: 16, paddingVertical: 8,
  },
  backBtn: { padding: 8, backgroundColor: "#f9fafb", borderRadius: 10 },
  cartBtn: { padding: 8, backgroundColor: "#f9fafb", borderRadius: 10 },
  imageContainer: {
    height: 280, backgroundColor: "#f9fafb",
    alignItems: "center", justifyContent: "center", position: "relative",
  },
  mainImage: { width: width, height: 280 },
  flashBadge: {
    position: "absolute", top: 12, left: 12,
    backgroundColor: "#ef4444", borderRadius: 8, paddingHorizontal: 8, paddingVertical: 4,
    flexDirection: "row", alignItems: "center", gap: 4,
  },
  flashBadgeText: { color: "#fff", fontSize: 11, fontWeight: "700" },
  thumbnails: { paddingHorizontal: 16, paddingVertical: 8, gap: 8 },
  thumbnail: {
    width: 60, height: 60, borderRadius: 10, overflow: "hidden",
    borderWidth: 2, borderColor: "transparent", backgroundColor: "#f9fafb",
  },
  thumbnailActive: { borderColor: "#059669" },
  thumbnailImage: { width: "100%", height: "100%" },
  info: { paddingHorizontal: 16, paddingTop: 12 },
  category: { fontSize: 12, color: "#059669", fontWeight: "600", marginBottom: 4 },
  name: { fontSize: 20, fontWeight: "700", color: "#111827", lineHeight: 26, marginBottom: 8 },
  ratingRow: { flexDirection: "row", alignItems: "center", gap: 8, marginBottom: 12 },
  ratingText: { fontSize: 13, color: "#6b7280" },
  priceContainer: { flexDirection: "row", alignItems: "baseline", gap: 10, marginBottom: 16 },
  price: { fontSize: 26, fontWeight: "800", color: "#111827" },
  originalPrice: { fontSize: 16, color: "#9ca3af", textDecorationLine: "line-through" },
  variantSection: { marginBottom: 12 },
  variantTitle: { fontSize: 13, fontWeight: "700", color: "#374151", marginBottom: 8, textTransform: "uppercase" },
  variantOptions: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  variantBtn: {
    paddingHorizontal: 14, paddingVertical: 6, borderRadius: 8,
    borderWidth: 2, borderColor: "#e5e7eb", backgroundColor: "#fff",
  },
  variantBtnActive: { borderColor: "#059669", backgroundColor: "#f0fdf4" },
  variantBtnDisabled: { borderColor: "#f3f4f6", backgroundColor: "#f9fafb" },
  variantBtnText: { fontSize: 13, fontWeight: "600", color: "#374151" },
  variantBtnTextActive: { color: "#059669" },
  variantBtnTextDisabled: { color: "#d1d5db", textDecorationLine: "line-through" },
  stockText: { fontSize: 13, fontWeight: "600", marginBottom: 12 },
  qtyRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", marginBottom: 16 },
  qtyLabel: { fontSize: 14, fontWeight: "600", color: "#374151" },
  qtyControl: { flexDirection: "row", alignItems: "center", borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 10 },
  qtyBtn: { width: 36, height: 36, alignItems: "center", justifyContent: "center" },
  qtyValue: { width: 36, textAlign: "center", fontSize: 16, fontWeight: "700", color: "#111827" },
  actionRow: { flexDirection: "row", gap: 10, marginBottom: 20 },
  addToCartBtn: {
    flex: 1, flexDirection: "row", alignItems: "center", justifyContent: "center",
    gap: 8, borderWidth: 2, borderColor: "#059669", borderRadius: 14, paddingVertical: 14,
  },
  addToCartText: { color: "#059669", fontWeight: "700", fontSize: 15 },
  buyNowBtn: {
    flex: 1, backgroundColor: "#059669", borderRadius: 14,
    alignItems: "center", justifyContent: "center", paddingVertical: 14,
  },
  buyNowText: { color: "#fff", fontWeight: "700", fontSize: 15 },
  outOfStockBtn: {
    backgroundColor: "#f3f4f6", borderRadius: 14, paddingVertical: 14,
    alignItems: "center", marginBottom: 20,
  },
  outOfStockText: { color: "#9ca3af", fontWeight: "600" },
  section: { paddingHorizontal: 16, marginTop: 24 },
  sectionTitle: { fontSize: 17, fontWeight: "700", color: "#111827", marginBottom: 12 },
  description: { fontSize: 14, color: "#4b5563", lineHeight: 22 },
  tagsRow: { flexDirection: "row", flexWrap: "wrap", gap: 6, marginTop: 12 },
  tag: { backgroundColor: "#f3f4f6", borderRadius: 100, paddingHorizontal: 10, paddingVertical: 4 },
  tagText: { fontSize: 11, color: "#6b7280" },
  reviewForm: {
    backgroundColor: "#f9fafb", borderRadius: 12, padding: 16, marginBottom: 16,
    borderWidth: 1, borderColor: "#e5e7eb",
  },
  reviewFormTitle: { fontSize: 14, fontWeight: "700", color: "#111827", marginBottom: 10 },
  commentInput: {
    backgroundColor: "#fff", borderWidth: 1, borderColor: "#e5e7eb",
    borderRadius: 10, padding: 12, marginTop: 10, marginBottom: 10, minHeight: 72,
  },
  commentInputPlaceholder: { color: "#9ca3af", fontSize: 13 },
  submitReviewBtn: {
    backgroundColor: "#059669", borderRadius: 10, paddingVertical: 12, alignItems: "center",
  },
  submitReviewText: { color: "#fff", fontWeight: "700", fontSize: 14 },
  noReviews: { alignItems: "center", paddingVertical: 24, gap: 8 },
  noReviewsText: { fontSize: 14, color: "#9ca3af" },
  reviewCard: {
    backgroundColor: "#f9fafb", borderRadius: 12, padding: 14,
    marginBottom: 10, borderWidth: 1, borderColor: "#f3f4f6",
  },
  reviewHeader: { flexDirection: "row", alignItems: "center", gap: 10, marginBottom: 8 },
  reviewAvatar: {
    width: 36, height: 36, borderRadius: 18, backgroundColor: "#d1fae5",
    alignItems: "center", justifyContent: "center",
  },
  reviewAvatarText: { fontSize: 14, fontWeight: "700", color: "#059669" },
  reviewerName: { fontSize: 13, fontWeight: "600", color: "#111827", marginBottom: 2 },
  reviewDate: { fontSize: 11, color: "#9ca3af" },
  reviewComment: { fontSize: 13, color: "#4b5563", lineHeight: 20 },
});
