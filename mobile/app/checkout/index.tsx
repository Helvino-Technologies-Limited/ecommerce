import React, { useState } from "react";
import {
  View, Text, ScrollView, TouchableOpacity, TextInput, StyleSheet,
  ActivityIndicator, Alert, Platform,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { SafeAreaView } from "react-native-safe-area-context";
import * as Location from "expo-location";
import Toast from "react-native-toast-message";
import { useCartStore } from "@/src/store/cart";
import { useAuthStore } from "@/src/store/auth";
import { formatPrice } from "@/src/lib/utils";
import { initiateSTKPush, formatPhoneForMpesa } from "@/src/lib/mpesa";
import api from "@/src/lib/api";

// Kenya counties data (abbreviated for mobile)
const KENYA_COUNTIES: Record<string, { towns: string[]; cost: number; days: number }> = {
  "Nairobi":      { towns: ["CBD", "Westlands", "Karen", "Embakasi", "Kasarani", "Langata", "Dagoretti", "Ruaraka", "Makadara", "Starehe", "Roysambu", "Kibra"], cost: 150, days: 1 },
  "Kiambu":       { towns: ["Kiambu Town", "Thika", "Ruiru", "Limuru", "Kikuyu", "Gatundu", "Juja", "Lari", "Kabete"], cost: 300, days: 2 },
  "Machakos":     { towns: ["Machakos Town", "Athi River", "Kangundo", "Mavoko", "Yatta", "Mwala", "Masinga"], cost: 300, days: 2 },
  "Kajiado":      { towns: ["Kajiado Town", "Ngong", "Kitengela", "Ongata Rongai", "Loitoktok", "Namanga"], cost: 300, days: 2 },
  "Murang'a":     { towns: ["Murang'a Town", "Kangema", "Kiharu", "Kandara", "Gatanga", "Maragwa"], cost: 300, days: 2 },
  "Nyeri":        { towns: ["Nyeri Town", "Othaya", "Mukurweini", "Kieni", "Mathira", "Tetu"], cost: 300, days: 2 },
  "Nakuru":       { towns: ["Nakuru Town", "Naivasha", "Gilgil", "Molo", "Njoro", "Rongai"], cost: 300, days: 2 },
  "Mombasa":      { towns: ["Mombasa Island", "Nyali", "Bamburi", "Likoni", "Kisauni", "Changamwe"], cost: 500, days: 3 },
  "Kwale":        { towns: ["Kwale Town", "Ukunda", "Diani", "Lungalunga", "Msambweni"], cost: 500, days: 3 },
  "Kilifi":       { towns: ["Kilifi Town", "Malindi", "Mtwapa", "Kaloleni", "Rabai"], cost: 500, days: 3 },
  "Meru":         { towns: ["Meru Town", "Nkubu", "Maua", "Tigania", "Igembe", "Timau"], cost: 500, days: 3 },
  "Embu":         { towns: ["Embu Town", "Runyenjes", "Mbeere", "Ena", "Kiritiri"], cost: 500, days: 3 },
  "Kisumu":       { towns: ["Kisumu CBD", "Kisumu East", "Kisumu West", "Nyando", "Muhoroni"], cost: 500, days: 3 },
  "Uasin Gishu":  { towns: ["Eldoret", "Turbo", "Moiben", "Ainabkoi", "Kapseret"], cost: 500, days: 3 },
  "Kakamega":     { towns: ["Kakamega Town", "Mumias", "Butere", "Matungu", "Khwisero"], cost: 500, days: 3 },
  "Kisii":        { towns: ["Kisii Town", "Suneka", "Ogembo", "Keroka", "Masimba"], cost: 500, days: 3 },
  "Garissa":      { towns: ["Garissa Town", "Dadaab", "Fafi", "Hulugho", "Ijara"], cost: 800, days: 5 },
  "Wajir":        { towns: ["Wajir Town", "Habaswein", "Buna", "Eldas", "Tarbaj"], cost: 800, days: 5 },
  "Mandera":      { towns: ["Mandera Town", "Lafey", "Mandera North", "Mandera South", "Banissa"], cost: 800, days: 5 },
  "Marsabit":     { towns: ["Marsabit Town", "Moyale", "Laisamis", "Saku", "North Horr"], cost: 800, days: 5 },
  "Turkana":      { towns: ["Lodwar", "Lokichogio", "Kakuma", "Kalokol", "Loima"], cost: 800, days: 5 },
};

const COUNTY_LIST = Object.keys(KENYA_COUNTIES).sort();

type PaymentMethod = "MPESA" | "CARD" | "WALLET";
type Step = "details" | "payment" | "confirmation";

export default function CheckoutScreen() {
  const { items, total, clearCart } = useCartStore();
  const { user, isAuthenticated } = useAuthStore();

  const [step, setStep] = useState<Step>("details");
  const [county, setCounty] = useState("");
  const [town, setTown] = useState("");
  const [streetAddress, setStreetAddress] = useState("");
  const [phone, setPhone] = useState(user?.phone ?? "");
  const [notes, setNotes] = useState("");
  const [latitude, setLatitude] = useState<number | null>(null);
  const [longitude, setLongitude] = useState<number | null>(null);
  const [locating, setLocating] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("MPESA");
  const [submitting, setSubmitting] = useState(false);
  const [showCountyPicker, setShowCountyPicker] = useState(false);
  const [showTownPicker, setShowTownPicker] = useState(false);
  const [orderId, setOrderId] = useState<string | null>(null);

  if (!isAuthenticated) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.centered}>
          <Text style={styles.notAuthText}>Sign in to checkout</Text>
          <TouchableOpacity style={styles.signInBtn} onPress={() => router.push("/auth/login")}>
            <Text style={styles.signInBtnText}>Sign In</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  const countyInfo = county ? KENYA_COUNTIES[county] : null;
  const subtotal = total();
  const currency = items[0]?.currency ?? "KES";
  const deliveryFee = subtotal >= 5000 ? 0 : (countyInfo?.cost ?? 500);
  const grandTotal = subtotal + deliveryFee;

  const useMyLocation = async () => {
    setLocating(true);
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== "granted") {
        Alert.alert("Permission denied", "Please allow location access in settings.");
        return;
      }
      const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      setLatitude(loc.coords.latitude);
      setLongitude(loc.coords.longitude);
      Toast.show({ type: "success", text1: "Location detected!" });
    } catch {
      Alert.alert("Error", "Unable to get location. Please try again.");
    } finally {
      setLocating(false);
    }
  };

  const handlePlaceOrder = async () => {
    if (!county || !town || !streetAddress) {
      Toast.show({ type: "error", text1: "Please complete delivery details" });
      return;
    }
    if (!phone) {
      Toast.show({ type: "error", text1: "Please enter your phone number" });
      return;
    }

    setSubmitting(true);
    try {
      const orderData = {
        items: items.map((i) => ({ productId: i.id.split("-")[0], quantity: i.quantity })),
        deliveryAddress: { streetAddress, city: town, county, town, latitude, longitude },
        deliveryFee,
        paymentMethod,
        notes,
        currency,
      };

      const { data: order } = await api.post("/orders", orderData);
      setOrderId(order.id);

      if (paymentMethod === "MPESA") {
        await initiateSTKPush({ orderId: order.id, phone: formatPhoneForMpesa(phone) });
        Toast.show({ type: "success", text1: "STK Push sent!", text2: "Complete payment on your phone" });
        clearCart();
        setStep("confirmation");
      } else if (paymentMethod === "CARD") {
        const { data: payData } = await api.post("/payments/card/initiate", { orderId: order.id });
        const link = payData?.data?.link;
        if (link) {
          // In production, open with Linking.openURL(link)
          Toast.show({ type: "info", text1: "Redirecting to payment..." });
          clearCart();
          setStep("confirmation");
        }
      } else {
        clearCart();
        setStep("confirmation");
        Toast.show({ type: "success", text1: "Order placed successfully!" });
      }
    } catch (err: any) {
      Toast.show({ type: "error", text1: err.response?.data?.message ?? "Checkout failed" });
    } finally {
      setSubmitting(false);
    }
  };

  if (step === "confirmation") {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.confirmationContainer}>
          <View style={styles.successIcon}>
            <Ionicons name="checkmark" size={40} color="#fff" />
          </View>
          <Text style={styles.confirmTitle}>Order Placed!</Text>
          <Text style={styles.confirmSubtext}>
            {paymentMethod === "MPESA"
              ? "Complete the M-Pesa STK Push on your phone to confirm."
              : "Your order has been placed successfully."}
          </Text>
          {county && (
            <Text style={styles.confirmDelivery}>
              Estimated delivery: {countyInfo?.days ?? 3} days to {town}, {county}
            </Text>
          )}
          <TouchableOpacity style={styles.trackBtn} onPress={() => { router.push("/tabs/orders"); }}>
            <Text style={styles.trackBtnText}>Track Order</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.shopBtn} onPress={() => router.push("/tabs/products")}>
            <Text style={styles.shopBtnText}>Continue Shopping</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} style={{ padding: 4 }}>
          <Ionicons name="arrow-back" size={22} color="#111827" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Checkout</Text>
        <View style={{ width: 36 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        {/* Delivery Location */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>
            <Ionicons name="location-outline" size={15} color="#059669" /> Delivery Location
          </Text>

          {/* County Picker */}
          <Text style={styles.fieldLabel}>County *</Text>
          <TouchableOpacity style={styles.picker} onPress={() => setShowCountyPicker(!showCountyPicker)}>
            <Text style={[styles.pickerText, !county && styles.pickerPlaceholder]}>
              {county || "Select county..."}
            </Text>
            <Ionicons name="chevron-down" size={16} color="#9ca3af" />
          </TouchableOpacity>

          {showCountyPicker && (
            <View style={styles.dropdownList}>
              <ScrollView style={{ maxHeight: 200 }}>
                {COUNTY_LIST.map((c) => (
                  <TouchableOpacity key={c} style={styles.dropdownItem}
                    onPress={() => { setCounty(c); setTown(""); setShowCountyPicker(false); }}>
                    <Text style={[styles.dropdownItemText, county === c && styles.dropdownItemSelected]}>
                      {c}
                    </Text>
                    {county === c && <Ionicons name="checkmark" size={14} color="#059669" />}
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          )}

          {countyInfo && (
            <Text style={styles.deliveryCostHint}>
              Delivery: KSh {countyInfo.cost.toLocaleString()} · Est. {countyInfo.days} day{countyInfo.days > 1 ? "s" : ""}
            </Text>
          )}

          {/* Town Picker */}
          <Text style={styles.fieldLabel}>Town / Area *</Text>
          <TouchableOpacity
            style={[styles.picker, !county && styles.pickerDisabled]}
            onPress={() => county && setShowTownPicker(!showTownPicker)}
          >
            <Text style={[styles.pickerText, !town && styles.pickerPlaceholder]}>
              {town || (county ? "Select town..." : "Select county first")}
            </Text>
            <Ionicons name="chevron-down" size={16} color="#9ca3af" />
          </TouchableOpacity>

          {showTownPicker && countyInfo && (
            <View style={styles.dropdownList}>
              <ScrollView style={{ maxHeight: 180 }}>
                {countyInfo.towns.map((t) => (
                  <TouchableOpacity key={t} style={styles.dropdownItem}
                    onPress={() => { setTown(t); setShowTownPicker(false); }}>
                    <Text style={[styles.dropdownItemText, town === t && styles.dropdownItemSelected]}>
                      {t}
                    </Text>
                    {town === t && <Ionicons name="checkmark" size={14} color="#059669" />}
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </View>
          )}

          {/* Street Address */}
          <Text style={styles.fieldLabel}>Street Address *</Text>
          <TextInput
            value={streetAddress}
            onChangeText={setStreetAddress}
            placeholder="e.g. 123 Kenyatta Avenue, Apt 4B"
            placeholderTextColor="#9ca3af"
            style={styles.textInput}
          />

          {/* GPS */}
          <TouchableOpacity
            style={styles.gpsBtn}
            onPress={useMyLocation}
            disabled={locating}
          >
            <Ionicons name="navigate-outline" size={16} color="#059669" />
            <Text style={styles.gpsBtnText}>
              {locating ? "Getting location..." : "Use My GPS Location"}
            </Text>
          </TouchableOpacity>
          {latitude !== null && longitude !== null && (
            <Text style={styles.coordsText}>
              GPS: {latitude.toFixed(4)}, {longitude.toFixed(4)}
            </Text>
          )}
        </View>

        {/* Contact */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Contact Details</Text>
          <Text style={styles.fieldLabel}>Phone Number *</Text>
          <View style={styles.phoneRow}>
            <View style={styles.countryCode}>
              <Text style={styles.countryCodeText}>+254</Text>
            </View>
            <TextInput
              value={phone}
              onChangeText={setPhone}
              placeholder="712 345 678"
              placeholderTextColor="#9ca3af"
              keyboardType="phone-pad"
              style={[styles.textInput, { flex: 1, borderTopLeftRadius: 0, borderBottomLeftRadius: 0 }]}
            />
          </View>
          <Text style={styles.fieldLabel}>Order Notes (optional)</Text>
          <TextInput
            value={notes}
            onChangeText={setNotes}
            placeholder="Any delivery instructions..."
            placeholderTextColor="#9ca3af"
            multiline
            numberOfLines={3}
            style={[styles.textInput, { height: 70, textAlignVertical: "top", paddingTop: 10 }]}
          />
        </View>

        {/* Payment Method */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Payment Method</Text>
          {[
            { id: "MPESA" as PaymentMethod, icon: "phone-portrait-outline", label: "M-Pesa", desc: "Pay via STK Push" },
            { id: "CARD" as PaymentMethod, icon: "card-outline", label: "Card", desc: "Visa, Mastercard" },
            { id: "WALLET" as PaymentMethod, icon: "wallet-outline", label: "Wallet", desc: `KSh ${(user?.walletBalance ?? 0).toLocaleString()}` },
          ].map((method) => (
            <TouchableOpacity
              key={method.id}
              style={[styles.paymentOption, paymentMethod === method.id && styles.paymentOptionActive]}
              onPress={() => setPaymentMethod(method.id)}
            >
              <View style={[styles.paymentIcon, paymentMethod === method.id && styles.paymentIconActive]}>
                <Ionicons name={method.icon as any} size={18} color={paymentMethod === method.id ? "#059669" : "#9ca3af"} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.paymentLabel}>{method.label}</Text>
                <Text style={styles.paymentDesc}>{method.desc}</Text>
              </View>
              <View style={[styles.radio, paymentMethod === method.id && styles.radioActive]}>
                {paymentMethod === method.id && <View style={styles.radioInner} />}
              </View>
            </TouchableOpacity>
          ))}
        </View>

        {/* Order Summary */}
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Order Summary</Text>
          {items.map((item) => (
            <View key={item.id} style={styles.summaryItem}>
              <Text style={styles.summaryItemName} numberOfLines={1}>
                {item.name} ×{item.quantity}
              </Text>
              <Text style={styles.summaryItemPrice}>
                {formatPrice((item.discountedPrice ?? item.price) * item.quantity, currency)}
              </Text>
            </View>
          ))}
          <View style={styles.summaryDivider} />
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>Subtotal</Text>
            <Text style={styles.summaryValue}>{formatPrice(subtotal, currency)}</Text>
          </View>
          <View style={styles.summaryRow}>
            <Text style={styles.summaryLabel}>Delivery</Text>
            <Text style={[styles.summaryValue, deliveryFee === 0 && { color: "#059669" }]}>
              {deliveryFee === 0 ? "FREE" : formatPrice(deliveryFee, currency)}
            </Text>
          </View>
          {county && (
            <View style={styles.summaryRow}>
              <Text style={[styles.summaryLabel, { color: "#9ca3af", fontSize: 12 }]}>Est. delivery</Text>
              <Text style={[styles.summaryValue, { color: "#9ca3af", fontSize: 12 }]}>
                {countyInfo?.days ?? 3} days · {county}
              </Text>
            </View>
          )}
          <View style={styles.totalRow}>
            <Text style={styles.totalLabel}>Total</Text>
            <Text style={styles.totalValue}>{formatPrice(grandTotal, currency)}</Text>
          </View>
        </View>

        <TouchableOpacity
          style={[styles.placeOrderBtn, submitting && { opacity: 0.7 }]}
          onPress={handlePlaceOrder}
          disabled={submitting}
        >
          {submitting ? (
            <ActivityIndicator color="#fff" size="small" />
          ) : (
            <>
              <Ionicons name="lock-closed" size={16} color="#fff" />
              <Text style={styles.placeOrderBtnText}>
                {paymentMethod === "MPESA" ? "Pay with M-Pesa" :
                 paymentMethod === "CARD" ? "Pay with Card" : "Place Order"}
              </Text>
            </>
          )}
        </TouchableOpacity>

        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  centered: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  notAuthText: { fontSize: 16, fontWeight: "600", color: "#374151" },
  signInBtn: { backgroundColor: "#059669", borderRadius: 14, paddingHorizontal: 24, paddingVertical: 12 },
  signInBtnText: { color: "#fff", fontWeight: "700", fontSize: 15 },
  header: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    paddingHorizontal: 16, paddingVertical: 12, backgroundColor: "#fff",
    borderBottomWidth: 1, borderBottomColor: "#f3f4f6",
  },
  headerTitle: { fontSize: 17, fontWeight: "700", color: "#111827" },
  content: { padding: 16 },
  card: {
    backgroundColor: "#fff", borderRadius: 14, padding: 16,
    marginBottom: 12, borderWidth: 1, borderColor: "#f3f4f6",
  },
  cardTitle: { fontSize: 15, fontWeight: "700", color: "#111827", marginBottom: 14 },
  fieldLabel: { fontSize: 13, fontWeight: "600", color: "#374151", marginBottom: 6, marginTop: 10 },
  picker: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 12, padding: 12,
    backgroundColor: "#f9fafb",
  },
  pickerDisabled: { opacity: 0.5 },
  pickerText: { fontSize: 14, color: "#111827" },
  pickerPlaceholder: { color: "#9ca3af" },
  dropdownList: {
    borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 12,
    marginTop: 4, backgroundColor: "#fff", overflow: "hidden",
  },
  dropdownItem: {
    flexDirection: "row", alignItems: "center", justifyContent: "space-between",
    padding: 12, borderBottomWidth: 1, borderBottomColor: "#f9fafb",
  },
  dropdownItemText: { fontSize: 14, color: "#374151" },
  dropdownItemSelected: { color: "#059669", fontWeight: "700" },
  deliveryCostHint: { fontSize: 12, color: "#059669", fontWeight: "600", marginTop: 4, marginBottom: 4 },
  textInput: {
    borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 12, padding: 12,
    fontSize: 14, color: "#111827", backgroundColor: "#f9fafb",
  },
  gpsBtn: {
    flexDirection: "row", alignItems: "center", gap: 6, marginTop: 10,
    backgroundColor: "#f0fdf4", borderWidth: 1, borderColor: "#bbf7d0",
    borderRadius: 10, paddingHorizontal: 14, paddingVertical: 10, alignSelf: "flex-start",
  },
  gpsBtnText: { fontSize: 13, color: "#059669", fontWeight: "600" },
  coordsText: { fontSize: 11, color: "#6b7280", marginTop: 4 },
  phoneRow: { flexDirection: "row" },
  countryCode: {
    backgroundColor: "#f3f4f6", borderWidth: 1, borderColor: "#e5e7eb",
    borderTopLeftRadius: 12, borderBottomLeftRadius: 12, paddingHorizontal: 12,
    justifyContent: "center", borderRightWidth: 0,
  },
  countryCodeText: { fontSize: 14, color: "#374151", fontWeight: "600" },
  paymentOption: {
    flexDirection: "row", alignItems: "center", padding: 12,
    borderWidth: 2, borderColor: "#e5e7eb", borderRadius: 12,
    marginBottom: 8, backgroundColor: "#fff",
  },
  paymentOptionActive: { borderColor: "#059669", backgroundColor: "#f0fdf4" },
  paymentIcon: {
    width: 36, height: 36, borderRadius: 10, backgroundColor: "#f3f4f6",
    alignItems: "center", justifyContent: "center", marginRight: 10,
  },
  paymentIconActive: { backgroundColor: "#d1fae5" },
  paymentLabel: { fontSize: 14, fontWeight: "600", color: "#111827" },
  paymentDesc: { fontSize: 12, color: "#6b7280" },
  radio: {
    width: 18, height: 18, borderRadius: 9, borderWidth: 2,
    borderColor: "#d1d5db", alignItems: "center", justifyContent: "center",
  },
  radioActive: { borderColor: "#059669" },
  radioInner: { width: 8, height: 8, borderRadius: 4, backgroundColor: "#059669" },
  summaryItem: { flexDirection: "row", justifyContent: "space-between", marginBottom: 6 },
  summaryItemName: { flex: 1, fontSize: 13, color: "#374151", marginRight: 8 },
  summaryItemPrice: { fontSize: 13, fontWeight: "600", color: "#111827" },
  summaryDivider: { height: 1, backgroundColor: "#f3f4f6", marginVertical: 10 },
  summaryRow: { flexDirection: "row", justifyContent: "space-between", marginBottom: 4 },
  summaryLabel: { fontSize: 13, color: "#6b7280" },
  summaryValue: { fontSize: 13, fontWeight: "600", color: "#111827" },
  totalRow: { flexDirection: "row", justifyContent: "space-between", marginTop: 8, paddingTop: 10, borderTopWidth: 1, borderTopColor: "#e5e7eb" },
  totalLabel: { fontSize: 16, fontWeight: "700", color: "#111827" },
  totalValue: { fontSize: 17, fontWeight: "800", color: "#059669" },
  placeOrderBtn: {
    backgroundColor: "#059669", borderRadius: 16, paddingVertical: 16,
    flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8, marginBottom: 8,
  },
  placeOrderBtnText: { color: "#fff", fontWeight: "700", fontSize: 16 },
  confirmationContainer: { flex: 1, alignItems: "center", justifyContent: "center", padding: 24 },
  successIcon: {
    width: 80, height: 80, borderRadius: 40, backgroundColor: "#059669",
    alignItems: "center", justifyContent: "center", marginBottom: 20,
  },
  confirmTitle: { fontSize: 26, fontWeight: "800", color: "#111827", marginBottom: 8 },
  confirmSubtext: { fontSize: 14, color: "#6b7280", textAlign: "center", marginBottom: 8, lineHeight: 22 },
  confirmDelivery: { fontSize: 13, color: "#059669", fontWeight: "600", marginBottom: 24, textAlign: "center" },
  trackBtn: {
    backgroundColor: "#059669", borderRadius: 14, paddingHorizontal: 32, paddingVertical: 13,
    marginBottom: 10, width: "100%", alignItems: "center",
  },
  trackBtnText: { color: "#fff", fontWeight: "700", fontSize: 16 },
  shopBtn: {
    borderWidth: 1, borderColor: "#e5e7eb", borderRadius: 14, paddingHorizontal: 32, paddingVertical: 13,
    width: "100%", alignItems: "center",
  },
  shopBtnText: { fontSize: 15, fontWeight: "600", color: "#374151" },
});
