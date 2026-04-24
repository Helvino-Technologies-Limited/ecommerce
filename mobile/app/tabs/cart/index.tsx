import React from "react";
import {
  View, Text, FlatList, TouchableOpacity, Image, StyleSheet,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { SafeAreaView } from "react-native-safe-area-context";
import { useCartStore } from "@/src/store/cart";
import { formatPrice } from "@/src/lib/utils";

export default function CartScreen() {
  const { items, removeItem, updateQuantity, total, clearCart } = useCartStore();

  if (items.length === 0) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>My Cart</Text>
        </View>
        <View style={styles.emptyContainer}>
          <Ionicons name="cart-outline" size={64} color="#d1d5db" />
          <Text style={styles.emptyTitle}>Your cart is empty</Text>
          <Text style={styles.emptySubtext}>Add items to get started</Text>
          <TouchableOpacity style={styles.shopBtn} onPress={() => router.push("/tabs/products")}>
            <Text style={styles.shopBtnText}>Start Shopping</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const subtotal = total();
  const currency = items[0]?.currency ?? "KES";

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My Cart ({items.length})</Text>
        <TouchableOpacity onPress={() => clearCart()}>
          <Text style={styles.clearText}>Clear All</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={items}
        keyExtractor={(i) => i.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <View style={styles.cartItem}>
            <View style={styles.itemImage}>
              {item.image ? (
                <Image source={{ uri: item.image }} style={styles.image} resizeMode="contain" />
              ) : (
                <Ionicons name="image-outline" size={28} color="#d1d5db" />
              )}
            </View>
            <View style={styles.itemInfo}>
              <Text style={styles.itemName} numberOfLines={2}>{item.name}</Text>
              <Text style={styles.itemPrice}>
                {formatPrice(item.discountedPrice ?? item.price, item.currency)}
              </Text>
              <View style={styles.qtyRow}>
                <TouchableOpacity
                  style={styles.qtyBtn}
                  onPress={() => updateQuantity(item.id, item.quantity - 1)}
                >
                  <Ionicons name="remove" size={14} color="#374151" />
                </TouchableOpacity>
                <Text style={styles.qtyText}>{item.quantity}</Text>
                <TouchableOpacity
                  style={styles.qtyBtn}
                  onPress={() => updateQuantity(item.id, item.quantity + 1)}
                >
                  <Ionicons name="add" size={14} color="#374151" />
                </TouchableOpacity>
              </View>
            </View>
            <View style={styles.itemRight}>
              <Text style={styles.itemTotal}>
                {formatPrice((item.discountedPrice ?? item.price) * item.quantity, item.currency)}
              </Text>
              <TouchableOpacity onPress={() => removeItem(item.id)}>
                <Ionicons name="trash-outline" size={18} color="#ef4444" />
              </TouchableOpacity>
            </View>
          </View>
        )}
        ListFooterComponent={<View style={{ height: 160 }} />}
      />

      {/* Summary bar */}
      <View style={styles.bottomBar}>
        <View style={styles.summaryRow}>
          <Text style={styles.subtotalLabel}>Subtotal</Text>
          <Text style={styles.subtotalValue}>{formatPrice(subtotal, currency)}</Text>
        </View>
        <Text style={styles.deliveryNote}>Delivery fee calculated at checkout</Text>
        <TouchableOpacity
          style={styles.checkoutBtn}
          onPress={() => router.push("/checkout")}
        >
          <Ionicons name="lock-closed-outline" size={16} color="#fff" />
          <Text style={styles.checkoutBtnText}>Proceed to Checkout</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: "#f3f4f6",
    backgroundColor: "#fff",
  },
  headerTitle: { fontSize: 18, fontWeight: "700", color: "#111827" },
  clearText: { fontSize: 13, color: "#ef4444", fontWeight: "600" },
  emptyContainer: { flex: 1, alignItems: "center", justifyContent: "center", gap: 10 },
  emptyTitle: { fontSize: 18, fontWeight: "700", color: "#111827" },
  emptySubtext: { fontSize: 14, color: "#9ca3af" },
  shopBtn: {
    backgroundColor: "#059669", borderRadius: 14, paddingHorizontal: 24, paddingVertical: 12, marginTop: 8,
  },
  shopBtnText: { color: "#fff", fontWeight: "700", fontSize: 15 },
  list: { padding: 16 },
  cartItem: {
    flexDirection: "row", backgroundColor: "#fff", borderRadius: 14,
    padding: 12, marginBottom: 10, borderWidth: 1, borderColor: "#f3f4f6",
    shadowColor: "#000", shadowOpacity: 0.03, shadowRadius: 4, elevation: 1,
  },
  itemImage: {
    width: 72, height: 72, backgroundColor: "#f9fafb", borderRadius: 10,
    alignItems: "center", justifyContent: "center", overflow: "hidden",
  },
  image: { width: "100%", height: "100%" },
  itemInfo: { flex: 1, marginHorizontal: 12 },
  itemName: { fontSize: 13, fontWeight: "600", color: "#111827", lineHeight: 18, marginBottom: 4 },
  itemPrice: { fontSize: 13, color: "#059669", fontWeight: "700", marginBottom: 8 },
  qtyRow: {
    flexDirection: "row", alignItems: "center",
    borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 8, alignSelf: "flex-start",
  },
  qtyBtn: { width: 28, height: 28, alignItems: "center", justifyContent: "center" },
  qtyText: { width: 28, textAlign: "center", fontSize: 14, fontWeight: "700", color: "#111827" },
  itemRight: { alignItems: "flex-end", justifyContent: "space-between" },
  itemTotal: { fontSize: 14, fontWeight: "700", color: "#111827" },
  bottomBar: {
    position: "absolute", bottom: 0, left: 0, right: 0,
    backgroundColor: "#fff", padding: 16,
    borderTopWidth: 1, borderTopColor: "#f3f4f6",
    shadowColor: "#000", shadowOpacity: 0.08, shadowRadius: 12, elevation: 8,
  },
  summaryRow: { flexDirection: "row", justifyContent: "space-between", marginBottom: 4 },
  subtotalLabel: { fontSize: 14, color: "#6b7280" },
  subtotalValue: { fontSize: 16, fontWeight: "700", color: "#111827" },
  deliveryNote: { fontSize: 12, color: "#9ca3af", marginBottom: 12 },
  checkoutBtn: {
    backgroundColor: "#059669", borderRadius: 14, paddingVertical: 15,
    flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8,
  },
  checkoutBtnText: { color: "#fff", fontWeight: "700", fontSize: 16 },
});
