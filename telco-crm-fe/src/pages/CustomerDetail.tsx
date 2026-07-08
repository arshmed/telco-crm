import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { getCustomerById, CustomerResponse } from "../api/customerApi";
import { getUserNotificationHistory, NotificationResponse } from "../api/notificationApi";

export default function CustomerDetail() {
  const { id } = useParams<{ id: string }>(); 
  const [customer, setCustomer] = useState<CustomerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState("Genel Bakış");

  // Notification State
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loadingNotifications, setLoadingNotifications] = useState(false);

  // Notification Send State
  const [isNotifModalOpen, setIsNotifModalOpen] = useState(false);
  const [notifChannel, setNotifChannel] = useState<'SMS' | 'EMAIL' | 'PUSH'>('SMS');
  const [notifTemplate, setNotifTemplate] = useState('CUSTOMER_REGISTERED');
  const [notifMessage, setNotifMessage] = useState('');
  const [sendingNotif, setSendingNotif] = useState(false);

  const fetchNotificationHistory = async () => {
    if (!id) return;
    setLoadingNotifications(true);
    try {
      const notifData = await getUserNotificationHistory(id);
      setNotifications(notifData.content || []);
    } catch (notifErr) {
      console.error("Bildirim geçmişi çekilemedi", notifErr);
    } finally {
      setLoadingNotifications(false);
    }
  };

  const handleSendNotification = async () => {
    if (!id || !notifMessage) return;
    setSendingNotif(true);
    try {
      const { sendNotification } = await import("../api/notificationApi");
      await sendNotification({
        userId: id,
        channel: notifChannel,
        templateCode: notifTemplate,
        payloadJson: JSON.stringify({ message: notifMessage })
      });
      // Başarılı olunca geçmişi güncelle ve modalı kapat
      await fetchNotificationHistory();
      setIsNotifModalOpen(false);
      setNotifMessage('');
    } catch (err) {
      console.error("Bildirim gönderilemedi", err);
      alert("Bildirim gönderilirken bir hata oluştu.");
    } finally {
      setSendingNotif(false);
    }
  };

  const tabs = [
    { id: "Genel Bakış", label: "Genel Bakış" },
    { id: "Abonelikler", label: "Abonelikler", badge: 2 },
    { id: "Siparişler", label: "Siparişler" },
    { id: "Faturalar", label: "Faturalar" },
    { id: "Ödemeler", label: "Ödemeler" },
    { id: "Ticketlar", label: "Ticketlar", badge: 1, badgeColor: "bg-[#ffe8cc] text-[#e8590c]" },
    { id: "Belgeler", label: "Belgeler" },
  ];

  useEffect(() => {
    if (!id) return;
    
    const fetchData = async () => {
      try {
        setLoading(true);
        const data = await getCustomerById(id);
        setCustomer(data);

        // Fetch notifications concurrently
        await fetchNotificationHistory();

      } catch (err: any) {
        if (err.response?.status === 401) {
          setError("Oturum süreniz doldu (401). Lütfen token'ınızı yenileyin.");
        } else {
          setError(err.message || "Müşteri bilgileri yüklenirken bir hata oluştu.");
        }
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, [id]);

  if (loading) {
    return <div className="p-8 text-center text-secondary flex justify-center items-center gap-2"><span className="material-symbols-outlined animate-spin">sync</span> Müşteri detayları yükleniyor...</div>;
  }

  if (error || !customer) {
    return <div className="p-8 text-center text-danger">{error || "Müşteri bulunamadı"}</div>;
  }

  const isCorporate = customer.type === "CORPORATE";
  const fullName = isCorporate ? customer.companyName : `${customer.firstName} ${customer.lastName}`;

  // Helper for notification icons
  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case 'EMAIL': return 'mail';
      case 'SMS': return 'sms';
      case 'PUSH': return 'notifications_active';
      default: return 'notifications';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'SENT': return 'bg-success border-success text-success';
      case 'PENDING': return 'bg-warning border-warning text-warning';
      case 'FAILED': return 'bg-error border-error text-error';
      default: return 'bg-outline-variant border-outline-variant text-secondary';
    }
  };

  return (
    <div className="flex flex-col gap-stack-lg relative max-w-[1440px] mx-auto">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-body-sm text-secondary">
        <Link to="/customers" className="hover:text-primary transition-colors">Müşteriler</Link>
        <span className="material-symbols-outlined text-[16px]">chevron_right</span>
        <span className="text-on-surface">Müşteri Detayı</span>
      </nav>

      {/* Entity Header */}
      <section className="bg-surface border border-outline-variant rounded flex flex-col sm:flex-row items-start sm:items-center justify-between p-stack-lg gap-4 shadow-sm">
        <div className="flex items-center gap-6">
          <div className="w-[72px] h-[72px] rounded bg-[#0b1e3b] text-surface flex items-center justify-center font-h1">
            {isCorporate ? fullName?.substring(0, 2).toUpperCase() : `${customer.firstName?.[0] || ""}${customer.lastName?.[0] || ""}`}
          </div>
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-3">
              <h2 className="font-h1 text-on-surface">{fullName}</h2>
              {customer.status === 'ACTIVE' && (
                <span className="px-2 py-0.5 rounded bg-success-bg text-success font-label-sm border border-success/20">Aktif</span>
              )}
              {customer.status === 'PENDING' && (
                <span className="px-2 py-0.5 rounded bg-warning-bg text-warning font-label-sm border border-warning/20">Beklemede</span>
              )}
              {customer.status === 'REJECTED' && (
                <span className="px-2 py-0.5 rounded bg-danger-bg text-danger font-label-sm border border-danger/20">Reddedildi</span>
              )}
              <span className="px-2 py-0.5 rounded bg-surface-container text-on-surface-variant font-label-sm border border-outline-variant flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">verified_user</span> Onaylı KYC
              </span>
            </div>
            <div className="flex flex-wrap items-center gap-4 text-body-sm text-secondary mt-1">
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[16px]">badge</span>
                <span className="font-mono-id">{customer.identityNumber || "-"}</span>
              </div>
              <div className="w-[1px] h-3 bg-outline-variant"></div>
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[16px]">call</span>
                <span className="font-mono-id">{customer.phone || "-"}</span>
              </div>
              <div className="w-[1px] h-3 bg-outline-variant"></div>
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[16px]">mail</span>
                <span>{customer.email || "-"}</span>
              </div>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <button className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 border border-outline-variant rounded bg-surface text-on-surface font-label-md hover:bg-surface-container-low transition-colors">
            <span className="material-symbols-outlined text-[18px]">edit</span>
            Düzenle
          </button>
          <Link to="/sales" className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 border-none rounded bg-primary text-surface font-label-md hover:bg-[#0033b3] transition-colors shadow-sm">
            <span className="material-symbols-outlined text-[18px]">add</span>
            Yeni İşlem
          </Link>
        </div>
      </section>

      {/* Tabs */}
      <div className="border-b border-outline-variant w-full overflow-x-auto hide-scrollbar">
        <nav className="flex gap-8 min-w-max px-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={clsx(
                "py-3 font-label-md flex items-center gap-2 border-b-2 transition-colors",
                activeTab === tab.id 
                  ? "border-primary text-primary" 
                  : "border-transparent text-secondary hover:text-on-surface"
              )}
            >
              {tab.label}
              {tab.badge && (
                <span className={clsx(
                  "px-1.5 py-0.5 rounded-full font-mono-label",
                  tab.badgeColor ? tab.badgeColor : "bg-surface-container-high text-on-surface-variant"
                )}>
                  {tab.badge}
                </span>
              )}
            </button>
          ))}
        </nav>
      </div>

      {/* Grid Layout (Genel Bakış Content) */}
      {activeTab === "Genel Bakış" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-gutter">
          {/* Left Column */}
          <div className="lg:col-span-8 flex flex-col gap-gutter">
            {/* Identity & Contact Row */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
              <div className="bg-surface border border-outline-variant rounded flex flex-col shadow-sm">
                <div className="px-stack-lg py-stack-md border-b border-outline-variant flex justify-between items-center">
                  <h3 className="font-h3 text-on-surface">Kimlik bilgileri</h3>
                  <button className="text-secondary hover:text-primary transition-colors">
                    <span className="material-symbols-outlined text-[18px]">open_in_new</span>
                  </button>
                </div>
                <div className="p-stack-lg grid grid-cols-2 gap-4">
                  <div>
                    <p className="font-label-sm text-secondary mb-1">Müşteri Tipi</p>
                    <p className="font-body-sm text-on-surface">{isCorporate ? "Kurumsal" : "Bireysel"}</p>
                  </div>
                  <div>
                    <p className="font-label-sm text-secondary mb-1">Müşteri No</p>
                    <p className="font-mono-id text-on-surface">{customer.id.substring(0,8).toUpperCase()}</p>
                  </div>
                  <div>
                    <p className="font-label-sm text-secondary mb-1">TCKN/VKN</p>
                    <p className="font-mono-id text-on-surface">{customer.identityNumber || "-"}</p>
                  </div>
                  <div>
                    <p className="font-label-sm text-secondary mb-1">Doğum/Kuruluş Tarihi</p>
                    <p className="font-mono-id text-on-surface">{customer.dateOfBirth ? new Date(customer.dateOfBirth).toLocaleDateString('tr-TR') : "-"}</p>
                  </div>
                </div>
              </div>

              <div className="bg-surface border border-outline-variant rounded flex flex-col shadow-sm">
                <div className="px-stack-lg py-stack-md border-b border-outline-variant flex justify-between items-center">
                  <h3 className="font-h3 text-on-surface">İletişim</h3>
                  <button className="text-secondary hover:text-primary transition-colors">
                    <span className="material-symbols-outlined text-[18px]">edit</span>
                  </button>
                </div>
                <div className="p-stack-lg flex flex-col gap-4">
                  <div className="flex items-start gap-3">
                    <span className="material-symbols-outlined text-outline mt-0.5 text-[20px]">smartphone</span>
                    <div>
                      <p className="font-body-sm text-on-surface">Mobil (Birincil)</p>
                      <p className="font-mono-id text-secondary mt-0.5">{customer.phone || "-"}</p>
                    </div>
                  </div>
                  <div className="w-full h-[1px] bg-surface-container-highest"></div>
                  <div className="flex items-start gap-3">
                    <span className="material-symbols-outlined text-outline mt-0.5 text-[20px]">mail</span>
                    <div>
                      <p className="font-body-sm text-on-surface">E-posta (Birincil)</p>
                      <p className="font-body-sm text-secondary mt-0.5">{customer.email || "-"}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Adresler */}
            <div className="bg-surface border border-outline-variant rounded flex flex-col shadow-sm">
              <div className="px-stack-lg py-stack-md border-b border-outline-variant flex justify-between items-center">
                <h3 className="font-h3 text-on-surface">Adresler</h3>
                <button className="text-primary font-label-sm hover:underline">Tümünü gör</button>
              </div>
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-surface-container-lowest border-b border-outline-variant">
                    <th className="px-stack-lg py-2 font-label-sm text-secondary w-[20%]">Tip</th>
                    <th className="px-stack-lg py-2 font-label-sm text-secondary w-[60%]">Adres</th>
                    <th className="px-stack-lg py-2 font-label-sm text-secondary w-[20%]">İl / İlçe</th>
                  </tr>
                </thead>
                <tbody className="font-body-sm">
                  {customer.addresses && customer.addresses.length > 0 ? (
                    customer.addresses.map((address) => (
                      <tr key={address.id} className="border-b border-outline-variant h-[44px]">
                        <td className="px-stack-lg py-2">
                          <span className="px-2 py-0.5 rounded bg-surface-container text-on-surface font-label-sm border border-outline-variant">
                            {address.isDefault ? "Varsayılan" : "Adres"}
                          </span>
                        </td>
                        <td className="px-stack-lg py-2 truncate max-w-[200px]">{address.line1}</td>
                        <td className="px-stack-lg py-2 text-secondary">{address.district}, {address.city}</td>
                      </tr>
                    ))
                  ) : (
                    <tr className="border-b border-outline-variant h-[44px]">
                      <td colSpan={3} className="px-stack-lg py-2 text-secondary">Kayıtlı adres bulunmuyor.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Right Column */}
          <div className="lg:col-span-4 flex flex-col gap-gutter">
            <div className="bg-surface border border-outline-variant rounded flex flex-col shadow-sm">
              <div className="px-stack-lg py-stack-md border-b border-outline-variant">
                <h3 className="font-h3 text-on-surface">Finansal Özet</h3>
              </div>
              <div className="p-stack-lg flex flex-col gap-6">
                <div className="flex justify-between items-end">
                  <div>
                    <p className="font-label-sm text-secondary mb-1">Güncel Borç Bakiyesi</p>
                    <div className="flex items-baseline gap-1">
                      <span className="font-h1 text-[28px] text-on-surface font-mono-id">₺0</span>
                      <span className="font-body-md text-secondary font-mono-id">,00</span>
                    </div>
                  </div>
                </div>

                <div className="w-full h-[1px] bg-surface-container-highest"></div>

                <div>
                  <p className="font-label-sm text-secondary mb-3">Aktif Abonelikler</p>
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center justify-between py-1.5">
                      <div className="flex flex-col">
                        <span className="font-body-sm text-on-surface font-medium">Standart Ev Paketi</span>
                        <span className="font-mono-id text-secondary text-[12px]">+90 532 123 45 67</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="font-body-sm text-on-surface font-mono-id">₺249,90</span>
                        <span className="px-1.5 py-0.5 rounded bg-success-bg text-success font-mono-label text-[10px] border border-success/20">Aktif</span>
                      </div>
                    </div>
                    <div className="w-full h-[1px] bg-surface-container-highest"></div>
                    <div className="flex items-center justify-between py-1.5">
                      <div className="flex flex-col">
                        <span className="font-body-sm text-on-surface font-medium">Ek internet paketi</span>
                        <span className="font-mono-id text-secondary text-[12px]">+90 532 123 45 67</span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="font-body-sm text-on-surface font-mono-id">₺49,90</span>
                        <span className="px-1.5 py-0.5 rounded bg-success-bg text-success font-mono-label text-[10px] border border-success/20">Aktif</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Gerçek İletişim / Olay Rayı (Notification Service Integration) */}
            <div className="bg-surface border border-outline-variant rounded flex flex-col flex-1 shadow-sm">
              <div className="px-stack-lg py-stack-md border-b border-outline-variant flex justify-between items-center">
                <h3 className="font-h3 text-on-surface">İletişim Geçmişi</h3>
                <button className="text-primary font-label-sm hover:underline">Tümü</button>
              </div>
              
              <div className="p-stack-lg flex flex-col gap-0 relative overflow-y-auto max-h-[400px]">
                {loadingNotifications ? (
                  <div className="text-center text-secondary font-body-sm py-4">Bildirimler yükleniyor...</div>
                ) : notifications.length > 0 ? (
                  <>
                    <div className="absolute left-[39px] top-[24px] bottom-[24px] w-[1px] bg-surface-container-highest z-0"></div>
                    
                    {notifications.map((notif) => (
                      <div key={notif.id} className="flex gap-4 relative z-10 pb-6 group">
                        <div className={clsx(
                          "w-8 h-8 rounded-full border flex items-center justify-center shrink-0 mt-0.5 bg-surface",
                          getStatusColor(notif.status)
                        )}>
                          <span className="material-symbols-outlined text-[16px]">
                            {getChannelIcon(notif.channel)}
                          </span>
                        </div>
                        <div className="flex flex-col flex-1">
                          <div className="flex justify-between items-start">
                            <p className="font-body-sm text-on-surface font-medium">{notif.subject || notif.templateCode}</p>
                            <span className="font-mono-label text-outline whitespace-nowrap ml-2">
                              {new Date(notif.sentAt || notif.createdAt).toLocaleDateString('tr-TR', { day:'numeric', month:'short', hour:'2-digit', minute:'2-digit' })}
                            </span>
                          </div>
                          <p className="font-body-sm text-secondary mt-0.5 line-clamp-2">{notif.body}</p>
                          <div className="flex gap-2 mt-1">
                            <span className="font-mono-label text-[10px] text-outline bg-surface-container-low px-1.5 py-0.5 rounded border border-outline-variant">{notif.channel}</span>
                            {notif.status === 'FAILED' && <span className="font-mono-label text-[10px] text-error bg-error-container px-1.5 py-0.5 rounded border border-error/20">BAŞARISIZ</span>}
                          </div>
                        </div>
                      </div>
                    ))}
                  </>
                ) : (
                  <div className="text-center text-secondary font-body-sm py-8">
                    <span className="material-symbols-outlined block text-[32px] text-surface-container-highest mb-2">notifications_off</span>
                    Müşteriye henüz hiçbir bildirim gönderilmemiş.
                  </div>
                )}
              </div>
              <div className="p-4 border-t border-outline-variant bg-surface-container-lowest text-center">
                <button 
                  onClick={() => setIsNotifModalOpen(true)}
                  className="text-primary hover:bg-primary-container/20 font-label-md px-4 py-1.5 rounded transition-colors flex items-center justify-center gap-2 w-full"
                >
                  <span className="material-symbols-outlined text-[18px]">send</span>
                  Yeni Bildirim Gönder
                </button>
              </div>
            </div>
            
          </div>
        </div>
      )}

      {/* Notification Send Modal */}
      {isNotifModalOpen && (
        <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-md w-full shadow-[0_12px_32px_rgba(11,30,59,0.1)] flex flex-col gap-4">
            <div className="flex justify-between items-center border-b border-outline-variant pb-2">
              <h3 className="font-h3 text-on-surface">Yeni Bildirim Gönder</h3>
              <button onClick={() => setIsNotifModalOpen(false)} className="text-secondary hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            
            <div className="flex flex-col gap-3">
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Kanal</label>
                <div className="flex gap-2">
                  {(['SMS', 'EMAIL', 'PUSH'] as const).map(channel => (
                    <button
                      key={channel}
                      onClick={() => setNotifChannel(channel)}
                      className={clsx(
                        "flex-1 py-1.5 rounded font-label-md border transition-colors",
                        notifChannel === channel ? "border-primary bg-primary-container text-primary" : "border-outline-variant text-secondary hover:bg-surface-container-low"
                      )}
                    >
                      {channel}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="font-label-sm text-secondary mb-1 block">Şablon</label>
                <select 
                  value={notifTemplate}
                  onChange={(e) => setNotifTemplate(e.target.value)}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none"
                >
                  <option value="CUSTOMER_REGISTERED">Müşteri Kaydı (CUSTOMER_REGISTERED)</option>
                  <option value="CUSTOMER_KYC_APPROVED">KYC Onaylandı (CUSTOMER_KYC_APPROVED)</option>
                  <option value="CUSTOMER_KYC_REJECTED">KYC Reddedildi (CUSTOMER_KYC_REJECTED)</option>
                  <option value="CUSTOMER_UPDATED">Müşteri Güncellendi (CUSTOMER_UPDATED)</option>
                  <option value="ORDER_CREATED">Sipariş Oluşturuldu (ORDER_CREATED)</option>
                  <option value="ORDER_CONFIRMED">Sipariş Onaylandı (ORDER_CONFIRMED)</option>
                  <option value="ORDER_CANCELLED">Sipariş İptal Edildi (ORDER_CANCELLED)</option>
                  <option value="QUOTA_THRESHOLD_REACHED">Kota Sınırı Uyarısı (QUOTA_THRESHOLD_REACHED)</option>
                  <option value="QUOTA_EXCEEDED">Kota Aşımı (QUOTA_EXCEEDED)</option>
                </select>
              </div>

              <div>
                <label className="font-label-sm text-secondary mb-1 block">Mesaj (JSON / Text)</label>
                <textarea 
                  value={notifMessage}
                  onChange={(e) => setNotifMessage(e.target.value)}
                  rows={4}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none resize-none"
                  placeholder="Gönderilecek bildirim içeriğini girin..."
                />
              </div>
            </div>

            <div className="flex gap-2 mt-2">
              <button 
                onClick={() => setIsNotifModalOpen(false)} 
                className="flex-1 py-2 border border-outline-variant text-on-surface rounded font-label-md hover:bg-surface-container-low transition-colors"
              >
                İptal
              </button>
              <button 
                onClick={handleSendNotification}
                disabled={sendingNotif || !notifMessage}
                className="flex-1 py-2 bg-primary text-surface rounded font-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {sendingNotif ? <span className="material-symbols-outlined animate-spin text-[18px]">sync</span> : <span className="material-symbols-outlined text-[18px]">send</span>}
                {sendingNotif ? "Gönderiliyor..." : "Gönder"}
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
