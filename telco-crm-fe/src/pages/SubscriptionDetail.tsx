import { useState } from "react";
import { Link } from "react-router-dom";

export default function SubscriptionDetail() {
  const [internetTotal, setInternetTotal] = useState(20);
  const [internetRemaining, setInternetRemaining] = useState(14.2);

  const internetPct = internetRemaining / internetTotal;

  // SVG progress ring math helper
  const radius = 40;
  const circumference = 2 * Math.PI * radius; // 251.2
  
  // Internet Quota
  const internetOffset = circumference - (internetPct * circumference);
  // Dakika Quota (45% left)
  const dakikaOffset = circumference - (0.45 * circumference);
  // SMS Quota (95% left)
  const smsOffset = circumference - (0.95 * circumference);

  const handleBuyAddon = () => {
    // 5GB ek paket alım simülasyonu
    setInternetTotal(prev => prev + 5);
    setInternetRemaining(prev => prev + 5);
    alert("5GB Ek İnternet Paketi hatta tanımlandı.");
  };

  return (
    <div className="flex flex-col gap-stack-lg max-w-[1440px] mx-auto">
      
      {/* Entity Header */}
      <header className="flex flex-col gap-stack-sm bg-surface p-container-padding border border-outline-variant rounded">
        <div className="flex justify-between items-start">
          <div className="flex items-center gap-4">
            <h2 className="font-mono-id text-[24px] font-semibold text-on-surface tracking-tight tabular-nums">+90 532 123 45 67</h2>
            <span className="px-2 py-1 bg-success-bg text-success font-label-sm rounded-full flex items-center gap-1 border border-success/20">
              <span className="w-1.5 h-1.5 rounded-full bg-success"></span>
              Aktif
            </span>
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
            <span className="text-outline font-label-sm">Müşteri</span>
            <Link to="/customers/1" className="text-primary hover:underline font-medium mt-0.5">Ahmet Yılmaz</Link>
          </div>
          <div className="flex flex-col">
            <span className="text-outline font-label-sm">Tarife</span>
            <span className="mt-0.5 font-medium">Premium 20GB Paketi</span>
          </div>
          <div className="flex flex-col">
            <span className="text-outline font-label-sm">Aktivasyon Tarihi</span>
            <span className="mt-0.5 font-medium tabular-nums">08.07.2023</span>
          </div>
        </div>
      </header>

      {/* Tabs */}
      <div className="flex border-b border-outline-variant">
        <button className="px-4 py-3 font-label-md text-primary border-b-2 border-primary">Genel Bakış</button>
        <button className="px-4 py-3 font-label-md text-on-surface-variant hover:text-on-surface transition-colors">Kullanım Geçmişi</button>
        <button className="px-4 py-3 font-label-md text-on-surface-variant hover:text-on-surface transition-colors">Faturalar</button>
        <button className="px-4 py-3 font-label-md text-on-surface-variant hover:text-on-surface transition-colors">Cihazlar</button>
      </div>

      <div className="flex flex-col lg:flex-row gap-gutter">
        
        {/* Left Column */}
        <div className="flex-1 flex flex-col gap-gutter">
          
          {/* Kota Kullanımı */}
          <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
            <h3 className="font-h3 text-on-surface">Kota Kullanımı</h3>
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
                  <p className="font-body-sm text-on-surface-variant tabular-nums">{internetTotal} GB'ın {internetRemaining.toFixed(1)} GB'ı kaldı</p>
                  <p className="font-mono-label text-outline mt-1 tabular-nums">Dönem: 1-31 Eki</p>
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
                    <span className="font-mono-id text-[18px] font-semibold text-on-surface tabular-nums">450 DK</span>
                  </div>
                </div>
                <div className="text-center">
                  <p className="font-body-sm text-on-surface-variant tabular-nums">1000 DK'nın 450 DK'sı kaldı</p>
                  <p className="font-mono-label text-outline mt-1 tabular-nums">Dönem: 1-31 Eki</p>
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
                    <span className="font-mono-id text-[18px] font-semibold text-on-surface tabular-nums">950 SMS</span>
                  </div>
                </div>
                <div className="text-center">
                  <p className="font-body-sm text-on-surface-variant tabular-nums">1000 SMS'in 950 SMS'i kaldı</p>
                  <p className="font-mono-label text-outline mt-1 tabular-nums">Dönem: 1-31 Eki</p>
                </div>
              </div>

            </div>
          </div>

          {/* SIM & Tarife Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
            
            {/* SIM Bilgileri */}
            <div className="bg-surface border border-outline-variant rounded-lg p-container-padding flex flex-col gap-4">
              <div className="flex justify-between items-center">
                <h3 className="font-h3 text-on-surface">SIM Bilgileri</h3>
                <span className="px-2 py-0.5 bg-success-bg text-success font-label-sm rounded border border-success/20">Aktif</span>
              </div>
              <div className="flex flex-col gap-3">
                <div className="flex justify-between items-center py-2 border-b border-outline-variant/30">
                  <span className="font-body-sm text-on-surface-variant">ICCID</span>
                  <div className="flex items-center gap-2">
                    <span className="font-mono-id text-on-surface tabular-nums">8990012345678901234</span>
                    <button className="text-outline hover:text-primary transition-colors"><span className="material-symbols-outlined text-[16px]">content_copy</span></button>
                  </div>
                </div>
                <div className="flex justify-between items-center py-2">
                  <span className="font-body-sm text-on-surface-variant">IMSI</span>
                  <div className="flex items-center gap-2">
                    <span className="font-mono-id text-on-surface tabular-nums">286012345678901</span>
                    <button className="text-outline hover:text-primary transition-colors"><span className="material-symbols-outlined text-[16px]">content_copy</span></button>
                  </div>
                </div>
              </div>
            </div>

            {/* Tarife Bilgileri */}
            <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
              <h3 className="font-h3 text-on-surface">Tarife Bilgileri</h3>
              <div className="flex justify-between items-end mb-2">
                <div>
                  <span className="font-body-sm text-on-surface-variant block mb-1">Mevcut Tarife</span>
                  <span className="font-label-md text-on-surface">Premium 20GB Paketi</span>
                </div>
                <div className="text-right">
                  <span className="font-body-sm text-on-surface-variant block mb-1">Aylık Ücret</span>
                  <span className="font-mono-id text-[16px] font-semibold text-on-surface tabular-nums">₺249,90</span>
                </div>
              </div>
              <div className="flex gap-2 mt-auto">
                <button className="flex-1 py-2 bg-primary text-surface font-label-sm rounded hover:bg-on-primary-fixed-variant transition-colors text-center">Tarife Değiştir</button>
                <button onClick={handleBuyAddon} className="flex-1 py-2 bg-surface border border-outline-variant text-on-surface font-label-sm rounded hover:bg-surface-container-low transition-colors text-center">Ek Paket Ekle (5GB)</button>
              </div>
            </div>

          </div>

        </div>

        {/* Right Column (Fixed) */}
        <div className="w-full lg:w-[360px] flex flex-col gap-gutter shrink-0">
          
          {/* Durum Yönetimi */}
          <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4">
            <h3 className="font-h3 text-on-surface">Durum Yönetimi</h3>
            <p className="font-body-sm text-on-surface-variant">Bu abonelik şu anda aktif durumdadır. İşlemler faturalandırmayı etkileyebilir.</p>
            <div className="flex flex-col gap-2 mt-2">
              <button className="w-full py-2 bg-surface border border-outline-variant text-on-surface font-label-md rounded-lg hover:bg-surface-container-low transition-colors flex justify-center items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">pause_circle</span> Askıya Al
              </button>
              <button className="w-full py-2 bg-danger-bg border border-danger/20 text-danger font-label-md rounded-lg hover:bg-danger hover:text-surface transition-colors flex justify-center items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">cancel</span> Sonlandır
              </button>
            </div>
          </div>

          {/* Olay Rayı (Event Rail) */}
          <div className="bg-surface border border-outline-variant rounded p-container-padding flex flex-col gap-4 flex-1">
            <div className="flex justify-between items-center">
              <h3 className="font-h3 text-on-surface">Olay Rayı</h3>
              <button className="text-primary text-body-sm font-medium hover:underline">Tümünü Gör</button>
            </div>
            
            <div className="relative pl-6 mt-2 flex flex-col gap-6 before:content-[''] before:absolute before:left-[11px] before:top-2 before:bottom-2 before:w-[2px] before:bg-surface-variant">
              
              <div className="relative">
                <div className="absolute -left-6 top-1 w-6 h-6 bg-surface rounded-full flex items-center justify-center border-2 border-primary z-10">
                  <span className="w-2 h-2 bg-primary rounded-full"></span>
                </div>
                <div className="flex flex-col gap-1">
                  <div className="flex justify-between items-start">
                    <span className="font-label-md text-on-surface">PackageChangeCompleted</span>
                    <span className="font-mono-label text-outline tabular-nums">01.10.2023 14:22</span>
                  </div>
                  <span className="font-body-sm text-on-surface-variant">Tariff upgraded to Premium 20GB</span>
                  <span className="inline-flex items-center px-2 py-0.5 bg-surface-container-low text-primary font-mono-label rounded border border-primary/20 w-fit mt-1">BSS_BILLING</span>
                </div>
              </div>

              <div className="relative">
                <div className="absolute -left-6 top-1 w-6 h-6 bg-surface rounded-full flex items-center justify-center border-2 border-outline-variant z-10">
                  <span className="w-2 h-2 bg-outline-variant rounded-full"></span>
                </div>
                <div className="flex flex-col gap-1">
                  <div className="flex justify-between items-start">
                    <span className="font-label-md text-on-surface">SubscriptionActivated</span>
                    <span className="font-mono-label text-outline tabular-nums">08.07.2023 09:15</span>
                  </div>
                  <span className="font-body-sm text-on-surface-variant">SIM card provisioned on HLR</span>
                  <span className="inline-flex items-center px-2 py-0.5 bg-surface-container-low text-primary font-mono-label rounded border border-primary/20 w-fit mt-1">OSS_PROV</span>
                </div>
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
