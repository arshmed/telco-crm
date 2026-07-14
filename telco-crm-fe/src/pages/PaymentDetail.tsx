import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { getPaymentById, refundPayment, PaymentResponse } from "../api/paymentApi";

const STATUS_CLASSES: Record<string, string> = {
  COMPLETED: "bg-success-bg text-success",
  FAILED: "bg-danger-bg text-danger",
  REFUNDED: "bg-warning-bg text-warning",
  PENDING: "bg-surface-variant text-on-surface-variant"
};

export default function PaymentDetail() {
  const { id } = useParams<{ id: string }>();
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refunding, setRefunding] = useState(false);

  useEffect(() => {
    if (!id) return;
    const fetchPayment = async () => {
      try {
        const data = await getPaymentById(id);
        setPayment(data);
      } catch (err) {
        console.error("Failed to fetch payment", err);
      } finally {
        setLoading(false);
      }
    };
    fetchPayment();
  }, [id]);

  const handleRefund = async () => {
    if (!id || !payment) return;
    if (!window.confirm("Bu ödemeyi iade etmek istediğinize emin misiniz?")) return;
    try {
      setRefunding(true);
      const data = await refundPayment(id, { reason: "Müşteri talebi" });
      setPayment(data);
    } catch (err) {
      alert("İade işlemi başarısız oldu.");
    } finally {
      setRefunding(false);
    }
  };

  if (loading) {
    return <div className="p-8 text-center text-secondary">Yükleniyor...</div>;
  }

  if (!payment) {
    return <div className="p-8 text-center text-secondary">Ödeme bulunamadı.</div>;
  }

  return (
    <div className="flex flex-col gap-stack-lg max-w-7xl mx-auto w-full">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="flex items-center text-on-surface-variant font-label-md">
        <Link to="/finance/billing" className="hover:text-primary transition-colors">Finans</Link>
        <span className="material-symbols-outlined mx-2 text-[16px]">chevron_right</span>
        <Link to="/finance/payments" className="hover:text-primary transition-colors">Ödemeler</Link>
        <span className="material-symbols-outlined mx-2 text-[16px]">chevron_right</span>
        <span className="font-mono-id text-on-surface">{payment.id.substring(0, 8)}...</span>
      </nav>

      {/* Entity Header */}
      <header className="bg-surface border border-outline-variant rounded-lg p-stack-lg flex flex-col md:flex-row md:items-start justify-between gap-stack-lg">
        <div className="flex flex-col gap-stack-sm">
          <div className="flex items-center gap-stack-sm">
            <h1 className="font-h1 text-on-surface font-mono-id tracking-tight">{payment.id}</h1>
            <span className={clsx("inline-flex items-center px-2 py-1 rounded font-label-sm uppercase tracking-wider", STATUS_CLASSES[payment.status] || STATUS_CLASSES.PENDING)}>
              {payment.status}
            </span>
          </div>
          <div className="flex flex-wrap items-center gap-x-stack-lg gap-y-stack-sm text-on-surface-variant font-body-sm mt-2">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">person</span>
              <span>Müşteri ID: <strong className="text-on-surface">{payment.customerId}</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">payments</span>
              <span>Tutar: <strong className="text-on-surface font-mono-id">{payment.amount.toFixed(2)} {payment.currency}</strong></span>
            </div>
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-[18px]">calendar_today</span>
              <span>Tarih: <strong className="text-on-surface">{new Date(payment.createdAt).toLocaleString('tr-TR')}</strong></span>
            </div>
          </div>
        </div>
        <div className="flex-shrink-0">
          {payment.status === 'COMPLETED' && (
            <button 
              onClick={handleRefund}
              disabled={refunding}
              className="bg-error-container text-on-error-container hover:bg-error hover:text-on-error border border-error-container font-label-md px-4 py-2 rounded transition-colors flex items-center gap-2 h-[40px] disabled:opacity-50">
              <span className="material-symbols-outlined text-[18px]">undo</span>
              {refunding ? "İade Ediliyor..." : "İade Et"}
            </button>
          )}
        </div>
      </header>

      {/* Main Content Layout */}
      <div className="flex flex-col lg:flex-row gap-stack-lg items-start">
        {/* Left Column: Details */}
        <div className="flex-1 flex flex-col gap-stack-lg w-full">
          {/* Payment Details Card */}
          <section className="bg-surface border border-outline-variant rounded-lg overflow-hidden">
            <div className="border-b border-outline-variant bg-surface-container-lowest px-stack-lg py-stack-md flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">credit_card</span>
              <h2 className="font-h3 text-on-surface">Ödeme Detayları</h2>
            </div>
            <div className="p-stack-lg grid grid-cols-1 md:grid-cols-2 gap-stack-lg">
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">Ödeme Yöntemi</span>
                <span className="font-body-md text-on-surface">{payment.method}</span>
              </div>
              <div className="flex flex-col gap-1">
                <span className="font-label-sm text-on-surface-variant">Otorizasyon / Harici Kod</span>
                <span className="font-mono-id text-on-surface">{payment.externalRef || '-'}</span>
              </div>
            </div>
          </section>

          {/* Related Order Card */}
          <section className="bg-surface border border-outline-variant rounded-lg overflow-hidden">
            <div className="border-b border-outline-variant bg-surface-container-lowest px-stack-lg py-stack-md flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[20px]">receipt_long</span>
              <h2 className="font-h3 text-on-surface">İlgili Sipariş</h2>
            </div>
            <div className="p-stack-lg flex items-center justify-between">
              <div className="flex items-center gap-stack-md">
                <div className="w-10 h-10 rounded bg-secondary-container text-on-secondary-container flex items-center justify-center">
                  <span className="material-symbols-outlined">receipt</span>
                </div>
                <div className="flex flex-col">
                  {payment.orderId ? (
                    <Link to={`/sales/saga/${payment.orderId}`} className="font-mono-id text-primary hover:text-on-primary-fixed-variant transition-colors underline-offset-2 hover:underline">{payment.orderId}</Link>
                  ) : (
                    <span className="font-mono-id text-on-surface-variant">Sipariş Kaydı Yok</span>
                  )}
                </div>
              </div>
              <span className="font-mono-id text-on-surface font-semibold">{payment.amount.toFixed(2)} {payment.currency}</span>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
