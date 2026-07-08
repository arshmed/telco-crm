import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { getCustomers, CustomerResponse, Page } from "../api/customerApi";

const CITIES = [
  "Adana", "Ankara", "Antalya", "Bursa", "Denizli", "Diyarbakır",
  "Erzurum", "Eskişehir", "Gaziantep", "İstanbul", "İzmir", "Kayseri",
  "Kocaeli", "Konya", "Malatya", "Mersin", "Muğla", "Samsun", "Şanlıurfa",
  "Sivas", "Trabzon", "Van"
];

const STATUS_CHIPS = [
  { key: "all", label: "Tümü" },
  { key: "ACTIVE", label: "Aktif" },
  { key: "PENDING", label: "KYC bekliyor" },
  { key: "REJECTED", label: "Reddedildi" },
];

export default function Customers() {
  const [data, setData] = useState<Page<CustomerResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusChip, setStatusChip] = useState("all");
  const [cityFilter, setCityFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        setLoading(true);
        const result = await getCustomers(currentPage);
        setData(result);
      } catch (err: any) {
        setError(err.response?.status === 401 
          ? "Oturum süreniz dolmuş veya yetkisiz erişim. Lütfen giriş yapın." 
          : "Müşteri listesi çekilirken bir hata oluştu. Backend servislerinin çalıştığından emin olun.");
      } finally {
        setLoading(false);
      }
    };

    fetchCustomers();
  }, [currentPage]);

  const filteredData = useMemo(() => {
    if (!data?.content) return [];
    return data.content.filter((c) => {
      if (statusChip !== "all" && c.status !== statusChip) return false;
      if (cityFilter !== "all") {
        const customerCity = c.addresses?.find((a) => a.isDefault)?.city || c.addresses?.[0]?.city || "";
        if (customerCity !== cityFilter) return false;
      }
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const name = c.type === "CORPORATE" ? c.companyName || "" : `${c.firstName} ${c.lastName}`;
        const match = name.toLowerCase().includes(q) || c.identityNumber?.includes(q) || c.phone?.includes(q);
        if (!match) return false;
      }
      return true;
    });
  }, [data, statusChip, cityFilter, searchQuery]);

  const getKycStatus = (customer: CustomerResponse) => {
    const hasVerified = customer.documents?.some((d) => d.verifiedAt);
    const hasDocs = customer.documents && customer.documents.length > 0;
    if (hasVerified) return { label: "Onaylı", className: "bg-success-bg text-success border border-success/20" };
    if (hasDocs) return { label: "Bekliyor", className: "bg-warning-bg text-warning border border-warning/20" };
    return { label: "Eksik", className: "bg-surface-container text-on-surface-variant border border-outline-variant" };
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-success-bg text-success border border-success/20';
      case 'PENDING': return 'bg-warning-bg text-warning border border-warning/20';
      case 'REJECTED': 
      case 'CANCELLED': return 'bg-danger-bg text-danger border border-danger/20';
      default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'Aktif';
      case 'PENDING': return 'Beklemede';
      case 'REJECTED': return 'Reddedildi';
      case 'CANCELLED': return 'İptal';
      default: return status;
    }
  };

  return (
    <div className="max-w-[1440px] mx-auto flex flex-col gap-stack-lg">
      <div className="flex items-center justify-between">
        <h2 className="font-h1 text-on-background">Müşteriler</h2>
        <button className="bg-primary text-on-primary font-label-md px-gutter py-2 rounded flex items-center gap-2 hover:bg-primary/90 transition-colors">
          <span className="material-symbols-outlined text-[18px]">add</span>
          Yeni müşteri
        </button>
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

      {/* Filter Bar */}
      <div className="bg-surface border border-outline-variant rounded p-stack-lg flex flex-col gap-stack-md shadow-sm">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-gutter">
          <div className="md:col-span-2 flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Arama</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
              <input
                type="text"
                placeholder="Ad, MSISDN veya TCKN"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-10 pl-10 pr-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm focus:border-primary focus:outline-none transition-colors"
              />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Şehir</label>
            <select
              value={cityFilter}
              onChange={(e) => { setCityFilter(e.target.value); setCurrentPage(0); }}
              className="w-full h-10 px-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm appearance-none focus:border-primary focus:outline-none cursor-pointer"
            >
              <option value="all">Tüm şehirler</option>
              {CITIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Müşteri Tipi</label>
            <select className="w-full h-10 px-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm appearance-none focus:border-primary focus:outline-none cursor-pointer">
              <option value="all">Tümü</option>
              <option value="INDIVIDUAL">Bireysel</option>
              <option value="CORPORATE">Kurumsal</option>
            </select>
          </div>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex flex-col shadow-sm">
        <div className="w-full overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead>
              <tr className="bg-background border-b border-outline-variant h-10">
                <th className="px-gutter font-label-sm text-secondary w-1/4">Müşteri</th>
                <th className="px-gutter font-label-sm text-secondary">Tip</th>
                <th className="px-gutter font-label-sm text-secondary">E-posta</th>
                <th className="px-gutter font-label-sm text-secondary">KYC</th>
                <th className="px-gutter font-label-sm text-secondary">Durum</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Kayıt Tarihi</th>
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
                filteredData.map((customer) => (
                  <tr key={customer.id} className="h-row-height-std hover:bg-surface-container-low transition-colors group">
                    <td className="px-gutter py-2">
                      <Link to={`/customers/${customer.id}`} className="flex flex-col group-hover:cursor-pointer">
                        <span className="font-body-sm text-on-surface font-medium group-hover:text-primary transition-colors">
                          {customer.type === 'CORPORATE' ? customer.companyName : `${customer.firstName} ${customer.lastName}`}
                        </span>
                        <span className="font-mono-id text-secondary mt-0.5">
                          {customer.identityNumber ? customer.identityNumber.substring(0, 3) + '•••••••' : customer.id.substring(0,8)}
                        </span>
                      </Link>
                    </td>
                    <td className="px-gutter font-body-sm">
                      {customer.type === 'CORPORATE' ? 'Kurumsal' : 'Bireysel'}
                    </td>
                    <td className="px-gutter font-body-sm text-secondary">
                      {customer.email}
                    </td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getKycStatus(customer).className}`}>
                        {getKycStatus(customer).label}
                      </span>
                    </td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getStatusColor(customer.status)}`}>
                        {getStatusLabel(customer.status)}
                      </span>
                    </td>
                    <td className="px-gutter font-mono-id text-right tabular-nums text-secondary">
                      {new Date(customer.createdAt).toLocaleDateString('tr-TR')}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-secondary font-body-md">
                    Henüz hiç müşteri bulunmuyor.
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
