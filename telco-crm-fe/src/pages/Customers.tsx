import { useEffect, useState, useMemo } from "react";
import { Link } from "react-router-dom";
import { getCustomers, createCustomer, CustomerResponse, CustomerRequest, Page } from "../api/customerApi";
import { formatDateTime } from "../utils/dateUtils";

const CITIES = [
  "Adana", "Ankara", "Antalya", "Bursa", "Denizli", "Diyarbakır",
  "Erzurum", "Eskişehir", "Gaziantep", "İstanbul", "İzmir", "Kayseri",
  "Kocaeli", "Konya", "Malatya", "Mersin", "Muğla", "Samsun", "Şanlıurfa",
  "Sivas", "Trabzon", "Van"
];

const STATUS_CHIPS = [
  { key: "all", label: "Tümü" },
  { key: "ACTIVE", label: "Aktif" },
  { key: "PENDING", label: "KYC bekliyor" },
  { key: "REJECTED", label: "Reddedildi" },
];

type FormErrors = {
  firstName?: string;
  lastName?: string;
  identityNumber?: string;
  dateOfBirth?: string;
  email?: string;
  phone?: string;
  companyName?: string;
  taxOffice?: string;
};

function validateForm(customer: CustomerRequest): FormErrors {
  const errors: FormErrors = {};

  if (!customer.firstName.trim()) {
    errors.firstName = "Ad zorunludur";
  } else if (customer.firstName.trim().length > 100) {
    errors.firstName = "Ad en fazla 100 karakter olabilir";
  }

  if (!customer.lastName.trim()) {
    errors.lastName = "Soyad zorunludur";
  } else if (customer.lastName.trim().length > 100) {
    errors.lastName = "Soyad en fazla 100 karakter olabilir";
  }

  if (!customer.identityNumber.trim()) {
    errors.identityNumber = "TCKN/VKN zorunludur";
  } else if (!/^\d+$/.test(customer.identityNumber)) {
    errors.identityNumber = "Sadece rakam girilebilir";
  } else if (customer.type === "INDIVIDUAL" && customer.identityNumber.length !== 11) {
    errors.identityNumber = "Bireysel müşteriler için TCKN 11 haneli olmalıdır";
  } else if (customer.type === "CORPORATE" && customer.identityNumber.length !== 10) {
    errors.identityNumber = "Kurumsal müşteriler için VKN 10 haneli olmalıdır";
  }

  if (!customer.dateOfBirth) {
    errors.dateOfBirth = "Doğum tarihi zorunludur";
  } else {
    const dob = new Date(customer.dateOfBirth);
    if (isNaN(dob.getTime())) {
      errors.dateOfBirth = "Geçerli bir tarih girin";
    } else if (dob >= new Date()) {
      errors.dateOfBirth = "Doğum tarihi bugünden önce olmalıdır";
    }
  }

  if (!customer.email.trim()) {
    errors.email = "E-posta zorunludur";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(customer.email)) {
    errors.email = "Geçerli bir e-posta adresi girin";
  }

  if (customer.phone && customer.phone.trim()) {
    const cleaned = customer.phone.replace(/[\s\-\(\)]/g, "");
    if (!/^(\+?90|0)?[5][0-9]{9}$/.test(cleaned)) {
      errors.phone = "Geçerli bir telefon numarası girin (ör: 532 847 74 63)";
    }
  }

  if (customer.type === "CORPORATE") {
    if (!customer.companyName?.trim()) {
      errors.companyName = "Şirket adı zorunludur";
    }
    if (!customer.taxOffice?.trim()) {
      errors.taxOffice = "Vergi dairesi zorunludur";
    }
  }

  return errors;
}

export default function Customers() {
  const [data, setData] = useState<Page<CustomerResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusChip, setStatusChip] = useState("all");
  const [cityFilter, setCityFilter] = useState("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [showNewCustomer, setShowNewCustomer] = useState(false);
  const [newCustomer, setNewCustomer] = useState<CustomerRequest>({
    type: 'INDIVIDUAL', firstName: '', lastName: '', identityNumber: '', dateOfBirth: '', email: '', phone: ''
  });
  const [creating, setCreating] = useState(false);
  const [formErrors, setFormErrors] = useState<FormErrors>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  useEffect(() => {
    const fetchCustomers = async () => {
      try {
        setLoading(true);
        const result = await getCustomers(currentPage);
        setData(result);
      } catch (err: any) {
        setError(err.response?.status === 401
          ? "Oturum süreniz dolmuş veya yetkisiz erişim. Lütfen giriş yapın."
          : "Müşteri listesi çekilirken bir hata oluştu. Backend servislerinin çalıştığından emin olun.");
      } finally {
        setLoading(false);
      }
    };

    fetchCustomers();
  }, [currentPage]);

  const filteredData = useMemo(() => {
    if (!data?.content) return [];
    return data.content.filter((c) => {
      if (statusChip !== "all" && c.status !== statusChip) return false;
      if (cityFilter !== "all") {
        const customerCity = c.addresses?.find((a) => a.isDefault)?.city || c.addresses?.[0]?.city || "";
        if (customerCity !== cityFilter) return false;
      }
      if (typeFilter !== "all" && c.type !== typeFilter) return false;
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const name = c.type === "CORPORATE" ? c.companyName || "" : `${c.firstName} ${c.lastName}`;
        const match = name.toLowerCase().includes(q) || c.identityNumber?.includes(q) || c.phone?.includes(q) || (c.customerNo || '').toLowerCase().includes(q);
        if (!match) return false;
      }
      return true;
    });
  }, [data, statusChip, cityFilter, typeFilter, searchQuery]);

  const getKycStatus = (customer: CustomerResponse) => {
    switch (customer.status) {
      case 'ACTIVE': return { label: "Onaylı", className: "bg-success-bg text-success border border-success/20" };
      case 'PENDING': return { label: "Bekliyor", className: "bg-warning-bg text-warning border border-warning/20" };
      case 'REJECTED': return { label: "Reddedildi", className: "bg-danger-bg text-danger border border-danger/20" };
      default: return { label: "Eksik", className: "bg-surface-container text-on-surface-variant border border-outline-variant" };
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-success-bg text-success border border-success/20';
      case 'PENDING': return 'bg-warning-bg text-warning border border-warning/20';
      case 'REJECTED':
      case 'CANCELLED': return 'bg-danger-bg text-danger border border-danger/20';
      default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'Aktif';
      case 'PENDING': return 'Beklemede';
      case 'REJECTED': return 'Reddedildi';
      case 'CANCELLED': return 'İptal';
      default: return status;
    }
  };

  const handleFieldChange = (field: string, value: string) => {
    setNewCustomer(p => ({ ...p, [field]: value }));
    setTouched(p => ({ ...p, [field]: true }));
    const updated = { ...newCustomer, [field]: value };
    setFormErrors(validateForm(updated));
  };

  const handleTypeChange = (type: 'INDIVIDUAL' | 'CORPORATE') => {
    setNewCustomer(p => ({ ...p, type, identityNumber: '', companyName: '', taxOffice: '' }));
    setTouched({});
    setFormErrors({});
  };

  const isFormValid = useMemo(() => {
    return Object.keys(validateForm(newCustomer)).length === 0;
  }, [newCustomer]);

  const resetForm = () => {
    setNewCustomer({ type: 'INDIVIDUAL', firstName: '', lastName: '', identityNumber: '', dateOfBirth: '', email: '', phone: '' });
    setFormErrors({});
    setTouched({});
  };

  const handleSubmit = async () => {
    const errors = validateForm(newCustomer);
    setFormErrors(errors);
    setTouched({ firstName: true, lastName: true, identityNumber: true, dateOfBirth: true, email: true, phone: true, companyName: true, taxOffice: true });
    if (Object.keys(errors).length > 0) return;

    setCreating(true);
    try {
      await createCustomer(newCustomer);
      setShowNewCustomer(false);
      resetForm();
      const result = await getCustomers(0);
      setData(result);
    } catch (err: any) {
      const msg = err.response?.data?.message || err.response?.data?.detail || "Müşteri oluşturulurken bir hata oluştu.";
      alert(msg);
    } finally {
      setCreating(false);
    }
  };

  const fieldClass = (field: keyof FormErrors) =>
    `w-full border rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none transition-colors ${
      touched[field] && formErrors[field] ? 'border-danger' : 'border-outline-variant'
    }`;

  return (
    <div className="max-w-[1440px] mx-auto flex flex-col gap-stack-lg">
      <div className="flex items-center justify-between">
        <h2 className="font-h1 text-on-background">Müşteriler</h2>
        <button
          onClick={() => setShowNewCustomer(true)}
          className="bg-primary text-on-primary font-label-md px-gutter py-2 rounded flex items-center gap-2 hover:bg-primary/90 transition-colors"
        >
          <span className="material-symbols-outlined text-[18px]">add</span>
          Yeni müşteri
        </button>
      </div>

      {/* Status Chips */}
      <div className="flex items-center gap-2 flex-wrap">
        {STATUS_CHIPS.map((chip) => (
          <button
            key={chip.key}
            onClick={() => { setStatusChip(chip.key); setCurrentPage(0); }}
            className={`px-3 py-1.5 rounded-full font-label-sm transition-colors ${
              statusChip === chip.key
                ? "bg-primary text-on-primary shadow-sm"
                : "bg-surface-container-low text-on-surface-variant border border-outline-variant hover:bg-surface-container"
            }`}
          >
            {chip.label}
          </button>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="bg-surface border border-outline-variant rounded p-stack-lg flex flex-col gap-stack-md shadow-sm">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-gutter">
          <div className="md:col-span-2 flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Arama</label>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline">search</span>
              <input
                type="text"
                placeholder="Ad, MSISDN veya TCKN"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-10 pl-10 pr-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm focus:border-primary focus:outline-none transition-colors"
              />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Şehir</label>
            <select
              value={cityFilter}
              onChange={(e) => { setCityFilter(e.target.value); setCurrentPage(0); }}
              className="w-full h-10 px-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm appearance-none focus:border-primary focus:outline-none cursor-pointer"
            >
              <option value="all">Tüm şehirler</option>
              {CITIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label className="font-label-sm text-secondary">Müşteri Tipi</label>
            <select
              value={typeFilter}
              onChange={(e) => { setTypeFilter(e.target.value); setCurrentPage(0); }}
              className="w-full h-10 px-3 border border-outline-variant rounded bg-surface-container-lowest text-body-sm appearance-none focus:border-primary focus:outline-none cursor-pointer"
            >
              <option value="all">Tümü</option>
              <option value="INDIVIDUAL">Bireysel</option>
              <option value="CORPORATE">Kurumsal</option>
            </select>
          </div>
        </div>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded overflow-hidden flex flex-col shadow-sm">
        <div className="w-full overflow-x-auto">
          <table className="w-full text-left border-collapse min-w-[800px]">
            <thead>
              <tr className="bg-background border-b border-outline-variant h-10">
                <th className="px-gutter font-label-sm text-secondary w-1/4">Müşteri</th>
                <th className="px-gutter font-label-sm text-secondary">Tip</th>
                <th className="px-gutter font-label-sm text-secondary">E-posta</th>
                <th className="px-gutter font-label-sm text-secondary">KYC</th>
                <th className="px-gutter font-label-sm text-secondary">Durum</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Kayıt Tarihi</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">

              {loading ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-secondary font-body-md">
                    <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span>
                    Yükleniyor...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-danger font-body-md bg-danger-bg/50">
                    <div className="flex items-center justify-center gap-2">
                      <span className="material-symbols-outlined">error</span>
                      {error}
                    </div>
                  </td>
                </tr>
              ) : filteredData.length > 0 ? (
                filteredData.map((customer) => (
                  <tr key={customer.id} className="h-row-height-std hover:bg-surface-container-low transition-colors group">
                    <td className="px-gutter py-2">
                      <Link to={`/customers/${customer.id}`} className="flex flex-col group-hover:cursor-pointer">
                        <span className="font-body-sm text-on-surface font-medium group-hover:text-primary transition-colors">
                          {customer.type === 'CORPORATE' ? customer.companyName : `${customer.firstName} ${customer.lastName}`}
                        </span>
                        <span className="font-mono-id text-secondary mt-0.5">
                          {customer.customerNo || (customer.identityNumber ? customer.identityNumber.substring(0, 3) + '•••••••' : customer.id.substring(0,8))}
                        </span>
                      </Link>
                    </td>
                    <td className="px-gutter font-body-sm">
                      {customer.type === 'CORPORATE' ? 'Kurumsal' : 'Bireysel'}
                    </td>
                    <td className="px-gutter font-body-sm text-secondary">
                      {customer.email}
                    </td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getKycStatus(customer).className}`}>
                        {getKycStatus(customer).label}
                      </span>
                    </td>
                    <td className="px-gutter">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getStatusColor(customer.status)}`}>
                        {getStatusLabel(customer.status)}
                      </span>
                    </td>
                    <td className="px-gutter font-mono-id text-right tabular-nums text-secondary">
                      {formatDateTime(customer.createdAt)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-secondary font-body-md">
                    Henüz hiç müşteri bulunmuyor.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        {data && (
          <div className="h-12 border-t border-outline-variant bg-surface flex items-center justify-between px-gutter">
            <span className="font-body-sm text-secondary">Toplam {data.totalElements} kayıt bulundu</span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentPage(p => Math.max(0, p - 1))}
                disabled={data.number === 0}
                className="p-1 rounded text-secondary hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <span className="font-mono-label text-on-surface px-2">
                {data.number + 1} / {data.totalPages === 0 ? 1 : data.totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(p => Math.min(data.totalPages - 1, p + 1))}
                disabled={data.number >= data.totalPages - 1}
                className="p-1 rounded text-secondary hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Yeni Müşteri Modal */}
      {showNewCustomer && (
        <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-md w-full shadow-lg flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-outline-variant pb-2">
              <h3 className="font-h3 text-on-surface">Yeni Müşteri</h3>
              <button onClick={() => { setShowNewCustomer(false); resetForm(); }} className="text-secondary hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="flex flex-col gap-3">
              {/* Müşteri Tipi */}
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Müşteri Tipi</label>
                <select value={newCustomer.type} onChange={e => handleTypeChange(e.target.value as 'INDIVIDUAL' | 'CORPORATE')}
                  className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none">
                  <option value="INDIVIDUAL">Bireysel</option>
                  <option value="CORPORATE">Kurumsal</option>
                </select>
              </div>

              {/* Ad / Soyad */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-label-sm text-secondary mb-1 block">Ad *</label>
                  <input value={newCustomer.firstName} onChange={e => handleFieldChange('firstName', e.target.value)}
                    className={fieldClass('firstName')} placeholder="Adınız" />
                  {touched.firstName && formErrors.firstName && (
                    <p className="text-danger text-[11px] mt-1">{formErrors.firstName}</p>
                  )}
                </div>
                <div>
                  <label className="font-label-sm text-secondary mb-1 block">Soyad *</label>
                  <input value={newCustomer.lastName} onChange={e => handleFieldChange('lastName', e.target.value)}
                    className={fieldClass('lastName')} placeholder="Soyadınız" />
                  {touched.lastName && formErrors.lastName && (
                    <p className="text-danger text-[11px] mt-1">{formErrors.lastName}</p>
                  )}
                </div>
              </div>

              {/* TCKN/VKN */}
              <div>
                <label className="font-label-sm text-secondary mb-1 block">
                  {newCustomer.type === 'INDIVIDUAL' ? 'TCKN *' : 'VKN *'}
                </label>
                <input value={newCustomer.identityNumber} onChange={e => handleFieldChange('identityNumber', e.target.value.replace(/\D/g, ''))}
                  className={fieldClass('identityNumber')}
                  placeholder={newCustomer.type === 'INDIVIDUAL' ? '11 haneli TCKN' : '10 haneli VKN'}
                  maxLength={newCustomer.type === 'INDIVIDUAL' ? 11 : 10} />
                {touched.identityNumber && formErrors.identityNumber && (
                  <p className="text-danger text-[11px] mt-1">{formErrors.identityNumber}</p>
                )}
              </div>

              {/* Doğum Tarihi */}
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Doğum Tarihi *</label>
                <input type="date" value={newCustomer.dateOfBirth || ''} onChange={e => handleFieldChange('dateOfBirth', e.target.value)}
                  className={fieldClass('dateOfBirth')} />
                {touched.dateOfBirth && formErrors.dateOfBirth && (
                  <p className="text-danger text-[11px] mt-1">{formErrors.dateOfBirth}</p>
                )}
              </div>

              {/* E-posta */}
              <div>
                <label className="font-label-sm text-secondary mb-1 block">E-posta *</label>
                <input type="email" value={newCustomer.email} onChange={e => handleFieldChange('email', e.target.value)}
                  className={fieldClass('email')} placeholder="ornek@email.com" />
                {touched.email && formErrors.email && (
                  <p className="text-danger text-[11px] mt-1">{formErrors.email}</p>
                )}
              </div>

              {/* Telefon */}
              <div>
                <label className="font-label-sm text-secondary mb-1 block">Telefon</label>
                <input value={newCustomer.phone || ''} onChange={e => handleFieldChange('phone', e.target.value)}
                  className={fieldClass('phone')} placeholder="532 847 74 63" />
                {touched.phone && formErrors.phone && (
                  <p className="text-danger text-[11px] mt-1">{formErrors.phone}</p>
                )}
              </div>

              {/* Kurumsal Alanlar */}
              {newCustomer.type === 'CORPORATE' && (
                <>
                  <div>
                    <label className="font-label-sm text-secondary mb-1 block">Şirket Adı *</label>
                    <input value={newCustomer.companyName || ''} onChange={e => handleFieldChange('companyName', e.target.value)}
                      className={fieldClass('companyName')} placeholder="Şirket adı" />
                    {touched.companyName && formErrors.companyName && (
                      <p className="text-danger text-[11px] mt-1">{formErrors.companyName}</p>
                    )}
                  </div>
                  <div>
                    <label className="font-label-sm text-secondary mb-1 block">Vergi Dairesi *</label>
                    <input value={newCustomer.taxOffice || ''} onChange={e => handleFieldChange('taxOffice', e.target.value)}
                      className={fieldClass('taxOffice')} placeholder="Vergi dairesi" />
                    {touched.taxOffice && formErrors.taxOffice && (
                      <p className="text-danger text-[11px] mt-1">{formErrors.taxOffice}</p>
                    )}
                  </div>
                </>
              )}
            </div>

            <div className="flex gap-2 mt-2">
              <button onClick={() => { setShowNewCustomer(false); resetForm(); }}
                className="flex-1 py-2 border border-outline-variant text-on-surface rounded font-label-md hover:bg-surface-container-low transition-colors">
                İptal
              </button>
              <button onClick={handleSubmit}
                disabled={creating || !isFormValid}
                className="flex-1 py-2 bg-primary text-surface rounded font-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2 disabled:opacity-50">
                {creating ? <span className="material-symbols-outlined animate-spin text-[18px]">sync</span> : <span className="material-symbols-outlined text-[18px]">add</span>}
                {creating ? "Oluşturuluyor..." : "Oluştur"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
