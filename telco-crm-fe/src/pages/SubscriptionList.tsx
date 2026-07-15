import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSubscriptions, SubscriptionResponse, SubscriptionStatus, Page } from "../api/subscriptionApi";
import { formatDateTime } from "../utils/dateUtils";

const STATUS_CHIPS: { key: string; label: string }[] = [
  { key: "all", label: "Tümü" },
  { key: "ACTIVE", label: "Aktif" },
  { key: "PENDING", label: "Beklemede" },
  { key: "SUSPENDED", label: "Askıda" },
  { key: "TERMINATED", label: "Sonlandırılmış" },
];

const getStatusColor = (status: SubscriptionStatus) => {
  switch (status) {
    case 'ACTIVE': return 'bg-success-bg text-success border border-success/20';
    case 'PENDING': return 'bg-warning-bg text-warning border border-warning/20';
    case 'SUSPENDED': return 'bg-info-bg text-info border border-info/20';
    case 'TERMINATED': return 'bg-danger-bg text-danger border border-danger/20';
    default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
  }
};

const getStatusLabel = (status: SubscriptionStatus) => {
  switch (status) {
    case 'ACTIVE': return 'Aktif';
    case 'PENDING': return 'Beklemede';
    case 'SUSPENDED': return 'Askıda';
    case 'TERMINATED': return 'Sonlandırılmış';
    default: return status;
  }
};

export default function SubscriptionList() {
  const [data, setData] = useState<Page<SubscriptionResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");

  const fetchSubscriptions = async (page: number) => {
    try {
      setLoading(true);
      const result = await getSubscriptions(page);
      setData(result);
    } catch (err: any) {
      setError(err.response?.status === 401
        ? "Oturum süreniz dolmuş veya yetkisiz erişim. Lütfen giriş yapın."
        : "Abonelik listesi çekilirken bir hata oluştu.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscriptions(currentPage);
  }, [currentPage]);

  const filteredData = data?.content?.filter((sub) => {
    if (statusFilter !== "all" && sub.status !== statusFilter) return false;
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const match = sub.msisdn?.includes(q) || sub.customerNo?.toLowerCase().includes(q) || sub.tariffCode?.toLowerCase().includes(q);
      if (!match) return false;
    }
    return true;
  }) || [];

  return (
    <div className="max-w-[1440px] mx-auto flex flex-col gap-stack-lg">
      <div className="flex items-center justify-between">
        <h2 className="font-h1 text-on-background">Abonelikler</h2>
      </div>

      {/* Status Chips */}
      <div className="flex items-center gap-2 flex-wrap">
        {STATUS_CHIPS.map((chip) => (
          <button
            key={chip.key}
            onClick={() => { setStatusFilter(chip.key); setCurrentPage(0); }}
            className={`px-3 py-1.5 rounded-full font-label-sm transition-colors ${
              statusFilter === chip.key
                ? "bg-primary text-on-primary shadow-sm"
                : "bg-surface-container-low text-on-surface-variant border border-outline-variant hover:bg-surface-container"
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="bg-surface border border-outline-variant rounded p-stack-lg flex flex-col gap-stack-md shadow-sm">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
          <div className="flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Arama</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
              <input
                type="text"
                placeholder="MSISDN, Müşteri No veya Tarife"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-10 pl-10 pr-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm focus:border-primary focus:outline-none transition-colors"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex flex-col shadow-sm">
        <div className="w-full overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead>
              <tr className="bg-background border-b border-outline-variant h-10">
                <th className="px-gutter font-label-sm text-secondary w-1/4">Abonelik</th>
                <th className="px-gutter font-label-sm text-secondary">Müşteri No</th>
                <th className="px-gutter font-label-sm text-secondary">MSISDN</th>
                <th className="px-gutter font-label-sm text-secondary">Tarife</th>
                <th className="px-gutter font-label-sm text-secondary">Durum</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Oluşturma Tarihi</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {loading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-secondary font-body-md">
                    <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span>
                    Yükleniyor...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-danger font-body-md bg-danger-bg/50">
                    <div className="flex items-center justify-center gap-2">
                      <span className="material-symbols-outlined">error</span>
                      {error}
                    </div>
                  </td>
                </tr>
              ) : filteredData.length > 0 ? (
                filteredData.map((sub) => (
                  <tr key={sub.id} className="h-row-height-std hover:bg-surface-container-low transition-colors group">
                    <td className="px-gutter py-2">
                      <Link to={`/subscriptions/${sub.id}`} className="flex flex-col group-hover:cursor-pointer">
                        <span className="font-body-sm text-on-surface font-medium group-hover:text-primary transition-colors">
                          {sub.id.substring(0, 8).toUpperCase()}
                        </span>
                      </Link>
                    </td>
                    <td className="px-gutter font-mono-id text-secondary">{sub.customerNo || "-"}</td>
                    <td className="px-gutter font-mono-id">{sub.msisdn || "-"}</td>
                    <td className="px-gutter font-body-sm">{sub.tariffCode || "-"}</td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getStatusColor(sub.status)}`}>
                        {getStatusLabel(sub.status)}
                      </span>
                    </td>
                    <td className="px-gutter font-mono-id text-right tabular-nums text-secondary">
                      {formatDateTime(sub.createdAt)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-secondary font-body-md">
                    Henüz hiç abonelik bulunmuyor.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {data && (
          <div className="h-12 border-t border-outline-variant bg-surface flex items-center justify-between px-gutter">
            <span className="font-body-sm text-secondary">Toplam {data.totalElements} kayıt bulundu</span>
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
