import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import clsx from "clsx";
import { getTariffs, getAddons, TariffResponse, AddonResponse } from "../api/catalogApi";
import { createOrder, CreateOrderRequest } from "../api/orderApi";
import { createPayment } from "../api/paymentApi";
import { getCustomerByNo, CustomerResponse } from "../api/customerApi";
import { isValidLuhn, isExpiryValid } from "../utils/cardValidation";

export default function OrderWizard() {
  const [step, setStep] = useState(1);
  const navigate = useNavigate();

  const [tariffs, setTariffs] = useState<TariffResponse[]>([]);
  const [addons, setAddons] = useState<AddonResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [customerNo, setCustomerNo] = useState("");
  const [verifiedCustomer, setVerifiedCustomer] = useState<CustomerResponse | null>(null);
  const [verifying, setVerifying] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);

  const [selectedTariff, setSelectedTariff] = useState<TariffResponse | null>(null);
  const [selectedAddons, setSelectedAddons] = useState<AddonResponse[]>([]);

  // Payment States
  const [cardHolder, setCardHolder] = useState("");
  const [cardNumber, setCardNumber] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [cvv, setCvv] = useState("");
  const [consent, setConsent] = useState(false);
  const [paymentError, setPaymentError] = useState<string | null>(null);
  const [createdOrderId, setCreatedOrderId] = useState<string | null>(null);

  const [orderIdempotencyKey] = useState(() => crypto.randomUUID());

  const [paymentIdempotencyKey, setPaymentIdempotencyKey] = useState(() => crypto.randomUUID());

  const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, "");
    if (value.length > 16) value = value.slice(0, 16);
    value = value.replace(/(\d{4})/g, "$1 ").trim();
    setCardNumber(value);
  };

  const handleExpiryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, "");
    if (value.length >= 2) {
      value = value.slice(0, 2) + "/" + value.slice(2, 4);
    }
    setExpiryDate(value);
  };

  const handleCvvChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, "");
    if (value.length > 3) value = value.slice(0, 3);
    setCvv(value);
  };

  const isPaymentValid =
    cardHolder.length > 3 &&
    isValidLuhn(cardNumber) &&
    isExpiryValid(expiryDate) &&
    cvv.length === 3 &&
    consent;

  const handleVerifyCustomer = async () => {
    const raw = customerNo.trim();
    if (!raw) return;

    const formatted = raw.match(/^\d+$/) ? `C-${raw.padStart(6, '0')}` : raw;
    setCustomerNo(formatted);

    setVerifying(true);
    setVerifyError(null);
    try {
      const customer = await getCustomerByNo(formatted);
      if (customer.status !== 'ACTIVE') {
        setVerifyError("Müşteri aktif durumda değil. Sadece aktif müşteriler için sipariş oluşturulabilir.");
        return;
      }
      setVerifiedCustomer(customer);
      setStep(2);
    } catch (err: any) {
      setVerifyError(err.response?.status === 404
        ? `"${formatted}" numaralı müşteri bulunamadı. Doğru format: C-000001`
        : "Müşteri doğrulanırken bir hata oluştu.");
    } finally {
      setVerifying(false);
    }
  };

  useEffect(() => {
    if (step === 2 && tariffs.length === 0) {
      const fetchData = async () => {
        setLoading(true);
        try {
          const [tariffData, addonData] = await Promise.all([
            getTariffs(),
            getAddons()
          ]);
          setTariffs(tariffData.content || []);
          setAddons(addonData || []);
        } catch (err) {
          console.error("Katalog çekilirken hata:", err);
        } finally {
          setLoading(false);
        }
      };
      fetchData();
    }
  }, [step, tariffs.length]);

  const handleAddonToggle = (addon: AddonResponse) => {
    if (selectedAddons.find(a => a.id === addon.id)) {
      setSelectedAddons(selectedAddons.filter(a => a.id !== addon.id));
    } else {
      setSelectedAddons([...selectedAddons, addon]);
    }
  };

  const handleCreateOrder = async () => {
    if (!selectedTariff) return;

    setPaymentError(null);
    setSubmitting(true);
    try {
      let orderId = createdOrderId;

      if (!orderId) {
        const orderPayload: CreateOrderRequest = {
          customerNo: customerNo,
          items: [
            {
              productCode: selectedTariff.code,
              productType: 'TARIFF',
              quantity: 1
            },
            ...selectedAddons.map(a => ({
              productCode: a.code,
              productType: 'ADDON' as const,
              quantity: 1
            }))
          ]
        };

        const order = await createOrder(orderPayload, orderIdempotencyKey);
        orderId = order.id;
        setCreatedOrderId(order.id);
      }

      try {
        const payment = await createPayment({
          paymentRequestId: paymentIdempotencyKey,
          orderId,
          method: 'CREDIT_CARD',
          cardHolder,
          cardNumber: cardNumber.replace(/\s/g, ""),
          expiryDate,
          cvv,
        });

        if (payment.status === 'COMPLETED') {
          navigate(`/sales/saga/${orderId}`);
        } else {
          setPaymentIdempotencyKey(crypto.randomUUID());
          setPaymentError(
            payment.failureReason
              ? `Kartınız reddedildi: ${payment.failureReason}. Lütfen farklı bir kartla tekrar deneyin.`
              : "Ödeme başarısız oldu. Lütfen farklı bir kartla tekrar deneyin."
          );
        }
      } catch (paymentErr: any) {
        setPaymentError(
          paymentErr.response?.data?.detail || "Ödeme işlenirken bir hata oluştu. Lütfen tekrar deneyin."
        );
      }
    } catch (error) {
      console.error("Sipariş oluşturulamadı:", error);
      alert("Sipariş oluşturulurken bir hata oluştu.");
    } finally {
      setSubmitting(false);
    }
  };

  const totalAmount = (selectedTariff?.monthlyFee || 0) + selectedAddons.reduce((acc, curr) => acc + curr.price, 0);
  const tax = totalAmount * 0.20;
  const grandTotal = totalAmount + tax;

  return (
    <div className="flex flex-col bg-background min-h-[calc(100vh-100px)]">
      {/* Header & Stepper */}
      <div className="bg-surface border-b border-outline-variant px-container-padding py-stack-lg -mt-6 -mx-6 mb-6">
        <div className="max-w-5xl mx-auto">
          <div className="flex items-center justify-between mb-stack-lg">
            <div>
              <h1 className="font-h1 text-on-surface">Yeni Sipariş Sihirbazı</h1>
              <p className="font-body-md text-secondary mt-1">
                Abonelik sürecini başlatın.
              </p>
            </div>
            <Link to="/customers" className="px-4 py-2 border border-outline-variant text-on-surface font-label-md rounded-lg hover:bg-surface-container-low transition-colors flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">close</span>
              İptal Et
            </Link>
          </div>

          {/* Stepper */}
          <div className="flex items-center w-full mt-8">
            {[
              { num: 1, label: "Müşteri Seçimi" },
              { num: 2, label: "Tarife" },
              { num: 3, label: "Ek Paketler" },
              { num: 4, label: "Özet ve Ödeme" },
            ].map((s, idx, arr) => (
              <div key={s.num} className={clsx("relative", idx === arr.length - 1 ? "w-8" : "flex-1")}>
                <div className="flex items-center">
                  <button 
                    onClick={() => setStep(s.num)}
                    className={clsx(
                      "z-10 flex items-center justify-center w-8 h-8 rounded-full transition-colors",
                      step > s.num ? "bg-success text-on-primary" : 
                      step === s.num ? "bg-primary border-2 border-primary text-on-primary shadow-[0_0_0_4px_rgba(0,64,224,0.1)]" :
                      "bg-surface border-2 border-outline-variant text-secondary"
                    )}
                  >
                    {step > s.num ? <span className="material-symbols-outlined text-[16px]">check</span> : <span className="font-label-md">{s.num}</span>}
                  </button>
                  {idx !== arr.length - 1 && (
                    <div className={clsx("flex-1 h-[2px] mx-2", step > s.num ? "bg-success" : "bg-outline-variant")}></div>
                  )}
                </div>
                <div className={clsx("absolute mt-2", idx === arr.length - 1 && "-ml-6 w-32")}>
                  <p className={clsx("font-label-sm", step === s.num ? "text-primary font-bold" : "text-on-surface")}>{s.label}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="flex-1">
        <div className="max-w-5xl mx-auto flex flex-col lg:flex-row gap-gutter pb-10">
          
          {/* Main Content Dynamic Switch based on Step */}
          <div className="flex-1 space-y-stack-lg">
            
            {step === 1 && (
              <div className="bg-surface border border-outline-variant rounded p-stack-lg py-12">
                 <h3 className="font-h3 text-on-surface mb-2 text-center">Müşteri Doğrulama</h3>
                   <p className="text-secondary mb-6 text-center">İşlem yapacağınız müşterinin numarasını girin.</p>
                   <div className="max-w-sm mx-auto flex flex-col gap-3">
                    <input 
                       type="text" 
                       value={customerNo} 
                       onChange={e => setCustomerNo(e.target.value.toUpperCase())} 
                       onKeyDown={e => e.key === 'Enter' && handleVerifyCustomer()}
                       className="border border-outline-variant rounded px-3 py-2 focus:border-primary" 
                       placeholder="C-000001 veya sadece 000001"
                    />
                   {verifyError && (
                     <div className="flex items-center gap-2 text-danger font-body-sm bg-danger-bg/50 px-3 py-2 rounded border border-danger/20">
                       <span className="material-symbols-outlined text-[18px]">error</span>
                       {verifyError}
                     </div>
                   )}
                   <button 
                     onClick={handleVerifyCustomer} 
                     disabled={!customerNo.trim() || verifying}
                     className="px-6 py-2 bg-primary text-surface rounded hover:bg-primary/90 disabled:opacity-50 flex items-center justify-center gap-2"
                   >
                     {verifying ? (
                       <><span className="material-symbols-outlined animate-spin text-[18px]">sync</span> Doğrulanıyor...</>
                     ) : (
                       <><span className="material-symbols-outlined text-[18px]">check_circle</span> Doğrula ve Devam Et</>
                     )}
                   </button>
                 </div>
              </div>
            )}

            {step === 2 && (
              <div className="bg-surface border border-outline-variant rounded p-stack-lg">
                <h3 className="font-h3 text-on-surface border-b border-outline-variant pb-2 mb-4">Tarife Seçimi</h3>
                {loading ? <p className="text-secondary">Yükleniyor...</p> : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {tariffs.map(t => (
                      <div 
                        key={t.id} 
                        onClick={() => { setSelectedTariff(t); setStep(3); }}
                        className={clsx(
                          "border rounded p-4 cursor-pointer hover:border-primary transition-colors",
                          selectedTariff?.id === t.id ? "border-primary bg-primary-container/10" : "border-outline-variant"
                        )}
                      >
                        <div className="flex justify-between items-start mb-2">
                          <h4 className="font-label-md text-on-surface font-bold">{t.name}</h4>
                          <span className="font-mono-id text-primary font-semibold">₺{t.monthlyFee}</span>
                        </div>
                        <p className="font-body-sm text-secondary flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">call</span> {t.minutesIncluded} DK</p>
                        <p className="font-body-sm text-secondary flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">network_cell</span> {t.dataMbIncluded / 1024} GB İnternet</p>
                      </div>
                    ))}
                    {tariffs.length === 0 && <p className="text-secondary col-span-2">Sistemde kayıtlı tarife bulunamadı.</p>}
                  </div>
                )}
              </div>
            )}

            {step === 3 && (
              <div className="bg-surface border border-outline-variant rounded p-stack-lg">
                <h3 className="font-h3 text-on-surface border-b border-outline-variant pb-2 mb-4">Ek Paket Seçimi (Opsiyonel)</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {addons.map(a => {
                    const isSelected = selectedAddons.some(sa => sa.id === a.id);
                    return (
                      <div 
                        key={a.id} 
                        onClick={() => handleAddonToggle(a)}
                        className={clsx(
                          "border rounded p-4 cursor-pointer hover:border-primary transition-colors flex items-center justify-between",
                          isSelected ? "border-primary bg-primary-container/10" : "border-outline-variant"
                        )}
                      >
                        <div>
                          <h4 className="font-label-md text-on-surface font-bold">{a.name}</h4>
                          <p className="font-body-sm text-secondary mt-1 capitalize">{a.type} Paketi</p>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono-id text-primary font-semibold">₺{a.price}</span>
                          <div className={clsx("w-5 h-5 rounded-full border flex items-center justify-center", isSelected ? "border-primary bg-primary" : "border-outline")}>
                            {isSelected && <span className="material-symbols-outlined text-on-primary text-[14px]">check</span>}
                          </div>
                        </div>
                      </div>
                    )
                  })}
                </div>
                <div className="mt-6 flex justify-end">
                  <button onClick={() => setStep(4)} className="px-6 py-2 bg-primary text-surface rounded hover:bg-primary/90">Devam Et</button>
                </div>
              </div>
            )}

            {step === 4 && (
              <>
                <div className="bg-surface border border-outline-variant rounded-lg overflow-hidden">
                  <div className="px-4 py-3 bg-surface-container-lowest border-b border-outline-variant">
                    <h3 className="font-h3 text-on-surface">Sipariş Kalemleri</h3>
                  </div>
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="bg-background border-b border-outline-variant">
                        <th className="px-4 py-2 font-label-sm text-secondary w-2/3">Ürün / Hizmet</th>
                        <th className="px-4 py-2 font-label-sm text-secondary w-1/3 text-right">Tutar</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedTariff && (
                        <tr className="border-b border-outline-variant h-[44px]">
                          <td className="px-4 py-2">
                            <div className="flex items-center gap-2">
                              <span className="material-symbols-outlined text-[18px] text-secondary">sim_card</span>
                              <span className="font-body-sm text-on-surface">{selectedTariff.name}</span>
                            </div>
                          </td>
                          <td className="px-4 py-2 text-right font-mono-id">₺{selectedTariff.monthlyFee}</td>
                        </tr>
                      )}
                      {selectedAddons.map(a => (
                        <tr key={a.id} className="border-b border-outline-variant h-[44px]">
                          <td className="px-4 py-2">
                            <div className="flex items-center gap-2">
                              <span className="material-symbols-outlined text-[18px] text-secondary">speed</span>
                              <span className="font-body-sm text-on-surface">{a.name}</span>
                            </div>
                          </td>
                          <td className="px-4 py-2 text-right font-mono-id">₺{a.price}</td>
                        </tr>
                      ))}
                      {!selectedTariff && (
                        <tr>
                          <td colSpan={2} className="p-4 text-center text-secondary">Tarife seçilmedi. Lütfen 2. adıma dönün.</td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                  <div className="p-4 bg-surface-container-low flex flex-col items-end gap-2 border-t border-outline-variant">
                    <div className="flex justify-between w-48">
                      <span className="font-body-sm text-secondary">Ara Toplam:</span>
                      <span className="font-mono-id text-on-surface">₺{totalAmount.toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between w-48">
                      <span className="font-body-sm text-secondary">Vergiler (%20):</span>
                      <span className="font-mono-id text-on-surface">₺{tax.toFixed(2)}</span>
                    </div>
                    <div className="w-48 h-[1px] bg-outline-variant my-1"></div>
                    <div className="flex justify-between w-48">
                      <span className="font-label-md font-bold text-on-surface">Genel Toplam:</span>
                      <span className="font-mono-id font-bold text-primary">₺{grandTotal.toFixed(2)}</span>
                    </div>
                  </div>
                </div>

                {/* Payment Details Form */}
                <div className="bg-surface border border-outline-variant rounded overflow-hidden mt-6">
                  <div className="px-4 py-3 bg-surface-container-lowest border-b border-outline-variant flex items-center justify-between">
                    <h3 className="font-h3 text-on-surface">Ödeme Bilgileri</h3>
                    <span className="material-symbols-outlined text-outline">credit_card</span>
                  </div>
                  <div className="p-stack-lg space-y-4">
                    {paymentError && (
                      <div className="flex items-center gap-2 text-danger font-body-sm bg-danger-bg/50 px-3 py-2 rounded border border-danger/20">
                        <span className="material-symbols-outlined text-[18px]">error</span>
                        {paymentError}
                      </div>
                    )}
                    <div className="flex items-center gap-2 text-info font-body-sm bg-info-bg/50 px-3 py-2 rounded border border-info/20">
                      <span className="material-symbols-outlined text-[18px]">info</span>
                      Demo kartı: 4242 4242 4242 4242 başarılı olur, 4000 0000 0000 0002 reddedilir.
                    </div>
                    <div>
                      <label className="block font-label-sm text-secondary mb-1">Kart Üzerindeki İsim</label>
                      <input 
                        type="text" 
                        value={cardHolder}
                        onChange={(e) => setCardHolder(e.target.value)}
                        placeholder="Ad Soyad" 
                        className="w-full h-[40px] px-3 border border-outline-variant rounded-lg font-body-sm focus:border-primary focus:ring-0 outline-none transition-colors" 
                      />
                    </div>
                    <div>
                      <label className="block font-label-sm text-secondary mb-1">Kart Numarası</label>
                      <div className="relative">
                        <span className="absolute left-3 top-2.5 material-symbols-outlined text-[20px] text-outline">credit_score</span>
                        <input 
                          type="text" 
                          value={cardNumber}
                          onChange={handleCardNumberChange}
                          placeholder="0000 0000 0000 0000" 
                          className="w-full h-[40px] pl-10 pr-3 border border-outline-variant rounded-lg font-mono-id focus:border-primary focus:ring-0 outline-none transition-colors" 
                        />
                      </div>
                    </div>
                    <div className="flex gap-4">
                      <div className="flex-1">
                        <label className="block font-label-sm text-secondary mb-1">Son Kullanma (AA/YY)</label>
                        <input 
                          type="text" 
                          value={expiryDate}
                          onChange={handleExpiryChange}
                          placeholder="12/25" 
                          className="w-full h-[40px] px-3 border border-outline-variant rounded-lg font-mono-id focus:border-primary focus:ring-0 outline-none transition-colors" 
                        />
                      </div>
                      <div className="w-1/3">
                        <label className="block font-label-sm text-secondary mb-1">CVV</label>
                        <input 
                          type="password" 
                          value={cvv}
                          onChange={handleCvvChange}
                          placeholder="***" 
                          className="w-full h-[40px] px-3 border border-outline-variant rounded-lg font-mono-id focus:border-primary focus:ring-0 outline-none transition-colors" 
                        />
                      </div>
                    </div>
                    <div className="flex items-start gap-3 pt-2 border-t border-outline-variant">
                      <input 
                        type="checkbox" 
                        id="consent" 
                        checked={consent}
                        onChange={(e) => setConsent(e.target.checked)}
                        className="mt-1 w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer" 
                      />
                      <label htmlFor="consent" className="font-body-sm text-secondary leading-relaxed cursor-pointer select-none">
                        Sözleşme ve ön bilgilendirme formunu okudum, kabul ediyorum. Kişisel verilerimin işlenmesine ve aktarılmasına açık rıza gösteriyorum.
                      </label>
                    </div>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Sticky Summary */}
          <div className="w-full lg:w-[280px] shrink-0">
            <div className="sticky top-[80px] bg-surface border border-outline-variant rounded overflow-hidden shadow-sm">
              <div className="p-4 border-b border-outline-variant bg-surface-container-lowest">
                <h3 className="font-h3 text-on-surface">Sipariş Özeti</h3>
                <span className="inline-block mt-2 px-2 py-1 rounded bg-info-bg text-info font-mono-label border border-info/20">DURUM: TASLAK</span>
              </div>
              <div className="p-4 space-y-4">
                <div>
                  <p className="font-label-sm text-secondary">Müşteri</p>
                  {verifiedCustomer ? (
                    <div className="mt-1">
                      <p className="font-body-sm text-on-surface font-medium">
                        {verifiedCustomer.type === 'CORPORATE' ? verifiedCustomer.companyName : `${verifiedCustomer.firstName} ${verifiedCustomer.lastName}`}
                      </p>
                      <p className="font-mono-id text-secondary text-[12px] truncate" title={customerNo}>{customerNo}</p>
                    </div>
                  ) : (
                    <p className="font-mono-id text-on-surface mt-1 truncate" title={customerNo}>{customerNo || '-'}</p>
                  )}
                </div>
                <div>
                  <p className="font-label-sm text-secondary">Seçilen Tarife</p>
                  <p className="font-body-sm text-on-surface mt-1">{selectedTariff ? selectedTariff.name : '-'}</p>
                </div>
              </div>
              <div className="p-4 bg-surface-container-low border-t border-outline-variant">
                <div className="flex items-center justify-between mb-4">
                  <span className="font-h2 text-on-surface">Toplam</span>
                  <span className="font-mono-id text-[16px] font-bold text-primary">₺{grandTotal.toFixed(2)}</span>
                </div>
                <button 
                  onClick={handleCreateOrder} 
                  disabled={!selectedTariff || submitting || !isPaymentValid}
                  className="w-full h-[40px] bg-primary text-surface font-label-md rounded-lg hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-[18px]">
                    {submitting ? "hourglass_empty" : "lock"}
                  </span>
                  {submitting ? "İşleniyor..." : "Siparişi Tamamla"}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
