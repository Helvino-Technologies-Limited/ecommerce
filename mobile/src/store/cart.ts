import { create } from "zustand";

export interface CartItem {
  id: string; name: string; price: number; discountedPrice?: number;
  image: string; currency: string; quantity: number; slug: string;
}

interface CartState {
  items: CartItem[];
  addItem: (item: Omit<CartItem, "quantity">) => void;
  removeItem: (id: string) => void;
  updateQuantity: (id: string, quantity: number) => void;
  clearCart: () => void;
  total: () => number;
  count: () => number;
}

export const useCartStore = create<CartState>()((set, get) => ({
  items: [],
  addItem: (item) => set((state) => {
    const existing = state.items.find((i) => i.id === item.id);
    if (existing) return { items: state.items.map((i) => i.id === item.id ? { ...i, quantity: i.quantity + 1 } : i) };
    return { items: [...state.items, { ...item, quantity: 1 }] };
  }),
  removeItem: (id) => set((state) => ({ items: state.items.filter((i) => i.id !== id) })),
  updateQuantity: (id, qty) => {
    if (qty <= 0) { get().removeItem(id); return; }
    set((state) => ({ items: state.items.map((i) => i.id === id ? { ...i, quantity: qty } : i) }));
  },
  clearCart: () => set({ items: [] }),
  total: () => get().items.reduce((s, i) => s + (i.discountedPrice ?? i.price) * i.quantity, 0),
  count: () => get().items.reduce((s, i) => s + i.quantity, 0),
}));
