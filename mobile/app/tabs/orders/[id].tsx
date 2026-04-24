import React, { useEffect, useRef, useState } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, ActivityIndicator, Image,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router, useLocalSearchParams } from "expo-router";
import { useQuery } from "@tanstack/react-query";
import { SafeAreaView } from "react-native-safe-area-context";
import api from "@/src/lib/api";
import { Order, OrderStatus, TrackingEvent } from "@/src/types";
import { formatPrice } from "@/src/lib/utils";
import * as SecureStore from "expo-secure-store";

const WS_URL = (process.env.EXPO_PUBLIC_API_URL ?? "").replace(/^http/, "ws").replace("/api/v1", "");

const TIMELINE_STATUSES: OrderStatus[] = [
  "PENDING", "CONFIRMED", "PROCESSING", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY", "DELIVERED",
];

const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: "Order Placed",
  CONFIRMED: "Order Confirmed",
  PROCESSING: "Processing",
  READY_FOR_PICKUP: "Ready for Pickup",
  OUT_FOR_DELIVERY: "Out for Delivery",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
  REFUNDED: "Refunded",
};

const STATUS_ICONS: Record<OrderStatus, string> = {
  PENDING: "time-outline",
  CONFIRMED: "checkmark-circle-outline",
  PROCESSING: "cube-outline",
  READY_FOR_PICKUP: "storefront-outline",
  OUT_FOR_DELIVERY: "bicycle-outline",
  DELIVERED: "checkmark-done-circle-outline",
  CANCELLED: "close-circle-outline",
  REFUNDED: "refresh-outline",
};

export default function OrderDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const wsRef = useRef<WebSocket | null>(null);
  const [riderPos, setRiderPos] = useState<{ lat: number; lng: number } | null>(null);

  const { data: order, isLoading } = useQuery<Order>({
    queryKey: ["order", id],
    queryFn: () => api.get(`/orders/${id}`).then((r) => r.data),
    refetchInterval: 30000,
  });

  useEffect(() => {
    if (!id || !order || order.status !== "OUT_FOR_DELIVERY") return;

    const connect = async () => {
      const token = await SecureStore.getItemAsync("accessToken");
      const url = `${WS_URL}/ws?orderId=${id}${token ? `&token=${token}` : ""}`;
      try {
        const ws = new WebSocket(url);
        wsRef.current = ws;
        ws.onmessage = (evt) => {
          try {
            const data = JSON.parse(evt.data);
            if (data.type === "RIDER_LOCATION") {
              setRiderPos({ lat: data.latitude, lng: data.longitude });
            }
          } catch {}
        };
      } catch {}
    };

    connect();
    return () => { wsRef.current?.close(); wsRef.current = null; };
  }, [id, order?.status]);

  if (isLoading || !order) {
    return (
      <View style={styles.centered}>
        <ActivityIndicator color="#059669" size="large" />
      </View>
    );
  }

  const currentStatusIdx = TIMELINE_STATUSES.indexOf(order.status);
  const isCancelledOrRefunded = order.status === "CANCELLED" || order.status === "REFUNDED";

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={22} color="#111827" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Order #{order.orderNumber}</Text>
        <View style={{ width: 36 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        {/* Status Banner */}
        <View style={[styles.statusBanner, order.status === "DELIVERED" ? styles.deliveredBanner :
          order.status === "CANCELLED" ? styles.cancelledBanner : styles.activeBanner]}>
          <Ionicons
            name={STATUS_ICONS[order.status] as any}
            size={24}
            color={order.status === "DELIVERED" ? "#059669" : order.status === "CANCELLED" ? "#ef4444" : "#2563eb"}
          />
          <View style={{ flex: 1, marginLeft: 10 }}>
            <Text style={styles.statusBannerText}>{STATUS_LABELS[order.status]}</Text>
            {order.estimatedDelivery && order.status !== "DELIVERED" && !isCancelledOrRefunded && (
              <Text style={styles.statusSubtext}>
                Est. delivery: {new Date(order.estimatedDelivery).toLocaleDateString("en-KE", { month: "short", day: "numeric" })}
              </Text>
            )}
          </View>
        </View>

        {/* Tracking Timeline */}
        {!isCancelledOrRefunded && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Tracking Timeline</Text>
            {TIMELINE_STATUSES.map((status, idx) => {
              const done = idx <= currentStatusIdx;
              const current = idx === currentStatusIdx;
              const evt = order.trackingHistory?.find((e) => e.status === status);

              return (
                <View key={status} style={styles.timelineItem}>
                  {/* Line */}
                  {idx < TIMELINE_STATUSES.length - 1 && (
                    <View style={[styles.timelineLine, done && idx < currentStatusIdx && styles.timelineLineDone]} />
                  )}
                  {/* Dot */}
                  <View style={[styles.timelineDot, done && styles.timelineDotDone]}>
                    <Ionicons
                      name={STATUS_ICONS[status] as any}
                      size={12}
                      color={done ? "#fff" : "#9ca3af"}
                    />
                  </View>
                  {/* Label */}
                  <View style={styles.timelineLabel}>
                    <View style={styles.timelineLabelRow}>
                      <Text style={[styles.timelineLabelText, !done && styles.timelineLabelInactive]}>
                        {STATUS_LABELS[status]}
                      </Text>
                      {current && (
                        <View style={styles.currentBadge}>
                          <Text style={styles.currentBadgeText}>Current</Text>
                        </View>
                      )}
                    </View>
                    {evt ? (
                      <Text style={styles.timelineTime}>
                        {evt.message} · {new Date(evt.createdAt).toLocaleTimeString("en-KE", { hour: "2-digit", minute: "2-digit" })}
                      </Text>
                    ) : !done ? (
                      <Text style={styles.timelineUpcoming}>Upcoming</Text>
                    ) : null}
                  </View>
                </View>
              );
            })}
          </View>
        )}

        {/* Rider / Map indicator */}
        {order.status === "OUT_FOR_DELIVERY" && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Rider Location</Text>
            <View style={styles.mapPlaceholder}>
              <Ionicons name="bicycle" size={32} color={riderPos ? "#059669" : "#d1d5db"} />
              <Text style={styles.mapText}>
                {riderPos
                  ? `Rider is nearby (${riderPos.lat.toFixed(3)}, ${riderPos.lng.toFixed(3)})`
                  : "Waiting for rider location update..."}
              </Text>
            </View>
          </View>
        )}

        {/* Delivery Address */}
        {order.deliveryAddress && (
          <View style={styles.card}>
            <Text style={styles.cardTitle}>Delivery Address</Text>
            <View style={styles.addressRow}>
              <Ionicons name="location-outline" size={16} color="#059669" />
              <View style={{ flex: 1, marginLeft: 8 }}>
                <Text style={styles.addressText}>{order.deliveryAddress.streetAddress}</Text>
                {order.deliveryAddress.city && (
                  <Text style={styles.addressSubtext}>
                    {order.deliveryAddress.city}, {order.deliveryAddress.county}
                  </Text>
                )}
              </View>
            </View>
          </View>
        )}

        {/* Order Items */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Order Items</Text>
          {order.items.map((item) => (
            <View key={item.id} style={styles.orderItem}>
              <View style={styles.orderItemImage}>
                {item.productImage ? (
                  <Image source={{ uri: item.productImage }} style={styles.itemImg} resizeMode="contain" />
                ) : (
                  <Ionicons name="image-outline" size={20} color="#d1d5db" />
                )}
              </View>
              <View style={{ flex: 1, marginLeft: 10 }}>
                <Text style={styles.itemName} numberOfLines={2}>{item.productName}</Text>
                <Text style={styles.itemQty}>Qty: {item.quantity} × {formatPrice(item.unitPrice, order.currency)}</Text>
              </View>
              <Text style={styles.itemTotal}>{formatPrice(item.totalPrice, order.currency)}</Text>
            </View>
          ))}
        </View>

        {/* Digital Receipt */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Digital Receipt</Text>
          <View style={styles.receiptHeader}>
            <Text style={styles.receiptCompany}>Helvino Technologies LTD</Text>
            <Text style={styles.receiptContact}>info@helvino.org · 0110421320</Text>
          </View>
          <View style={styles.receiptRow}>
            <Text style={styles.receiptLabel}>Order #</Text>
            <Text style={styles.receiptValue}>{order.orderNumber}</Text>
          </View>
          <View style={styles.receiptRow}>
            <Text style={styles.receiptLabel}>Date</Text>
            <Text style={styles.receiptValue}>
              {new Date(order.createdAt).toLocaleDateString("en-KE")}
            </Text>
          </View>
          <View style={styles.receiptRow}>
            <Text style={styles.receiptLabel}>Payment</Text>
            <Text style={styles.receiptValue}>{order.paymentMethod}</Text>
          </View>
          <View style={[styles.receiptRow, styles.receiptDivider]}>
            <Text style={styles.receiptLabel}>Subtotal</Text>
            <Text style={styles.receiptValue}>{formatPrice(order.subtotal, order.currency)}</Text>
          </View>
          {order.deliveryFee > 0 && (
            <View style={styles.receiptRow}>
              <Text style={styles.receiptLabel}>Delivery</Text>
              <Text style={styles.receiptValue}>{formatPrice(order.deliveryFee, order.currency)}</Text>
            </View>
          )}
          {order.discountAmount > 0 && (
            <View style={styles.receiptRow}>
              <Text style={[styles.receiptLabel, { color: "#059669" }]}>Discount</Text>
              <Text style={[styles.receiptValue, { color: "#059669" }]}>
                -{formatPrice(order.discountAmount, order.currency)}
              </Text>
            </View>
          )}
          <View style={[styles.receiptRow, styles.totalRow]}>
            <Text style={styles.totalLabel}>Total</Text>
            <Text style={styles.totalValue}>{formatPrice(order.totalAmount, order.currency)}</Text>
          </View>
        </View>

        <TouchableOpacity
          style={styles.allOrdersBtn}
          onPress={() => router.push("/tabs/orders")}
        >
          <Text style={styles.allOrdersBtnText}>View All Orders</Text>
        </TouchableOpacity>

        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  centered: { flex: 1, alignItems: "center", justifyContent: "center" },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 16, paddingVertical: 12, backgroundColor: "#fff",
    borderBottomWidth: 1, borderBottomColor: "#f3f4f6",
  },
  backBtn: { padding: 4 },
  headerTitle: { fontSize: 16, fontWeight: "700", color: "#111827" },
  content: { padding: 16 },
  statusBanner: {
    flexDirection: "row", alignItems: "center", padding: 14,
    borderRadius: 14, marginBottom: 12,
  },
  deliveredBanner: { backgroundColor: "#d1fae5" },
  cancelledBanner: { backgroundColor: "#fee2e2" },
  activeBanner: { backgroundColor: "#dbeafe" },
  statusBannerText: { fontSize: 15, fontWeight: "700", color: "#111827" },
  statusSubtext: { fontSize: 12, color: "#6b7280", marginTop: 2 },
  card: {
    backgroundColor: "#fff", borderRadius: 14, padding: 16,
    marginBottom: 12, borderWidth: 1, borderColor: "#f3f4f6",
  },
  cardTitle: { fontSize: 15, fontWeight: "700", color: "#111827", marginBottom: 14 },
  timelineItem: {
    flexDirection: "row", paddingLeft: 8, marginBottom: 16, position: "relative",
  },
  timelineLine: {
    position: "absolute", left: 19, top: 22, bottom: -16,
    width: 2, backgroundColor: "#e5e7eb",
  },
  timelineLineDone: { backgroundColor: "#059669" },
  timelineDot: {
    width: 24, height: 24, borderRadius: 12, backgroundColor: "#e5e7eb",
    alignItems: "center", justifyContent: "center", zIndex: 1,
  },
  timelineDotDone: { backgroundColor: "#059669" },
  timelineLabel: { flex: 1, marginLeft: 12, paddingTop: 2 },
  timelineLabelRow: { flexDirection: "row", alignItems: "center", gap: 6 },
  timelineLabelText: { fontSize: 14, fontWeight: "600", color: "#111827" },
  timelineLabelInactive: { color: "#9ca3af" },
  currentBadge: { backgroundColor: "#d1fae5", borderRadius: 100, paddingHorizontal: 8, paddingVertical: 2 },
  currentBadgeText: { fontSize: 10, fontWeight: "700", color: "#059669" },
  timelineTime: { fontSize: 11, color: "#6b7280", marginTop: 2 },
  timelineUpcoming: { fontSize: 11, color: "#d1d5db", marginTop: 2 },
  mapPlaceholder: {
    backgroundColor: "#f0fdf4", borderRadius: 10, padding: 20,
    alignItems: "center", gap: 8, borderWidth: 1, borderColor: "#bbf7d0",
  },
  mapText: { fontSize: 13, color: "#6b7280", textAlign: "center" },
  addressRow: { flexDirection: "row", alignItems: "flex-start" },
  addressText: { fontSize: 14, color: "#111827", fontWeight: "500" },
  addressSubtext: { fontSize: 12, color: "#6b7280", marginTop: 2 },
  orderItem: { flexDirection: "row", alignItems: "center", marginBottom: 12 },
  orderItemImage: {
    width: 52, height: 52, backgroundColor: "#f9fafb", borderRadius: 8,
    alignItems: "center", justifyContent: "center", overflow: "hidden",
  },
  itemImg: { width: "100%", height: "100%" },
  itemName: { fontSize: 13, fontWeight: "600", color: "#111827", lineHeight: 17 },
  itemQty: { fontSize: 12, color: "#6b7280", marginTop: 3 },
  itemTotal: { fontSize: 13, fontWeight: "700", color: "#111827" },
  receiptHeader: { alignItems: "center", paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: "#f3f4f6", marginBottom: 12 },
  receiptCompany: { fontSize: 15, fontWeight: "700", color: "#111827" },
  receiptContact: { fontSize: 11, color: "#6b7280", marginTop: 2 },
  receiptRow: { flexDirection: "row", justifyContent: "space-between", marginBottom: 6 },
  receiptDivider: { borderTopWidth: 1, borderTopColor: "#f3f4f6", paddingTop: 8, marginTop: 4 },
  receiptLabel: { fontSize: 13, color: "#6b7280" },
  receiptValue: { fontSize: 13, fontWeight: "600", color: "#111827" },
  totalRow: { borderTopWidth: 1, borderTopColor: "#e5e7eb", paddingTop: 10, marginTop: 4 },
  totalLabel: { fontSize: 15, fontWeight: "700", color: "#111827" },
  totalValue: { fontSize: 16, fontWeight: "800", color: "#059669" },
  allOrdersBtn: {
    borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 14, paddingVertical: 12,
    alignItems: "center", marginBottom: 8,
  },
  allOrdersBtnText: { fontSize: 14, fontWeight: "600", color: "#374151" },
});
