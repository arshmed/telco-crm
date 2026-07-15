import { useState, useEffect } from "react";
import { Link, useParams } from "react-router-dom";
import clsx from "clsx";
import { getCustomerById, CustomerResponse, updateCustomer, approveKyc, rejectKyc, deleteCustomer, uploadDocument, getDocumentTypes, DocumentTypeOption } from "../api/customerApi";
import { listOrders, OrderResponse } from "../api/orderApi";
import { formatDateTime } from "../utils/dateUtils";

export default function CustomerDetail() {
  const { id } = useParams<{ id: string }>(); 
  const [customer, setCustomer] = useState<CustomerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState("Genel Bakış");

  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loadingOrders, setLoadingOrders] = useState(false);

  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [uploadType, setUploadType] = useState('ID_CARD');
  const [uploadFileRef, setUploadFileRef] = useState('');
  const [uploading, setUploading] = useState(false);
  const [docTypes, setDocTypes] = useState<DocumentTypeOption[]>([]);

  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ firstName: '', lastName: '', email: '', phone: '' });
  const [saving, setSaving] = useState(false);

  const fetchOrders = async () => {
    if (!id) return;
    setLoadingOrders(true);
    try {
      const orderData = await listOrders(0, 50, id);
      setOrders(orderData.content || []);
    } catch (err) {
      console.error("Siparişler çekilemedi", err);
    } finally {
      setLoadingOrders(false);
    }
  };

  const handleApproveKyc = async () => {
    if (!id) return;
    try {
      const updated = await approveKyc(id);
      setCustomer(updated);
    } catch {
      alert("KYC onaylanırken bir hata oluştu.");
    }
  };

  const handleRejectKyc = async () => {
    if (!id) return;
    try {
      const updated = await rejectKyc(id);
      setCustomer(updated);
    } catch {
      alert("KYC reddedilirken bir hata oluştu.");
    }
  };

  const handleSaveEdit = async () => {
    if (!id || !customer) return;
    setSaving(true);
    try {
      const updated = await updateCustomer(id, {
        type: customer.type,
        firstName: editForm.firstName,
        lastName: editForm.lastName,
        identityNumber: customer.identityNumber,
        email: editForm.email,
        phone: editForm.phone,
        dateOfBirth: customer.dateOfBirth,
        companyName: customer.companyName,
        taxOffice: customer.taxOffice,
      });
      setCustomer(updated);
      setIsEditing(false);
    } catch {
      alert("Müşteri güncellenirken bir hata oluştu.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!id) return;
    setDeleting(true);
    try {
      await deleteCustomer(id);
      window.location.href = '/customers';
    } catch {
      alert("Müşteri silinirken bir hata oluştu.");
    } finally {
      setDeleting(false);
    }
  };

  const handleUploadDocument = async () => {
    if (!id || !uploadFileRef) return;
    setUploading(true);
    try {
      const newDoc = await uploadDocument(id, { type: uploadType, fileRef: uploadFileRef });
      setCustomer(prev => prev ? { ...prev, documents: [...(prev.documents || []), newDoc] } : prev);
      setIsUploadModalOpen(false);
      setUploadFileRef('');
      setUploadType('ID_CARD');
    } catch {
      alert("Belge yüklenirken bir hata oluştu.");
    } finally {
      setUploading(false);
    }
  };

  const tabs = [
    { id: "Genel Bakış", label: "Genel Bakış" },
    { id: "Siparişler", label: "Siparişler" },
    { id: "Belgeler", label: "Belgeler" },
  ];

  useEffect(() => {
    if (!id) return;
    const fetchData = async () => {
      try {
        setLoading(true);
        const data = await getCustomerById(id);
        setCustomer(data);
        setEditForm({ firstName: data.firstName, lastName: data.lastName, email: data.email, phone: data.phone });
      } catch (err: any) {
        if (err.response?.status === 401) {
          setError("Oturum süreniz doldu. Lütfen token'ınızı yenileyin.");
        } else {
          setError(err.message || "Müşteri bilgileri yüklenirken bir hata oluştu.");
        }
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [id]);

  useEffect(() => {
    if (activeTab === "Siparişler" && id) fetchOrders();
  }, [activeTab, id]);

  useEffect(() => {
    if (isUploadModalOpen && docTypes.length === 0) {
      getDocumentTypes().then(setDocTypes).catch(() => {});
    }
  }, [isUploadModalOpen]);

  if (loading) {
    return <div className="p-8 text-center text-secondary flex justify-center items-center gap-2"><span className="material-symbols-outlined animate-spin">sync</span> Müşteri detayları yükleniyor...</div>;
  }

  if (error || !customer) {
    return <div className="p-8 text-center text-danger">{error || "Müşteri bulunamadı"}</div>;
  }

  const isCorporate = customer.type === "CORPORATE";
  const fullName = isCorporate ? customer.companyName : `${customer.firstName} ${customer.lastName}`;

  const kycStatus = customer.status === 'ACTIVE' ? 'Onaylı KYC' : customer.status === 'PENDING' ? 'KYC Bekliyor' : 'KYC Reddedildi';
  const kycClass = customer.status === 'ACTIVE'
    ? "bg-success-bg text-success border border-success/20"
    : customer.status === 'PENDING'
    ? "bg-warning-bg text-warning border border-warning/20"
    : "bg-danger-bg text-danger border border-danger/20";

  const getOrderStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING_PAYMENT': return 'bg-warning-bg text-warning border border-warning/20';
      case 'PAID': return 'bg-info-bg text-info border border-info/20';
      case 'FULFILLED': return 'bg-success-bg text-success border border-success/20';
      case 'CANCELLED': return 'bg-danger-bg text-danger border border-danger/20';
      default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
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
              {isEditing ? (
                <div className="flex items-center gap-2">
                  <input value={editForm.firstName} onChange={e => setEditForm(p => ({...p, firstName: e.target.value}))} className="border border-outline-variant rounded px-2 py-1 font-body-sm w-28" placeholder="Ad" />
                  <input value={editForm.lastName} onChange={e => setEditForm(p => ({...p, lastName: e.target.value}))} className="border border-outline-variant rounded px-2 py-1 font-body-sm w-28" placeholder="Soyad" />
                </div>
              ) : (
                <h2 className="font-h1 text-on-surface">{fullName}</h2>
              )}
              {customer.status === 'ACTIVE' && <span className="px-2 py-0.5 rounded bg-success-bg text-success font-label-sm border border-success/20">Aktif</span>}
              {customer.status === 'PENDING' && <span className="px-2 py-0.5 rounded bg-warning-bg text-warning font-label-sm border border-warning/20">Beklemede</span>}
              {customer.status === 'REJECTED' && <span className="px-2 py-0.5 rounded bg-danger-bg text-danger font-label-sm border border-danger/20">Reddedildi</span>}
              <span className={clsx("px-2 py-0.5 rounded font-label-sm flex items-center gap-1", kycClass)}>
                <span className="material-symbols-outlined text-[14px]">verified_user</span> {kycStatus}
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
                {isEditing ? (
                  <input value={editForm.phone} onChange={e => setEditForm(p => ({...p, phone: e.target.value}))} className="border border-outline-variant rounded px-2 py-0.5 font-mono-id text-[14px] w-32" />
                ) : (
                  <span className="font-mono-id">{customer.phone || "-"}</span>
                )}
              </div>
              <div className="w-[1px] h-3 bg-outline-variant"></div>
              <div className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[16px]">mail</span>
                {isEditing ? (
                  <input value={editForm.email} onChange={e => setEditForm(p => ({...p, email: e.target.value}))} className="border border-outline-variant rounded px-2 py-0.5 text-[14px] w-48" />
                ) : (
                  <span>{customer.email || "-"}</span>
                )}
              </div>
            </div>
          </div>
        </div>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          {isEditing ? (
            <>
              <button onClick={handleSaveEdit} disabled={saving} className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 bg-primary text-on-primary rounded font-label-md hover:bg-primary/90 transition-colors disabled:opacity-50">
                <span className="material-symbols-outlined text-[18px]">check</span>
                {saving ? "Kaydediliyor..." : "Kaydet"}
              </button>
              <button onClick={() => { setIsEditing(false); setEditForm({ firstName: customer.firstName, lastName: customer.lastName, email: customer.email, phone: customer.phone }); }} className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 border border-outline-variant rounded bg-surface text-on-surface font-label-md hover:bg-surface-container-low transition-colors">
                İptal
              </button>
            </>
          ) : (
            <>
              <button onClick={() => setIsEditing(true)} className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 border border-outline-variant rounded bg-surface text-on-surface font-label-md hover:bg-surface-container-low transition-colors">
                <span className="material-symbols-outlined text-[18px]">edit</span>
                Düzenle
              </button>
              {customer.status === 'PENDING' && (
                <>
                  <button onClick={handleApproveKyc} className="h-10 px-4 flex items-center justify-center gap-4 bg-success text-on-primary rounded font-label-md hover:bg-success/90 transition-colors">
                    <span className="material-symbols-outlined text-[18px]">check_circle</span>
                    KYC Onayla
                  </button>
                  <button onClick={handleRejectKyc} className="h-10 px-4 flex items-center justify-center gap-4 bg-danger text-on-primary rounded font-label-md hover:bg-danger/90 transition-colors">
                    <span className="material-symbols-outlined text-[18px]">cancel</span>
                    KYC Reddet
                  </button>
                </>
              )}
              <Link to={`/sales/new?customer=${id}`} className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 border-none rounded bg-primary text-surface font-label-md hover:bg-[#0033b3] transition-colors shadow-sm">
                <span className="material-symbols-outlined text-[18px]">add</span>
                Yeni İşlem
              </Link>
              <button onClick={() => setIsDeleteModalOpen(true)} className="flex-1 sm:flex-none h-10 px-4 flex items-center justify-center gap-2 border border-danger/20 rounded bg-surface text-danger font-label-md hover:bg-danger-bg transition-colors">
                <span className="material-symbols-outlined text-[18px]">delete</span>
                Sil
              </button>
            </>
          )}
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
            </button>
          ))}
        </nav>
      </div>

      {/* Genel Bakış Tab */}
      {activeTab === "Genel Bakış" && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-gutter">
          <div className="lg:col-span-8 flex flex-col gap-gutter">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-gutter">
              <div className="bg-surface border border-outline-variant rounded flex flex-col shadow-sm">
                <div className="px-stack-lg py-stack-md border-b border-outline-variant">
                  <h3 className="font-h3 text-on-surface">Kimlik bilgileri</h3>
                </div>
                <div className="p-stack-lg grid grid-cols-2 gap-4">
                  <div>
                    <p className="font-label-sm text-secondary mb-1">Müşteri Tipi</p>
                    <p className="font-body-sm text-on-surface">{isCorporate ? "Kurumsal" : "Bireysel"}</p>
                  </div>
                  <div>
                    <p className="font-label-sm text-secondary mb-1">Müşteri No</p>
                    <p className="font-mono-id text-on-surface">{customer.customerNo || "-"}</p>
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
                <div className="px-stack-lg py-stack-md border-b border-outline-variant">
                  <h3 className="font-h3 text-on-surface">İletişim</h3>
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
              <div className="px-stack-lg py-stack-md border-b border-outline-variant">
                <h3 className="font-h3 text-on-surface">Adresler</h3>
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
        </div>
      )}

      {/* Siparişler Tab */}
      {activeTab === "Siparişler" && (
        <div className="bg-surface border border-outline-variant rounded overflow-hidden shadow-sm">
          {loadingOrders ? (
            <div className="p-8 text-center text-secondary">
              <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span>
              Siparişler yükleniyor...
            </div>
          ) : orders.length > 0 ? (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-background border-b border-outline-variant h-10">
                  <th className="px-gutter font-label-sm text-secondary">Sipariş No</th>
                  <th className="px-gutter font-label-sm text-secondary">Durum</th>
                  <th className="px-gutter font-label-sm text-secondary">Kalem</th>
                  <th className="px-gutter font-label-sm text-secondary text-right">Toplam</th>
                  <th className="px-gutter font-label-sm text-secondary text-right">Tarih</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {orders.map((order) => (
                  <tr key={order.id} className="h-row-height-std hover:bg-surface-container-low transition-colors">
                    <td className="px-gutter py-2">
                      <Link to={`/sales/saga/${order.id}`} className="font-mono-id text-primary hover:underline">
                        {order.id.substring(0, 8).toUpperCase()}
                      </Link>
                    </td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getOrderStatusColor(order.status)}`}>
                        {order.status}
                      </span>
                    </td>
                    <td className="px-gutter font-body-sm text-secondary">{order.items?.length || 0} kalem</td>
                    <td className="px-gutter font-mono-id text-right tabular-nums">{order.totalAmount?.toFixed(2)} {order.currency}</td>
                    <td className="px-gutter font-mono-id text-right tabular-nums text-secondary">{formatDateTime(order.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="p-8 text-center text-secondary">
              <span className="material-symbols-outlined block text-[32px] text-surface-container-highest mb-2">receipt_long</span>
              Bu müşteriye ait sipariş bulunmuyor.
            </div>
          )}
        </div>
      )}

      {/* Belgeler Tab */}
      {activeTab === "Belgeler" && (
        <div className="bg-surface border border-outline-variant rounded overflow-hidden shadow-sm">
          {customer.documents && customer.documents.length > 0 ? (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-background border-b border-outline-variant h-10">
                  <th className="px-gutter font-label-sm text-secondary">Belge Tipi</th>
                  <th className="px-gutter font-label-sm text-secondary">Dosya</th>
                  <th className="px-gutter font-label-sm text-secondary">Durum</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {customer.documents.map((doc) => (
                  <tr key={doc.id} className="h-row-height-std">
                    <td className="px-gutter font-body-sm">{doc.type}</td>
                    <td className="px-gutter font-mono-id text-secondary text-[13px]">{doc.fileRef}</td>
                    <td className="px-gutter">
                      <span className={clsx(
                        "inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm border",
                        doc.verifiedAt ? "bg-success-bg text-success border-success/20" : "bg-warning-bg text-warning border-warning/20"
                      )}>
                        {doc.verifiedAt ? "Onaylı" : "Bekliyor"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="p-8 text-center text-secondary">
              <span className="material-symbols-outlined block text-[32px] text-surface-container-highest mb-2">folder_off</span>
              Bu müşteriye ait belge bulunmuyor.
            </div>
          )}
          <div className="p-4 border-t border-outline-variant bg-surface-container-lowest">
            <button onClick={() => setIsUploadModalOpen(true)} className="text-primary hover:bg-primary-container/20 font-label-md px-4 py-1.5 rounded transition-colors flex items-center justify-center gap-2">
              <span className="material-symbols-outlined text-[18px]">upload_file</span>
              Belge Ekle
            </button>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {isDeleteModalOpen && (
        <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-sm w-full shadow-[0_12px_32px_rgba(11,30,59,0.1)] flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-danger-bg flex items-center justify-center">
                <span className="material-symbols-outlined text-danger text-[20px]">warning</span>
              </div>
              <h3 className="font-h3 text-on-surface">Müşteriyi Sil</h3>
            </div>
            <p className="font-body-sm text-on-surface-variant">Bu müşteriyi silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.</p>
            <div className="flex gap-2 mt-2">
              <button onClick={() => setIsDeleteModalOpen(false)} className="flex-1 py-2 border border-outline-variant text-on-surface rounded font-label-md hover:bg-surface-container-low transition-colors">İptal</button>
              <button onClick={handleDelete} disabled={deleting}
                className="flex-1 py-2 bg-danger text-on-primary rounded font-label-md hover:bg-danger/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50">
                {deleting ? <span className="material-symbols-outlined animate-spin text-[18px]">sync</span> : <span className="material-symbols-outlined text-[18px]">delete</span>}
                {deleting ? "Siliniyor..." : "Evet, Sil"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Document Upload Modal */}
      {isUploadModalOpen && (
        <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-md w-full shadow-[0_12px_32px_rgba(11,30,59,0.1)] flex flex-col gap-4">
            <div className="flex justify-between items-center border-b border-outline-variant pb-2">
              <h3 className="font-h3 text-on-surface">Belge Ekle</h3>
              <button onClick={() => setIsUploadModalOpen(false)} className="text-secondary hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="flex flex-col gap-3">
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Belge Tipi</label>
                <select value={uploadType} onChange={(e) => setUploadType(e.target.value)}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none">
                  {docTypes.map(dt => (
                    <option key={dt.code} value={dt.code}>{dt.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Dosya Referansı</label>
                <input value={uploadFileRef} onChange={(e) => setUploadFileRef(e.target.value)}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none"
                  placeholder="Dosya yolu veya URL'si" />
              </div>
            </div>
            <div className="flex gap-2 mt-2">
              <button onClick={() => setIsUploadModalOpen(false)} className="flex-1 py-2 border border-outline-variant text-on-surface rounded font-label-md hover:bg-surface-container-low transition-colors">İptal</button>
              <button onClick={handleUploadDocument} disabled={uploading || !uploadFileRef}
                className="flex-1 py-2 bg-primary text-surface rounded font-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50">
                {uploading ? <span className="material-symbols-outlined animate-spin text-[18px]">sync</span> : <span className="material-symbols-outlined text-[18px]">upload_file</span>}
                {uploading ? "Yükleniyor..." : "Yükle"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
