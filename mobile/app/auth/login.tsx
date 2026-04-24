import React, { useState } from "react";
import {
  View, Text, TextInput, TouchableOpacity, StyleSheet,
  ScrollView, KeyboardAvoidingView, Platform, ActivityIndicator,
} from "react-native";
import { router } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { SafeAreaView } from "react-native-safe-area-context";
import { useAuthStore } from "@/src/store/auth";
import Toast from "react-native-toast-message";

export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const { login, isLoading } = useAuthStore();

  const handleLogin = async () => {
    if (!email.trim() || !password) {
      Toast.show({ type: "error", text1: "Please fill all fields" });
      return;
    }
    try {
      await login(email.trim().toLowerCase(), password);
      Toast.show({ type: "success", text1: "Welcome back!" });
      router.replace("/tabs/home");
    } catch (err: any) {
      Toast.show({ type: "error", text1: err.message ?? "Login failed" });
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : "height"}
      >
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {/* Logo */}
          <View style={styles.logoContainer}>
            <View style={styles.logo}>
              <Ionicons name="storefront" size={40} color="#fff" />
            </View>
            <Text style={styles.logoText}>Helvino Shop</Text>
            <Text style={styles.logoSubtext}>Your everyday marketplace</Text>
          </View>

          {/* Form */}
          <View style={styles.form}>
            <Text style={styles.title}>Welcome Back</Text>
            <Text style={styles.subtitle}>Sign in to continue shopping</Text>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Email Address</Text>
              <View style={styles.inputWrapper}>
                <Ionicons name="mail-outline" size={18} color="#9ca3af" style={styles.inputIcon} />
                <TextInput
                  value={email}
                  onChangeText={setEmail}
                  placeholder="you@example.com"
                  placeholderTextColor="#9ca3af"
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoComplete="email"
                  style={styles.input}
                />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Password</Text>
              <View style={styles.inputWrapper}>
                <Ionicons name="lock-closed-outline" size={18} color="#9ca3af" style={styles.inputIcon} />
                <TextInput
                  value={password}
                  onChangeText={setPassword}
                  placeholder="••••••••"
                  placeholderTextColor="#9ca3af"
                  secureTextEntry={!showPassword}
                  autoComplete="password"
                  style={[styles.input, { flex: 1 }]}
                />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={styles.eyeBtn}>
                  <Ionicons name={showPassword ? "eye-off-outline" : "eye-outline"} size={18} color="#9ca3af" />
                </TouchableOpacity>
              </View>
            </View>

            <TouchableOpacity style={styles.forgotBtn}>
              <Text style={styles.forgotText}>Forgot password?</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.loginBtn, isLoading && { opacity: 0.7 }]}
              onPress={handleLogin}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.loginBtnText}>Sign In</Text>
              )}
            </TouchableOpacity>

            {/* Divider */}
            <View style={styles.divider}>
              <View style={styles.dividerLine} />
              <Text style={styles.dividerText}>OR</Text>
              <View style={styles.dividerLine} />
            </View>

            {/* M-Pesa Login hint */}
            <View style={styles.mpesaHint}>
              <Ionicons name="phone-portrait-outline" size={16} color="#059669" />
              <Text style={styles.mpesaHintText}>
                M-Pesa users: sign in with your email, then pay via STK Push at checkout
              </Text>
            </View>

            <TouchableOpacity
              style={styles.registerBtn}
              onPress={() => router.push("/auth/register")}
            >
              <Text style={styles.registerBtnText}>
                Don't have an account?{" "}
                <Text style={styles.registerBtnLink}>Create one</Text>
              </Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#f9fafb" },
  scroll: { flexGrow: 1, paddingHorizontal: 24, paddingBottom: 40 },
  logoContainer: { alignItems: "center", paddingTop: 40, paddingBottom: 32 },
  logo: {
    width: 80, height: 80, backgroundColor: "#059669",
    borderRadius: 24, alignItems: "center", justifyContent: "center",
    marginBottom: 12,
  },
  logoText: { fontSize: 24, fontWeight: "800", color: "#111827" },
  logoSubtext: { fontSize: 13, color: "#6b7280", marginTop: 4 },
  form: {
    backgroundColor: "#fff",
    borderRadius: 20,
    padding: 24,
    shadowColor: "#000",
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 2,
  },
  title: { fontSize: 22, fontWeight: "700", color: "#111827", marginBottom: 4 },
  subtitle: { fontSize: 14, color: "#6b7280", marginBottom: 24 },
  inputGroup: { marginBottom: 16 },
  label: { fontSize: 13, fontWeight: "600", color: "#374151", marginBottom: 6 },
  inputWrapper: {
    flexDirection: "row",
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#e5e7eb",
    borderRadius: 12,
    backgroundColor: "#f9fafb",
    paddingHorizontal: 12,
  },
  inputIcon: { marginRight: 8 },
  input: { flex: 1, paddingVertical: 12, fontSize: 14, color: "#111827" },
  eyeBtn: { padding: 4 },
  forgotBtn: { alignSelf: "flex-end", marginBottom: 20 },
  forgotText: { fontSize: 13, color: "#059669", fontWeight: "600" },
  loginBtn: {
    backgroundColor: "#059669",
    borderRadius: 14,
    paddingVertical: 15,
    alignItems: "center",
    marginBottom: 20,
  },
  loginBtnText: { color: "#fff", fontWeight: "700", fontSize: 16 },
  divider: { flexDirection: "row", alignItems: "center", marginBottom: 16 },
  dividerLine: { flex: 1, height: 1, backgroundColor: "#e5e7eb" },
  dividerText: { fontSize: 12, color: "#9ca3af", marginHorizontal: 12 },
  mpesaHint: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 8,
    backgroundColor: "#f0fdf4",
    borderRadius: 10,
    padding: 12,
    marginBottom: 20,
  },
  mpesaHintText: { fontSize: 12, color: "#059669", flex: 1, lineHeight: 18 },
  registerBtn: { alignItems: "center" },
  registerBtnText: { fontSize: 14, color: "#6b7280" },
  registerBtnLink: { color: "#059669", fontWeight: "700" },
});
