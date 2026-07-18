import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import {
  getTickets,
  createTicket,
  TicketSummary,
  TicketCategory,
  TicketPriority,
} from "../api/ticketApi";
import { getCustomers, CustomerResponse } from "../api/customerApi";

const CATEGORY_LABELS: Record<TicketCategory, string> = {
  COMPLAINT: "Şikayet",
  REQUEST: "Talep",
  FAULT: "Arıza",
};

const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: "Düşük",
  MEDIUM: "Orta",
  HIGH: "Yüksek",
  URGENT: "Acil",
};

const PRIORITY_STRIPES: Record<TicketPriority, string> = {
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

const EMPTY_FORM = {
  customerId: "",
  category: "COMPLAINT" as TicketCategory,
  priority: "MEDIUM" as TicketPriority,
  description: "",
};

export default function Tickets() {
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const [showForm, setShowForm] = useState(false);
  const [customers, setCustomers] = useState<CustomerResponse[]>([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const loadTickets = () => {
    setLoading(true);
    setError(false);
    getTickets(statusFilter)
      .then((page) => setTickets(page.content))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  };

  useEffect(loadTickets, [statusFilter]);

  const openForm = () => {
    setForm(EMPTY_FORM);
    setShowForm(true);
    if (customers.length === 0) {
      getCustomers(0, 100).then((page) => setCustomers(page.content)).catch(() => {});
    }
  };

  const handleCreate = async () => {
    if (!form.customerId || !form.description.trim()) return;
    setSaving(true);
    try {
      await createTicket({ ...form, description: form.description.trim() });
      setShowForm(false);
      loadTickets();
    } catch {
      alert("Ticket oluşturulamadı.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex justify-between items-end">
        <div>
          <h2 className="font-h1 text-on-surface">Ticketlar</h2>
          <p className="font-body-sm text-on-surface-variant mt-1">Destek talepleri kuyruğu yönetimi</p>
        </div>
        <button onClick={openForm}
          className="bg-primary text-on-primary font-label-md px-gutter py-2 rounded flex items-center gap-2 hover:bg-primary/90 transition-colors">
          <span className="material-symbols-outlined text-[18px]">add</span>
          Yeni Ticket
        </button>
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
              <label className="flex items-center gap-3 cursor-pointer">
                <input
                  type="radio"
                  name="status"
                  checked={statusFilter === "RESOLVED"}
                  onChange={() => setStatusFilter("RESOLVED")}
                  className="text-primary focus:ring-primary"
                />
                <span className="font-body-md text-on-surface">Çözüldü</span>
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
              <Link
                to={`/support/${ticket.id}`}
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
              </Link>
            );
          })}
        </div>
      </div>

      {showForm && (
        <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-lg w-full shadow-lg flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-outline-variant pb-2">
              <h3 className="font-h3 text-on-surface">Yeni Ticket</h3>
              <button onClick={() => setShowForm(false)} className="text-secondary hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex flex-col gap-3">
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Müşteri</label>
                <select value={form.customerId} onChange={(e) => setForm((p) => ({ ...p, customerId: e.target.value }))}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface font-body-sm focus:border-primary outline-none">
                  <option value="">Müşteri seçin</option>
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>{c.firstName} {c.lastName} ({c.customerNo})</option>
                  ))}
                </select>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-label-sm text-secondary mb-1 block">Kategori</label>
                  <select value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value as TicketCategory }))}
                    className="w-full border border-outline-variant rounded px-3 py-2 bg-surface font-body-sm focus:border-primary outline-none">
                    {(Object.keys(CATEGORY_LABELS) as TicketCategory[]).map((c) => (
                      <option key={c} value={c}>{CATEGORY_LABELS[c]}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="font-label-sm text-secondary mb-1 block">Öncelik</label>
                  <select value={form.priority} onChange={(e) => setForm((p) => ({ ...p, priority: e.target.value as TicketPriority }))}
                    className="w-full border border-outline-variant rounded px-3 py-2 bg-surface font-body-sm focus:border-primary outline-none">
                    {(Object.keys(PRIORITY_LABELS) as TicketPriority[]).map((p) => (
                      <option key={p} value={p}>{PRIORITY_LABELS[p]}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Açıklama</label>
                <textarea value={form.description} onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
                  rows={4} placeholder="Sorunu açıklayın..."
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-background font-body-sm focus:border-primary outline-none resize-none"></textarea>
              </div>
            </div>

            <div className="flex gap-2 mt-2">
              <button onClick={() => setShowForm(false)}
                className="flex-1 py-2 border border-outline-variant text-on-surface rounded font-label-md hover:bg-surface-container-low transition-colors">İptal</button>
              <button onClick={handleCreate} disabled={!form.customerId || !form.description.trim() || saving}
                className="flex-1 py-2 bg-primary text-on-primary rounded font-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50">
                <span className="material-symbols-outlined text-[18px]">save</span>
                {saving ? "Kaydediliyor..." : "Oluştur"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
