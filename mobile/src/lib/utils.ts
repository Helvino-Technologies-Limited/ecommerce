export function formatPrice(amount: number, currency: string = "KES"): string {
  const symbols: Record<string, string> = {
    KES: "KSh", USD: "$", EUR: "€", GBP: "£",
    UGX: "UGX", TZS: "TSh", NGN: "₦", GHS: "GH₵", ZAR: "R",
  };
  const symbol = symbols[currency] ?? currency;
  return `${symbol} ${amount.toLocaleString("en-KE", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

export function timeUntil(date: string): string {
  const diff = new Date(date).getTime() - Date.now();
  if (diff <= 0) return "Ended";
  const h = Math.floor(diff / 3600000);
  const m = Math.floor((diff % 3600000) / 60000);
  const s = Math.floor((diff % 60000) / 1000);
  if (h > 24) return `${Math.floor(h / 24)}d ${h % 24}h`;
  return `${h}h ${m}m ${s}s`;
}
