import { useState } from "react";
import clsx from "clsx";

const PAYMENT_FILTERS = ["Tümü", "Tamamlandı", "Başarısız", "İade"];

const payments = [
  { id: "PAY-9012-XT", invoice: "INV-4451", customer: "Ahmet Yılmaz", amount: "₺ 1.250,00", method: "Kredi Kartı", date: "01.07.2026", status: "TAMAMLANDI", statusClass: "bg-success-bg text-success" },
  { id: "PAY-8812-KL", invoice: "INV-4399", customer: "Mehmet Demir", amount: "₺ 890,00", method: "Banka Havalesi", date: "28.06.2026", status: "BAŞARISIZ", statusClass: "bg-danger-bg text-danger" },
  { id: "PAY-7723-MN", invoice: "INV-4388", customer: "Ayşe Kaya", amount: "₺ 2.100,00", method: "Kredi Kartı", date: "25.06.2026", status: "TAMAMLANDI", statusClass: "bg-success-bg text-success" },
  { id: "PAY-6614-OP", invoice: "INV-4377", customer: "Can Yıldız", amount: "₺ 540,00", method: "Otomatik Ödeme", date: "20.06.2026", status: "İADE", statusClass: "bg-warning-bg text-warning" },
];

export default function Payments() {
  const [activeFilter, setActiveFilter] = useState("Tümü");

  const filteredPayments = activeFilter === "Tümü"
    ? payments
    : payments.filter(p => p.status === activeFilter.toUpperCase());

  return (
    <div className="flex flex-col gap-stack-lg max-w-7xl mx-auto w-full">
      {/* Header */}
      <div>
        <h1 className="font-h1 text-on-surface">Ödemeler</h1>
      </div>

      {/* Filter Bar */}
      <div className="bg-surface border border-outline-variant rounded p-3 flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between">
        <div className="flex gap-2 w-full sm:w-auto overflow-x-auto pb-2 sm:pb-0">
          {PAYMENT_FILTERS.map((f) => (
            <button
              key={f}
              onClick={() => setActiveFilter(f)}
              className={clsx(
                "px-3 py-1.5 rounded-md font-label-md whitespace-nowrap transition-colors",
                activeFilter === f
                  ? "bg-primary-container text-on-primary-fixed"
                  : "text-on-surface-variant hover:bg-surface-variant"
              )}
            >
              {f}
            </button>
          ))}
        </div>
        <div className="flex gap-3 w-full sm:w-auto">
          <div className="relative flex-1 sm:w-64">
            <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-outline text-[18px]">search</span>
            <input 
              type="text" 
              placeholder="Ödeme No veya Müşteri" 
              className="w-full h-10 pl-9 pr-3 rounded border border-outline-variant bg-surface focus:border-primary focus:ring-0 text-body-sm" 
            />
          </div>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex-1">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-background border-b border-outline-variant">
                <th className="font-label-sm text-on-surface-variant py-3 px-4">Ödeme No</th>
                <th className="font-label-sm text-on-surface-variant py-3 px-4">Fatura No</th>
                <th className="font-label-sm text-on-surface-variant py-3 px-4">Müşteri</th>
                <th className="font-label-sm text-on-surface-variant py-3 px-4">Yöntem</th>
                <th className="font-label-sm text-on-surface-variant py-3 px-4 text-right">Tutar</th>
                <th className="font-label-sm text-on-surface-variant py-3 px-4">Tarih</th>
                <th className="font-label-sm text-on-surface-variant py-3 px-4">Durum</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {filteredPayments.map((p) => (
                <tr key={p.id} className="h-row-height-std hover:bg-surface-container transition-colors cursor-pointer">
                  <td className="px-4 font-mono-id text-on-surface">{p.id}</td>
                  <td className="px-4 font-mono-id text-primary">{p.invoice}</td>
                  <td className="px-4 font-body-sm text-primary font-medium">{p.customer}</td>
                  <td className="px-4 font-body-sm text-on-surface-variant">{p.method}</td>
                  <td className="px-4 text-right font-mono-id tabular-nums">{p.amount}</td>
                  <td className="px-4 font-mono-id text-on-surface-variant">{p.date}</td>
                  <td className="px-4">
                    <span className={clsx("inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold", p.statusClass)}>{p.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
