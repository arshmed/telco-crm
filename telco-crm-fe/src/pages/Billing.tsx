export default function Billing() {
  return (
    <div className="max-w-[1200px] mx-auto space-y-stack-lg">
      {/* Page Header */}
      <div className="flex justify-between items-end">
        <div>
          <h2 className="font-h1 text-on-surface">Fatura Kesimi</h2>
          <p className="font-body-md text-on-surface-variant mt-1">Dönemsel toplu faturalandırma işlemlerini başlatın ve izleyin.</p>
        </div>
        <div className="flex gap-3">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-info-bg text-info font-label-sm border border-info/20">
            <span className="material-symbols-outlined text-[14px]">admin_panel_settings</span>
            ADMIN_ACCESS
          </span>
        </div>
      </div>

      {/* Action & Active Run Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-gutter">
        
        {/* Start New Run Card */}
        <div className="col-span-1 bg-surface border border-outline-variant rounded p-5 shadow-sm flex flex-col">
          <h3 className="font-h3 text-on-surface mb-4 flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px] text-primary">play_circle</span>
            Yeni Koşu Başlat
          </h3>
          <div className="space-y-4 flex-1">
            <div className="flex flex-col gap-1">
              <label className="font-label-sm text-on-surface-variant">Faturalandırma Dönemi</label>
              <div className="relative">
                <select className="w-full h-[40px] pl-3 pr-10 border border-outline-variant rounded-lg font-body-md text-on-surface focus:ring-0 focus:border-primary bg-surface appearance-none">
                  <option value="2026-06">Haziran 2026</option>
                  <option value="2026-05">Mayıs 2026</option>
                </select>
                <span className="material-symbols-outlined absolute right-3 top-2.5 text-on-surface-variant pointer-events-none text-[20px]">calendar_month</span>
              </div>
            </div>
            <div className="flex flex-col gap-1">
              <label className="font-label-sm text-on-surface-variant">Koşu Tipi</label>
              <select className="w-full h-[40px] px-3 border border-outline-variant rounded-lg font-body-md text-on-surface focus:ring-0 focus:border-primary bg-surface">
                <option>Standart Bireysel (B2C)</option>
                <option>Kurumsal (B2B)</option>
                <option>Ara Dönem (Ad-hoc)</option>
              </select>
            </div>
          </div>
          <button disabled className="mt-6 w-full h-[40px] bg-primary text-on-primary rounded-lg font-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
            Kesimi Başlat
            <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
          </button>
          <p className="font-label-sm text-danger mt-2 text-center">Devam eden bir koşu var.</p>
        </div>

        {/* Active Run Progress */}
        <div className="col-span-1 lg:col-span-2 bg-surface border border-outline-variant rounded p-5 shadow-sm relative overflow-hidden">
          <div className="absolute inset-0 bg-primary/5 animate-pulse pointer-events-none"></div>
          
          <div className="relative z-10 flex justify-between items-start mb-6">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <span className="relative flex h-3 w-3">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-info opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-3 w-3 bg-info"></span>
                </span>
                <h3 className="font-h3 text-on-surface">Aktif Koşu: BR-2606-B2C</h3>
              </div>
              <p className="font-body-md text-on-surface-variant">Haziran 2026 - Standart Bireysel</p>
            </div>
            <div className="text-right">
              <div className="font-mono-id text-on-surface-variant mb-1">Süre: <span className="font-semibold text-on-surface">12 dk 45 sn</span></div>
              <div className="font-label-sm text-on-surface-variant">Tahmini Bitiş: ~3 dk</div>
            </div>
          </div>

          <div className="relative z-10 space-y-2 mb-6">
            <div className="flex justify-between font-label-md">
              <span className="text-on-surface">İlerleme Durumu</span>
              <span className="text-primary font-bold">95.4%</span>
            </div>
            <div className="h-2 w-full bg-surface-container-high rounded-full overflow-hidden">
              <div className="h-full bg-primary rounded-full" style={{ width: '95.4%' }}></div>
            </div>
            <div className="flex justify-between font-body-sm text-on-surface-variant">
              <span>95.412 işlendi</span>
              <span>Hedef: 100.000 abone</span>
            </div>
          </div>

          <div className="relative z-10 grid grid-cols-3 gap-4 border-t border-outline-variant pt-4">
            <div>
              <div className="font-label-sm text-on-surface-variant mb-1 uppercase tracking-wider">Üretilen Fatura</div>
              <div className="font-mono-id text-[18px] text-on-surface font-semibold">95.398</div>
            </div>
            <div>
              <div className="font-label-sm text-on-surface-variant mb-1 uppercase tracking-wider">Hatalı İşlem</div>
              <div className="font-mono-id text-[18px] text-danger font-semibold flex items-center gap-1">
                14
                <span className="material-symbols-outlined text-[16px] text-danger">warning</span>
              </div>
            </div>
            <div className="flex items-end justify-end">
              <button className="text-primary font-label-md hover:underline flex items-center gap-1">
                Canlı Logları Gör
                <span className="material-symbols-outlined text-[16px]">open_in_new</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* History Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex flex-col">
        <div className="px-5 py-4 border-b border-outline-variant flex justify-between items-center bg-background/50">
          <h3 className="font-h3 text-on-surface">Geçmiş Koşular</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-surface border-b border-outline-variant h-[40px]">
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Koşu No</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Dönem</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Başlangıç Tarihi</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap text-right">Süre</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap text-right">İşlenen Abone</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap text-right">Üretilen Fatura</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap text-right">Hata</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Durum</th>
                <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {/* Active */}
              <tr className="h-row-height-std hover:bg-surface-container-low transition-colors bg-primary/5">
                <td className="px-4 font-mono-id text-on-surface">BR-2606-B2C</td>
                <td className="px-4 font-body-sm text-on-surface">Haziran 2026</td>
                <td className="px-4 font-body-sm text-on-surface-variant">01.07.2026 02:00</td>
                <td className="px-4 font-mono-id text-on-surface-variant text-right">12 dk</td>
                <td className="px-4 font-mono-id text-on-surface text-right">95.412</td>
                <td className="px-4 font-mono-id text-on-surface text-right">95.398</td>
                <td className="px-4 font-mono-id text-danger text-right">14</td>
                <td className="px-4">
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-info-bg text-info border border-info/20">
                    Devam Ediyor
                  </span>
                </td>
                <td className="px-4">
                  <button className="text-primary font-label-sm hover:underline">Detay</button>
                </td>
              </tr>
              {/* Completed */}
              <tr className="h-row-height-std hover:bg-surface-container-low transition-colors">
                <td className="px-4 font-mono-id text-on-surface">BR-2605-B2C</td>
                <td className="px-4 font-body-sm text-on-surface">Mayıs 2026</td>
                <td className="px-4 font-body-sm text-on-surface-variant">01.06.2026 02:00</td>
                <td className="px-4 font-mono-id text-on-surface-variant text-right">18 dk</td>
                <td className="px-4 font-mono-id text-on-surface text-right">100.000</td>
                <td className="px-4 font-mono-id text-on-surface text-right">98.190</td>
                <td className="px-4 font-mono-id text-on-surface-variant text-right">0</td>
                <td className="px-4">
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-success-bg text-success border border-success/20">
                    Tamamlandı
                  </span>
                </td>
                <td className="px-4">
                  <div className="flex items-center gap-2">
                    <button className="text-primary font-label-sm hover:underline">Detay</button>
                    <button className="text-secondary font-label-sm hover:text-on-surface transition-colors">Log</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
