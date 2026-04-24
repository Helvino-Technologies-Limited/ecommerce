import React, { useState, useEffect, useRef } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, FlatList,
  Dimensions, StyleSheet, TextInput, Image, RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useQuery } from "@tanstack/react-query";
import { router } from "expo-router";
import api from "@/src/lib/api";
import { Product, Category } from "@/src/types";
import { formatPrice, timeUntil } from "@/src/lib/utils";
import { useCartStore } from "@/src/store/cart";
import Toast from "react-native-toast-message";

const { width } = Dimensions.get("window");

const HERO_SLIDES = [
  { id: "1", title: "Big Summer Sale", subtitle: "Up to 70% off", bg: ["#059669", "#0d9488"] },
  { id: "2", title: "New Arrivals", subtitle: "Latest trends in fashion", bg: ["#7c3aed", "#4f46e5"] },
  { id: "3", title: "Flash Deals", subtitle: "Limited time prices", bg: ["#ef4444", "#f97316"] },
];

export default function HomeScreen() {
  const [heroIdx, setHeroIdx] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const heroRef = useRef<FlatList>(null);
  const addItem = useCartStore((s) => s.addItem);

  const { data: featured, refetch: refetchFeatured } = useQuery<Product[]>({
    queryKey: ["featured"],
    queryFn: () => api.get("/products/featured").then((r) => r.data),
  });

  const { data: flashSales } = useQuery<Product[]>({
    queryKey: ["flash-sales"],
    queryFn: () => api.get("/products/flash-sales").then((r) => r.data),
  });

  const { data: categories } = useQuery<Category[]>({
    queryKey: ["categories"],
    queryFn: () => api.get("/categories").then((r) => r.data),
  });

  useEffect(() => {
    const t = setInterval(() => {
      const next = (heroIdx + 1) % HERO_SLIDES.length;
      setHeroIdx(next);
      heroRef.current?.scrollToIndex({ index: next, animated: true });
    }, 4000);
    return () => clearInterval(t);
  }, [heroIdx]);

  const onRefresh = async () => {
    setRefreshing(true);
    await refetchFeatured();
    setRefreshing(false);
  };

  const ProductItem = ({ item }: { item: Product }) => (
    <TouchableOpacity style={styles.productCard} onPress={() => router.push(`/products/${item.slug}`)}>
      <Image source={{ uri: item.images[0] ?? "" }} style={styles.productImage} resizeMode="contain" />
      {item.flashSale && item.flashSaleDiscount && (
        <View style={styles.discountBadge}>
          <Text style={styles.discountText}>-{item.flashSaleDiscount}%</Text>
        </View>
      )}
      <View style={styles.productInfo}>
        <Text style={styles.productName} numberOfLines={2}>{item.name}</Text>
        <Text style={styles.productPrice}>{formatPrice(item.price, item.currency)}</Text>
      </View>
      <TouchableOpacity style={styles.addBtn} onPress={() => {
        addItem({ id: item.id, name: item.name, price: item.price, image: item.images[0] ?? "", currency: item.currency, slug: item.slug });
        Toast.show({ type: "success", text1: "Added to cart" });
      }}>
        <Ionicons name="cart" size={16} color="#fff" />
      </TouchableOpacity>
    </TouchableOpacity>
  );

  return (
    <ScrollView style={styles.container} showsVerticalScrollIndicator={false}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#059669" />}>

      {/* Hero Carousel */}
      <FlatList
        ref={heroRef}
        data={HERO_SLIDES}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        keyExtractor={(i) => i.id}
        onMomentumScrollEnd={(e) => setHeroIdx(Math.round(e.nativeEvent.contentOffset.x / width))}
        renderItem={({ item }) => (
          <View style={[styles.heroSlide, { backgroundColor: item.bg[0] }]}>
            <Text style={styles.heroTitle}>{item.title}</Text>
            <Text style={styles.heroSubtitle}>{item.subtitle}</Text>
            <TouchableOpacity style={styles.heroBtn} onPress={() => router.push("/tabs/products")}>
              <Text style={styles.heroBtnText}>Shop Now</Text>
            </TouchableOpacity>
          </View>
        )}
      />
      <View style={styles.dots}>
        {HERO_SLIDES.map((_, i) => (
          <View key={i} style={[styles.dot, heroIdx === i && styles.dotActive]} />
        ))}
      </View>

      {/* Search */}
      <View style={styles.searchContainer}>
        <Ionicons name="search" size={18} color="#9ca3af" style={styles.searchIcon} />
        <TextInput
          value={searchQuery}
          onChangeText={setSearchQuery}
          onSubmitEditing={() => router.push(`/tabs/products?q=${searchQuery}`)}
          placeholder="Search products..."
          style={styles.searchInput}
          returnKeyType="search"
        />
      </View>

      {/* Categories */}
      {categories && categories.length > 0 && (
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Categories</Text>
            <TouchableOpacity onPress={() => router.push("/tabs/products")}>
              <Text style={styles.seeAll}>See All</Text>
            </TouchableOpacity>
          </View>
          <FlatList
            data={categories.slice(0, 8)}
            horizontal
            showsHorizontalScrollIndicator={false}
            keyExtractor={(c) => c.id}
            renderItem={({ item }) => (
              <TouchableOpacity style={styles.categoryItem}
                onPress={() => router.push(`/tabs/products?category=${item.slug}`)}>
                <Text style={styles.categoryEmoji}>🛍️</Text>
                <Text style={styles.categoryName} numberOfLines={1}>{item.name}</Text>
              </TouchableOpacity>
            )}
          />
        </View>
      )}

      {/* Flash Sales */}
      {flashSales && flashSales.length > 0 && (
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <View style={{ flexDirection: "row", alignItems: "center", gap: 6 }}>
              <Ionicons name="flash" size={18} color="#ef4444" />
              <Text style={styles.sectionTitle}>Flash Sales</Text>
            </View>
            <TouchableOpacity onPress={() => router.push("/flash-sales")}>
              <Text style={styles.seeAll}>See All</Text>
            </TouchableOpacity>
          </View>
          <FlatList
            data={flashSales}
            horizontal
            showsHorizontalScrollIndicator={false}
            keyExtractor={(p) => p.id}
            renderItem={({ item }) => <ProductItem item={item} />}
          />
        </View>
      )}

      {/* Featured */}
      {featured && featured.length > 0 && (
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>Featured Products</Text>
            <TouchableOpacity onPress={() => router.push("/tabs/products")}>
              <Text style={styles.seeAll}>See All</Text>
            </TouchableOpacity>
          </View>
          <View style={styles.productsGrid}>
            {featured.slice(0, 6).map((item) => <ProductItem key={item.id} item={item} />)}
          </View>
        </View>
      )}

      <View style={{ height: 90 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  heroSlide: { width, height: 200, justifyContent: "center", paddingHorizontal: 24 },
  heroTitle: { fontSize: 28, fontWeight: "800", color: "#fff", marginBottom: 4 },
  heroSubtitle: { fontSize: 14, color: "rgba(255,255,255,0.8)", marginBottom: 16 },
  heroBtn: { backgroundColor: "#fff", paddingHorizontal: 20, paddingVertical: 10, borderRadius: 100, alignSelf: "flex-start" },
  heroBtnText: { fontWeight: "700", color: "#111", fontSize: 13 },
  dots: { flexDirection: "row", justifyContent: "center", marginTop: 8, gap: 6 },
  dot: { width: 6, height: 6, borderRadius: 3, backgroundColor: "#d1d5db" },
  dotActive: { width: 18, backgroundColor: "#059669" },
  searchContainer: { flexDirection: "row", alignItems: "center", backgroundColor: "#fff", margin: 12, borderRadius: 50, borderWidth: 1, borderColor: "#e5e7eb", paddingHorizontal: 12 },
  searchIcon: { marginRight: 8 },
  searchInput: { flex: 1, paddingVertical: 10, fontSize: 14, color: "#111" },
  section: { paddingHorizontal: 12, marginBottom: 8 },
  sectionHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 10 },
  sectionTitle: { fontSize: 16, fontWeight: "700", color: "#111" },
  seeAll: { fontSize: 12, color: "#059669", fontWeight: "600" },
  categoryItem: { alignItems: "center", marginRight: 12, width: 68 },
  categoryEmoji: { fontSize: 28, marginBottom: 4 },
  categoryName: { fontSize: 11, color: "#374151", textAlign: "center" },
  productsGrid: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  productCard: { width: (width - 32) / 2 - 4, backgroundColor: "#fff", borderRadius: 12, overflow: "hidden", borderWidth: 1, borderColor: "#f3f4f6", marginRight: 8 },
  productImage: { width: "100%", height: 140, backgroundColor: "#f9fafb" },
  discountBadge: { position: "absolute", top: 8, left: 8, backgroundColor: "#ef4444", borderRadius: 4, paddingHorizontal: 6, paddingVertical: 2 },
  discountText: { color: "#fff", fontSize: 10, fontWeight: "700" },
  productInfo: { padding: 8 },
  productName: { fontSize: 12, color: "#111", fontWeight: "500", marginBottom: 4, lineHeight: 16 },
  productPrice: { fontSize: 14, fontWeight: "700", color: "#059669" },
  addBtn: { position: "absolute", bottom: 8, right: 8, width: 28, height: 28, backgroundColor: "#059669", borderRadius: 14, alignItems: "center", justifyContent: "center" },
});
