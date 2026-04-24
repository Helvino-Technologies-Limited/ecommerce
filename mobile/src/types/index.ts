export interface Product {
  id: string;
  name: string;
  slug: string;
  description: string;
  shortDescription?: string;
  price: number;
  compareAtPrice?: number;
  currency: string;
  stockQuantity: number;
  images: string[];
  category: Category;
  sku?: string;
  featured: boolean;
  flashSale: boolean;
  flashSaleDiscount?: number;
  flashSaleEndsAt?: string;
  averageRating: number;
  reviewCount: number;
  salesCount: number;
  tags: string[];
  active: boolean;
  createdAt: string;
  variants?: ProductVariant[];
}

export interface ProductVariant {
  id: string;
  variantType: string;
  variantValue: string;
  stockQuantity: number;
  priceAdjustment: number;
  available: boolean;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  iconUrl?: string;
  children?: Category[];
  parent?: Category;
}

export interface Review {
  id: string;
  rating: number;
  comment?: string;
  verified: boolean;
  createdAt: string;
  user?: { firstName: string; lastName: string };
}

export interface Order {
  id: string;
  orderNumber: string;
  status: OrderStatus;
  paymentMethod?: string;
  paymentStatus: string;
  currency: string;
  subtotal: number;
  deliveryFee: number;
  discountAmount: number;
  totalAmount: number;
  items: OrderItem[];
  deliveryAddress?: Address;
  estimatedDelivery?: string;
  deliveredAt?: string;
  createdAt: string;
  trackingHistory?: TrackingEvent[];
}

export interface OrderItem {
  id: string;
  product?: Product;
  productName: string;
  productImage: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export type OrderStatus =
  | "PENDING" | "CONFIRMED" | "PROCESSING"
  | "READY_FOR_PICKUP" | "OUT_FOR_DELIVERY"
  | "DELIVERED" | "CANCELLED" | "REFUNDED";

export interface Address {
  id?: string;
  label?: string;
  streetAddress: string;
  apartment?: string;
  city: string;
  county?: string;
  town?: string;
  country?: string;
  postalCode?: string;
  latitude?: number;
  longitude?: number;
  isDefault?: boolean;
}

export interface TrackingEvent {
  id: string;
  status: OrderStatus;
  message: string;
  riderLatitude?: number;
  riderLongitude?: number;
  createdAt: string;
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: "CUSTOMER" | "ADMIN" | "SUPER_ADMIN" | "RIDER";
  walletBalance: number;
  loyaltyPoints: number;
  emailVerified: boolean;
  phoneVerified: boolean;
  enabled: boolean;
  createdAt: string;
}

export interface ApiPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface DeliveryZone {
  id: string;
  county: string;
  town: string;
  deliveryCost: number;
  estimatedDays: number;
  active: boolean;
}
