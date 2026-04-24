import React, { useState, useEffect } from "react";
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
  Alert, ScrollView,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import MapView, { Marker, Polyline } from "react-native-maps";
import * as Location from "expo-location";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/src/lib/api";
import { formatPrice } from "@/src/lib/utils";

export default function RiderScreen() {
  const [tab, setTab] = useState<"pending" | "active" | "history">("pending");
  const [location, setLocation] = useState<any>(null);
  const [activeOrder, setActiveOrder] = useState<any>(null);
  const qc = useQueryClient();

  const { data: orders } = useQuery({
    queryKey: ["rider-orders", tab],
    queryFn: () => api.get("/rider/orders", { params: { status: tab } }).then((r) => r.data),
    refetchInterval: 15000,
  });

  useEffect(() => {
    const startTracking = async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") return;

      Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, timeInterval: 10000, distanceInterval: 10 },
        (loc) => {
          setLocation(loc);
          if (activeOrder) {
            api.post(`/rider/orders/${activeOrder.id}/location`, {
              latitude: loc.coords.latitude,
              longitude: loc.coords.longitude,
            }).catch(() => {});
          }
        }
      );
    };
    startTracking();
  }, [activeOrder]);

  const acceptOrder = useMutation({
    mutationFn: (orderId: string) => api.post(`/rider/orders/${orderId}/accept`),
    onSuccess: (_, orderId) => {
      qc.invalidateQueries({ queryKey: ["rider-orders"] });
      Alert.alert("Order Accepted", "You've accepted this order");
    },
  });

  const updateStatus = useMutation({
    mutationFn: ({ orderId, status }: { orderId: string; status: string }) =>
      api.patch(`/rider/orders/${orderId}/status`, { status }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["rider-orders"] }); },
  });

  const confirmDelivery = useMutation({
    mutationFn: (orderId: string) => api.post(`/rider/orders/${orderId}/confirm-delivery`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rider-orders"] });
      setActiveOrder(null);
      Alert.alert("Delivery Confirmed", "Great job! Order marked as delivered.");
    },
  });

  const OrderCard = ({ order }: { order: any }) => (
    <View style={styles.orderCard}>
      <View style={styles.orderHeader}>
        <Text style={styles.orderNum}>#{order.orderNumber}</Text>
        <Text style={styles.orderAmount}>{formatPrice(order.totalAmount, order.currency)}</Text>
      </View>

      <View style={styles.orderDetail}>
        <Ionicons name="person" size={14} color="#6b7280" />
        <Text style={styles.orderDetailText}>{order.customer?.firstName} {order.customer?.lastName}</Text>
      </View>

      {order.deliveryAddress && (
        <View style={styles.orderDetail}>
          <Ionicons name="location" size={14} color="#6b7280" />
          <Text style={styles.orderDetailText}>{order.deliveryAddress.streetAddress}, {order.deliveryAddress.city}</Text>
        </View>
      )}

      <View style={styles.orderDetail}>
        <Ionicons name="cube" size={14} color="#6b7280" />
        <Text style={styles.orderDetailText}>{order.items?.length ?? 0} items</Text>
      </View>

      {tab === "pending" && (
        <TouchableOpacity style={styles.acceptBtn} onPress={() => acceptOrder.mutate(order.id)}>
          <Text style={styles.acceptBtnText}>Accept Order</Text>
        </TouchableOpacity>
      )}

      {tab === "active" && (
        <View style={styles.actionRow}>
          <TouchableOpacity
            style={[styles.actionBtn, { backgroundColor: "#dbeafe" }]}
            onPress={() => updateStatus.mutate({ orderId: order.id, status: "OUT_FOR_DELIVERY" })}>
            <Text style={{ color: "#1d4ed8", fontWeight: "600", fontSize: 12 }}>Out for Delivery</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.actionBtn, { backgroundColor: "#dcfce7" }]}
            onPress={() => { setActiveOrder(order); confirmDelivery.mutate(order.id); }}>
            <Text style={{ color: "#15803d", fontWeight: "600", fontSize: 12 }}>Confirm Delivery</Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );

  return (
    <View style={styles.container}>
      {/* Map (when active order) */}
      {activeOrder && location && (
        <MapView
          style={styles.map}
          initialRegion={{
            latitude: location.coords.latitude,
            longitude: location.coords.longitude,
            latitudeDelta: 0.01, longitudeDelta: 0.01,
          }}>
          <Marker coordinate={{ latitude: location.coords.latitude, longitude: location.coords.longitude }}
            title="You" pinColor="#059669" />
          {activeOrder.deliveryAddress?.latitude && (
            <Marker coordinate={{
              latitude: activeOrder.deliveryAddress.latitude,
              longitude: activeOrder.deliveryAddress.longitude,
            }} title="Delivery" pinColor="#ef4444" />
          )}
        </MapView>
      )}

      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Rider Dashboard</Text>
        <View style={styles.statusIndicator}>
          <View style={styles.statusDot} />
          <Text style={styles.statusText}>Online</Text>
        </View>
      </View>

      {/* Tabs */}
      <View style={styles.tabs}>
        {(["pending", "active", "history"] as const).map((t) => (
          <TouchableOpacity key={t} style={[styles.tab, tab === t && styles.tabActive]} onPress={() => setTab(t)}>
            <Text style={[styles.tabText, tab === t && styles.tabTextActive]}>
              {t.charAt(0).toUpperCase() + t.slice(1)}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <FlatList
        data={orders ?? []}
        keyExtractor={(o: any) => o.id}
        renderItem={({ item }) => <OrderCard order={item} />}
        contentContainerStyle={{ padding: 12, paddingBottom: 100 }}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons name="bicycle" size={48} color="#d1d5db" />
            <Text style={styles.emptyText}>No {tab} orders</Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  map: { height: 200 },
  header: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", padding: 16, backgroundColor: "#fff", borderBottomWidth: 1, borderBottomColor: "#f3f4f6" },
  headerTitle: { fontSize: 18, fontWeight: "700", color: "#111" },
  statusIndicator: { flexDirection: "row", alignItems: "center", gap: 6, backgroundColor: "#dcfce7", paddingHorizontal: 10, paddingVertical: 4, borderRadius: 100 },
  statusDot: { width: 8, height: 8, backgroundColor: "#16a34a", borderRadius: 4 },
  statusText: { fontSize: 12, color: "#15803d", fontWeight: "600" },
  tabs: { flexDirection: "row", backgroundColor: "#fff", borderBottomWidth: 1, borderBottomColor: "#f3f4f6" },
  tab: { flex: 1, paddingVertical: 12, alignItems: "center" },
  tabActive: { borderBottomWidth: 2, borderBottomColor: "#059669" },
  tabText: { fontSize: 13, color: "#6b7280", fontWeight: "500" },
  tabTextActive: { color: "#059669", fontWeight: "700" },
  orderCard: { backgroundColor: "#fff", borderRadius: 12, padding: 14, marginBottom: 8, borderWidth: 1, borderColor: "#f3f4f6" },
  orderHeader: { flexDirection: "row", justifyContent: "space-between", marginBottom: 8 },
  orderNum: { fontWeight: "700", color: "#059669", fontSize: 14 },
  orderAmount: { fontWeight: "700", color: "#111", fontSize: 14 },
  orderDetail: { flexDirection: "row", alignItems: "center", gap: 6, marginBottom: 4 },
  orderDetailText: { fontSize: 12, color: "#374151" },
  acceptBtn: { marginTop: 8, backgroundColor: "#059669", borderRadius: 8, paddingVertical: 10, alignItems: "center" },
  acceptBtnText: { color: "#fff", fontWeight: "700", fontSize: 13 },
  actionRow: { flexDirection: "row", gap: 8, marginTop: 8 },
  actionBtn: { flex: 1, borderRadius: 8, paddingVertical: 8, alignItems: "center" },
  empty: { alignItems: "center", paddingVertical: 48 },
  emptyText: { color: "#9ca3af", fontSize: 14, marginTop: 8 },
});
