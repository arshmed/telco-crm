import { useState, useEffect } from "react";
import clsx from "clsx";
import { getPayments, PaymentResponse, Page } from "../api/paymentApi";
import { getCustomerById, CustomerResponse } from "../api/customerApi";
import { formatDateTime } from "../utils/dateUtils";

const PAYMENT_FILTERS = [
  { key: "ALL", label: "Tümü" },
  { key: "COMPLETED", label: "Tamamlandı" },
  { key: "FAILED", label: "Başarısız" },
  { key: "REFUNDED", label: "İade" },
];

const STATUS_CLASSES: Record<string, string> = {
  COMPLETED: "bg-success-bg text-success",
  FAILED: "bg-danger-bg text-danger",
  REFUNDED: "bg-warning-bg text-warning",
};

const STATUS_LABELS: Record<string, string> = {
  COMPLETED: "Tamamlandı",
  FAILED: "Başarısız",
  REFUNDED: "İade",
  PENDING: "Beklemede",
};

const METHOD_LABELS: Record<string, string> = {
  CREDIT_CARD: "Kredi Kartı",
  BANK_TRANSFER: "Havale/EFT",
  WALLET: "Cüzdan",
};

export default function Payments() {
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [payments, setPayments] = useState<Page<PaymentResponse> | null>(null);
  const [customers, setCustomers] = useState<Map<string, CustomerResponse>>(new Map());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchPayments();
  }, []);

  const fetchPayments = async () => {
    try {
      setLoading(true);
      const data = await getPayments(0, 50);
      setPayments(data);

      const uniqueIds = [...new Set(data.content.map((p) => p.customerId).filter(Boolean))];
      const results = await Promise.allSettled(uniqueIds.map((id) => getCustomerById(id)));
      const map = new Map<string, CustomerResponse>();
      results.forEach((r, i) => {
        if (r.status === "fulfilled") map.set(uniqueIds[i], r.value);
      });
      setCustomers(map);
    } catch (err) {
      console.error("Failed to fetch payments", err);
    } finally {
      setLoading(false);
    }
  };

  const getFilteredPayments = () => {
    if (!payments?.content) return [];
    if (activeFilter === "ALL") return payments.content;
    return payments.content.filter((p) => p.status === activeFilter);
  };

  return (
    <div className="flex flex-col gap-stack-lg max-w-7xl mx-auto w-full">
      {/* Finance Tabs */}
      <div className="border-b border-outline-variant mb-2">
        <nav className="flex gap-8">
          <a href="/finance/billing"
            className="py-3 font-label-md border-b-2 border-transparent text-secondary hover:text-on-surface transition-colors">
            Faturalar
          </a>
          <a href="/finance/payments"
            className="py-3 font-label-md border-b-2 border-primary text-primary transition-colors">
            Ödemeler
          </a>
        </nav>
      </div>

      {/* Header */}
      <div>
        <h1 className="font-h1 text-on-surface">Ödemeler</h1>
      </div>

      {/* Filter Bar */}
      <div className="bg-surface border border-outline-variant rounded p-3 flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex gap-2 w-full sm:w-auto overflow-x-auto pb-2 sm:pb-0">
          {PAYMENT_FILTERS.map((f) => (
            <button
              key={f.key}
              onClick={() => setActiveFilter(f.key)}
              className={clsx(
                "px-3 py-1.5 rounded-md font-label-md whitespace-nowrap transition-colors",
                activeFilter === f.key
                  ? "bg-primary-container text-on-primary-fixed"
                  : "text-on-surface-variant hover:bg-surface-variant"
              )}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex-1">
        {loading ? (
          <div className="p-8 text-center text-secondary">Yükleniyor...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-background border-b border-outline-variant">
                  <th className="font-label-sm text-on-surface-variant py-3 px-4">Müşteri</th>
                  <th className="font-label-sm text-on-surface-variant py-3 px-4 text-right">Tutar</th>
                  <th className="font-label-sm text-on-surface-variant py-3 px-4">Yöntem</th>
                  <th className="font-label-sm text-on-surface-variant py-3 px-4">Referans</th>
                  <th className="font-label-sm text-on-surface-variant py-3 px-4">Tarih</th>
                  <th className="font-label-sm text-on-surface-variant py-3 px-4">Durum</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {getFilteredPayments().length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center py-6 text-on-surface-variant">Ödeme bulunamadı</td>
                  </tr>
                ) : (
                  getFilteredPayments().map((p) => {
                    const customer = customers.get(p.customerId);
                    const customerLabel = customer
                      ? `${customer.firstName} ${customer.lastName}`
                      : p.customerId?.substring(0, 8) + "...";
                    return (
                      <tr key={p.id} className="h-row-height-std hover:bg-surface-container transition-colors cursor-pointer" onClick={() => window.location.href = `/finance/payments/${p.id}`}>
                        <td className="px-4 font-body-sm text-on-surface">{customerLabel}</td>
                        <td className="px-4 text-right font-mono-id tabular-nums">{p.amount?.toFixed(2)} {p.currency}</td>
                        <td className="px-4 font-body-sm text-on-surface-variant">{METHOD_LABELS[p.method] || p.method}</td>
                        <td className="px-4 font-mono-id text-secondary text-[13px]">{p.externalRef || "-"}</td>
                        <td className="px-4 font-body-sm text-on-surface-variant">{formatDateTime(p.createdAt)}</td>
                        <td className="px-4">
                          <span className={clsx("inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium", STATUS_CLASSES[p.status] || "bg-outline-variant text-surface")}>
                            {STATUS_LABELS[p.status] || p.status}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
