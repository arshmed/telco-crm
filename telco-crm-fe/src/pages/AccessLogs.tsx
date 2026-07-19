import { useEffect, useState } from "react";
import { AdminTabs } from "../components/admin/AdminTabs";
import { useToast } from "../context/ToastContext";
import { getAuditLogs, AuditLogResponse, Page } from "../api/identityApi";

const ENTITY_TYPE_LABELS: Record<string, string> = {
  USER: "Kullanıcı",
  ROLE: "Rol",
  PERMISSION: "İzin",
};

const ACTION_LABELS: Record<string, string> = {
  CREATED: "Oluşturuldu",
  UPDATED: "Güncellendi",
  ROLE_ASSIGNED: "Rol Atandı",
  PERMISSION_ASSIGNED: "İzin Atandı",
};

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function initialsOf(name: string) {
  return name.slice(0, 2).toUpperCase();
}

export default function AccessLogs() {
  const { showError } = useToast();
  const [data, setData] = useState<Page<AuditLogResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [entityTypeFilter, setEntityTypeFilter] = useState("");
  const [searchTerm, setSearchTerm] = useState("");

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const result = await getAuditLogs(currentPage, 20, entityTypeFilter || undefined);
      setData(result);
    } catch {
      showError("Erişim günlükleri yüklenirken bir hata oluştu.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, [currentPage, entityTypeFilter]);

  const filteredLogs = (data?.content ?? []).filter(
    (log) =>
      log.performedBy.toLowerCase().includes(searchTerm.toLowerCase()) ||
      log.entityId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col space-y-stack-lg max-w-[1440px] mx-auto w-full">
      <AdminTabs active="logs" />

      {/* Toolbar / Filters */}
      <div className="bg-surface border border-outline-variant rounded p-3 flex flex-wrap items-center gap-3 shadow-sm">
        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-auto">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">person</span>
          <input
            type="text"
            placeholder="Kullanıcı veya varlık ID ara..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border-none bg-transparent outline-none focus:ring-0 p-0 text-body-sm text-on-surface w-56"
          />
        </div>

        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-auto relative">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">filter_list</span>
          <select
            value={entityTypeFilter}
            onChange={(e) => {
              setCurrentPage(0);
              setEntityTypeFilter(e.target.value);
            }}
            className="border-none bg-transparent outline-none focus:ring-0 p-0 pr-6 text-body-sm text-on-surface appearance-none w-36 cursor-pointer"
          >
            <option value="">Tüm Varlıklar</option>
            <option value="USER">Kullanıcı</option>
            <option value="ROLE">Rol</option>
            <option value="PERMISSION">İzin</option>
          </select>
          <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none text-outline-variant text-[16px]">
            arrow_drop_down
          </span>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded flex-1 flex flex-col overflow-hidden shadow-sm">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse">
            <thead className="bg-background border-b border-outline-variant">
              <tr>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-48">TARİH</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-56">YAPAN</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-32">VARLIK</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-40">İŞLEM</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3">DETAY</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {loading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i}>
                    <td colSpan={5} className="p-3">
                      <div className="h-4 w-full bg-surface-container-low rounded animate-pulse" />
                    </td>
                  </tr>
                ))
              ) : filteredLogs.length > 0 ? (
                filteredLogs.map((log) => (
                  <tr key={log.id} className="h-row-height-std hover:bg-surface-container-low transition-colors group">
                    <td className="p-3 font-mono-id text-on-surface whitespace-nowrap">{formatDateTime(log.createdAt)}</td>
                    <td className="p-3">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full flex items-center justify-center font-label-sm text-[10px] flex-shrink-0 bg-primary-container text-on-primary-container">
                          {initialsOf(log.performedBy)}
                        </div>
                        <span className="font-body-sm text-on-surface">{log.performedBy}</span>
                      </div>
                    </td>
                    <td className="p-3">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium bg-surface-container text-on-surface-variant">
                        {ENTITY_TYPE_LABELS[log.entityType] ?? log.entityType}
                      </span>
                    </td>
                    <td className="p-3 font-body-sm text-on-surface">{ACTION_LABELS[log.action] ?? log.action}</td>
                    <td className="p-3 font-body-sm text-on-surface-variant">{log.detail ?? "-"}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-secondary font-body-md">
                    {searchTerm || entityTypeFilter ? "Kriterlere uygun kayıt bulunamadı." : "Henüz erişim günlüğü yok."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {data && data.totalElements > 0 && (
          <div className="bg-background border-t border-outline-variant p-3 flex items-center justify-between shrink-0">
            <span className="font-body-sm text-on-surface-variant">Toplam {data.totalElements} kayıt</span>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={data.number === 0}
                className="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:bg-surface-container-highest transition-colors disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <span className="font-mono-label text-on-surface px-2">
                {data.number + 1} / {data.totalPages === 0 ? 1 : data.totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={data.number >= data.totalPages - 1}
                className="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface hover:bg-surface-container-highest transition-colors disabled:opacity-50"
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
