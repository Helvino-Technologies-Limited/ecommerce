import api from "./api";

export interface StkPushRequest {
  orderId: string;
  phone: string;
}

export interface StkPushResponse {
  CheckoutRequestID?: string;
  ResponseCode?: string;
  ResponseDescription?: string;
  CustomerMessage?: string;
  message?: string;
}

/**
 * Initiates an M-Pesa STK Push for the given order.
 * The backend handles the Daraja API call and returns the STK response.
 */
export async function initiateSTKPush(request: StkPushRequest): Promise<StkPushResponse> {
  const { data } = await api.post<StkPushResponse>("/payments/mpesa/stk-push", request);
  return data;
}

/**
 * Format a phone number to 254XXXXXXXXX format
 */
export function formatPhoneForMpesa(phone: string): string {
  const cleaned = phone.replace(/\D/g, "");
  if (cleaned.startsWith("254")) return cleaned;
  if (cleaned.startsWith("0")) return `254${cleaned.slice(1)}`;
  if (cleaned.startsWith("7") || cleaned.startsWith("1")) return `254${cleaned}`;
  return cleaned;
}
