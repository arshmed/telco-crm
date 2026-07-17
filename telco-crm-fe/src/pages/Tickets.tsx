import { useState, useEffect } from "react";
import { getTickets, TicketSummary } from "../api/ticketApi";

const CATEGORY_LABELS: Record<TicketSummary["category"], string> = {
  COMPLAINT: "Şikayet",
  REQUEST: "Talep",
  FAULT: "Arıza",
};

const PRIORITY_STRIPES: Record<TicketSummary["priority"], string> = {
  URGENT: "bg-error",
  HIGH: "bg-error",
  MEDIUM: "bg-tertiary-container",
  LOW: "bg-outline-variant",
};

function formatSlaRemaining(slaDueAt: string, status: TicketSummary["status"]) {
  if (status === "RESOLVED") return { text: "Çözüldü", tone: "text-success bg-success-bg" };

  const diffMs = new Date(slaDueAt).getTime() - Date.now();
  if (diffMs <= 0) return { text: "SLA aşıldı", tone: "text-danger bg-danger-bg" };

  const hours = Math.floor(diffMs / 3_600_000);
  const minutes = Math.floor((diffMs % 3_600_000) / 60_000);
  const text = hours > 0 ? `${hours} sa ${minutes} dk kaldı` : `${minutes} dk kaldı`;
  return { text, tone: "text-warning bg-warning-bg" };
}

export default function Tickets() {
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError(false);
    getTickets(statusFilter)
      .then((page) => setTickets(page.content))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [statusFilter]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-between items-end">
        <div>
          <h2 className="font-h1 text-on-surface">Ticketlar</h2>
          <p className="font-body-sm text-on-surface-variant mt-1">Destek talepleri kuyruğu yönetimi</p>
        </div>
      </div>

      <div className="flex flex-col lg:flex-row gap-6 items-start">
        <aside className="w-full lg:w-[240px] shrink-0 flex flex-col gap-6 bg-surface border border-outline-variant rounded-lg p-4">
          <div>
            <h3 className="font-label-sm text-secondary mb-3 uppercase tracking-wider">Durum</h3>
            <div className="flex flex-col gap-2">
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="radio"
                  name="status"
                  checked={statusFilter === undefined}
                  onChange={() => setStatusFilter(undefined)}
                  className="text-primary focus:ring-primary"
                />
                <span className="font-body-md text-on-surface">Tümü</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="radio"
                  name="status"
                  checked={statusFilter === "ASSIGNED"}
                  onChange={() => setStatusFilter("ASSIGNED")}
                  className="text-primary focus:ring-primary"
                />
                <span className="font-body-md text-on-surface">Açık</span>
              </label>
            </div>
          </div>
        </aside>

        <div className="flex-1 flex flex-col gap-3 w-full">
          {loading && <p className="font-body-md text-on-surface-variant p-4">Yükleniyor...</p>}

          {error && !loading && (
            <p className="font-body-md text-danger p-4">Ticketlar yüklenemedi.</p>
          )}

          {!loading && !error && tickets.length === 0 && (
            <p className="font-body-md text-on-surface-variant p-4">Ticket bulunamadı.</p>
          )}

          {!loading && !error && tickets.map((ticket) => {
            const sla = formatSlaRemaining(ticket.slaDueAt, ticket.status);
            return (
              <div
                key={ticket.id}
                className="bg-surface border border-outline-variant rounded-lg flex relative overflow-hidden hover:border-primary-fixed-dim transition-colors group"
              >
                <div className={`w-1 absolute left-0 top-0 bottom-0 ${PRIORITY_STRIPES[ticket.priority]}`}></div>
                <div className="p-4 pl-5 w-full flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div className="flex flex-col gap-1 flex-1 min-w-0">
                    <div className="flex items-center gap-3">
                      <span className="font-mono-id text-secondary">#{ticket.id.slice(0, 8)}</span>
                      <span className="bg-surface-container-high text-on-surface-variant font-label-sm text-[10px] px-2 py-0.5 rounded-full uppercase">
                        {CATEGORY_LABELS[ticket.category]}
                      </span>
                    </div>
                    <h4 className="font-body-md font-semibold text-on-surface line-clamp-1">
                      {ticket.description}
                    </h4>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="material-symbols-outlined text-[16px] text-outline">person</span>
                      <span className="font-body-sm text-on-surface-variant">{ticket.customerName}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-6 sm:justify-end shrink-0">
                    <div className="flex flex-col items-end">
                      <span className="font-label-sm text-[10px] text-secondary uppercase">SLA</span>
                      <div className={`flex items-center gap-1 px-2 py-1 rounded ${sla.tone}`}>
                        <span className="material-symbols-outlined text-[14px]">timer</span>
                        <span className="font-mono-id text-[12px] font-medium">{sla.text}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
