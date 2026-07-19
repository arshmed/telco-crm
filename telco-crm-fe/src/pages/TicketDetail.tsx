import { useState } from "react";
import { Link } from "react-router-dom";
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
  const [isInternalNote, setIsInternalNote] = useState(false);

  const handleSendReply = () => {
    if (!replyText.trim()) return;

    const newMessage = {
      id: messages.length + 1,
      type: isInternalNote ? "agent-internal" : "agent-public",
      sender: "Mevcut Kullanıcı",
      role: "L1 Destek Uzmanı",
      time: "Şimdi",
      body: replyText
    };

    setMessages([...messages, newMessage]);
    setReplyText("");
  };
  return (
    <div className="flex-1 overflow-y-auto bg-background p-container-padding flex flex-col gap-6">
      {/* Breadcrumbs / Header Area */}
      <div className="mb-stack-lg">
        <div className="flex items-center gap-2 font-body-sm text-secondary mb-1">
          <Link to="/support" className="hover:text-primary transition-colors">Destek</Link>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="hover:text-primary transition-colors cursor-pointer">Ağ Şikayetleri</span>
          <span className="material-symbols-outlined text-[14px]">chevron_right</span>
          <span className="font-mono-id text-on-surface-variant">TCK-2023-8941A</span>
        </div>
        <div className="flex justify-between items-start">
          <h2 className="font-h1 text-on-surface">Fiber İnternet Bağlantı Kopması ve Hız Düşüklüğü</h2>
        </div>
      </div>

      {/* Two Column Layout */}
      <div className="grid grid-cols-1 xl:grid-cols-[1fr_320px] gap-gutter items-start">
        
        {/* LEFT COLUMN (Wide) */}
        <div className="flex flex-col gap-gutter">
          {/* Ticket Description Card */}
          <div className="bg-surface border border-outline-variant rounded overflow-hidden">
            <div className="p-6">
              {/* Meta Row */}
              <div className="flex items-center gap-6 mb-6 pb-4 border-b border-surface-variant">
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Oluşturan</span>
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center font-bold text-[10px]">MY</div>
                    <span className="font-body-md text-on-surface">Mehmet Yılmaz</span>
                  </div>
                </div>
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Tarih</span>
                  <span className="font-body-md text-on-surface">24 Eki 2023, 14:30</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Kategori</span>
                  <span className="font-body-md text-on-surface">Arıza / Sabit Genişbant</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-label-sm text-secondary mb-1">Öncelik</span>
                  <div className="flex items-center gap-1 text-tertiary">
                    <span className="material-symbols-outlined text-[16px]">priority_high</span>
                    <span className="font-label-md">Yüksek</span>
                  </div>
                </div>
              </div>

              {/* Body */}
              <div className="font-body-lg text-on-surface leading-relaxed whitespace-pre-line">
                Merhaba, son 3 gündür akşam saat 20:00'den sonra sürekli bağlantı kopmaları yaşıyorum. Bağlantı geldiğinde ise hız testi yaptığımda 100 Mbps olan paketimin sadece 12 Mbps verdiğini görüyorum. Modem arayüzüne girip resetledim ancak sorun düzelmedi. Uzaktan çalışma yapıyorum ve bu durum işlerimi çok aksatıyor. Acil destek rica ediyorum.{"\n\n"}
                Not: Modem ışıklarında DSL sabit yanıyor ancak internet ışığı kırmızıya dönüyor.
              </div>

              {/* Attachments */}
              <div className="mt-6 pt-4 border-t border-surface-variant flex gap-4">
                <div className="flex items-center gap-2 px-3 py-2 border border-outline-variant rounded bg-surface-container-low hover:bg-surface-variant cursor-pointer transition-colors">
                  <span className="material-symbols-outlined text-secondary">image</span>
                  <span className="font-body-sm text-on-surface">hiz_testi_ekran.png</span>
                </div>
                <div className="flex items-center gap-2 px-3 py-2 border border-outline-variant rounded bg-surface-container-low hover:bg-surface-variant cursor-pointer transition-colors">
                  <span className="material-symbols-outlined text-secondary">description</span>
                  <span className="font-body-sm text-on-surface">modem_log.txt</span>
                </div>
              </div>
            </div>
          </div>

          {/* Comment Thread (Messaging Flow) */}
          <div className="flex flex-col gap-4 mb-4">
            <h3 className="font-h3 text-on-surface px-1">İletişim Geçmişi</h3>
            
            {messages.map((msg, idx) => (
              <div key={msg.id} className="flex gap-4">
                <div className="w-8 flex flex-col items-center shrink-0">
                  {msg.type === "system" && (
                    <div className="w-8 h-8 rounded-full bg-surface-variant border border-outline-variant flex items-center justify-center text-secondary">
                      <span className="material-symbols-outlined text-[16px]">smart_toy</span>
                    </div>
                  )}
                  {(msg.type === "agent-public" || msg.type === "agent-internal") && (
                    <div className="w-8 h-8 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-bold text-[10px]">
                      {msg.sender.substring(0, 2).toUpperCase()}
                    </div>
                  )}
                  {msg.type === "customer" && (
                    <div className="w-8 h-8 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center font-bold text-[10px]">
                      {msg.sender.substring(0, 2).toUpperCase()}
                    </div>
                  )}
                  {idx !== messages.length - 1 && <div className="w-px h-full bg-surface-variant mt-2"></div>}
                </div>
                <div className="flex-1 pb-4">
                  <div className="flex items-baseline gap-2 mb-1">
                    <span className="font-label-md text-on-surface">{msg.sender}</span>
                    {msg.type === "agent-public" && msg.role && (
                      <span className="font-label-sm text-primary px-2 py-0.5 bg-primary-fixed rounded">{msg.role}</span>
                    )}
                    {msg.type === "agent-internal" && (
                      <span className="font-label-sm text-tertiary px-2 py-0.5 bg-tertiary-fixed rounded flex items-center gap-1">
                        <span className="material-symbols-outlined text-[12px]">lock</span>
                        İç Not
                      </span>
                    )}
                    {msg.type === "customer" && (
                      <span className="font-label-sm text-secondary px-2 py-0.5 bg-surface-variant rounded">Müşteri</span>
                    )}
                    <span className="font-body-sm text-secondary">{msg.time}</span>
                  </div>
                  <div className={clsx(
                    "rounded p-4 font-body-md text-on-surface whitespace-pre-line",
                    msg.type === "agent-internal" ? "bg-surface-variant border border-outline-variant border-l-4 border-l-tertiary rounded-l-none" : "bg-surface border border-outline-variant",
                    msg.type === "system" && "font-body-sm text-on-surface-variant"
                  )}>
                    {msg.body}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Reply Box with Segmented Control */}
          <div className="bg-surface border border-outline-variant rounded p-4 sticky bottom-4 z-10 shadow-[0_-4px_24px_rgba(11,30,59,0.05)]">
            <div className="flex items-center justify-between mb-3">
              <div className="flex bg-surface-variant p-1 rounded border border-outline-variant">
                <button 
                  onClick={() => setIsInternalNote(false)}
                  className={clsx(
                    "px-4 py-1.5 rounded font-label-sm flex items-center gap-1 cursor-pointer transition-colors",
                    !isInternalNote ? "bg-surface border border-outline-variant text-on-surface shadow-sm" : "text-on-surface-variant hover:text-on-surface border border-transparent"
                  )}
                >
                  <span className="material-symbols-outlined text-[16px]">public</span>
                  Müşteriye Görünür
                </button>
                <button 
                  onClick={() => setIsInternalNote(true)}
                  className={clsx(
                    "px-4 py-1.5 rounded font-label-sm flex items-center gap-1 cursor-pointer transition-colors",
                    isInternalNote ? "bg-surface border border-outline-variant text-on-surface shadow-sm" : "text-on-surface-variant hover:text-on-surface border border-transparent"
                  )}
                >
                  <span className="material-symbols-outlined text-[16px]">lock</span>
                  İç Not
                </button>
              </div>
              <div className="flex gap-2">
                <button className="p-2 text-secondary hover:text-primary transition-colors cursor-pointer rounded">
                  <span className="material-symbols-outlined">attach_file</span>
                </button>
                <button className="p-2 text-secondary hover:text-primary transition-colors cursor-pointer rounded">
                  <span className="material-symbols-outlined">quick_phrases</span>
                </button>
              </div>
            </div>
            <textarea 
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              className="w-full bg-background border border-outline-variant rounded p-3 font-body-md text-on-surface focus:border-primary focus:ring-0 resize-none mb-3" 
              placeholder={isInternalNote ? "İç notunuzu buraya yazın (Müşteri göremez)..." : "Yanıtınızı buraya yazın..."} 
              rows={3}
            ></textarea>
            <div className="flex justify-end">
              <button 
                onClick={handleSendReply}
                disabled={!replyText.trim()}
                className="bg-primary hover:bg-[#0035be] text-on-primary font-label-md px-6 py-2 rounded transition-colors cursor-pointer flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span className="material-symbols-outlined text-[18px]">send</span>
                Gönder
              </button>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN (320px) */}
        <div className="w-full xl:w-[320px] flex flex-col gap-gutter shrink-0">
          
          {/* 1. Status Card */}
          <div className="bg-surface border border-outline-variant rounded p-5">
            <div className="flex justify-between items-center mb-4">
              <span className="font-label-sm text-secondary">Mevcut Durum</span>
              <div className="bg-secondary-container text-on-secondary-fixed font-label-sm px-2.5 py-1 rounded flex items-center gap-1">
                <span className="material-symbols-outlined text-[14px]">hourglass_empty</span>
                BEKLİYOR
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

          {/* 2. SLA Card */}
          <div className="bg-error text-on-error rounded p-5 border border-[#93000a]">
            <div className="flex items-center gap-2 mb-3">
              <span className="material-symbols-outlined">warning</span>
              <h3 className="font-h3">SLA İhlali</h3>
            </div>
            <div className="grid grid-cols-2 gap-4 mb-2">
              <div>
                <span className="block font-label-sm opacity-80 mb-1">Hedef Çözüm</span>
                <span className="block font-body-md font-bold">4 Saat</span>
              </div>
              <div>
                <span className="block font-label-sm opacity-80 mb-1">Geçen Süre</span>
                <span className="block font-body-md font-bold">4s 15dk</span>
              </div>
            </div>
            <div className="w-full bg-[#93000a] h-1.5 rounded-full mt-3 overflow-hidden">
              <div className="bg-surface w-full h-full rounded-full"></div>
            </div>
            <div className="font-mono-label mt-2 text-right">L1 Çözüm Süresi Aşıldı</div>
          </div>

          {/* 3. Mini Event Rail */}
          <div className="bg-surface border border-outline-variant rounded p-5">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider mb-4">Olay Geçmişi</h3>
            <div className="flex flex-col gap-0 relative">
              <div className="absolute left-[15px] top-[8px] bottom-[8px] w-[1px] bg-surface-container-higher"></div>
              <div className="flex gap-3 relative pb-4">
                <div className="w-[30px] shrink-0 flex justify-center">
                  <div className="w-1.5 h-1.5 rounded-full bg-success border-2 border-success mt-2"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-body-sm text-on-surface font-medium">Port resetlendi</p>
                  <p className="font-mono-label text-outline">24 Eki 15:10 · Ayşe Bozdağ</p>
                </div>
              </div>
              <div className="flex gap-3 relative pb-4">
                <div className="w-[30px] shrink-0 flex justify-center">
                  <div className="w-1.5 h-1.5 rounded-full bg-info border-2 border-info mt-2"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-body-sm text-on-surface font-medium">L1 ekibine atandı</p>
                  <p className="font-mono-label text-outline">24 Eki 14:35 · Sistem</p>
                </div>
              </div>
              <div className="flex gap-3 relative pb-4">
                <div className="w-[30px] shrink-0 flex justify-center">
                  <div className="w-1.5 h-1.5 rounded-full bg-info border-2 border-info mt-2"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-body-sm text-on-surface font-medium">Otomatik tanılama yapıldı</p>
                  <p className="font-mono-label text-outline">24 Eki 14:31 · Sistem</p>
                </div>
              </div>
              <div className="flex gap-3 relative">
                <div className="w-[30px] shrink-0 flex justify-center">
                  <div className="w-1.5 h-1.5 rounded-full bg-outline border-2 border-outline mt-2"></div>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-body-sm text-on-surface font-medium">Ticket oluşturuldu</p>
                  <p className="font-mono-label text-outline">24 Eki 14:30 · Mehmet Yılmaz</p>
                </div>
              </div>
            </div>
          </div>

          {/* 4. Customer Context Card */}
          <div className="bg-surface border border-outline-variant rounded p-5">
            <h3 className="font-label-sm text-secondary uppercase tracking-wider mb-4">Müşteri Bağlamı</h3>
            <div className="flex items-center gap-3 mb-5">
              <div className="w-10 h-10 rounded bg-surface-variant flex items-center justify-center text-on-surface">
                <span className="material-symbols-outlined">person</span>
              </div>
              <div>
                <div className="font-label-md text-on-surface">Mehmet Yılmaz</div>
                <div className="font-mono-id text-secondary">CUST-99281-TR</div>
              </div>
            </div>
            <div className="flex flex-col gap-3">
              <div className="flex justify-between items-center py-2 border-t border-surface-variant">
                <span className="font-body-sm text-on-surface-variant">Aktif Abonelik</span>
                <span className="font-mono-id text-on-surface bg-surface-container-low px-2 py-0.5 rounded border border-outline-variant">905321234567</span>
              </div>
              <div className="flex justify-between items-center py-2 border-t border-surface-variant">
                <span className="font-body-sm text-on-surface-variant">Açık Fatura</span>
                <a href="#" className="font-mono-id text-primary hover:underline">₺450.00</a>
              </div>
              <div className="flex justify-between items-center py-2 border-t border-surface-variant">
                <span className="font-body-sm text-on-surface-variant">Tarife</span>
                <span className="font-body-sm text-on-surface font-medium text-right">Fiber 100Mbps Limitsiz</span>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
}
