export function PageLoader() {
  return (
    <div className="flex-1 flex items-center justify-center min-h-[240px]">
      <div className="flex flex-col items-center gap-3 text-on-surface-variant">
        <span className="material-symbols-outlined text-[32px] animate-spin">progress_activity</span>
        <span className="font-label-sm">Yükleniyor...</span>
      </div>
    </div>
  );
}
