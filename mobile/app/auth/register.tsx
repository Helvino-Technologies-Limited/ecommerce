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

export default function RegisterScreen() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const { register, isLoading } = useAuthStore();

  const handleRegister = async () => {
    if (!firstName || !lastName || !email || !password) {
      Toast.show({ type: "error", text1: "Please fill all required fields" });
      return;
    }
    if (password !== confirmPassword) {
      Toast.show({ type: "error", text1: "Passwords do not match" });
      return;
    }
    if (password.length < 6) {
      Toast.show({ type: "error", text1: "Password must be at least 6 characters" });
      return;
    }
    try {
      await register({ firstName, lastName, email: email.trim().toLowerCase(), password, phone });
      Toast.show({ type: "success", text1: "Account created!", text2: "Welcome to Helvino Shop" });
      router.replace("/tabs/home");
    } catch (err: any) {
      Toast.show({ type: "error", text1: err.message ?? "Registration failed" });
    }
  };

  const inputCls = (focused: boolean = false) => ({
    flexDirection: "row" as const,
    alignItems: "center" as const,
    borderWidth: 1,
    borderColor: "#e5e7eb",
    borderRadius: 12,
    backgroundColor: "#f9fafb",
    paddingHorizontal: 12,
  });

  return (
    <SafeAreaView style={styles.container}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === "ios" ? "padding" : "height"}>
        <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
          {/* Header */}
          <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
            <Ionicons name="arrow-back" size={22} color="#111827" />
          </TouchableOpacity>

          <View style={styles.header}>
            <View style={styles.logo}>
              <Ionicons name="person-add" size={32} color="#fff" />
            </View>
            <Text style={styles.title}>Create Account</Text>
            <Text style={styles.subtitle}>Join Helvino Shop today</Text>
          </View>

          <View style={styles.form}>
            {/* Names */}
            <View style={styles.row}>
              <View style={[styles.inputGroup, { flex: 1, marginRight: 8 }]}>
                <Text style={styles.label}>First Name *</Text>
                <View style={inputCls()}>
                  <TextInput value={firstName} onChangeText={setFirstName} placeholder="John"
                    placeholderTextColor="#9ca3af" style={styles.input} autoCapitalize="words" />
                </View>
              </View>
              <View style={[styles.inputGroup, { flex: 1 }]}>
                <Text style={styles.label}>Last Name *</Text>
                <View style={inputCls()}>
                  <TextInput value={lastName} onChangeText={setLastName} placeholder="Doe"
                    placeholderTextColor="#9ca3af" style={styles.input} autoCapitalize="words" />
                </View>
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Email Address *</Text>
              <View style={inputCls()}>
                <Ionicons name="mail-outline" size={18} color="#9ca3af" style={{ marginRight: 8 }} />
                <TextInput value={email} onChangeText={setEmail} placeholder="you@example.com"
                  placeholderTextColor="#9ca3af" keyboardType="email-address" autoCapitalize="none"
                  style={[styles.input, { flex: 1 }]} />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Phone Number (optional)</Text>
              <View style={inputCls()}>
                <Ionicons name="call-outline" size={18} color="#9ca3af" style={{ marginRight: 8 }} />
                <TextInput value={phone} onChangeText={setPhone} placeholder="0712345678"
                  placeholderTextColor="#9ca3af" keyboardType="phone-pad"
                  style={[styles.input, { flex: 1 }]} />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Password *</Text>
              <View style={inputCls()}>
                <Ionicons name="lock-closed-outline" size={18} color="#9ca3af" style={{ marginRight: 8 }} />
                <TextInput value={password} onChangeText={setPassword} placeholder="At least 6 characters"
                  placeholderTextColor="#9ca3af" secureTextEntry={!showPassword}
                  style={[styles.input, { flex: 1 }]} />
                <TouchableOpacity onPress={() => setShowPassword(!showPassword)} style={{ padding: 4 }}>
                  <Ionicons name={showPassword ? "eye-off-outline" : "eye-outline"} size={18} color="#9ca3af" />
                </TouchableOpacity>
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Confirm Password *</Text>
              <View style={inputCls()}>
                <Ionicons name="lock-closed-outline" size={18} color="#9ca3af" style={{ marginRight: 8 }} />
                <TextInput value={confirmPassword} onChangeText={setConfirmPassword} placeholder="Repeat your password"
                  placeholderTextColor="#9ca3af" secureTextEntry={!showPassword}
                  style={[styles.input, { flex: 1 }]} />
              </View>
            </View>

            <Text style={styles.terms}>
              By creating an account, you agree to our{" "}
              <Text style={styles.termsLink}>Terms of Service</Text> and{" "}
              <Text style={styles.termsLink}>Privacy Policy</Text>.
            </Text>

            <TouchableOpacity
              style={[styles.registerBtn, isLoading && { opacity: 0.7 }]}
              onPress={handleRegister}
              disabled={isLoading}
            >
              {isLoading ? (
                <ActivityIndicator color="#fff" size="small" />
              ) : (
                <Text style={styles.registerBtnText}>Create Account</Text>
              )}
            </TouchableOpacity>

            <TouchableOpacity onPress={() => router.push("/auth/login")} style={styles.loginLink}>
              <Text style={styles.loginLinkText}>
                Already have an account? <Text style={styles.loginLinkBold}>Sign In</Text>
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
  backBtn: { marginTop: 8, marginBottom: 8, alignSelf: "flex-start", padding: 4 },
  header: { alignItems: "center", paddingBottom: 24 },
  logo: {
    width: 64, height: 64, backgroundColor: "#059669",
    borderRadius: 20, alignItems: "center", justifyContent: "center", marginBottom: 12,
  },
  title: { fontSize: 22, fontWeight: "700", color: "#111827" },
  subtitle: { fontSize: 14, color: "#6b7280", marginTop: 4 },
  form: {
    backgroundColor: "#fff",
    borderRadius: 20,
    padding: 24,
    shadowColor: "#000",
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 2,
  },
  row: { flexDirection: "row" },
  inputGroup: { marginBottom: 16 },
  label: { fontSize: 13, fontWeight: "600", color: "#374151", marginBottom: 6 },
  input: { paddingVertical: 12, fontSize: 14, color: "#111827", flex: 1 },
  terms: { fontSize: 12, color: "#6b7280", lineHeight: 18, marginBottom: 20, textAlign: "center" },
  termsLink: { color: "#059669", fontWeight: "600" },
  registerBtn: {
    backgroundColor: "#059669",
    borderRadius: 14,
    paddingVertical: 15,
    alignItems: "center",
    marginBottom: 16,
  },
  registerBtnText: { color: "#fff", fontWeight: "700", fontSize: 16 },
  loginLink: { alignItems: "center" },
  loginLinkText: { fontSize: 14, color: "#6b7280" },
  loginLinkBold: { color: "#059669", fontWeight: "700" },
});
