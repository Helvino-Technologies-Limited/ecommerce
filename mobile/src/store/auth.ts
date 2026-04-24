import { create } from "zustand";
import * as SecureStore from "expo-secure-store";
import { User } from "../types";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => Promise<void>;
  loadFromStorage: () => Promise<void>;
  setUser: (user: User) => void;
}

interface RegisterData {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phone?: string;
}

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? "https://ecommerce-ok90.onrender.com/api/v1";

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: false,

  loadFromStorage: async () => {
    try {
      const token = await SecureStore.getItemAsync("accessToken");
      const userJson = await SecureStore.getItemAsync("user");
      if (token && userJson) {
        const user = JSON.parse(userJson) as User;
        set({ token, user, isAuthenticated: true });
      }
    } catch {}
  },

  login: async (email: string, password: string) => {
    set({ isLoading: true });
    try {
      const res = await fetch(`${API_URL}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message ?? "Login failed");
      }
      const data = await res.json();
      const { accessToken, refreshToken, user } = data;
      await SecureStore.setItemAsync("accessToken", accessToken);
      await SecureStore.setItemAsync("refreshToken", refreshToken ?? "");
      await SecureStore.setItemAsync("user", JSON.stringify(user));
      set({ token: accessToken, user, isAuthenticated: true });
    } finally {
      set({ isLoading: false });
    }
  },

  register: async (data: RegisterData) => {
    set({ isLoading: true });
    try {
      const res = await fetch(`${API_URL}/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message ?? "Registration failed");
      }
      const result = await res.json();
      const { accessToken, refreshToken, user } = result;
      await SecureStore.setItemAsync("accessToken", accessToken);
      await SecureStore.setItemAsync("refreshToken", refreshToken ?? "");
      await SecureStore.setItemAsync("user", JSON.stringify(user));
      set({ token: accessToken, user, isAuthenticated: true });
    } finally {
      set({ isLoading: false });
    }
  },

  logout: async () => {
    await SecureStore.deleteItemAsync("accessToken");
    await SecureStore.deleteItemAsync("refreshToken");
    await SecureStore.deleteItemAsync("user");
    set({ token: null, user: null, isAuthenticated: false });
  },

  setUser: (user: User) => set({ user }),
}));
