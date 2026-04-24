import React from "react";
import {
  View, Text, TouchableOpacity, Image, StyleSheet, Dimensions,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { Product } from "../types";
import { formatPrice } from "../lib/utils";
import { useCartStore } from "../store/cart";
import Toast from "react-native-toast-message";

interface Props {
  product: Product;
  compact?: boolean;
}

const { width } = Dimensions.get("window");

export default function ProductCard({ product, compact = false }: Props) {
  const addItem = useCartStore((s) => s.addItem);

  const currentPrice = product.flashSale && product.flashSaleDiscount
    ? product.price * (1 - product.flashSaleDiscount / 100)
    : product.price;

  const handleAddToCart = () => {
    addItem({
      id: product.id,
      name: product.name,
      price: product.price,
      discountedPrice: currentPrice !== product.price ? currentPrice : undefined,
      image: product.images[0] ?? "",
      currency: product.currency,
      slug: product.slug,
    });
    Toast.show({ type: "success", text1: "Added to cart", text2: product.name });
  };

  const cardWidth = compact ? (width - 48) / 3 - 4 : (width - 32) / 2 - 4;

  return (
    <TouchableOpacity
      style={[styles.card, { width: cardWidth }]}
      onPress={() => router.push(`/tabs/products/${product.slug}`)}
      activeOpacity={0.85}
    >
      {/* Image */}
      <View style={[styles.imageContainer, compact && { height: 90 }]}>
        {product.images[0] ? (
          <Image
            source={{ uri: product.images[0] }}
            style={styles.image}
            resizeMode="contain"
          />
        ) : (
          <View style={styles.imagePlaceholder}>
            <Ionicons name="image-outline" size={24} color="#d1d5db" />
          </View>
        )}
        {product.flashSale && product.flashSaleDiscount ? (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>-{product.flashSaleDiscount}%</Text>
          </View>
        ) : null}
        {product.stockQuantity === 0 && (
          <View style={[styles.badge, styles.outOfStockBadge]}>
            <Text style={styles.badgeText}>Out of Stock</Text>
          </View>
        )}
      </View>

      {/* Info */}
      <View style={[styles.info, compact && { padding: 6 }]}>
        {!compact && product.category && (
          <Text style={styles.category}>{product.category.name}</Text>
        )}
        <Text style={[styles.name, compact && { fontSize: 11 }]} numberOfLines={2}>
          {product.name}
        </Text>

        {/* Star rating */}
        {product.reviewCount > 0 && (
          <View style={styles.ratingRow}>
            {[1,2,3,4,5].map((s) => (
              <Ionicons
                key={s}
                name={s <= Math.round(product.averageRating) ? "star" : "star-outline"}
                size={10}
                color={s <= Math.round(product.averageRating) ? "#f59e0b" : "#d1d5db"}
              />
            ))}
            <Text style={styles.ratingText}>({product.reviewCount})</Text>
          </View>
        )}

        <View style={styles.priceRow}>
          <View>
            <Text style={[styles.price, compact && { fontSize: 13 }]}>
              {formatPrice(currentPrice, product.currency)}
            </Text>
            {currentPrice < product.price && (
              <Text style={styles.originalPrice}>{formatPrice(product.price, product.currency)}</Text>
            )}
          </View>
          {product.stockQuantity > 0 && (
            <TouchableOpacity style={styles.cartBtn} onPress={handleAddToCart}>
              <Ionicons name="cart" size={compact ? 14 : 16} color="#fff" />
            </TouchableOpacity>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#fff",
    borderRadius: 12,
    overflow: "hidden",
    borderWidth: 1,
    borderColor: "#f3f4f6",
    marginBottom: 8,
  },
  imageContainer: {
    height: 140,
    backgroundColor: "#f9fafb",
    position: "relative",
  },
  image: {
    width: "100%",
    height: "100%",
  },
  imagePlaceholder: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  badge: {
    position: "absolute",
    top: 6,
    left: 6,
    backgroundColor: "#ef4444",
    borderRadius: 4,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  outOfStockBadge: {
    backgroundColor: "#6b7280",
  },
  badgeText: {
    color: "#fff",
    fontSize: 10,
    fontWeight: "700",
  },
  info: {
    padding: 8,
  },
  category: {
    fontSize: 10,
    color: "#059669",
    fontWeight: "600",
    marginBottom: 2,
  },
  name: {
    fontSize: 12,
    color: "#111827",
    fontWeight: "500",
    lineHeight: 16,
    marginBottom: 4,
  },
  ratingRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 1,
    marginBottom: 4,
  },
  ratingText: {
    fontSize: 9,
    color: "#9ca3af",
    marginLeft: 2,
  },
  priceRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  price: {
    fontSize: 14,
    fontWeight: "700",
    color: "#059669",
  },
  originalPrice: {
    fontSize: 10,
    color: "#9ca3af",
    textDecorationLine: "line-through",
  },
  cartBtn: {
    width: 28,
    height: 28,
    backgroundColor: "#059669",
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
  },
});
