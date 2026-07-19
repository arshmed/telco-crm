import { useState, useEffect } from "react";
import clsx from "clsx";
import {
  getTariffs, createTariff, updateTariff, deleteTariff, publishTariff,
  getAddons, createAddon, updateAddon, deleteAddon,
  TariffResponse, AddonResponse
} from "../api/catalogApi";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { ROLES } from "../constants/roles";

export default function CatalogManager() {
  const { hasRole } = useAuth();
  const { showError, showSuccess } = useToast();
  const canManageCatalog = hasRole(ROLES.MARKETING_MANAGER);
  const [activeTab, setActiveTab] = useState<"tariffs" | "addons">("tariffs");
  const [tariffs, setTariffs] = useState<TariffResponse[]>([]);
  const [addons, setAddons] = useState<AddonResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingItem, setEditingItem] = useState<any>(null);
  const [formType, setFormType] = useState<"tariff" | "addon">("tariff");

  const [tariffForm, setTariffForm] = useState({
    code: '', name: '', type: 'POSTPAID' as 'POSTPAID' | 'PREPAID',
    segment: 'ALL' as 'INDIVIDUAL' | 'CORPORATE' | 'YOUTH' | 'ALL',
    monthlyFee: 0, currency: 'TRY', minutesIncluded: 0, smsIncluded: 0, dataMbIncluded: 0,
    effectiveFrom: new Date().toISOString().split('T')[0]
  });

  const [addonForm, setAddonForm] = useState({
    code: '', name: '', type: 'DATA' as 'DATA' | 'SMS' | 'MINUTES' | 'VAS',
    price: 0, currency: 'TRY', validityDays: 30
  });

  const fetchData = async () => {
    setLoading(true);
    try {
      const [t, a] = await Promise.all([getTariffs(), getAddons()]);
      setTariffs(t.content || []);
      setAddons(a || []);
    } catch (err) {
      console.error("Katalog verileri çekilemedi", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handlePublish = async (code: string) => {
    try { await publishTariff(code); fetchData(); showSuccess("Tarife başarıyla yayınlandı."); } catch { showError("Yayınlama başarısız."); }
  };

  const handleDelete = async (code: string, type: 'tariff' | 'addon') => {
    if (!confirm("Silmek istediğinize emin misiniz?")) return;
    try {
      if (type === 'tariff') await deleteTariff(code);
      else await deleteAddon(code);
      fetchData();
      showSuccess(type === 'tariff' ? "Tarife başarıyla silindi." : "Addon başarıyla silindi.");
    } catch { showError("Silme başarısız."); }
  };

  const handleSaveTariff = async () => {
    try {
      const wasEditing = !!editingItem;
      if (editingItem) {
        await updateTariff(editingItem.code, tariffForm);
      } else {
        await createTariff(tariffForm);
      }
      setShowForm(false); setEditingItem(null); fetchData();
      showSuccess(wasEditing ? "Tarife başarıyla güncellendi." : "Tarife başarıyla oluşturuldu.");
    } catch { showError("Kaydetme başarısız."); }
  };

  const handleSaveAddon = async () => {
    try {
      const wasEditing = !!editingItem;
      if (editingItem) {
        await updateAddon(editingItem.code, addonForm);
      } else {
        await createAddon(addonForm);
      }
      setShowForm(false); setEditingItem(null); fetchData();
      showSuccess(wasEditing ? "Addon başarıyla güncellendi." : "Addon başarıyla oluşturuldu.");
    } catch { showError("Kaydetme başarısız."); }
  };

  const startEdit = (item: any, type: 'tariff' | 'addon') => {
    setFormType(type);
    setEditingItem(item);
    if (type === 'tariff') {
      setTariffForm({
        code: item.code, name: item.name, type: item.type, segment: item.segment,
        monthlyFee: item.monthlyFee, currency: item.currency,
        minutesIncluded: item.minutesIncluded, smsIncluded: item.smsIncluded,
        dataMbIncluded: item.dataMbIncluded, effectiveFrom: item.effectiveFrom || ''
      });
    } else {
      setAddonForm({
        code: item.code, name: item.name, type: item.type,
        price: item.price, currency: item.currency, validityDays: item.validityDays
      });
    }
    setShowForm(true);
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE': return 'bg-success-bg text-success border border-success/20';
      case 'DRAFT': return 'bg-warning-bg text-warning border border-warning/20';
      case 'RETIRED': return 'bg-surface-container text-on-surface-variant border border-outline-variant';
      default: return 'bg-surface-container text-on-surface-variant border border-outline-variant';
    }
  };

  return (
    <div className="max-w-[1440px] mx-auto flex flex-col gap-stack-lg">
      <div className="flex items-center justify-between">
        <h2 className="font-h1 text-on-background">Ürün Kataloğu</h2>
        {canManageCatalog && (
          <button onClick={() => { setShowForm(true); setEditingItem(null); setFormType(activeTab === 'tariffs' ? 'tariff' : 'addon'); }}
            className="bg-primary text-on-primary font-label-md px-gutter py-2 rounded flex items-center gap-2 hover:bg-primary/90 transition-colors">
            <span className="material-symbols-outlined text-[18px]">add</span>
            {activeTab === 'tariffs' ? 'Yeni Tarife' : 'Yeni Addon'}
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="border-b border-outline-variant">
        <nav className="flex gap-8">
          <button onClick={() => setActiveTab("tariffs")}
            className={clsx("py-3 font-label-md border-b-2 transition-colors",
              activeTab === "tariffs" ? "border-primary text-primary" : "border-transparent text-secondary hover:text-on-surface"
            )}>
            Tarifeler ({tariffs.length})
          </button>
          <button onClick={() => setActiveTab("addons")}
            className={clsx("py-3 font-label-md border-b-2 transition-colors",
              activeTab === "addons" ? "border-primary text-primary" : "border-transparent text-secondary hover:text-on-surface"
            )}>
            Addon'lar ({addons.length})
          </button>
        </nav>
      </div>

      {loading ? (
        <div className="p-8 text-center text-secondary">
          <span className="material-symbols-outlined animate-spin inline-block align-middle mr-2">sync</span> Yükleniyor...
        </div>
      ) : activeTab === "tariffs" ? (
        <div className="bg-surface border border-outline-variant rounded overflow-hidden shadow-sm">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-background border-b border-outline-variant h-10">
                <th className="px-gutter font-label-sm text-secondary">Kod</th>
                <th className="px-gutter font-label-sm text-secondary">Ad</th>
                <th className="px-gutter font-label-sm text-secondary">Tip</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Aylık</th>
                <th className="px-gutter font-label-sm text-secondary text-right">DK</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Data</th>
                <th className="px-gutter font-label-sm text-secondary">Durum</th>
                <th className="px-gutter font-label-sm text-secondary text-right">İşlem</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {tariffs.map((t) => (
                <tr key={t.id} className="h-row-height-std hover:bg-surface-container-low transition-colors">
                  <td className="px-gutter font-mono-id text-primary">{t.code}</td>
                  <td className="px-gutter font-body-sm">{t.name}</td>
                  <td className="px-gutter font-body-sm text-secondary">{t.type}</td>
                  <td className="px-gutter font-mono-id text-right">₺{t.monthlyFee}</td>
                  <td className="px-gutter font-mono-id text-right">{t.minutesIncluded}</td>
                  <td className="px-gutter font-mono-id text-right">{(t.dataMbIncluded / 1024).toFixed(1)} GB</td>
                  <td className="px-gutter">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-label-sm ${getStatusColor(t.status)}`}>{t.status}</span>
                  </td>
                  <td className="px-gutter text-right">
                    {canManageCatalog && (
                      <div className="flex items-center justify-end gap-1">
                        {t.status === 'DRAFT' && (
                          <button onClick={() => handlePublish(t.code)} className="text-success hover:bg-success-bg px-2 py-1 rounded font-label-sm transition-colors">Yayınla</button>
                        )}
                        <button onClick={() => startEdit(t, 'tariff')} className="text-secondary hover:text-primary px-2 py-1 rounded transition-colors">
                          <span className="material-symbols-outlined text-[16px]">edit</span>
                        </button>
                        <button onClick={() => handleDelete(t.code, 'tariff')} className="text-secondary hover:text-danger px-2 py-1 rounded transition-colors">
                          <span className="material-symbols-outlined text-[16px]">delete</span>
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
              {tariffs.length === 0 && (
                <tr><td colSpan={8} className="p-8 text-center text-secondary">Kayıtlı tarife bulunmuyor.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="bg-surface border border-outline-variant rounded overflow-hidden shadow-sm">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-background border-b border-outline-variant h-10">
                <th className="px-gutter font-label-sm text-secondary">Kod</th>
                <th className="px-gutter font-label-sm text-secondary">Ad</th>
                <th className="px-gutter font-label-sm text-secondary">Tip</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Fiyat</th>
                <th className="px-gutter font-label-sm text-secondary text-right">Geçerlilik</th>
                <th className="px-gutter font-label-sm text-secondary text-right">İşlem</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {addons.map((a) => (
                <tr key={a.id} className="h-row-height-std hover:bg-surface-container-low transition-colors">
                  <td className="px-gutter font-mono-id text-primary">{a.code}</td>
                  <td className="px-gutter font-body-sm">{a.name}</td>
                  <td className="px-gutter font-body-sm text-secondary capitalize">{a.type}</td>
                  <td className="px-gutter font-mono-id text-right">₺{a.price}</td>
                  <td className="px-gutter font-mono-id text-right">{a.validityDays} gün</td>
                  <td className="px-gutter text-right">
                    {canManageCatalog && (
                      <div className="flex items-center justify-end gap-1">
                        <button onClick={() => startEdit(a, 'addon')} className="text-secondary hover:text-primary px-2 py-1 rounded transition-colors">
                          <span className="material-symbols-outlined text-[16px]">edit</span>
                        </button>
                        <button onClick={() => handleDelete(a.code, 'addon')} className="text-secondary hover:text-danger px-2 py-1 rounded transition-colors">
                          <span className="material-symbols-outlined text-[16px]">delete</span>
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
              {addons.length === 0 && (
                <tr><td colSpan={6} className="p-8 text-center text-secondary">Kayıtlı addon bulunmuyor.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Form Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-lg w-full shadow-lg flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-outline-variant pb-2">
              <h3 className="font-h3 text-on-surface">{editingItem ? 'Düzenle' : 'Yeni'} {formType === 'tariff' ? 'Tarife' : 'Addon'}</h3>
              <button onClick={() => { setShowForm(false); setEditingItem(null); }} className="text-secondary hover:text-on-surface">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            {formType === 'tariff' ? (
              <div className="flex flex-col gap-3">
                <div className="grid grid-cols-2 gap-3">
                  <div><label className="font-label-sm text-secondary mb-1 block">Kod</label>
                    <input value={tariffForm.code} onChange={e => setTariffForm(p => ({...p, code: e.target.value}))} disabled={!!editingItem}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none disabled:opacity-50" /></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">Ad</label>
                    <input value={tariffForm.name} onChange={e => setTariffForm(p => ({...p, name: e.target.value}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div><label className="font-label-sm text-secondary mb-1 block">Tip</label>
                    <select value={tariffForm.type} onChange={e => setTariffForm(p => ({...p, type: e.target.value as any}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none">
                      <option value="POSTPAID">Postpaid</option><option value="PREPAID">Prepaid</option></select></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">Segment</label>
                    <select value={tariffForm.segment} onChange={e => setTariffForm(p => ({...p, segment: e.target.value as any}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none">
                      <option value="ALL">Tümü</option>
                      <option value="INDIVIDUAL">Bireysel</option>
                      <option value="CORPORATE">Kurumsal</option>
                      <option value="YOUTH">Genç</option></select></div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div><label className="font-label-sm text-secondary mb-1 block">Aylık Ücret (₺)</label>
                    <input type="number" value={tariffForm.monthlyFee} onChange={e => setTariffForm(p => ({...p, monthlyFee: Number(e.target.value)}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div><label className="font-label-sm text-secondary mb-1 block">DK</label>
                    <input type="number" value={tariffForm.minutesIncluded} onChange={e => setTariffForm(p => ({...p, minutesIncluded: Number(e.target.value)}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">SMS</label>
                    <input type="number" value={tariffForm.smsIncluded} onChange={e => setTariffForm(p => ({...p, smsIncluded: Number(e.target.value)}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">Data (MB)</label>
                    <input type="number" value={tariffForm.dataMbIncluded} onChange={e => setTariffForm(p => ({...p, dataMbIncluded: Number(e.target.value)}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-3">
                <div className="grid grid-cols-2 gap-3">
                  <div><label className="font-label-sm text-secondary mb-1 block">Kod</label>
                    <input value={addonForm.code} onChange={e => setAddonForm(p => ({...p, code: e.target.value}))} disabled={!!editingItem}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none disabled:opacity-50" /></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">Ad</label>
                    <input value={addonForm.name} onChange={e => setAddonForm(p => ({...p, name: e.target.value}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div><label className="font-label-sm text-secondary mb-1 block">Tip</label>
                    <select value={addonForm.type} onChange={e => setAddonForm(p => ({...p, type: e.target.value as any}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none">
                      <option value="DATA">DATA</option><option value="SMS">SMS</option><option value="MINUTES">MINUTES</option><option value="VAS">VAS</option></select></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">Fiyat (₺)</label>
                    <input type="number" value={addonForm.price} onChange={e => setAddonForm(p => ({...p, price: Number(e.target.value)}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                  <div><label className="font-label-sm text-secondary mb-1 block">Geçerlilik (gün)</label>
                    <input type="number" value={addonForm.validityDays} onChange={e => setAddonForm(p => ({...p, validityDays: Number(e.target.value)}))}
                      className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none" /></div>
                </div>
              </div>
            )}

            <div className="flex gap-2 mt-2">
              <button onClick={() => { setShowForm(false); setEditingItem(null); }}
                className="flex-1 py-2 border border-outline-variant text-on-surface rounded font-label-md hover:bg-surface-container-low transition-colors">İptal</button>
              <button onClick={formType === 'tariff' ? handleSaveTariff : handleSaveAddon}
                className="flex-1 py-2 bg-primary text-surface rounded font-label-md hover:bg-primary/90 transition-colors flex items-center justify-center gap-2">
                <span className="material-symbols-outlined text-[18px]">save</span> Kaydet
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
