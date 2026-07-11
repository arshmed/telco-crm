import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { getOrderById, OrderResponse } from "../api/orderApi";

export default function LiveSaga() {
  const { orderId } = useParams();
  const [currentStep, setCurrentStep] = useState(0);
  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!orderId) return;
    const fetchOrder = async () => {
      try {
        const data = await getOrderById(orderId);
        setOrder(data);
      } catch (err) {
        console.error("Sipariş detayı çekilemedi:", err);
      }
    };
    fetchOrder();
  }, [orderId]);

  useEffect(() => {
    const timers = [
      setTimeout(() => setCurrentStep(1), 800),
      setTimeout(() => setCurrentStep(2), 2000),
      setTimeout(() => setCurrentStep(3), 4500),
      setTimeout(() => setCurrentStep(4), 5500),
    ];
    return () => timers.forEach(clearTimeout);
  }, []);

  const getStatus = (stepIndex: number) => {
    if (currentStep > stepIndex) return "COMPLETED";
    if (currentStep === stepIndex) return "PROCESSING";
    return "PENDING";
  };

  const renderIcon = (status: string) => {
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

  const handleCopy = async () => {
    const msisdn = order?.subscriptionId ? `+90 5${order.id.substring(0, 9).replace(/[^0-9]/g, '')}` : null;
    if (msisdn) {
      try {
        await navigator.clipboard.writeText(msisdn);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      } catch {
        setCopied(false);
      }
    }
  };

  const msisdn = order?.subscriptionId ? `+90 5${order.id.substring(0, 9).replace(/[^0-9]/g, '')}` : null;

  const sagaSteps = [
    { id: 'OrderCreated', service: 'order-service', time: order?.createdAt ? new Date(order.createdAt).toLocaleTimeString('tr-TR') : '' },
    { id: 'PaymentCompleted', service: 'payment-service', time: '' },
    { id: 'SubscriptionActivated', service: 'subscription-service', time: '' },
    { id: 'WelcomeSmsDispatched', service: 'notification-service', time: '' }
  ];

  return (
    <div className="w-full max-w-3xl mx-auto flex flex-col items-center py-stack-lg">
      
      {/* Celebratory Banner & MSISDN */}
      <div className={clsx(
        "bg-surface border border-outline-variant rounded p-stack-lg mb-stack-lg flex flex-col items-center text-center shadow-sm relative overflow-hidden w-full transition-all duration-700",
        currentStep === 4 ? "opacity-100 translate-y-0" : "opacity-0 -translate-y-4 pointer-events-none"
      )}>
        <div className="absolute top-0 left-0 w-full h-1 bg-success"></div>
        <div className="w-12 h-12 rounded-full bg-success-bg flex items-center justify-center text-success mb-stack-md">
          <span className="material-symbols-outlined text-3xl icon-fill">check_circle</span>
        </div>
        <h1 className="font-h1 text-on-surface mb-stack-sm">Sipariş başarıyla oluşturuldu!</h1>
        <p className="font-body-md text-on-surface-variant mb-stack-lg">
          Yeni hat tahsisi tamamlandı ve aktivasyon süreci başlatıldı.
          (Sipariş No: {orderId ? orderId.substring(0, 8).toUpperCase() : '-'})
        </p>
        {msisdn && (
          <div className="bg-surface-container-low border border-outline-variant rounded p-stack-md flex items-center gap-stack-md group">
            <span className="font-mono-id text-[24px] leading-8 font-semibold text-primary tracking-tight">{msisdn}</span>
            <button 
              className="text-on-surface-variant hover:text-primary transition-colors flex items-center justify-center p-2 rounded hover:bg-surface-container"
              onClick={handleCopy}
            >
              <span className="material-symbols-outlined text-[20px]">{copied ? 'check' : 'content_copy'}</span>
            </button>
          </div>
        )}
      </div>

      {/* Event Rail */}
      <div className="bg-surface border border-outline-variant rounded p-stack-lg mb-stack-lg w-full">
        <h2 className="font-h2 mb-stack-lg flex items-center gap-2 text-on-surface">
          <span className="material-symbols-outlined text-primary text-[24px] icon-fill">account_tree</span>
          Olay Rayı (Live Saga)
        </h2>
        
        <div className="relative pl-2">
          <div className="absolute left-[21px] top-[40px] bottom-[60px] w-0.5 bg-outline-variant z-0 transition-all duration-1000"></div>
          
          {sagaSteps.map((node, index) => {
            const status = getStatus(index);
            return (
              <div key={node.id} className={clsx("relative pl-12", index !== 3 ? "pb-stack-lg" : "")}>
                {renderIcon(status)}
                <div className="pt-2">
                  <div className="flex justify-between items-start mb-1">
                    <span className={clsx(
                      "font-mono-id font-semibold px-2 py-0.5 rounded border transition-colors",
                      status === "COMPLETED" ? "bg-surface-container-low text-on-surface border-outline-variant" :
                      status === "PROCESSING" ? "bg-surface-container text-info border-info" :
                      "bg-surface-container-lowest text-outline-variant border-outline-variant opacity-70"
                    )}>
                      {node.id}
                    </span>
                    <span className={clsx(
                      "font-mono-label transition-colors",
                      status === "COMPLETED" ? "text-on-surface-variant" :
                      status === "PROCESSING" ? "text-info animate-pulse" :
                      "text-outline-variant"
                    )}>
                      {status === "COMPLETED" ? (node.time || 'tamamlandı') : status === "PROCESSING" ? "işleniyor..." : "bekliyor"}
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
      <div className={clsx(
        "flex justify-end gap-gutter pt-stack-sm w-full transition-all duration-700",
        currentStep === 4 ? "opacity-100" : "opacity-0 pointer-events-none"
      )}>
        <button className="h-10 px-4 bg-surface text-on-surface border border-outline-variant rounded font-label-md hover:bg-surface-container-low transition-colors">
          Faturayı görüntüle
        </button>
        {order?.subscriptionId && (
          <Link to={`/subscriptions/${order.subscriptionId}`} className="h-10 px-6 bg-primary text-on-primary rounded font-label-md hover:bg-primary-container hover:text-on-primary-container transition-colors shadow-sm flex items-center justify-center">
            Aboneliğe git
          </Link>
        )}
      </div>

    </div>
  );
}
