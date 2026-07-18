import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { getQuota, getUsageHistory, QuotaResponse, UsageRecordResponse, Page } from "../api/usageApi";
import {
  getSubscriptionById,
  SubscriptionResponse,
  suspendSubscription,
  reactivateSubscription,
  terminateSubscription,
  changeTariff,
  addAddonToSubscription,
  getSubscriptionAddons,
  SubscriptionAddonResponse,
} from "../api/subscriptionApi";
import { getTariffs, getAddons, TariffResponse, AddonResponse } from "../api/catalogApi";

export default function SubscriptionDetail() {
  const { id } = useParams<{ id: string }>();
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [quota, setQuota] = useState<QuotaResponse | null>(null);
  const [usageHistory, setUsageHistory] = useState<UsageRecordResponse[]>([]);
  const [, setLoadingSubscription] = useState(true);
  const [loadingQuota, setLoadingQuota] = useState(true);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [activeTab, setActiveTab] = useState("Genel Bakış");
  const [actionLoading, setActionLoading] = useState(false);

  const [availableTariffs, setAvailableTariffs] = useState<TariffResponse[]>([]);
  const [availableAddons, setAvailableAddons] = useState<AddonResponse[]>([]);
  const [subscriptionAddons, setSubscriptionAddons] = useState<SubscriptionAddonResponse[]>([]);
  const [showTariffPicker, setShowTariffPicker] = useState(false);
  const [showAddonPicker, setShowAddonPicker] = useState(false);
  const [selectedTariffCode, setSelectedTariffCode] = useState("");
  const [selectedAddonCode, setSelectedAddonCode] = useState("");

  useEffect(() => {
    if (!id) return;
    setLoadingSubscription(true);
    getSubscriptionById(id)
      .then(setSubscription)
      .catch(() => {})
      .finally(() => setLoadingSubscription(false));
  }, [id]);

  useEffect(() => {
    if (!id) return;
    setLoadingQuota(true);
    getQuota(id)
      .then(setQuota)
      .catch(() => {})
      .finally(() => setLoadingQuota(false));
  }, [id]);

  useEffect(() => {
    if (activeTab !== "Kullanım Geçmişi" || !id) return;
    setLoadingHistory(true);
    getUsageHistory(id)
      .then((data: Page<UsageRecordResponse>) => setUsageHistory(data.content || []))
      .catch(() => {})
      .finally(() => setLoadingHistory(false));
  }, [activeTab, id]);

  useEffect(() => {
    if (!id) return;
    getSubscriptionAddons(id).then(setSubscriptionAddons).catch(() => {});
  }, [id]);

  useEffect(() => {
    if (!subscription) return;
    getTariffs()
      .then((data) => setAvailableTariffs((data.content || []).filter((t) => t.code !== subscription.tariffCode)))
      .catch(() => {});
    getAddons(subscription.tariffCode).then(setAvailableAddons).catch(() => {});
  }, [subscription?.tariffCode]);

  const handleChangeTariff = async () => {
    if (!id || !selectedTariffCode) return;
    setActionLoading(true);
    try {
      const updated = await changeTariff(id, selectedTariffCode);
      setSubscription(updated);
      setShowTariffPicker(false);
      setSelectedTariffCode("");
    } catch {
      alert("Tarife değiştirilirken bir hata oluştu.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddAddon = async () => {
    if (!id || !selectedAddonCode) return;
    setActionLoading(true);
    try {
      await addAddonToSubscription(id, selectedAddonCode);
      const updated = await getSubscriptionAddons(id);
      setSubscriptionAddons(updated);
      setShowAddonPicker(false);
      setSelectedAddonCode("");
    } catch {
      alert("Ek paket eklenirken bir hata oluştu.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleAction = async (action: () => Promise<SubscriptionResponse>) => {
    setActionLoading(true);
    try {
      const updated = await action();
      setSubscription(updated);
    } catch {
      alert("İşlem gerçekleştirilirken bir hata oluştu.");
    } finally {
      setActionLoading(false);
    }
  };

  const internetTotal = quota?.dataMbIncluded ? quota.dataMbIncluded / 1024 : 0;
  const internetRemaining = quota?.dataMbRemaining ? quota.dataMbRemaining / 1024 : 0;
  const internetPct = internetTotal > 0 ? internetRemaining / internetTotal : 0;
  const minutesPct = quota && quota.minutesIncluded > 0 ? quota.minutesRemaining / quota.minutesIncluded : 0;
  const smsPct = quota && quota.smsIncluded > 0 ? quota.smsRemaining / quota.smsIncluded : 0;

  const radius = 40;
  const circumference = 2 * Math.PI * radius;

  const internetOffset = circumference - (internetPct * circumference);
  const dakikaOffset = circumference - (minutesPct * circumference);
  const smsOffset = circumference - (smsPct * circumference);

  const formatPeriod = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-success-bg text-success border border-success/20';
      case 'PENDING': return 'bg-warning-bg text-warning border border-warning/20';
      case 'SUSPENDED': return 'bg-info-bg text-info border border-info/20';
      case 'TERMINATED': return 'bg-danger-bg text-danger border border-danger/20';
      default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'Aktif';
      case 'PENDING': return 'Beklemede';
      case 'SUSPENDED': return 'Askıda';
      case 'TERMINATED': return 'Sonlandırılmış';
      default: return status;
    }
  };

  return (
    <div className="flex flex-col gap-stack-lg max-w-[1440px] mx-auto">
      
      {/* Entity Header */}
      <header className="flex flex-col gap-stack-sm bg-surface p-container-padding border border-outline-variant rounded">
        <div className="flex justify-between items-start">
          <div className="flex items-center gap-4">
            <h2 className="font-mono-id text-[24px] font-semibold text-on-surface tracking-tight tabular-nums">{subscription?.msisdn || quota?.msisdn || id}</h2>
            {subscription && (
              <span className={`px-2 py-1 font-label-sm rounded-full flex items-center gap-1 ${getStatusColor(subscription.status)}`}>
                <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                {getStatusLabel(subscription.status)}
              </span>
            )}
          </div>
          <div className="flex gap-2">
            <button className="px-4 py-2 bg-surface border border-outline-variant text-on-surface font-label-md rounded-lg hover:bg-surface-container-low transition-colors">
              Düzenle
            </button>
            <button className="p-2 bg-surface border border-outline-variant text-on-surface rounded-lg hover:bg-surface-container-low transition-colors flex items-center justify-center">
              <span className="material-symbols-outlined">more_vert</span>
            </button>
          </div>
        </div>
        <div className="flex gap-6 mt-2 text-on-surface-variant font-body-sm border-t border-outline-variant pt-3">
          <div className="flex flex-col">
            <span className="text-outline font-label-sm">Abonelik ID</span>
            <span className="font-mono-id text-primary mt-0.5">{id?.substring(0, 8).toUpperCase()}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-outline font-label-sm">Tarife</span>
            <span className="mt-0.5 font-medium">{subscription?.tariffCode || quota?.tariffCode || "-"}</span>
          </div>
          <div className="flex flex-col">
            <span className="text-outline font-label-sm">Dönem</span>
            <span className="mt-0.5 font-medium tabular-nums">{quota ? `${formatPeriod(quota.periodStart)} - ${formatPeriod(quota.periodEnd)}` : "-"}</span>
          </div>
        </div>
      </header>

      {/* Tabs */}
      <div className="flex border-b border-outline-variant">
        {["Genel Bakış", "Kullanım Geçmişi"].map((tab) => (
          <button key={tab} onClick={() => setActiveTab(tab)}
            className={`px-4 py-3 font-label-md transition-colors ${activeTab === tab ? "text-primary border-b-2 border-primary" : "text-on-surface-variant hover:text-on-surface"}`}>
            {tab}
          </button>
        ))}
      </div>

      <div className="flex flex-col lg:flex-row gap-gutter">
        
        {/* Left Column */}
        <div className="flex-1 flex flex-col gap-gutter">
          
          {activeTab === "Genel Bakış" && (
            <>
              {/* Kota Kullanımı */}
              <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
                <h3 className="font-h3 text-on-surface">Kota Kullanımı</h3>
                {loadingQuota ? (
                  <div className="p-8 text-center text-secondary">
                    <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span>
                    Yükleniyor...
                  </div>
                ) : quota ? (
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {/* Internet Quota */}
                    <div className="flex flex-col items-center gap-2">
                      <span className="font-label-md text-on-surface-variant">İnternet</span>
                      <div className="relative w-32 h-32">
                        <svg className="w-full h-full -rotate-90 transform-origin-center" viewBox="0 0 100 100">
                          <circle cx="50" cy="50" r={radius} fill="transparent" strokeWidth="8" className="text-surface-container-high stroke-current" />
                          <circle cx="50" cy="50" r={radius} fill="transparent" strokeWidth="8" strokeDasharray={circumference} strokeDashoffset={internetOffset} strokeLinecap="round" className="text-primary stroke-current transition-all duration-1000 ease-out" />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                          <span className="font-mono-id text-[18px] font-semibold text-on-surface tabular-nums">{internetRemaining.toFixed(1)} GB</span>
                        </div>
                      </div>
                      <div className="text-center">
                        <p className="font-body-sm text-on-surface-variant tabular-nums">{internetTotal.toFixed(0)} GB'ın {internetRemaining.toFixed(1)} GB'ı kaldı</p>
                        <p className="font-mono-label text-outline mt-1 tabular-nums">Dönem: {formatPeriod(quota.periodStart)} - {formatPeriod(quota.periodEnd)}</p>
                      </div>
                    </div>
                    {/* Dakika Quota */}
                    <div className="flex flex-col items-center gap-2">
                      <span className="font-label-md text-on-surface-variant">Dakika</span>
                      <div className="relative w-32 h-32">
                        <svg className="w-full h-full -rotate-90 transform-origin-center" viewBox="0 0 100 100">
                          <circle cx="50" cy="50" r={radius} fill="transparent" strokeWidth="8" className="text-surface-container-high stroke-current" />
                          <circle cx="50" cy="50" r={radius} fill="transparent" strokeWidth="8" strokeDasharray={circumference} strokeDashoffset={dakikaOffset} strokeLinecap="round" className="text-secondary stroke-current transition-all duration-500" />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                          <span className="font-mono-id text-[18px] font-semibold text-on-surface tabular-nums">{quota.minutesRemaining} DK</span>
                        </div>
                      </div>
                      <div className="text-center">
                        <p className="font-body-sm text-on-surface-variant tabular-nums">{quota.minutesIncluded} DK'nın {quota.minutesRemaining} DK'sı kaldı</p>
                        <p className="font-mono-label text-outline mt-1 tabular-nums">Dönem: {formatPeriod(quota.periodStart)} - {formatPeriod(quota.periodEnd)}</p>
                      </div>
                    </div>
                    {/* SMS Quota */}
                    <div className="flex flex-col items-center gap-2">
                      <span className="font-label-md text-on-surface-variant">SMS</span>
                      <div className="relative w-32 h-32">
                        <svg className="w-full h-full -rotate-90 transform-origin-center" viewBox="0 0 100 100">
                          <circle cx="50" cy="50" r={radius} fill="transparent" strokeWidth="8" className="text-surface-container-high stroke-current" />
                          <circle cx="50" cy="50" r={radius} fill="transparent" strokeWidth="8" strokeDasharray={circumference} strokeDashoffset={smsOffset} strokeLinecap="round" className="text-outline-variant stroke-current transition-all duration-500" />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                          <span className="font-mono-id text-[18px] font-semibold text-on-surface tabular-nums">{quota.smsRemaining} SMS</span>
                        </div>
                      </div>
                      <div className="text-center">
                        <p className="font-body-sm text-on-surface-variant tabular-nums">{quota.smsIncluded} SMS'in {quota.smsRemaining} SMS'i kaldı</p>
                        <p className="font-mono-label text-outline mt-1 tabular-nums">Dönem: {formatPeriod(quota.periodStart)} - {formatPeriod(quota.periodEnd)}</p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="p-8 text-center text-secondary">Kota bilgisi bulunamadı.</div>
                )}
              </div>

              {/* Tarife Bilgileri */}
              <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
                <h3 className="font-h3 text-on-surface">Tarife Bilgileri</h3>
                <div className="flex justify-between items-end mb-2">
                  <div>
                    <span className="font-body-sm text-on-surface-variant block mb-1">Mevcut Tarife</span>
                    <span className="font-label-md text-on-surface">{subscription?.tariffCode || quota?.tariffCode || "-"}</span>
                  </div>
                </div>

                {subscriptionAddons.length > 0 && (
                  <div className="flex flex-col gap-1">
                    <span className="font-body-sm text-on-surface-variant">Ek Paketler</span>
                    <ul className="flex flex-col gap-1">
                      {subscriptionAddons.map((a) => (
                        <li key={a.id} className="font-label-sm text-on-surface flex items-center gap-2">
                          <span className="material-symbols-outlined text-[16px] text-primary">add_circle</span>
                          {a.addonCode}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {subscription?.status !== 'ACTIVE' ? (
                  <p className="font-body-sm text-secondary">Tarife/paket değişikliği yalnızca aktif abonelikler için yapılabilir.</p>
                ) : showTariffPicker ? (
                  <div className="flex flex-col gap-2">
                    <select
                      value={selectedTariffCode}
                      onChange={(e) => setSelectedTariffCode(e.target.value)}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none"
                    >
                      <option value="">Yeni tarife seçin</option>
                      {availableTariffs.map((t) => (
                        <option key={t.code} value={t.code}>{t.name} (₺{t.monthlyFee})</option>
                      ))}
                    </select>
                    <div className="flex gap-2">
                      <button onClick={handleChangeTariff} disabled={!selectedTariffCode || actionLoading}
                        className="flex-1 py-2 bg-primary text-surface font-label-sm rounded hover:bg-on-primary-fixed-variant transition-colors disabled:opacity-50">Onayla</button>
                      <button onClick={() => { setShowTariffPicker(false); setSelectedTariffCode(""); }}
                        className="flex-1 py-2 border border-outline-variant text-on-surface font-label-sm rounded hover:bg-surface-container-low transition-colors">Vazgeç</button>
                    </div>
                  </div>
                ) : showAddonPicker ? (
                  <div className="flex flex-col gap-2">
                    <select
                      value={selectedAddonCode}
                      onChange={(e) => setSelectedAddonCode(e.target.value)}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none"
                    >
                      <option value="">Eklenecek paket seçin</option>
                      {availableAddons.map((a) => (
                        <option key={a.code} value={a.code}>{a.name} (₺{a.price})</option>
                      ))}
                    </select>
                    <div className="flex gap-2">
                      <button onClick={handleAddAddon} disabled={!selectedAddonCode || actionLoading}
                        className="flex-1 py-2 bg-primary text-surface font-label-sm rounded hover:bg-on-primary-fixed-variant transition-colors disabled:opacity-50">Onayla</button>
                      <button onClick={() => { setShowAddonPicker(false); setSelectedAddonCode(""); }}
                        className="flex-1 py-2 border border-outline-variant text-on-surface font-label-sm rounded hover:bg-surface-container-low transition-colors">Vazgeç</button>
                    </div>
                  </div>
                ) : (
                  <div className="flex gap-2 mt-auto">
                    <button onClick={() => setShowTariffPicker(true)} className="flex-1 py-2 bg-primary text-surface font-label-sm rounded hover:bg-on-primary-fixed-variant transition-colors text-center">Tarife Değiştir</button>
                    <button onClick={() => setShowAddonPicker(true)} className="flex-1 py-2 bg-surface border border-outline-variant text-on-surface font-label-sm rounded hover:bg-surface-container-low transition-colors text-center">Ek Paket Ekle</button>
                  </div>
                )}
              </div>
            </>
          )}

          {activeTab === "Kullanım Geçmişi" && (
            <div className="bg-surface border border-outline-variant rounded overflow-hidden shadow-sm">
              {loadingHistory ? (
                <div className="p-8 text-center text-secondary">
                  <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span>
                  Kullanım geçmişi yükleniyor...
                </div>
              ) : usageHistory.length > 0 ? (
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-background border-b border-outline-variant h-10">
                      <th className="px-gutter font-label-sm text-secondary">Tür</th>
                      <th className="px-gutter font-label-sm text-secondary">Miktar</th>
                      <th className="px-gutter font-label-sm text-secondary">Tarih</th>
                      <th className="px-gutter font-label-sm text-secondary">CDR Ref</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant">
                    {usageHistory.map((record) => (
                      <tr key={record.id} className="h-row-height-std hover:bg-surface-container-low transition-colors">
                        <td className="px-gutter">
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm bg-surface-container text-on-surface border border-outline-variant">
                            {record.type}
                          </span>
                        </td>
                        <td className="px-gutter font-mono-id tabular-nums">{record.quantity}</td>
                        <td className="px-gutter font-mono-id text-secondary text-[13px]">{new Date(record.recordedAt).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}</td>
                        <td className="px-gutter font-mono-id text-secondary text-[13px]">{record.cdrRef}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <div className="p-8 text-center text-secondary">
                  <span className="material-symbols-outlined block text-[32px] text-surface-container-highest mb-2">history</span>
                  Kullanım geçmişi bulunmuyor.
                </div>
              )}
            </div>
          )}

        </div>

        {/* Right Column */}
        <div className="w-full lg:w-[360px] flex flex-col gap-gutter shrink-0">
          
          {/* Durum Yönetimi */}
          {subscription && (
            <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
              <h3 className="font-h3 text-on-surface">Durum Yönetimi</h3>
              <p className="font-body-sm text-on-surface-variant">
                Bu abonelik şu anda <strong>{getStatusLabel(subscription.status)}</strong> durumdadır. İşlemler faturalandırmayı etkileyebilir.
              </p>
              <div className="flex flex-col gap-2 mt-2">
                {subscription.status === 'ACTIVE' && (
                  <button
                    onClick={() => handleAction(() => suspendSubscription(subscription.id))}
                    disabled={actionLoading}
                    className="w-full py-2 bg-surface border border-outline-variant text-on-surface font-label-md rounded-lg hover:bg-surface-container-low transition-colors flex justify-center items-center gap-2 disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[18px]">pause_circle</span> Askıya Al
                  </button>
                )}
                {subscription.status === 'SUSPENDED' && (
                  <button
                    onClick={() => handleAction(() => reactivateSubscription(subscription.id))}
                    disabled={actionLoading}
                    className="w-full py-2 bg-success text-on-primary font-label-md rounded-lg hover:bg-success/90 transition-colors flex justify-center items-center gap-2 disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[18px]">play_circle</span> Yeniden Aktifleştir
                  </button>
                )}
                {(subscription.status === 'ACTIVE' || subscription.status === 'SUSPENDED') && (
                  <button
                    onClick={() => handleAction(() => terminateSubscription(subscription.id))}
                    disabled={actionLoading}
                    className="w-full py-2 bg-danger-bg border border-danger/20 text-danger font-label-md rounded-lg hover:bg-danger hover:text-surface transition-colors flex justify-center items-center gap-2 disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[18px]">cancel</span> Sonlandır
                  </button>
                )}
              </div>
            </div>
          )}

          {/* Bilgi Kartları */}
          <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
            <h3 className="font-h3 text-on-surface">Abonelik Bilgileri</h3>
            <div className="flex flex-col gap-3">
              <div className="flex justify-between items-center py-2 border-b border-outline-variant/30">
                <span className="font-body-sm text-on-surface-variant">Abonelik ID</span>
                <span className="font-mono-id text-on-surface tabular-nums text-[13px]">{id?.substring(0, 8).toUpperCase()}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b border-outline-variant/30">
                <span className="font-body-sm text-on-surface-variant">MSISDN</span>
                <span className="font-mono-id text-on-surface tabular-nums">{subscription?.msisdn || quota?.msisdn || "-"}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b border-outline-variant/30">
                <span className="font-body-sm text-on-surface-variant">Müşteri No</span>
                <span className="font-mono-id text-on-surface tabular-nums text-[13px]">{subscription?.customerNo || "-"}</span>
              </div>
              <div className="flex justify-between items-center py-2">
                <span className="font-body-sm text-on-surface-variant">Tarife</span>
                <span className="font-body-sm text-on-surface">{subscription?.tariffCode || quota?.tariffCode || "-"}</span>
              </div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
