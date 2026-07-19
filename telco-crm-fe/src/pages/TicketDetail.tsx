import { useState, useEffect, useCallback } from "react";
import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { useAuth } from "../context/AuthContext";
import { ROLES } from "../constants/roles";

export default function TicketDetail() {
  const { hasRole } = useAuth();
  const canManageTicket = hasRole(ROLES.CALL_CENTER_AGENT);
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: "system",
      sender: "Sistem Tanılama",
      time: "24 Eki, 14:31",
      body: "OTOMATİK KONTROL BAŞARILI\nHatta port hatası tespit edilmedi. Bölgesel genel arıza kaydı bulunmuyor. CPE (Modem) tarafında son 24 saatte 42 adet PPPoE düşmesi raporlandı. Kayıt L1 Destek ekibine otomatik yönlendirildi."
    },
    {
      id: 2,
      type: "agent-public",
      sender: "Ayşe Bozdağ",
      role: "L1 Destek Uzmanı",
      time: "24 Eki, 15:10",
      body: "Mehmet Bey merhaba, yaşadığınız sorun için üzgünüz. Hattınızı sistem üzerinden güncelledim ve portunuzu resetledim. Modeminizi 10 dakika kapalı tutup tekrar açmanızı rica edeceğim. Sorun devam ederse saha ekibimizi adresinize yönlendireceğim."
    },
    {
      id: 3,
      type: "agent-internal",
      sender: "Ayşe Bozdağ",
      time: "24 Eki, 15:12",
      body: "Müşteri port değerlerinde SNR marjı çok düşük görünüyor (6dB sınırında). Büyük ihtimalle iç tesisat veya ankastre kutusunda oksitlenme var. Müşteri dönüşüne göre direkt saha ekibine (L2) paslayacağım, L1'de çözülecek bir durum değil."
    },
    {
      id: 4,
      type: "customer",
      sender: "Mehmet Yılmaz",
      time: "24 Eki, 16:45",
      body: "Ayşe Hanım dediğinizi yaptım ancak kopmalar devam ediyor. Lütfen teknik ekip yönlendirin."
    }
  ]);

  const [replyText, setReplyText] = useState("");
  const [sending, setSending] = useState(false);

  const [actionLoading, setActionLoading] = useState(false);
  const [showAssign, setShowAssign] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState("");
  const [showResolve, setShowResolve] = useState(false);
  const [resolutionText, setResolutionText] = useState("");

  const reload = useCallback(() => {
    if (!id) return;
    setLoading(true);
    setError(false);
    getTicketById(id)
      .then((data) => {
        setTicket(data);
        getCustomerById(data.customerId).then(setCustomer).catch(() => setCustomer(null));
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    reload();
  }, [reload]);

  const handleSendReply = async () => {
    if (!id || !replyText.trim()) return;
    setSending(true);
    try {
      await addComment(id, replyText.trim());
      setReplyText("");
      reload();
    } catch {
      alert("Yorum eklenemedi.");
    } finally {
      setSending(false);
    }
  };

  const handleAssign = async () => {
    if (!id || !selectedTeam) return;
    setActionLoading(true);
    try {
      const updated = await assignTicket(id, selectedTeam);
      setTicket(updated);
      setShowAssign(false);
      setSelectedTeam("");
    } catch {
      alert("Ekip ataması yapılamadı.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleResolve = async () => {
    if (!id || !resolutionText.trim()) return;
    setActionLoading(true);
    try {
      const updated = await resolveTicket(id, resolutionText.trim());
      setTicket(updated);
      setShowResolve(false);
      setResolutionText("");
    } catch {
      alert("Ticket kapatılamadı.");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading && !ticket) {
    return <div className="p-8 text-center text-secondary">Yükleniyor...</div>;
  }

  if (error || !ticket) {
    return (
      <div className="p-8 text-center text-danger">
        Ticket yüklenemedi. <Link to="/support" className="text-primary hover:underline">Listeye dön</Link>
      </div>
    );
  }

  const resolved = ticket.status === "RESOLVED";
  const sla = slaState(ticket.slaDueAt, resolved);
  const customerName = customer ? `${customer.firstName} ${customer.lastName}` : ticket.customerId.slice(0, 8);

  return (
    <div className="flex-1 overflow-y-auto bg-background p-container-padding flex flex-col gap-6">
      <div className="mb-stack-lg">
        <div className="flex items-center gap-2 font-body-sm text-secondary mb-1">
          <Link to="/support" className="hover:text-primary transition-colors">Destek</Link>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="text-on-surface-variant">{CATEGORY_LABELS[ticket.category]}</span>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="font-mono-id text-on-surface-variant">#{ticket.id.slice(0, 8)}</span>
        </div>
        <h2 className="font-h1 text-on-surface line-clamp-2">{ticket.description}</h2>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[1fr_320px] gap-gutter items-start">
        <div className="flex flex-col gap-gutter">
          <div className="bg-surface border border-outline-variant rounded overflow-hidden">
            <div className="p-6">
              <div className="flex items-center gap-6 mb-6 pb-4 border-b border-surface-variant flex-wrap">
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Oluşturan</span>
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center font-bold text-[10px]">
                      {initials(customerName)}
                    </div>
                    <span className="font-body-md text-on-surface">{customerName}</span>
                  </div>
                </div>
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Tarih</span>
                  <span className="font-body-md text-on-surface">{formatDateTime(ticket.createdAt)}</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Kategori</span>
                  <span className="font-body-md text-on-surface">{CATEGORY_LABELS[ticket.category]}</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Öncelik</span>
                  <span className="font-body-md text-on-surface">{PRIORITY_LABELS[ticket.priority]}</span>
                </div>
              </div>

              <div className="font-body-lg text-on-surface leading-relaxed whitespace-pre-line">
                {ticket.description}
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-4 mb-4">
            <h3 className="font-h3 text-on-surface px-1">İletişim Geçmişi</h3>

            {ticket.comments.length === 0 && (
              <p className="font-body-sm text-on-surface-variant px-1">Henüz yorum yok.</p>
            )}

            {ticket.comments.map((comment, idx) => (
              <div key={comment.id} className="flex gap-4">
                <div className="w-8 flex flex-col items-center shrink-0">
                  <div className="w-8 h-8 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-bold text-[10px]">
                    {initials(comment.authorId)}
                  </div>
                  {idx !== ticket.comments.length - 1 && <div className="w-px h-full bg-surface-variant mt-2"></div>}
                </div>
                <div className="flex-1 pb-4">
                  <div className="flex items-baseline gap-2 mb-1">
                    <span className="font-label-md text-on-surface">{comment.authorId}</span>
                    <span className="font-body-sm text-secondary">{formatDateTime(comment.createdAt)}</span>
                  </div>
                  <div className="rounded p-4 font-body-md text-on-surface whitespace-pre-line bg-surface border border-outline-variant">
                    {comment.body}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {!resolved && (
            <div className="bg-surface border border-outline-variant rounded p-4 sticky bottom-4 z-10 shadow-[0_-4px_24px_rgba(11,30,59,0.05)]">
              <textarea
                value={replyText}
                onChange={(e) => setReplyText(e.target.value)}
                className="w-full bg-background border border-outline-variant rounded p-3 font-body-md text-on-surface focus:border-primary focus:ring-0 resize-none mb-3"
                placeholder="Yanıtınızı buraya yazın..."
                rows={3}
              ></textarea>
              <div className="flex justify-end">
                <button
                  onClick={handleSendReply}
                  disabled={!replyText.trim() || sending}
                  className="bg-primary hover:bg-[#0035be] text-on-primary font-label-md px-6 py-2 rounded transition-colors cursor-pointer flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-[18px]">send</span>
                  {sending ? "Gönderiliyor..." : "Gönder"}
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="w-full xl:w-[320px] flex flex-col gap-gutter shrink-0">
          <div className="bg-surface border border-outline-variant rounded p-5">
            <div className="flex justify-between items-center mb-4">
              <span className="font-label-sm text-secondary">Mevcut Durum</span>
              <div
                className={clsx(
                  "font-label-sm px-2.5 py-1 rounded flex items-center gap-1",
                  resolved ? "bg-success-bg text-success" : "bg-secondary-container text-on-secondary-fixed"
                )}
              >
                <span className="material-symbols-outlined text-[14px]">{resolved ? "check_circle" : "hourglass_empty"}</span>
                {resolved ? "ÇÖZÜLDÜ" : "AÇIK"}
              </div>
            </div>
            <div className="font-body-sm text-on-surface-variant mb-6 pb-4 border-b border-surface-variant">
              Saha ekibi ataması bekleniyor. Müşteri yanıtı alındı.
            </div>
            {canManageTicket && (
              <div className="flex flex-col gap-2">
                <button className="w-full bg-primary hover:bg-[#0035be] text-on-primary font-label-md py-2 rounded transition-colors cursor-pointer">
                  Üstlen
                </button>
                <button className="w-full bg-surface border border-primary text-primary hover:bg-primary-fixed font-label-md py-2 rounded transition-colors cursor-pointer">
                  Saha Ekibine Ata
                </button>
                <button className="w-full mt-2 text-secondary hover:text-on-surface font-label-sm py-1 transition-colors cursor-pointer text-center">
                  Çözüldü Olarak Kapat
                </button>
              </div>
            )}
          </div>

            {resolved ? (
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-secondary">Çözüm</span>
                <p className="font-body-sm text-on-surface whitespace-pre-line">{ticket.resolution}</p>
                {ticket.resolvedAt && (
                  <span className="font-mono-label text-outline mt-1">{formatDateTime(ticket.resolvedAt)}</span>
                )}
              </div>
            ) : showAssign ? (
              <div className="flex flex-col gap-2">
                <select
                  value={selectedTeam}
                  onChange={(e) => setSelectedTeam(e.target.value)}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface font-body-sm focus:border-primary outline-none"
                >
                  <option value="">Ekip seçin</option>
                  {TEAMS.map((team) => (
                    <option key={team} value={team}>{team}</option>
                  ))}
                </select>
                <div className="flex gap-2">
                  <button onClick={handleAssign} disabled={!selectedTeam || actionLoading}
                    className="flex-1 py-2 bg-primary text-on-primary font-label-sm rounded hover:bg-[#0035be] transition-colors disabled:opacity-50">Onayla</button>
                  <button onClick={() => { setShowAssign(false); setSelectedTeam(""); }}
                    className="flex-1 py-2 border border-outline-variant text-on-surface font-label-sm rounded hover:bg-surface-container-low transition-colors">Vazgeç</button>
                </div>
              </div>
            ) : showResolve ? (
              <div className="flex flex-col gap-2">
                <textarea
                  value={resolutionText}
                  onChange={(e) => setResolutionText(e.target.value)}
                  rows={3}
                  placeholder="Çözüm açıklaması..."
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-background font-body-sm focus:border-primary outline-none resize-none"
                ></textarea>
                <div className="flex gap-2">
                  <button onClick={handleResolve} disabled={!resolutionText.trim() || actionLoading}
                    className="flex-1 py-2 bg-primary text-on-primary font-label-sm rounded hover:bg-[#0035be] transition-colors disabled:opacity-50">Kapat</button>
                  <button onClick={() => { setShowResolve(false); setResolutionText(""); }}
                    className="flex-1 py-2 border border-outline-variant text-on-surface font-label-sm rounded hover:bg-surface-container-low transition-colors">Vazgeç</button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                <button onClick={() => setShowAssign(true)}
                  className="w-full bg-surface border border-primary text-primary hover:bg-primary-fixed font-label-md py-2 rounded transition-colors cursor-pointer">
                  Ekibe Ata
                </button>
                <button onClick={() => setShowResolve(true)}
                  className="w-full bg-primary hover:bg-[#0035be] text-on-primary font-label-md py-2 rounded transition-colors cursor-pointer">
                  Çözüldü Olarak Kapat
                </button>
              </div>
            )}
          </div>

          <div className={clsx("rounded p-5 border", sla.breached ? "bg-error text-on-error border-[#93000a]" : "bg-surface border-outline-variant")}>
            <div className="flex items-center gap-2 mb-3">
              <span className="material-symbols-outlined">{sla.breached ? "warning" : "timer"}</span>
              <h3 className="font-h3">SLA</h3>
            </div>
            <div className={clsx("font-label-sm mb-1", sla.breached ? "opacity-80" : "text-secondary")}>Hedef Çözüm</div>
            <div className="font-body-md font-bold mb-3">{formatDateTime(ticket.slaDueAt)}</div>
            <div className={clsx("font-mono-label", sla.breached ? "" : "text-on-surface-variant")}>{sla.label}</div>
          </div>

          <div className="bg-surface border border-outline-variant rounded p-5">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider mb-4">Müşteri Bağlamı</h3>
            <div className="flex items-center gap-3 mb-5">
              <div className="w-10 h-10 rounded bg-surface-variant flex items-center justify-center text-on-surface">
                <span className="material-symbols-outlined">person</span>
              </div>
              <div>
                <div className="font-label-md text-on-surface">{customerName}</div>
                <div className="font-mono-id text-secondary">{customer?.customerNo || ticket.customerId.slice(0, 8)}</div>
              </div>
            </div>
            {customer && (
              <Link to={`/customers/${customer.id}`} className="font-label-sm text-primary hover:underline flex items-center gap-1">
                Müşteri detayına git
                <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
              </Link>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
