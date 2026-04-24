import React from "react";
import {
  View, Text, TouchableOpacity, StyleSheet, ScrollView, Alert,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuthStore } from "@/src/store/auth";

interface MenuItemProps {
  icon: string;
  label: string;
  onPress: () => void;
  danger?: boolean;
  badge?: string;
}

function MenuItem({ icon, label, onPress, danger, badge }: MenuItemProps) {
  return (
    <TouchableOpacity style={styles.menuItem} onPress={onPress}>
      <View style={[styles.menuIcon, danger && { backgroundColor: "#fee2e2" }]}>
        <Ionicons name={icon as any} size={18} color={danger ? "#ef4444" : "#059669"} />
      </View>
      <Text style={[styles.menuLabel, danger && { color: "#ef4444" }]}>{label}</Text>
      {badge && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{badge}</Text>
        </View>
      )}
      {!danger && <Ionicons name="chevron-forward" size={16} color="#9ca3af" style={{ marginLeft: "auto" }} />}
    </TouchableOpacity>
  );
}

export default function AccountScreen() {
  const { user, isAuthenticated, logout } = useAuthStore();

  const handleLogout = () => {
    Alert.alert("Sign Out", "Are you sure you want to sign out?", [
      { text: "Cancel", style: "cancel" },
      { text: "Sign Out", style: "destructive", onPress: () => { logout(); router.replace("/auth/login"); } },
    ]);
  };

  if (!isAuthenticated || !user) {
    return (
      <SafeAreaView style={styles.container} edges={["top"]}>
        <View style={styles.notAuthContainer}>
          <View style={styles.avatarLarge}>
            <Ionicons name="person" size={40} color="#9ca3af" />
          </View>
          <Text style={styles.notAuthTitle}>Not signed in</Text>
          <Text style={styles.notAuthSubtext}>Sign in to manage your account</Text>
          <TouchableOpacity style={styles.signInBtn} onPress={() => router.push("/auth/login")}>
            <Text style={styles.signInBtnText}>Sign In</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => router.push("/auth/register")}>
            <Text style={styles.registerLink}>Create Account</Text>
          </TouchableOpacity>
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container} edges={["top"]}>
      <ScrollView showsVerticalScrollIndicator={false}>
        {/* Profile Header */}
        <View style={styles.profileHeader}>
          <View style={styles.avatar}>
            <Text style={styles.avatarText}>
              {user.firstName[0]}{user.lastName[0]}
            </Text>
          </View>
          <View style={{ flex: 1, marginLeft: 14 }}>
            <Text style={styles.profileName}>{user.firstName} {user.lastName}</Text>
            <Text style={styles.profileEmail}>{user.email}</Text>
            {user.phone && <Text style={styles.profilePhone}>{user.phone}</Text>}
          </View>
        </View>

        {/* Stats */}
        <View style={styles.statsRow}>
          <View style={styles.statCard}>
            <Ionicons name="wallet-outline" size={20} color="#059669" />
            <Text style={styles.statValue}>KSh {Number(user.walletBalance ?? 0).toLocaleString()}</Text>
            <Text style={styles.statLabel}>Wallet Balance</Text>
          </View>
          <View style={styles.statCard}>
            <Ionicons name="star-outline" size={20} color="#f59e0b" />
            <Text style={styles.statValue}>{user.loyaltyPoints ?? 0}</Text>
            <Text style={styles.statLabel}>Loyalty Points</Text>
          </View>
          <View style={styles.statCard}>
            <Ionicons name="bag-outline" size={20} color="#6366f1" />
            <Text style={styles.statValue}>-</Text>
            <Text style={styles.statLabel}>Orders</Text>
          </View>
        </View>

        {/* Menu */}
        <View style={styles.menuSection}>
          <Text style={styles.sectionTitle}>My Account</Text>
          <View style={styles.menuCard}>
            <MenuItem icon="bag-outline" label="My Orders" onPress={() => router.push("/tabs/orders")} />
            <MenuItem icon="location-outline" label="Saved Addresses" onPress={() => {}} />
            <MenuItem icon="wallet-outline" label="Wallet & Payments" onPress={() => {}} />
            <MenuItem icon="heart-outline" label="Wishlist" onPress={() => {}} />
          </View>
        </View>

        <View style={styles.menuSection}>
          <Text style={styles.sectionTitle}>Settings</Text>
          <View style={styles.menuCard}>
            <MenuItem icon="person-outline" label="Edit Profile" onPress={() => {}} />
            <MenuItem icon="notifications-outline" label="Notifications" onPress={() => {}} />
            <MenuItem icon="lock-closed-outline" label="Change Password" onPress={() => {}} />
            <MenuItem icon="shield-checkmark-outline" label="Privacy & Security" onPress={() => {}} />
          </View>
        </View>

        <View style={styles.menuSection}>
          <Text style={styles.sectionTitle}>Support</Text>
          <View style={styles.menuCard}>
            <MenuItem icon="chatbubble-outline" label="Contact Us" onPress={() => {}} />
            <MenuItem icon="help-circle-outline" label="FAQ" onPress={() => {}} />
            <MenuItem icon="document-text-outline" label="Terms & Conditions" onPress={() => {}} />
            <MenuItem icon="shield-outline" label="Privacy Policy" onPress={() => {}} />
          </View>
        </View>

        <View style={[styles.menuSection, { marginBottom: 30 }]}>
          <View style={styles.menuCard}>
            <MenuItem icon="log-out-outline" label="Sign Out" onPress={handleLogout} danger />
          </View>
        </View>

        <Text style={styles.versionText}>Helvino Shop v1.0.0</Text>
        <Text style={styles.companyText}>Helvino Technologies LTD · info@helvino.org</Text>
        <View style={{ height: 30 }} />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  notAuthContainer: { flex: 1, alignItems: "center", justifyContent: "center", gap: 12 },
  avatarLarge: {
    width: 80, height: 80, borderRadius: 40, backgroundColor: "#f3f4f6",
    alignItems: "center", justifyContent: "center",
  },
  notAuthTitle: { fontSize: 18, fontWeight: "700", color: "#111827" },
  notAuthSubtext: { fontSize: 14, color: "#9ca3af" },
  signInBtn: { backgroundColor: "#059669", borderRadius: 14, paddingHorizontal: 32, paddingVertical: 12 },
  signInBtnText: { color: "#fff", fontWeight: "700", fontSize: 15 },
  registerLink: { fontSize: 14, color: "#059669", fontWeight: "600" },
  profileHeader: {
    flexDirection: "row", alignItems: "center", padding: 20,
    backgroundColor: "#fff", borderBottomWidth: 1, borderBottomColor: "#f3f4f6",
  },
  avatar: {
    width: 56, height: 56, borderRadius: 28, backgroundColor: "#d1fae5",
    alignItems: "center", justifyContent: "center",
  },
  avatarText: { fontSize: 20, fontWeight: "800", color: "#059669" },
  profileName: { fontSize: 17, fontWeight: "700", color: "#111827" },
  profileEmail: { fontSize: 13, color: "#6b7280", marginTop: 2 },
  profilePhone: { fontSize: 12, color: "#9ca3af", marginTop: 1 },
  statsRow: { flexDirection: "row", padding: 12, gap: 8 },
  statCard: {
    flex: 1, backgroundColor: "#fff", borderRadius: 14, padding: 14,
    alignItems: "center", gap: 4, borderWidth: 1, borderColor: "#f3f4f6",
  },
  statValue: { fontSize: 16, fontWeight: "800", color: "#111827" },
  statLabel: { fontSize: 10, color: "#9ca3af", textAlign: "center" },
  menuSection: { marginHorizontal: 12, marginBottom: 8 },
  sectionTitle: { fontSize: 12, fontWeight: "700", color: "#9ca3af", marginBottom: 6, marginLeft: 4, textTransform: "uppercase" },
  menuCard: { backgroundColor: "#fff", borderRadius: 14, overflow: "hidden", borderWidth: 1, borderColor: "#f3f4f6" },
  menuItem: {
    flexDirection: "row", alignItems: "center", padding: 14,
    borderBottomWidth: 1, borderBottomColor: "#f9fafb",
  },
  menuIcon: {
    width: 34, height: 34, borderRadius: 10, backgroundColor: "#f0fdf4",
    alignItems: "center", justifyContent: "center", marginRight: 12,
  },
  menuLabel: { fontSize: 14, fontWeight: "500", color: "#111827", flex: 1 },
  badge: { backgroundColor: "#d1fae5", borderRadius: 100, paddingHorizontal: 8, paddingVertical: 2, marginRight: 6 },
  badgeText: { fontSize: 11, fontWeight: "700", color: "#059669" },
  versionText: { textAlign: "center", fontSize: 12, color: "#9ca3af" },
  companyText: { textAlign: "center", fontSize: 11, color: "#d1d5db", marginTop: 2 },
});
