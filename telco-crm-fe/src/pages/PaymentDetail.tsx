import { Link } from "react-router-dom";

export default function PaymentDetail() {
  return (
    <div className="flex flex-col gap-stack-lg max-w-7xl mx-auto w-full">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="flex items-center text-on-surface-variant font-label-md">
        <Link to="/finance/billing" className="hover:text-primary transition-colors">Finans</Link>
        <span className="material-symbols-outlined mx-2 text-[16px]">chevron_right</span>
        <Link to="/finance/payments" className="hover:text-primary transition-colors">Ödemeler</Link>
        <span className="material-symbols-outlined mx-2 text-[16px]">chevron_right</span>
        <span className="font-mono-id text-on-surface">PAY-8829-1A</span>
      </nav>

      {/* Entity Header */}
      <header className="bg-surface border border-outline-variant rounded-lg p-stack-lg flex flex-col md:flex-row md:items-start justify-between gap-stack-lg">
        <div className="flex flex-col gap-stack-sm">
          <div className="flex items-center gap-stack-sm">
            <h1 className="font-h1 text-on-surface font-mono-id tracking-tight">PAY-8829-1A</h1>
            <span className="inline-flex items-center px-2 py-1 rounded bg-[#e6f4ea] text-[#137333] font-label-sm uppercase tracking-wider">
              COMPLETED
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-x-stack-lg gap-y-stack-sm text-on-surface-variant font-body-sm mt-2">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">person</span>
              <span>Müşteri: <strong className="text-on-surface">Ayşe Yılmaz</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">payments</span>
              <span>Tutar: <strong className="text-on-surface font-mono-id">₺323,88</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">calendar_today</span>
              <span>Tarih: <strong className="text-on-surface">14.10.2023 14:35</strong></span>
            </div>
          </div>
        </div>
        <div className="flex-shrink-0">
          <button className="bg-error-container text-on-error-container hover:bg-error hover:text-on-error border border-error-container font-label-md px-4 py-2 rounded transition-colors flex items-center gap-2 h-[40px]">
            <span className="material-symbols-outlined text-[18px]">undo</span>
            İade Et
          </button>
        </div>
      </header>

      {/* Main Content Layout */}
      <div className="flex flex-col lg:flex-row gap-stack-lg items-start">
        {/* Left Column: Details */}
        <div className="flex-1 flex flex-col gap-stack-lg w-full">
          {/* Payment Details Card */}
          <section className="bg-surface border border-outline-variant rounded-lg overflow-hidden">
            <div className="border-b border-outline-variant bg-surface-container-lowest px-stack-lg py-stack-md flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">credit_card</span>
              <h2 className="font-h3 text-on-surface">Ödeme Detayları</h2>
            </div>
            <div className="p-stack-lg grid grid-cols-1 md:grid-cols-2 gap-stack-lg">
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">İşlem ID (TXN)</span>
                <span className="font-mono-id text-on-surface">TXN-998273-XYZ</span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">Kart Sahibi</span>
                <span className="font-body-md text-on-surface">AYŞE YILMAZ</span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">Maskeli Kart No</span>
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-on-surface-variant text-[16px]">credit_score</span>
                  <span className="font-mono-id text-on-surface">4543 **** **** 9012</span>
                </div>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">Banka / Kurum</span>
                <span className="font-body-md text-on-surface flex items-center gap-2">
                  <span className="material-symbols-outlined text-on-surface-variant text-[16px]">account_balance</span>
                  Garanti BBVA
                </span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">Otorizasyon Kodu</span>
                <span className="font-mono-id text-on-surface">AUTH-8821</span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">3D Secure</span>
                <span className="font-body-md text-on-surface flex items-center gap-1">
                  <span className="material-symbols-outlined text-success text-[16px] icon-fill">check_circle</span>
                  Doğrulandı
                </span>
              </div>
            </div>
          </section>

          {/* Related Invoice Card */}
          <section className="bg-surface border border-outline-variant rounded-lg overflow-hidden">
            <div className="border-b border-outline-variant bg-surface-container-lowest px-stack-lg py-stack-md flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">receipt_long</span>
              <h2 className="font-h3 text-on-surface">İlgili Fatura</h2>
            </div>
            <div className="p-stack-lg flex items-center justify-between">
              <div className="flex items-center gap-stack-md">
                <div className="w-10 h-10 rounded bg-secondary-container text-on-secondary-container flex items-center justify-center">
                  <span className="material-symbols-outlined">receipt</span>
                </div>
                <div className="flex flex-col">
                  <a href="#" className="font-mono-id text-primary hover:text-on-primary-fixed-variant transition-colors underline-offset-2 hover:underline">INV-2023-001</a>
                  <span className="font-body-sm text-on-surface-variant">Eylül 2023 Dönemi - Fiber İnternet Paketi</span>
                </div>
              </div>
              <span className="font-mono-id text-on-surface font-semibold">₺323,88</span>
            </div>
          </section>
        </div>

        {/* Right Column: Timeline */}
        <div className="w-full lg:w-[360px] flex-shrink-0 flex flex-col gap-stack-lg">
          <section className="bg-surface border border-outline-variant rounded-lg overflow-hidden flex flex-col h-full">
            <div className="border-b border-outline-variant bg-surface-container-lowest px-stack-lg py-stack-md flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">history</span>
              <h2 className="font-h3 text-on-surface">Deneme Geçmişi</h2>
            </div>
            <div className="p-stack-lg flex-1 relative">
              {/* Timeline Line */}
              <div className="absolute left-[39px] top-[32px] bottom-[32px] w-[1px] bg-outline-variant"></div>
              
              <div className="flex flex-col gap-stack-lg relative z-10">
                {/* Attempt 1 (Success) */}
                <div className="flex gap-stack-md group">
                  <div className="flex flex-col items-center gap-1 w-12 pt-1">
                    <span className="font-mono-label text-on-surface-variant">14:35</span>
                  </div>
                  <div className="w-6 h-6 rounded-full bg-surface border border-success flex items-center justify-center flex-shrink-0 mt-0.5 z-10">
                    <span className="w-2.5 h-2.5 rounded-full bg-success"></span>
                  </div>
                  <div className="flex-1 bg-surface-container-lowest border border-outline-variant rounded p-stack-md flex flex-col gap-1 transition-colors group-hover:bg-surface-container-low">
                    <div className="flex justify-between items-start">
                      <span className="font-label-md text-on-surface">Deneme 1 <span className="text-success ml-1">(Başarılı)</span></span>
                    </div>
                    <span className="font-mono-id text-on-surface-variant text-[11px]">PSP: APPROVED</span>
                    <span className="font-mono-id text-on-surface-variant text-[11px]">Süre: 1.2s</span>
                  </div>
                </div>

                {/* Attempt 2 (Failed) */}
                <div className="flex gap-stack-md group">
                  <div className="flex flex-col items-center gap-1 w-12 pt-1">
                    <span className="font-mono-label text-on-surface-variant">14:32</span>
                  </div>
                  <div className="w-6 h-6 rounded-full bg-surface border border-error flex items-center justify-center flex-shrink-0 mt-0.5 z-10">
                    <span className="w-2.5 h-2.5 rounded-full bg-error"></span>
                  </div>
                  <div className="flex-1 bg-surface-container-lowest border border-outline-variant rounded p-stack-md flex flex-col gap-1 transition-colors group-hover:bg-surface-container-low">
                    <div className="flex justify-between items-start">
                      <span className="font-label-md text-on-surface">Deneme 2 <span className="text-error ml-1">(Başarısız)</span></span>
                    </div>
                    <span className="font-mono-id text-error text-[11px]">PSP: INSUFFICIENT_FUNDS</span>
                    <span className="font-body-sm text-on-surface-variant text-[12px] mt-1 border-t border-outline-variant pt-1">
                      Bakiye yetersizliği nedeniyle reddedildi.
                    </span>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="border-t border-outline-variant bg-surface-container-lowest p-stack-md text-center">
              <p className="font-body-sm text-on-surface-variant flex items-center justify-center gap-1">
                <span className="material-symbols-outlined text-[14px]">info</span>
                Başarısız ödemeler 24 / 72 / 168 saat aralıklarla otomatik yeniden denenir.
              </p>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
