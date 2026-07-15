import { useEffect, useState } from 'react';
import { billingApi, InvoiceResponse, BillCycleResponse } from '../api/billingApi';
import { formatDate } from '../utils/dateUtils';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Taslak',
  ISSUED: 'Kesildi',
  PAID: 'Ödendi',
  OVERDUE: 'Vadesi Geçti',
  CANCELLED: 'İptal',
};

const STATUS_CLASSES: Record<string, string> = {
  DRAFT: 'bg-on-surface-variant/10 text-on-surface-variant border-on-surface-variant/20',
  ISSUED: 'bg-info-bg text-info border-info/20',
  PAID: 'bg-success-bg text-success border-success/20',
  OVERDUE: 'bg-danger-bg text-danger border-danger/20',
  CANCELLED: 'bg-warning-bg text-warning border-warning/20',
};

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(amount);
}

export default function Billing() {
  const [invoices, setInvoices] = useState<InvoiceResponse[]>([]);
  const [cycles, setCycles] = useState<BillCycleResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState(() => new Date().toISOString().split('T')[0]);
  const [triggerResult, setTriggerResult] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  async function loadData() {
    setLoading(true);
    try {
      const allInvoices = await billingApi.getInvoices();
      setInvoices(allInvoices.content || []);
    } catch {
      setInvoices([]);
    }
    setLoading(false);
  }

  async function handleTriggerRun() {
    setTriggering(true);
    setTriggerResult(null);
    try {
      const result = await billingApi.triggerBillRun(selectedPeriod || undefined);
      setTriggerResult(`${result.generated} fatura başarıyla üretildi.`);
      loadData();
    } catch (err: any) {
      setTriggerResult(err?.response?.data?.message || 'Fatura kesimi başarısız oldu.');
    }
    setTriggering(false);
  }

  const dueCount = invoices.filter(i => i.status === 'OVERDUE').length;
  const paidCount = invoices.filter(i => i.status === 'PAID').length;
  const issuedCount = invoices.filter(i => i.status === 'ISSUED').length;
  const totalRevenue = invoices.filter(i => i.status === 'PAID').reduce((s, i) => s + i.grandTotal, 0);

  return (
    <div className="max-w-[1200px] mx-auto space-y-stack-lg flex flex-col gap-4">
      {/* Finance Tabs */}
      <div className="border-b border-outline-variant mb-2">
        <nav className="flex gap-8">
          <a href="/finance/billing"
            className="py-3 font-label-md border-b-2 border-primary text-primary transition-colors">
            Faturalar
          </a>
          <a href="/finance/payments"
            className="py-3 font-label-md border-b-2 border-transparent text-secondary hover:text-on-surface transition-colors">
            Ödemeler
          </a>
        </nav>
      </div>

      {/* Page Header */}
      <div className="flex justify-between items-end">
        <div>
          <h2 className="font-h1 text-on-surface">Fatura Yönetimi</h2>
          <p className="font-body-md text-on-surface-variant mt-1">Faturaları görüntüleyin, fatura kesimi başlatın.</p>
        </div>
        <div className="flex gap-3">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded bg-info-bg text-info font-label-sm border border-info/20">
            <span className="material-symbols-outlined text-[14px]">admin_panel_settings</span>
            ADMIN_ACCESS
          </span>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-surface border border-outline-variant rounded p-4">
          <div className="font-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Toplam Fatura</div>
          <div className="font-mono-id text-[20px] text-on-surface font-semibold">{invoices.length}</div>
        </div>
        <div className="bg-surface border border-outline-variant rounded p-4">
          <div className="font-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Ödenen</div>
          <div className="font-mono-id text-[20px] text-success font-semibold">{paidCount}</div>
        </div>
        <div className="bg-surface border border-outline-variant rounded p-4">
          <div className="font-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Vadesi Geçen</div>
          <div className="font-mono-id text-[20px] text-danger font-semibold flex items-center gap-1">
            {dueCount}
            {dueCount > 0 && <span className="material-symbols-outlined text-[16px] text-danger">warning</span>}
          </div>
        </div>
        <div className="bg-surface border border-outline-variant rounded p-4">
          <div className="font-label-sm text-on-surface-variant uppercase tracking-wider mb-1">Toplam Tahsilat</div>
          <div className="font-mono-id text-[18px] text-on-surface font-semibold">{formatCurrency(totalRevenue)}</div>
        </div>
      </div>

      {/* Bill Run Action */}
      <div className="bg-surface border border-outline-variant rounded p-5 shadow-sm">
        <h3 className="font-h3 text-on-surface mb-4 flex items-center gap-2">
          <span className="material-symbols-outlined text-[20px] text-primary">play_circle</span>
          Toplu Fatura Kesimi Başlat
        </h3>
        <div className="flex items-end gap-4">
          <div className="flex-1 max-w-xs">
            <label className="font-label-sm text-on-surface-variant mb-1 block">Faturalandırma Dönemi (opsiyonel)</label>
            <input
              type="date"
              value={selectedPeriod}
              onChange={e => setSelectedPeriod(e.target.value)}
              className="w-full h-[40px] px-3 border border-outline-variant rounded-lg font-body-md text-on-surface focus:ring-0 focus:border-primary bg-surface"
            />
          </div>
          <button
            onClick={handleTriggerRun}
            disabled={triggering}
            className="h-[40px] px-6 bg-primary text-on-primary rounded-lg font-label-md hover:bg-primary/90 transition-colors flex items-center gap-2 disabled:opacity-50"
          >
            {triggering ? (
              <>
                <span className="animate-spin material-symbols-outlined text-[18px]">refresh</span>
                İşleniyor...
              </>
            ) : (
              <>
                Kesimi Başlat
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </>
            )}
          </button>
        </div>
        {triggerResult && (
          <p className={`font-body-sm mt-3 ${triggerResult.includes('başarıyla') ? 'text-success' : 'text-danger'}`}>
            {triggerResult}
          </p>
        )}
      </div>

      {/* Invoice History Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex flex-col">
        <div className="px-5 py-4 border-b border-outline-variant flex justify-between items-center bg-background/50">
          <h3 className="font-h3 text-on-surface">Fatura Geçmişi</h3>
        </div>
        {loading ? (
          <div className="p-10 text-center font-body-md text-on-surface-variant">Yükleniyor...</div>
        ) : invoices.length === 0 ? (
          <div className="p-10 text-center font-body-md text-on-surface-variant">Henüz fatura bulunmuyor.</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-surface border-b border-outline-variant h-[40px]">
                  <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Fatura No</th>
                  <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Müşteri</th>
                  <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Dönem</th>
                  <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Vade</th>
                  <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap text-right">Tutar</th>
                  <th className="px-4 font-label-sm text-on-surface-variant whitespace-nowrap">Durum</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {invoices.map(inv => (
                  <tr key={inv.id} className="h-row-height-std hover:bg-surface-container-low transition-colors">
                    <td className="px-4 font-mono-id text-primary">{inv.invoiceNumber}</td>
                    <td className="px-4 font-mono-id text-secondary">{inv.customerNo || inv.customerId.slice(0, 8)}</td>
                    <td className="px-4 font-body-sm text-on-surface-variant">
                      {formatDate(inv.periodStart)} - {formatDate(inv.periodEnd)}
                    </td>
                    <td className="px-4 font-body-sm text-on-surface-variant">{formatDate(inv.dueDate)}</td>
                    <td className="px-4 font-mono-id text-on-surface text-right font-semibold">{formatCurrency(inv.grandTotal)}</td>
                    <td className="px-4">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium border ${STATUS_CLASSES[inv.status] || ''}`}>
                        {STATUS_LABELS[inv.status] || inv.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
