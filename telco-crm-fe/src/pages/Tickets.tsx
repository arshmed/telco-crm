export default function Tickets() {
  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-between items-end">
        <div>
          <h2 className="font-h1 text-on-surface">Ticketlar</h2>
          <p className="font-body-sm text-on-surface-variant mt-1">Destek talepleri kuyruğu yönetimi</p>
        </div>
        <button className="bg-primary text-on-primary font-label-md h-[40px] px-4 rounded-lg flex items-center gap-2 hover:bg-primary-container hover:text-on-primary-container transition-colors">
          <span className="material-symbols-outlined text-[18px]">add</span>
          Yeni Ticket
        </button>
      </div>

      <div className="flex flex-col lg:flex-row gap-6 items-start">
        {/* Filters Sidebar */}
        <aside className="w-full lg:w-[240px] shrink-0 flex flex-col gap-6 bg-surface border border-outline-variant rounded-lg p-4">
          <div>
            <h3 className="font-label-sm text-secondary mb-3 uppercase tracking-wider">Durum</h3>
            <div className="flex flex-col gap-2">
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="radio" name="status" defaultChecked className="text-primary focus:ring-primary" />
                <span className="font-body-md text-on-surface">Tümü</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="radio" name="status" className="text-primary focus:ring-primary" />
                <span className="font-body-md text-on-surface">Açık</span>
              </label>
            </div>
          </div>
        </aside>

        {/* Ticket List */}
        <div className="flex-1 flex flex-col gap-3 w-full">
          {/* High Priority Ticket */}
          <div className="bg-surface border border-outline-variant rounded-lg flex relative overflow-hidden hover:border-primary-fixed-dim transition-colors cursor-pointer group">
            <div className="w-1 bg-error absolute left-0 top-0 bottom-0"></div>
            <div className="p-4 pl-5 w-full flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex flex-col gap-1 flex-1">
                <div className="flex items-center gap-3">
                  <span className="font-mono-id text-secondary">#TCK-8902</span>
                  <span className="bg-surface-container-high text-on-surface-variant font-label-sm text-[10px] px-2 py-0.5 rounded-full uppercase">Teknik</span>
                </div>
                <h4 className="font-body-md font-semibold text-on-surface group-hover:text-primary transition-colors">Fiber internet bağlantısı sürekli kopuyor</h4>
                <div className="flex items-center gap-2 mt-1">
                  <span className="material-symbols-outlined text-[16px] text-outline">person</span>
                  <span className="font-body-sm text-on-surface-variant">Mehmet Kaya</span>
                </div>
              </div>
              <div className="flex items-center gap-6 sm:justify-end shrink-0">
                <div className="flex flex-col items-end">
                  <span className="font-label-sm text-[10px] text-secondary uppercase">SLA</span>
                  <div className="flex items-center gap-1 text-warning bg-warning-bg px-2 py-1 rounded">
                    <span className="material-symbols-outlined text-[14px]">timer</span>
                    <span className="font-mono-id text-[12px] font-medium">1 sa 15 dk kaldı</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Medium Priority Ticket */}
          <div className="bg-surface border border-outline-variant rounded-lg flex relative overflow-hidden hover:border-primary-fixed-dim transition-colors cursor-pointer group">
            <div className="w-1 bg-tertiary-container absolute left-0 top-0 bottom-0"></div>
            <div className="p-4 pl-5 w-full flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex flex-col gap-1 flex-1">
                <div className="flex items-center gap-3">
                  <span className="font-mono-id text-secondary">#TCK-8895</span>
                  <span className="bg-surface-container-high text-on-surface-variant font-label-sm text-[10px] px-2 py-0.5 rounded-full uppercase">Fatura</span>
                </div>
                <h4 className="font-body-md font-semibold text-on-surface group-hover:text-primary transition-colors">Aylık faturamda beklenmeyen ek ücretler var</h4>
                <div className="flex items-center gap-2 mt-1">
                  <span className="material-symbols-outlined text-[16px] text-outline">person</span>
                  <span className="font-body-sm text-on-surface-variant">Ayşe Yıldız</span>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
