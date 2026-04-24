import React from "react";
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, ActivityIndicator,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { SafeAreaView } from "react-native-safe-area-context";
import api from "@/src/lib/api";
import { Order, ApiPage } from "@/src/types";
import { formatPrice } from "@/src/lib/utils";
import { useAuthStore } from "@/src/store/auth";

const STATUS_COLORS: Record<string, { bg: string; text: string }> = {
  PENDING:          { bg: "#fef3c7", text: "#92400e" },
  CONFIRMED:        { bg: "#dbeafe", text: "#1e40af" },
  PROCESSING:       { bg: "#ede9fe", text: "#5b21b6" },
  READY_FOR_PICKUP: { bg: "#f3e8ff", text: "#7e22ce" },
  OUT_FOR_DELIVERY: { bg: "#fed7aa", text: "#9a3412" },
  DELIVERED:        { bg: "#d1fae5", text: "#065f46" },
  CANCELLED:        { bg: "#fee2e2", text: "#991b1b" },
  REFUNDED:         { bg: "#f3f4f6", text: "#374151" },
};

const STATUS_LABELS: Record<string, string> = {
  PENDING: "Pending",
  CONFIRMED: "Confirmed",
  PROCESSING: "Processing",
  READY_FOR_PICKUP: "Ready for Pickup",
  OUT_FOR_DELIVERY: "Out for Delivery",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
  REFUNDED: "Refunded",
};

export default function OrdersScreen() {
  const { isAuthenticated } = useAuthStore();

  const { data, isLoading } = useQuery<ApiPage<Order>>({
    queryKey: ["my-orders"],
    queryFn: () => api.get("/orders/my-orders").then((r) => r.data),
    enabled: isAuthenticated,
    refetchInterval: 30000,
  });

  const orders = data?.content ?? [];

  if (!isAuthenticated) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.notAuthContainer}>
          <Ionicons name="bag-outline" size={48} color="#d1d5db" />
          <Text style={styles.notAuthTitle}>Sign in to view orders</Text>
          <TouchableOpacity style={styles.signInBtn} onPress={() => router.push("/auth/login")}>
            <Text style={styles.signInBtnText}>Sign In</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>My Orders</Text>
      </View>

      {isLoading ? (
        <View style={styles.centered}>
          <ActivityIndicator color="#059669" size="large" />
        </View>
      ) : orders.length === 0 ? (
        <View style={styles.centered}>
          <Ionicons name="bag-outline" size={48} color="#d1d5db" />
          <Text style={styles.emptyTitle}>No orders yet</Text>
          <Text style={styles.emptySubtext}>Place an order and track it here</Text>
          <TouchableOpacity style={styles.shopBtn} onPress={() => router.push("/tabs/products")}>
            <Text style={styles.shopBtnText}>Start Shopping</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <FlatList
          data={orders}
          keyExtractor={(o) => o.id}
          contentContainerStyle={styles.list}
          renderItem={({ item: order }) => {
            const colors = STATUS_COLORS[order.status] ?? STATUS_COLORS.PENDING;
            return (
              <TouchableOpacity
                style={styles.orderCard}
                onPress={() => router.push(`/tabs/orders/${order.id}`)}
              >
                <View style={styles.orderHeader}>
                  <Text style={styles.orderNumber}>#{order.orderNumber}</Text>
                  <View style={[styles.statusBadge, { backgroundColor: colors.bg }]}>
                    <Text style={[styles.statusText, { color: colors.text }]}>
                      {STATUS_LABELS[order.status] ?? order.status}
                    </Text>
                  </View>
                </View>

                <View style={styles.orderMeta}>
                  <Text style={styles.itemCount}>
                    {order.items?.length ?? 0} item{(order.items?.length ?? 0) !== 1 ? "s" : ""}
                  </Text>
                  <Text style={styles.orderDate}>
                    {new Date(order.createdAt).toLocaleDateString("en-KE", {
                      year: "numeric", month: "short", day: "numeric",
                    })}
                  </Text>
                </View>

                <View style={styles.orderFooter}>
                  <Text style={styles.orderTotal}>{formatPrice(order.totalAmount, order.currency)}</Text>
                  <View style={styles.viewRow}>
                    <Text style={styles.viewText}>Track Order</Text>
                    <Ionicons name="chevron-forward" size={14} color="#059669" />
                  </View>
                </View>
              </TouchableOpacity>
            );
          }}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  header: {
    paddingHorizontal: 16, paddingVertical: 14, borderBottomWidth: 1,
    borderBottomColor: "#f3f4f6", backgroundColor: "#fff",
  },
  headerTitle: { fontSize: 18, fontWeight: "700", color: "#111827" },
  centered: { flex: 1, alignItems: "center", justifyContent: "center", gap: 10 },
  notAuthContainer: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  notAuthTitle: { fontSize: 16, fontWeight: "600", color: "#374151" },
  signInBtn: { backgroundColor: "#059669", borderRadius: 12, paddingHorizontal: 24, paddingVertical: 11 },
  signInBtnText: { color: "#fff", fontWeight: "700", fontSize: 15 },
  emptyTitle: { fontSize: 16, fontWeight: "700", color: "#111827" },
  emptySubtext: { fontSize: 13, color: "#9ca3af" },
  shopBtn: { backgroundColor: "#059669", borderRadius: 12, paddingHorizontal: 20, paddingVertical: 10, marginTop: 4 },
  shopBtnText: { color: "#fff", fontWeight: "700" },
  list: { padding: 16 },
  orderCard: {
    backgroundColor: "#fff", borderRadius: 14, padding: 14, marginBottom: 10,
    borderWidth: 1, borderColor: "#f3f4f6", shadowColor: "#000", shadowOpacity: 0.03,
    shadowRadius: 4, elevation: 1,
  },
  orderHeader: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginBottom: 8 },
  orderNumber: { fontSize: 15, fontWeight: "700", color: "#059669" },
  statusBadge: { borderRadius: 100, paddingHorizontal: 10, paddingVertical: 3 },
  statusText: { fontSize: 11, fontWeight: "700" },
  orderMeta: { flexDirection: "row", justifyContent: "space-between", marginBottom: 10 },
  itemCount: { fontSize: 13, color: "#6b7280" },
  orderDate: { fontSize: 12, color: "#9ca3af" },
  orderFooter: { flexDirection: "row", justifyContent: "space-between", alignItems: "center" },
  orderTotal: { fontSize: 16, fontWeight: "700", color: "#111827" },
  viewRow: { flexDirection: "row", alignItems: "center", gap: 2 },
  viewText: { fontSize: 13, color: "#059669", fontWeight: "600" },
});
