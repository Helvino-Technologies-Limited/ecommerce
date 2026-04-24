import axios from "axios";
import * as SecureStore from "expo-secure-store";

export const api = axios.create({
  baseURL: process.env.EXPO_PUBLIC_API_URL ?? "https://api.helvino.org/api/v1",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync("accessToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    if (err.response?.status === 401) {
      try {
        const refresh = await SecureStore.getItemAsync("refreshToken");
        const { data } = await axios.post(
          `${process.env.EXPO_PUBLIC_API_URL ?? "https://api.helvino.org/api/v1"}/auth/refresh`,
          { refreshToken: refresh }
        );
        await SecureStore.setItemAsync("accessToken", data.accessToken);
        err.config.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(err.config);
      } catch {
        await SecureStore.deleteItemAsync("accessToken");
        await SecureStore.deleteItemAsync("refreshToken");
      }
    }
    return Promise.reject(err);
  }
);

export default api;
