import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listOrders, OrderResponse, Page, cancelOrder } from "../api/orderApi";

const STATUS_CHIPS = [
  { key: "all", label: "Tümü" },
  { key: "PENDING_PAYMENT", label: "Ödeme Bekliyor" },
  { key: "PAID", label: "Ödendi" },
  { key: "FULFILLED", label: "Tamamlandı" },
  { key: "CANCELLED", label: "İptal" },
];

export default function OrderList() {
  const [data, setData] = useState<Page<OrderResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusChip, setStatusChip] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");

  const fetchOrders = async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await listOrders(currentPage);
      setData(result);
    } catch (err: any) {
      setError(err.response?.status === 401
        ? "Oturum süreniz dolmuş. Lütfen giriş yapın."
        : "Sipariş listesi çekilirken bir hata oluştu.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, [currentPage]);

  const filteredData = data?.content?.filter((o) => {
    if (statusChip !== "all" && o.status !== statusChip) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
                      return o.id.toLowerCase().includes(q) || (o.customerNo || '').toLowerCase().includes(q) || o.customerId.toLowerCase().includes(q);
    }
    return true;
  }) || [];

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING_PAYMENT': return 'bg-warning-bg text-warning border border-warning/20';
      case 'PAID': return 'bg-info-bg text-info border border-info/20';
      case 'FULFILLED': return 'bg-success-bg text-success border border-success/20';
      case 'CANCELLED': return 'bg-danger-bg text-danger border border-danger/20';
      default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'PENDING_PAYMENT': return 'Ödeme Bekliyor';
      case 'PAID': return 'Ödendi';
      case 'FULFILLED': return 'Tamamlandı';
      case 'CANCELLED': return 'İptal';
      default: return status;
    }
  };

  const handleCancel = async (orderId: string) => {
    const reason = prompt("İptal nedenini girin:");
    if (!reason) return;
    try {
      await cancelOrder(orderId, reason);
      fetchOrders();
    } catch {
      alert("Sipariş iptal edilemedi.");
    }
  };

  return (
    <div className="max-w-[1440px] mx-auto flex flex-col gap-stack-lg">
      <div className="flex items-center justify-between">
        <h2 className="font-h1 text-on-background">Siparişler</h2>
        <Link to="/sales/new" className="bg-primary text-on-primary font-label-md px-gutter py-2 rounded flex items-center gap-2 hover:bg-primary/90 transition-colors">
          <span className="material-symbols-outlined text-[18px]">add</span>
          Yeni Sipariş
        </Link>
      </div>

      {/* Status Chips */}
      <div className="flex items-center gap-2 flex-wrap">
        {STATUS_CHIPS.map((chip) => (
          <button
            key={chip.key}
            onClick={() => { setStatusChip(chip.key); setCurrentPage(0); }}
            className={`px-3 py-1.5 rounded-full font-label-sm transition-colors ${
              statusChip === chip.key
                ? "bg-primary text-on-primary shadow-sm"
                : "bg-surface-container-low text-on-surface-variant border border-outline-variant hover:bg-surface-container"
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>

      {/* Search */}
      <div className="bg-surface border border-outline-variant rounded p-stack-lg flex flex-col gap-stack-md shadow-sm">
        <div className="flex flex-col gap-1 max-w-md">
          <label className="font-label-sm text-secondary">Arama</label>
          <div className="relative">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
            <input
              type="text"
              placeholder="Sipariş No veya Müşteri No"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full h-10 pl-10 pr-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm focus:border-primary focus:outline-none transition-colors"
            />
          </div>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex flex-col shadow-sm">
        <div className="w-full overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[900px]">
            <thead>
              <tr className="bg-background border-b border-outline-variant h-10">
                <th className="px-gutter font-label-sm text-secondary">Sipariş No</th>
                <th className="px-gutter font-label-sm text-secondary">Müşteri</th>
                <th className="px-gutter font-label-sm text-secondary">Durum</th>
                <th className="px-gutter font-label-sm text-secondary">Kalemler</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Toplam</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Tarih</th>
                <th className="px-gutter font-label-sm text-secondary text-right">İşlem</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {loading ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-secondary font-body-md">
                    <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span>
                    Yükleniyor...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-danger font-body-md bg-danger-bg/50">
                    <div className="flex items-center justify-center gap-2">
                      <span className="material-symbols-outlined">error</span>
                      {error}
                    </div>
                  </td>
                </tr>
              ) : filteredData.length > 0 ? (
                filteredData.map((order) => (
                  <tr key={order.id} className="h-row-height-std hover:bg-surface-container-low transition-colors">
                    <td className="px-gutter py-2">
                      <Link to={`/sales/saga/${order.id}`} className="font-mono-id text-primary hover:underline">
                        {order.id.substring(0, 8).toUpperCase()}
                      </Link>
                    </td>
                    <td className="px-gutter font-mono-id text-secondary">
                      {order.customerNo || order.customerId.substring(0, 8).toUpperCase()}
                    </td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getStatusColor(order.status)}`}>
                        {getStatusLabel(order.status)}
                      </span>
                    </td>
                    <td className="px-gutter font-body-sm text-secondary">
                      {order.items?.length || 0} kalem
                    </td>
                    <td className="px-gutter font-mono-id text-right tabular-nums text-on-surface font-medium">
                      {order.totalAmount?.toFixed(2)} {order.currency}
                    </td>
                    <td className="px-gutter font-mono-id text-right tabular-nums text-secondary">
                      {new Date(order.createdAt).toLocaleDateString('tr-TR')}
                    </td>
                    <td className="px-gutter text-right">
                      {order.status === 'PENDING_PAYMENT' && (
                        <button
                          onClick={() => handleCancel(order.id)}
                          className="text-danger hover:bg-danger-bg px-2 py-1 rounded font-label-sm transition-colors"
                        >
                          İptal Et
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-secondary font-body-md">
                    Henüz hiç sipariş bulunmuyor.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {data && (
          <div className="h-12 border-t border-outline-variant bg-surface flex items-center justify-between px-gutter">
            <span className="font-body-sm text-secondary">Toplam {data.totalElements} kayıt</span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                disabled={data.number === 0}
                className="p-1 rounded text-secondary hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <span className="font-mono-label text-on-surface px-2">
                {data.number + 1} / {data.totalPages === 0 ? 1 : data.totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(p => Math.min(data.totalPages - 1, p + 1))}
                disabled={data.number >= data.totalPages - 1}
                className="p-1 rounded text-secondary hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
