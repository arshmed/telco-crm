import { useState, useEffect } from "react";
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { getSubscriptionsByStatus, getSubscriptions, getMonthlyActivations, getTariffDistribution, MonthlyActivation, TariffDistribution } from "../api/subscriptionApi";
import { billingApi, InvoiceStats } from "../api/billingApi";
import { getRecentNotifications, NotificationResponse } from "../api/notificationApi";
import { formatDateTime } from "../utils/dateUtils";

const MONTH_LABELS: Record<string, string> = {
  "01": "Oca", "02": "Şub", "03": "Mar", "04": "Nis", "05": "May", "06": "Haz",
  "07": "Tem", "08": "Ağu", "09": "Eyl", "10": "Eki", "11": "Kas", "12": "Ara",
};

function formatCurrency(amount: number) {
  return new Intl.NumberFormat("tr-TR", { style: "currency", currency: "TRY" }).format(amount);
}

const EVENT_ICONS: Record<string, { icon: string; color: string }> = {
  "ORDER_CREATED": { icon: "shopping_cart", color: "text-info" },
  "PAYMENT_COMPLETED": { icon: "check_circle", color: "text-success" },
  "PAYMENT_FAILED": { icon: "error", color: "text-danger" },
  "SUBSCRIPTION_ACTIVATED": { icon: "sim_card", color: "text-success" },
  "CUSTOMER_REGISTERED": { icon: "person_add", color: "text-primary" },
};

export default function Overview() {
  const [activeSubCount, setActiveSubCount] = useState<number | null>(null);
  const [newActivations, setNewActivations] = useState<number | null>(null);
  const [overdueCount, setOverdueCount] = useState<number | null>(null);
  const [monthlyData, setMonthlyData] = useState<{ month: string; subscribers: number }[]>([]);
  const [tariffData, setTariffData] = useState<{ name: string; value: number }[]>([]);
  const [recentEvents, setRecentEvents] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchKpis() {
      setLoading(true);
      try {
        const [activeSubs, monthly, tarifs, overdue, events] = await Promise.allSettled([
          getSubscriptionsByStatus("ACTIVE", 0, 1),
          getMonthlyActivations(7),
          getTariffDistribution(),
          billingApi.getInvoiceStats(),
          getRecentNotifications(),
        ]);

        if (activeSubs.status === "fulfilled") setActiveSubCount(activeSubs.value.totalElements);

        if (monthly.status === "fulfilled") {
          setMonthlyData(monthly.value.map((m) => ({
            month: MONTH_LABELS[m.month.substring(5)] || m.month,
            subscribers: m.count,
          })));
        }

        if (tarifs.status === "fulfilled") {
          setTariffData(tarifs.value.map((t) => ({
            name: t.tariffCode,
            value: t.count,
          })));
        }

        if (overdue.status === "fulfilled") setOverdueCount(overdue.value.overdueCount);

        if (events.status === "fulfilled") setRecentEvents(events.value);

        // New activations: count from subscriptions created this month
        const allSubs = await getSubscriptions(0, 100).catch(() => null);
        if (allSubs) {
          const now = new Date();
          const thisMonth = allSubs.content.filter((s) => {
            if (!s.activatedAt) return false;
            const d = new Date(s.activatedAt);
            return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth();
          });
          setNewActivations(thisMonth.length);
        }
      } catch {
        // APIs failed
      } finally {
        setLoading(false);
      }
    }
    fetchKpis();
  }, []);

  return (
    <div className="max-w-[1440px] mx-auto flex flex-col gap-stack-lg">
      <div className="flex justify-between items-end mb-stack-lg">
        <h1 className="font-h1 text-on-surface">Genel Bakış</h1>
      </div>

      {/* KPI Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-gutter mb-stack-lg">
        <div className="bg-surface border border-outline-variant rounded p-gutter flex flex-col justify-between h-full shadow-sm hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start mb-2">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider">Aktif abone</h3>
            <span className="material-symbols-outlined text-outline text-[20px]">group</span>
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="font-h1 text-[32px] text-on-surface">
              {loading ? <span className="text-on-surface-variant">...</span> : activeSubCount?.toLocaleString("tr-TR") ?? "-"}
            </span>
          </div>
        </div>

        <div className="bg-surface border border-outline-variant rounded p-gutter flex flex-col justify-between h-full shadow-sm hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start mb-2">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider">Yeni aktivasyon</h3>
            <span className="material-symbols-outlined text-outline text-[20px]">person_add</span>
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="font-h1 text-[32px] text-on-surface">
              {loading ? <span className="text-on-surface-variant">...</span> : newActivations?.toLocaleString("tr-TR") ?? "-"}
            </span>
            {newActivations !== null && newActivations > 0 && (
              <span className="font-label-sm text-success bg-success-bg px-1.5 py-0.5 rounded flex items-center">
                <span className="material-symbols-outlined text-[14px] mr-0.5">arrow_upward</span>
                bu ay
              </span>
            )}
          </div>
        </div>

        <div className="bg-surface border border-outline-variant rounded p-gutter flex flex-col justify-between h-full shadow-sm hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start mb-2">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider">Vadesi geçmiş fatura</h3>
            <span className="material-symbols-outlined text-danger text-[20px]">receipt_long</span>
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="font-h1 text-[32px] text-danger">
              {loading ? <span className="text-on-surface-variant">...</span> : overdueCount?.toLocaleString("tr-TR") ?? "-"}
            </span>
            <span className="font-body-sm text-secondary">adet</span>
          </div>
        </div>

        <div className="bg-surface border border-outline-variant rounded p-gutter flex flex-col justify-between h-full shadow-sm hover:shadow-md transition-shadow">
          <div className="flex justify-between items-start mb-2">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider">Açık ticket</h3>
            <span className="material-symbols-outlined text-outline text-[20px]">support_agent</span>
          </div>
          <div className="flex flex-col">
            <span className="font-h1 text-[32px] text-on-surface">24</span>
            <span className="font-body-sm text-danger mt-1">3'ü SLA riskinde</span>
          </div>
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-gutter mb-stack-lg">
        <div className="bg-surface border border-outline-variant rounded p-gutter flex flex-col">
          <h3 className="font-h3 text-on-surface mb-3">Abone Büyümesi</h3>
          <div className="flex-1">
            {loading ? (
              <div className="flex items-center justify-center h-[260px] text-on-surface-variant">Yükleniyor...</div>
            ) : monthlyData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <LineChart data={monthlyData} margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#c4c5d9" />
                  <XAxis dataKey="month" tick={{ fontSize: 12, fill: "#434656" }} axisLine={{ stroke: "#c4c5d9" }} />
                  <YAxis tick={{ fontSize: 12, fill: "#434656" }} axisLine={{ stroke: "#c4c5d9" }} tickFormatter={(v) => `${v}`} />
                  <Tooltip contentStyle={{ borderRadius: "0.25rem", border: "1px solid #c4c5d9", fontSize: "13px" }} />
                  <Line type="monotone" dataKey="subscribers" stroke="#0040e0" strokeWidth={2} dot={{ fill: "#0040e0", r: 3 }} activeDot={{ r: 5 }} />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-[260px] text-on-surface-variant">Veri yok</div>
            )}
          </div>
        </div>

        <div className="bg-surface border border-outline-variant rounded p-gutter flex flex-col">
          <h3 className="font-h3 text-on-surface mb-3">Paket Dağılımı</h3>
          <div className="flex-1">
            {loading ? (
              <div className="flex items-center justify-center h-[260px] text-on-surface-variant">Yükleniyor...</div>
            ) : tariffData.length > 0 ? (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={tariffData} layout="vertical" margin={{ top: 4, right: 8, left: -16, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#c4c5d9" horizontal={false} />
                  <XAxis type="number" tick={{ fontSize: 12, fill: "#434656" }} axisLine={{ stroke: "#c4c5d9" }} />
                  <YAxis type="category" dataKey="name" tick={{ fontSize: 12, fill: "#434656" }} axisLine={false} width={120} />
                  <Tooltip contentStyle={{ borderRadius: "0.25rem", border: "1px solid #c4c5d9", fontSize: "13px" }} />
                  <Bar dataKey="value" fill="#2e5bff" radius={[0, 2, 2, 0]} barSize={20} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-[260px] text-on-surface-variant">Veri yok</div>
            )}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-gutter">
        <div className="bg-surface border border-outline-variant rounded flex flex-col h-96">
          <div className="p-gutter border-b border-outline-variant">
            <h3 className="font-h3 text-on-surface">Son olaylar</h3>
          </div>
          <div className="overflow-y-auto flex-1 p-0">
            {loading ? (
              <div className="p-8 text-center text-on-surface-variant">Yükleniyor...</div>
            ) : recentEvents.length > 0 ? (
              <ul className="divide-y divide-outline-variant">
                {recentEvents.map((event) => {
                  const evt = EVENT_ICONS[event.templateCode] || { icon: "info", color: "text-outline" };
                  return (
                    <li key={event.id} className="px-gutter py-3 flex items-start space-x-3 hover:bg-surface-container-low transition-colors">
                      <span className={`material-symbols-outlined ${evt.color} text-[20px] mt-0.5`}>{evt.icon}</span>
                      <div className="flex-1 min-w-0">
                        <div className="flex justify-between">
                          <p className="font-mono-id text-on-surface truncate">{event.templateCode}</p>
                          <span className="font-body-sm text-secondary whitespace-nowrap">{formatDateTime(event.createdAt)}</span>
                        </div>
                        <p className="font-body-sm text-on-surface mt-0.5 truncate">{event.subject || event.body}</p>
                      </div>
                    </li>
                  );
                })}
              </ul>
            ) : (
              <div className="p-8 text-center text-on-surface-variant">Henüz olay yok</div>
            )}
          </div>
        </div>

        <div className="bg-surface border border-outline-variant rounded flex flex-col h-96">
          <div className="p-gutter border-b border-outline-variant flex justify-between items-center">
            <h3 className="font-h3 text-on-surface">SLA'sı yaklaşan ticketlar</h3>
            <a href="#" className="font-label-sm text-primary hover:underline">Tümünü gör</a>
          </div>
          <div className="overflow-x-auto flex-1">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-background border-b border-outline-variant">
                  <th className="p-2 pl-gutter font-label-sm text-secondary font-medium w-24">Ticket ID</th>
                  <th className="p-2 font-label-sm text-secondary font-medium">Müşteri</th>
                  <th className="p-2 font-label-sm text-secondary font-medium text-right pr-gutter">Kalan Süre</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                <tr className="hover:bg-surface-container-low transition-colors h-[44px]">
                  <td className="p-2 pl-gutter font-mono-id text-on-surface">TCK-410</td>
                  <td className="p-2 font-body-sm text-on-surface truncate max-w-[150px]">Ali Vefa</td>
                  <td className="p-2 pr-gutter text-right">
                    <span className="font-label-sm text-danger bg-danger-bg px-2 py-0.5 rounded">15 dk</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
