import React, { useState, useCallback } from "react";
import {
  View, Text, FlatList, TextInput, TouchableOpacity,
  StyleSheet, ActivityIndicator, RefreshControl,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useQuery } from "@tanstack/react-query";
import { router, useLocalSearchParams } from "expo-router";
import { SafeAreaView } from "react-native-safe-area-context";
import api from "@/src/lib/api";
import { Product, Category, ApiPage } from "@/src/types";
import ProductCard from "@/src/components/ProductCard";

export default function ProductsScreen() {
  const params = useLocalSearchParams<{ q?: string; category?: string }>();
  const [search, setSearch] = useState(params.q ?? "");
  const [selectedCategory, setSelectedCategory] = useState(params.category ?? "");
  const [page, setPage] = useState(0);

  const { data: categories } = useQuery<Category[]>({
    queryKey: ["categories"],
    queryFn: () => api.get("/categories").then((r) => r.data),
  });

  const queryKey = ["products", search, selectedCategory, page];

  const { data, isLoading, refetch, isFetching } = useQuery<ApiPage<Product>>({
    queryKey,
    queryFn: () => {
      const params: Record<string, any> = { page, size: 20 };
      if (search) params.q = search;
      if (selectedCategory) params.category = selectedCategory;
      return api.get("/products", { params }).then((r) => r.data);
    },
  });

  const products = data?.content ?? [];
  const hasMore = data ? !data.last : false;

  const handleSearch = useCallback(() => {
    setPage(0);
  }, []);

  const CategoryPill = ({ cat }: { cat: Category }) => (
    <TouchableOpacity
      style={[styles.pill, selectedCategory === cat.slug && styles.pillActive]}
      onPress={() => { setSelectedCategory(selectedCategory === cat.slug ? "" : cat.slug); setPage(0); }}
    >
      <Text style={[styles.pillText, selectedCategory === cat.slug && styles.pillTextActive]}>
        {cat.name}
      </Text>
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={22} color="#111827" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Products</Text>
        <View style={{ width: 36 }} />
      </View>

      {/* Search Bar */}
      <View style={styles.searchRow}>
        <Ionicons name="search" size={16} color="#9ca3af" style={{ marginRight: 8 }} />
        <TextInput
          value={search}
          onChangeText={setSearch}
          onSubmitEditing={handleSearch}
          returnKeyType="search"
          placeholder="Search products..."
          placeholderTextColor="#9ca3af"
          style={styles.searchInput}
        />
        {search.length > 0 && (
          <TouchableOpacity onPress={() => { setSearch(""); setPage(0); }}>
            <Ionicons name="close-circle" size={16} color="#9ca3af" />
          </TouchableOpacity>
        )}
      </View>

      {/* Category Filter */}
      {categories && categories.length > 0 && (
        <FlatList
          data={[{ id: "", name: "All", slug: "" } as Category, ...categories]}
          keyExtractor={(c) => c.id || "all"}
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.pillsContainer}
          renderItem={({ item }) => <CategoryPill cat={item} />}
        />
      )}

      {/* Products Grid */}
      {isLoading ? (
        <View style={styles.centered}>
          <ActivityIndicator color="#059669" size="large" />
        </View>
      ) : products.length === 0 ? (
        <View style={styles.centered}>
          <Ionicons name="search-outline" size={48} color="#d1d5db" />
          <Text style={styles.emptyText}>No products found</Text>
          <Text style={styles.emptySubtext}>Try a different search or category</Text>
        </View>
      ) : (
        <FlatList
          data={products}
          keyExtractor={(p) => p.id}
          numColumns={2}
          contentContainerStyle={styles.grid}
          columnWrapperStyle={styles.row}
          showsVerticalScrollIndicator={false}
          refreshControl={<RefreshControl refreshing={isFetching} onRefresh={refetch} tintColor="#059669" />}
          renderItem={({ item }) => <ProductCard product={item} />}
          onEndReached={() => hasMore && setPage((p) => p + 1)}
          onEndReachedThreshold={0.3}
          ListFooterComponent={hasMore ? <ActivityIndicator color="#059669" style={{ marginVertical: 16 }} /> : null}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 16, paddingVertical: 12,
  },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 18, fontWeight: "700", color: "#111827" },
  searchRow: {
    flexDirection: "row", alignItems: "center",
    backgroundColor: "#fff", borderRadius: 50, borderWidth: 1, borderColor: "#e5e7eb",
    marginHorizontal: 16, marginBottom: 8, paddingHorizontal: 12,
  },
  searchInput: { flex: 1, paddingVertical: 10, fontSize: 14, color: "#111827" },
  pillsContainer: { paddingHorizontal: 16, paddingBottom: 8, gap: 8 },
  pill: {
    paddingHorizontal: 14, paddingVertical: 6, borderRadius: 100,
    backgroundColor: "#f3f4f6", borderWidth: 1, borderColor: "#e5e7eb",
    marginRight: 8,
  },
  pillActive: { backgroundColor: "#059669", borderColor: "#059669" },
  pillText: { fontSize: 12, fontWeight: "600", color: "#374151" },
  pillTextActive: { color: "#fff" },
  grid: { paddingHorizontal: 16, paddingBottom: 20 },
  row: { justifyContent: "space-between" },
  centered: { flex: 1, alignItems: "center", justifyContent: "center", gap: 8 },
  emptyText: { fontSize: 16, fontWeight: "600", color: "#374151", marginTop: 8 },
  emptySubtext: { fontSize: 13, color: "#9ca3af" },
});
