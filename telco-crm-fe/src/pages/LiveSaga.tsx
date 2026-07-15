import { useState, useEffect, useRef } from "react";
import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { getOrderById, OrderResponse } from "../api/orderApi";

const POLL_INTERVAL_MS = 2000;
const MAX_POLL_ATTEMPTS = 60; // ~2 dakika

const SAGA_STEPS = [
  { id: 'AWAITING_PAYMENT', label: 'Ödeme Alınıyor', service: 'payment-service' },
  { id: 'AWAITING_SUBSCRIPTION', label: 'Abonelik Aktive Ediliyor', service: 'subscription-service' },
  { id: 'COMPLETED', label: 'Sipariş Tamamlandı', service: 'order-service' },
];

type StepStatus = "COMPLETED" | "PROCESSING" | "PENDING" | "FAILED";

export default function LiveSaga() {
  const { orderId } = useParams();
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [timedOut, setTimedOut] = useState(false);
  const attemptsRef = useRef(0);

  useEffect(() => {
    if (!orderId) return;

    let cancelled = false;
    let intervalId: ReturnType<typeof setInterval>;

    const poll = async () => {
      try {
        const data = await getOrderById(orderId);
        if (cancelled) return;
        setOrder(data);

        const currentStep = data.sagaState?.currentStep;
        attemptsRef.current += 1;

        if (currentStep === 'COMPLETED' || currentStep === 'FAILED') {
          clearInterval(intervalId);
        } else if (attemptsRef.current >= MAX_POLL_ATTEMPTS) {
          clearInterval(intervalId);
          setTimedOut(true);
        }
      } catch (err) {
        console.error("Sipariş detayı çekilemedi:", err);
      }
    };

    poll();
    intervalId = setInterval(poll, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, [orderId]);

  const currentStepId = order?.sagaState?.currentStep;
  const isFailed = currentStepId === 'FAILED' || currentStepId === 'COMPENSATING';
  const isCompleted = currentStepId === 'COMPLETED';
  const currentStepIndex = SAGA_STEPS.findIndex(s => s.id === currentStepId);

  // order.paymentId sadece ödeme gerçekten tamamlandıysa dolar (bkz. OrderStateRules.markPaymentCompleted).
  // Ödeme reddedildiyse saga hiç AWAITING_SUBSCRIPTION'a geçmeden FAILED'a düşer ve paymentId hep null kalır.
  const paymentSucceeded = !!order?.paymentId;

  const getStatus = (index: number): StepStatus => {
    if (isCompleted) return "COMPLETED";
    if (isFailed) {
      if (!paymentSucceeded) {
        return index === 0 ? "FAILED" : "PENDING";
      }
      // Ödeme başarılıydı, sonraki bir adımda (abonelik aktivasyonu) başarısız olup iade edildi
      if (index === 0) return "COMPLETED";
      if (index === 1) return "FAILED";
      return "PENDING";
    }
    if (currentStepIndex === -1) return "PENDING";
    if (index < currentStepIndex) return "COMPLETED";
    if (index === currentStepIndex) return "PROCESSING";
    return "PENDING";
  };

  const renderIcon = (status: StepStatus) => {
    if (status === "COMPLETED") {
      return (
        <div className="absolute left-0 top-0 w-10 h-10 rounded-full bg-success-bg flex items-center justify-center z-10 border border-success">
          <span className="material-symbols-outlined text-success text-[20px] icon-fill">check</span>
        </div>
      );
    }
    if (status === "PROCESSING") {
      return (
        <>
          <div className="absolute left-0 top-0 w-10 h-10 z-0">
            <div className="absolute inset-[-4px] rounded-full border-2 border-info animate-[ping_1.5s_cubic-bezier(0,0,0.2,1)_infinite] opacity-50"></div>
          </div>
          <div className="absolute left-0 top-0 w-10 h-10 rounded-full bg-info-bg flex items-center justify-center z-10 border border-info">
            <span className="material-symbols-outlined text-info text-[20px] animate-spin">sync</span>
          </div>
        </>
      );
    }
    return (
      <div className="absolute left-0 top-0 w-10 h-10 rounded-full bg-surface-container flex items-center justify-center z-10 border border-outline-variant">
        <span className="material-symbols-outlined text-outline-variant text-[20px]">hourglass_empty</span>
      </div>
    );
  };

  return (
    <div className="w-full max-w-3xl mx-auto flex flex-col items-center py-stack-lg">

      {/* Celebratory Banner */}
      <div className={clsx(
        "bg-surface border border-outline-variant rounded p-stack-lg mb-stack-lg flex flex-col items-center text-center shadow-sm relative overflow-hidden w-full transition-all duration-700",
        isCompleted ? "opacity-100 translate-y-0" : "opacity-0 -translate-y-4 pointer-events-none h-0 mb-0 p-0"
      )}>
        <div className="absolute top-0 left-0 w-full h-1 bg-success"></div>
        <div className="w-12 h-12 rounded-full bg-success-bg flex items-center justify-center text-success mb-stack-md">
          <span className="material-symbols-outlined text-3xl icon-fill">check_circle</span>
        </div>
        <h1 className="font-h1 text-on-surface mb-stack-sm">Sipariş başarıyla tamamlandı!</h1>
        <p className="font-body-md text-on-surface-variant">
          Abonelik aktive edildi. (Sipariş No: {orderId ? orderId.substring(0, 8).toUpperCase() : '-'})
        </p>
      </div>

      {/* Failure Banner */}
      {isFailed && (
        <div className="bg-danger-bg/50 border border-danger/30 rounded p-stack-lg mb-stack-lg flex flex-col items-center text-center w-full">
          <div className="w-12 h-12 rounded-full bg-danger-bg flex items-center justify-center text-danger mb-stack-md">
            <span className="material-symbols-outlined text-3xl icon-fill">error</span>
          </div>
          <h1 className="font-h1 text-on-surface mb-stack-sm">Sipariş tamamlanamadı</h1>
          <p className="font-body-md text-on-surface-variant">
            {order?.sagaState?.errorMessage || order?.cancellationReason || "İşlem geri alındı, ödemeniz varsa iade edilecektir."}
          </p>
        </div>
      )}

      {/* Timeout Notice */}
      {timedOut && !isCompleted && !isFailed && (
        <div className="bg-warning-bg/50 border border-warning/30 rounded p-stack-md mb-stack-lg flex items-center gap-2 w-full">
          <span className="material-symbols-outlined text-warning text-[20px]">schedule</span>
          <p className="font-body-sm text-on-surface-variant">
            İşlem beklenenden uzun sürüyor. Sipariş arka planda işlenmeye devam ediyor olabilir, sayfayı daha sonra tekrar kontrol edebilirsiniz.
          </p>
        </div>
      )}

      {/* Event Rail */}
      <div className="bg-surface border border-outline-variant rounded p-stack-lg mb-stack-lg w-full">
        <h2 className="font-h2 mb-stack-lg flex items-center gap-2 text-on-surface">
          <span className="material-symbols-outlined text-primary text-[24px] icon-fill">account_tree</span>
          Olay Rayı (Live Saga)
        </h2>

        <div className="relative pl-2">
          <div className="absolute left-[21px] top-[40px] bottom-[60px] w-0.5 bg-outline-variant z-0 transition-all duration-1000"></div>

          {SAGA_STEPS.map((node, index) => {
            const status = getStatus(index);
            return (
              <div key={node.id} className={clsx("relative pl-12", index !== SAGA_STEPS.length - 1 ? "pb-stack-lg" : "")}>
                {renderIcon(status)}
                <div className="pt-2">
                  <div className="flex justify-between items-start mb-1">
                    <span className={clsx(
                      "font-mono-id font-semibold px-2 py-0.5 rounded border transition-colors",
                      status === "COMPLETED" ? "bg-surface-container-low text-on-surface border-outline-variant" :
                      status === "PROCESSING" ? "bg-surface-container text-info border-info" :
                      "bg-surface-container-lowest text-outline-variant border-outline-variant opacity-70"
                    )}>
                      {node.label}
                    </span>
                    <span className={clsx(
                      "font-mono-label transition-colors",
                      status === "COMPLETED" ? "text-on-surface-variant" :
                      status === "PROCESSING" ? "text-info animate-pulse" :
                      "text-outline-variant"
                    )}>
                      {status === "COMPLETED" ? "tamamlandı" : status === "PROCESSING" ? "işleniyor..." : "bekliyor"}
                    </span>
                  </div>
                  <div className={clsx("font-body-sm", status === "PENDING" ? "text-outline-variant" : "text-on-surface-variant")}>
                    Servis: <span className="font-mono-label">{node.service}</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Sipariş Özeti */}
      {order && (
        <div className="bg-surface border border-outline-variant rounded p-stack-lg w-full mb-stack-lg">
          <h3 className="font-h3 text-on-surface mb-4">Sipariş Detayları</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="font-label-sm text-secondary">Durum</p>
              <p className="font-body-sm text-on-surface mt-1">{order.status}</p>
            </div>
            <div>
              <p className="font-label-sm text-secondary">Toplam</p>
              <p className="font-mono-id text-on-surface mt-1">{order.totalAmount?.toFixed(2)} {order.currency}</p>
            </div>
            <div>
              <p className="font-label-sm text-secondary">Kalem Sayısı</p>
              <p className="font-body-sm text-on-surface mt-1">{order.items?.length || 0}</p>
            </div>
            <div>
              <p className="font-label-sm text-secondary">Oluşturulma</p>
              <p className="font-body-sm text-on-surface mt-1">{new Date(order.createdAt).toLocaleString('tr-TR')}</p>
            </div>
          </div>
        </div>
      )}

      {/* Actions */}
      {isCompleted && order?.subscriptionId && (
        <div className="flex justify-end gap-gutter pt-stack-sm w-full">
          <Link to={`/subscriptions/${order.subscriptionId}`} className="h-10 px-6 bg-primary text-on-primary rounded font-label-md hover:bg-primary-container hover:text-on-primary-container transition-colors shadow-sm flex items-center justify-center">
            Aboneliğe git
          </Link>
        </div>
      )}

    </div>
  );
}
