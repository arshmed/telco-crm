import { useEffect, useState } from "react";
import clsx from "clsx";
import { AdminTabs } from "../components/admin/AdminTabs";
import { useToast } from "../context/ToastContext";
import { roleLabel } from "../constants/roles";
import {
  getUsers,
  createUser,
  assignRole,
  getRoles,
  UserResponse,
  RoleResponse,
  Page,
  CreateUserRequest,
} from "../api/identityApi";

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: "Aktif",
  INACTIVE: "Pasif",
  SUSPENDED: "Askıya Alındı",
};

const STATUS_CLASSES: Record<string, string> = {
  ACTIVE: "bg-success text-success",
  INACTIVE: "bg-outline-variant text-outline-variant",
  SUSPENDED: "bg-danger text-danger",
};

function formatDateTime(iso: string) {
  return new Date(iso).toLocaleString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function Admin() {
  const { showError, showSuccess } = useToast();
  const [data, setData] = useState<Page<UserResponse> | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [showNewUserModal, setShowNewUserModal] = useState(false);
  const [assignRoleTarget, setAssignRoleTarget] = useState<UserResponse | null>(null);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const result = await getUsers(currentPage, 20);
      setData(result);
    } catch {
      showError("Kullanıcılar yüklenirken bir hata oluştu.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [currentPage]);

  useEffect(() => {
    getRoles()
      .then(setRoles)
      .catch(() => showError("Roller yüklenirken bir hata oluştu."));
  }, []);

  const filteredUsers = (data?.content ?? []).filter(
    (u) =>
      u.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      u.fullName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col gap-stack-md max-w-[1440px] mx-auto w-full">
      <AdminTabs active="users" />

      {/* Toolbar */}
      <div className="bg-surface border border-outline-variant rounded p-3 flex flex-wrap items-center justify-between gap-3 shadow-sm">
        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-72">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">search</span>
          <input
            type="text"
            placeholder="Kullanıcı adı, e-posta veya ad soyad ara..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border-none bg-transparent focus:ring-0 p-0 text-body-sm text-on-surface w-full outline-none"
          />
        </div>
        <button
          onClick={() => setShowNewUserModal(true)}
          className="h-10 px-4 bg-primary hover:bg-[#0033b3] text-surface font-label-sm rounded flex items-center gap-2 transition-colors"
        >
          <span className="material-symbols-outlined text-[16px]">add</span>
          Yeni Kullanıcı
        </button>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded flex-1 flex flex-col overflow-hidden shadow-sm">
        <div className="grid grid-cols-12 gap-4 px-4 h-11 items-center border-b border-outline-variant bg-background/50 shrink-0">
          <div className="col-span-4 font-label-sm text-secondary uppercase tracking-wider">Kullanıcı</div>
          <div className="col-span-3 font-label-sm text-secondary uppercase tracking-wider">Roller</div>
          <div className="col-span-2 font-label-sm text-secondary uppercase tracking-wider">Durum</div>
          <div className="col-span-2 font-label-sm text-secondary uppercase tracking-wider">Oluşturulma</div>
          <div className="col-span-1 font-label-sm text-secondary uppercase tracking-wider text-right">İşlem</div>
        </div>

        {loading ? (
          <div className="flex-1 flex flex-col divide-y divide-outline-variant">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="h-row-height-std px-4 flex items-center">
                <div className="h-4 w-full bg-surface-container-low rounded animate-pulse" />
              </div>
            ))}
          </div>
        ) : filteredUsers.length > 0 ? (
          <div className="flex-1 overflow-auto flex flex-col divide-y divide-outline-variant">
            {filteredUsers.map((user) => (
              <div
                key={user.id}
                className="grid grid-cols-12 gap-4 px-4 h-row-height-std items-center hover:bg-surface-container-low transition-colors"
              >
                <div className="col-span-4 flex flex-col min-w-0">
                  <span className="font-body-sm text-on-surface font-medium truncate">{user.fullName}</span>
                  <span className="font-mono-id text-outline-variant truncate">
                    {user.username} · {user.email}
                  </span>
                </div>
                <div className="col-span-3 flex flex-wrap gap-1">
                  {user.roles.length > 0 ? (
                    user.roles.map((r) => (
                      <span
                        key={r}
                        className="px-2 py-0.5 rounded-sm font-mono-label uppercase bg-primary-container text-on-primary-container"
                      >
                        {roleLabel(r)}
                      </span>
                    ))
                  ) : (
                    <span className="font-body-sm text-outline-variant italic">Rol atanmamış</span>
                  )}
                </div>
                <div className="col-span-2 flex items-center gap-2">
                  <div className={clsx("w-2 h-2 rounded-full", STATUS_CLASSES[user.status]?.split(" ")[0])}></div>
                  <span className={clsx("font-body-sm font-medium", STATUS_CLASSES[user.status]?.split(" ")[1])}>
                    {STATUS_LABELS[user.status] ?? user.status}
                  </span>
                </div>
                <div className="col-span-2 font-body-sm text-on-surface-variant">{formatDateTime(user.createdAt)}</div>
                <div className="col-span-1 flex justify-end">
                  <button
                    onClick={() => setAssignRoleTarget(user)}
                    className="text-primary hover:bg-primary-container/10 px-2 py-1 rounded font-label-sm transition-colors whitespace-nowrap"
                  >
                    Rol Ata
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center p-8 text-center text-secondary font-body-md">
            {searchTerm ? "Aranan kriterlere uygun kullanıcı bulunamadı." : "Henüz kullanıcı yok."}
          </div>
        )}

        {data && data.totalElements > 0 && (
          <div className="h-12 border-t border-outline-variant bg-surface flex items-center justify-between px-gutter shrink-0">
            <span className="font-body-sm text-secondary">Toplam {data.totalElements} kayıt</span>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={data.number === 0}
                className="p-1 rounded text-secondary hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <span className="font-mono-label text-on-surface px-2">
                {data.number + 1} / {data.totalPages === 0 ? 1 : data.totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={data.number >= data.totalPages - 1}
                className="p-1 rounded text-secondary hover:bg-surface-container-low disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
        )}
      </div>

      {showNewUserModal && (
        <NewUserModal
          onClose={() => setShowNewUserModal(false)}
          onCreated={() => {
            setShowNewUserModal(false);
            showSuccess("Kullanıcı başarıyla oluşturuldu.");
            fetchUsers();
          }}
        />
      )}

      {assignRoleTarget && (
        <AssignRoleModal
          user={assignRoleTarget}
          roles={roles}
          onClose={() => setAssignRoleTarget(null)}
          onAssigned={() => {
            setAssignRoleTarget(null);
            showSuccess("Rol başarıyla atandı.");
            fetchUsers();
          }}
        />
      )}
    </div>
  );
}

interface NewUserForm {
  username: string;
  email: string;
  fullName: string;
  phoneNumber: string;
  isCustomer: boolean;
  customerId: string;
}

function NewUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { showError } = useToast();
  const [form, setForm] = useState<NewUserForm>({
    username: "",
    email: "",
    fullName: "",
    phoneNumber: "",
    isCustomer: false,
    customerId: "",
  });
  const [errors, setErrors] = useState<Partial<Record<keyof NewUserForm, string>>>({});
  const [submitting, setSubmitting] = useState(false);

  const validate = () => {
    const next: Partial<Record<keyof NewUserForm, string>> = {};
    if (!form.username.trim()) next.username = "Kullanıcı adı zorunludur.";
    if (!form.email.trim()) next.email = "E-posta zorunludur.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = "Geçerli bir e-posta adresi girin.";
    if (!form.fullName.trim()) next.fullName = "Ad soyad zorunludur.";
    if (form.isCustomer && !form.customerId.trim()) next.customerId = "Müşteri hesabı için müşteri ID zorunludur.";
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    try {
      const payload: CreateUserRequest = {
        username: form.username.trim(),
        email: form.email.trim(),
        fullName: form.fullName.trim(),
        phoneNumber: form.phoneNumber.trim() || undefined,
        customerId: form.isCustomer ? form.customerId.trim() : undefined,
      };
      await createUser(payload);
      onCreated();
    } catch (err: any) {
      showError(err.response?.data?.detail || "Kullanıcı oluşturulurken bir hata oluştu.");
    } finally {
      setSubmitting(false);
    }
  };

  const inputClass = (field: keyof NewUserForm) =>
    clsx(
      "w-full border rounded px-3 py-2 bg-surface text-body-sm focus:outline-none transition-colors",
      errors[field] ? "border-danger focus:border-danger" : "border-outline-variant focus:border-primary"
    );

  return (
    <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-lg w-full shadow-lg flex flex-col gap-4 max-h-[90vh] overflow-y-auto">
        <div className="flex justify-between items-center border-b border-outline-variant pb-2">
          <h3 className="font-h3 text-on-surface">Yeni Kullanıcı</h3>
          <button onClick={onClose} className="text-secondary hover:text-on-surface">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className="flex flex-col gap-3">
          <div>
            <label className="font-label-sm text-secondary mb-1 block">Kullanıcı Adı</label>
            <input
              value={form.username}
              onChange={(e) => setForm((p) => ({ ...p, username: e.target.value }))}
              className={inputClass("username")}
            />
            {errors.username && <p className="font-body-sm text-danger mt-1">{errors.username}</p>}
          </div>
          <div>
            <label className="font-label-sm text-secondary mb-1 block">E-posta</label>
            <input
              value={form.email}
              onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
              className={inputClass("email")}
            />
            {errors.email && <p className="font-body-sm text-danger mt-1">{errors.email}</p>}
          </div>
          <div>
            <label className="font-label-sm text-secondary mb-1 block">Ad Soyad</label>
            <input
              value={form.fullName}
              onChange={(e) => setForm((p) => ({ ...p, fullName: e.target.value }))}
              className={inputClass("fullName")}
            />
            {errors.fullName && <p className="font-body-sm text-danger mt-1">{errors.fullName}</p>}
          </div>
          <div>
            <label className="font-label-sm text-secondary mb-1 block">Telefon (opsiyonel)</label>
            <input
              value={form.phoneNumber}
              onChange={(e) => setForm((p) => ({ ...p, phoneNumber: e.target.value }))}
              className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:outline-none focus:border-primary transition-colors"
            />
          </div>

          <label className="flex items-center gap-2 cursor-pointer mt-1">
            <input
              type="checkbox"
              checked={form.isCustomer}
              onChange={(e) => setForm((p) => ({ ...p, isCustomer: e.target.checked }))}
              className="w-4 h-4 accent-primary"
            />
            <span className="font-body-sm text-on-surface">Bu bir müşteri hesabı mı?</span>
          </label>

          {form.isCustomer && (
            <div>
              <label className="font-label-sm text-secondary mb-1 block">Müşteri ID</label>
              <input
                value={form.customerId}
                onChange={(e) => setForm((p) => ({ ...p, customerId: e.target.value }))}
                placeholder="Müşterinin sistemdeki ID'si (UUID)"
                className={inputClass("customerId")}
              />
              {errors.customerId && <p className="font-body-sm text-danger mt-1">{errors.customerId}</p>}
            </div>
          )}
        </div>

        <div className="flex gap-2 justify-end border-t border-outline-variant pt-3">
          <button
            onClick={onClose}
            className="h-10 px-4 border border-outline-variant rounded bg-surface text-on-surface font-label-md hover:bg-surface-container-low transition-colors"
          >
            İptal
          </button>
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="h-10 px-4 bg-primary text-on-primary rounded font-label-md hover:bg-[#0033b3] transition-colors disabled:opacity-50"
          >
            {submitting ? "Kaydediliyor..." : "Kaydet"}
          </button>
        </div>
      </div>
    </div>
  );
}

function AssignRoleModal({
  user,
  roles,
  onClose,
  onAssigned,
}: {
  user: UserResponse;
  roles: RoleResponse[];
  onClose: () => void;
  onAssigned: () => void;
}) {
  const { showError } = useToast();
  const [selectedRole, setSelectedRole] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const availableRoles = roles.filter((r) => !user.roles.includes(r.name));

  const handleAssign = async () => {
    if (!selectedRole) return;
    setSubmitting(true);
    try {
      await assignRole(user.id, selectedRole);
      onAssigned();
    } catch (err: any) {
      showError(err.response?.data?.detail || "Rol atanırken bir hata oluştu.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-sm w-full shadow-lg flex flex-col gap-4">
        <div className="flex justify-between items-center border-b border-outline-variant pb-2">
          <h3 className="font-h3 text-on-surface">Rol Ata</h3>
          <button onClick={onClose} className="text-secondary hover:text-on-surface">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <p className="font-body-sm text-on-surface-variant">
          <strong className="text-on-surface">{user.fullName}</strong> ({user.username}) kullanıcısına yeni bir rol
          atayın.
        </p>
        {availableRoles.length > 0 ? (
          <select
            value={selectedRole}
            onChange={(e) => setSelectedRole(e.target.value)}
            className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none"
          >
            <option value="">Rol seçin</option>
            {availableRoles.map((r) => (
              <option key={r.name} value={r.name}>
                {roleLabel(r.name)}
              </option>
            ))}
          </select>
        ) : (
          <p className="font-body-sm text-outline-variant italic">Bu kullanıcıya atanabilecek başka rol yok.</p>
        )}
        <div className="flex gap-2 justify-end">
          <button
            onClick={onClose}
            className="h-10 px-4 border border-outline-variant rounded bg-surface text-on-surface font-label-md hover:bg-surface-container-low transition-colors"
          >
            İptal
          </button>
          <button
            onClick={handleAssign}
            disabled={!selectedRole || submitting}
            className="h-10 px-4 bg-primary text-on-primary rounded font-label-md hover:bg-[#0033b3] transition-colors disabled:opacity-50"
          >
            {submitting ? "Atanıyor..." : "Ata"}
          </button>
        </div>
      </div>
    </div>
  );
}
