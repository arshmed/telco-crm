import { useEffect, useState } from "react";
import clsx from "clsx";
import { AdminTabs } from "../components/admin/AdminTabs";
import { useToast } from "../context/ToastContext";
import { roleLabel } from "../constants/roles";
import {
  getRoles,
  createRole,
  assignPermission,
  getPermissions,
  createPermission,
  RoleResponse,
  PermissionResponse,
} from "../api/identityApi";

export default function RolesPermissions() {
  const { showError, showSuccess } = useToast();
  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);
  const [loadingRoles, setLoadingRoles] = useState(true);
  const [loadingPermissions, setLoadingPermissions] = useState(true);

  const [showNewRoleModal, setShowNewRoleModal] = useState(false);
  const [showNewPermissionModal, setShowNewPermissionModal] = useState(false);
  const [assignPermissionTarget, setAssignPermissionTarget] = useState<RoleResponse | null>(null);

  const fetchRoles = async () => {
    setLoadingRoles(true);
    try {
      setRoles(await getRoles());
    } catch {
      showError("Roller yüklenirken bir hata oluştu.");
    } finally {
      setLoadingRoles(false);
    }
  };

  const fetchPermissions = async () => {
    setLoadingPermissions(true);
    try {
      setPermissions(await getPermissions());
    } catch {
      showError("İzinler yüklenirken bir hata oluştu.");
    } finally {
      setLoadingPermissions(false);
    }
  };

  useEffect(() => {
    fetchRoles();
    fetchPermissions();
  }, []);

  return (
    <div className="flex-1 flex flex-col gap-stack-lg max-w-[1440px] mx-auto w-full">
      <AdminTabs active="roles" />

      {/* Roller */}
      <div className="bg-surface border border-outline-variant rounded flex flex-col overflow-hidden shadow-sm">
        <div className="px-4 h-14 flex items-center justify-between border-b border-outline-variant bg-background/50 shrink-0">
          <h2 className="font-h3 text-on-surface">Roller</h2>
          <button
            onClick={() => setShowNewRoleModal(true)}
            className="h-9 px-3 bg-primary hover:bg-[#0033b3] text-surface font-label-sm rounded flex items-center gap-2 transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">add</span>
            Yeni Rol Ekle
          </button>
        </div>

        {loadingRoles ? (
          <div className="flex flex-col divide-y divide-outline-variant">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-20 px-4 flex items-center">
                <div className="h-4 w-2/3 bg-surface-container-low rounded animate-pulse" />
              </div>
            ))}
          </div>
        ) : roles.length > 0 ? (
          <div className="flex flex-col divide-y divide-outline-variant">
            {roles.map((role) => (
              <div key={role.id} className="p-4 flex flex-col gap-2">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex flex-col gap-1 min-w-0">
                    <span className="font-mono-id uppercase text-primary font-medium">{roleLabel(role.name)}</span>
                    <span className="font-mono-label text-outline-variant">{role.name}</span>
                    {role.description && (
                      <p className="font-body-sm text-on-surface-variant mt-1">{role.description}</p>
                    )}
                  </div>
                  <button
                    onClick={() => setAssignPermissionTarget(role)}
                    className="shrink-0 text-primary hover:bg-primary-container/10 px-2 py-1 rounded font-label-sm transition-colors whitespace-nowrap"
                  >
                    İzin Ekle
                  </button>
                </div>
                <div className="flex flex-wrap gap-1">
                  {role.permissions.length > 0 ? (
                    role.permissions.map((p) => (
                      <span
                        key={p}
                        className="px-2 py-0.5 rounded-sm font-mono-label uppercase bg-secondary-container text-on-secondary-container"
                      >
                        {p}
                      </span>
                    ))
                  ) : (
                    <span className="font-body-sm text-outline-variant italic">İzin atanmamış</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 text-center text-secondary font-body-md">Henüz rol yok.</div>
        )}
      </div>

      {/* İzinler */}
      <div className="bg-surface border border-outline-variant rounded flex flex-col overflow-hidden shadow-sm">
        <div className="px-4 h-14 flex items-center justify-between border-b border-outline-variant bg-background/50 shrink-0">
          <h2 className="font-h3 text-on-surface">İzinler</h2>
          <button
            onClick={() => setShowNewPermissionModal(true)}
            className="h-9 px-3 bg-surface border border-outline-variant text-on-surface font-label-sm rounded flex items-center gap-2 hover:bg-surface-container-low transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">add</span>
            Yeni İzin
          </button>
        </div>

        {loadingPermissions ? (
          <div className="flex flex-col divide-y divide-outline-variant">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-12 px-4 flex items-center">
                <div className="h-4 w-1/2 bg-surface-container-low rounded animate-pulse" />
              </div>
            ))}
          </div>
        ) : permissions.length > 0 ? (
          <div className="flex flex-col divide-y divide-outline-variant">
            {permissions.map((p) => (
              <div key={p.id} className="h-row-height-std px-4 flex items-center justify-between gap-4">
                <span className="font-mono-id text-on-surface">{p.name}</span>
                <span className="font-body-sm text-on-surface-variant truncate">{p.description}</span>
              </div>
            ))}
          </div>
        ) : (
          <div className="p-8 text-center text-secondary font-body-md">Henüz izin yok.</div>
        )}
      </div>

      {showNewRoleModal && (
        <NewRoleModal
          onClose={() => setShowNewRoleModal(false)}
          onCreated={() => {
            setShowNewRoleModal(false);
            showSuccess("Rol başarıyla oluşturuldu.");
            fetchRoles();
          }}
        />
      )}

      {showNewPermissionModal && (
        <NewPermissionModal
          onClose={() => setShowNewPermissionModal(false)}
          onCreated={() => {
            setShowNewPermissionModal(false);
            showSuccess("İzin başarıyla oluşturuldu.");
            fetchPermissions();
          }}
        />
      )}

      {assignPermissionTarget && (
        <AssignPermissionModal
          role={assignPermissionTarget}
          permissions={permissions}
          onClose={() => setAssignPermissionTarget(null)}
          onAssigned={() => {
            setAssignPermissionTarget(null);
            showSuccess("İzin başarıyla role eklendi.");
            fetchRoles();
          }}
        />
      )}
    </div>
  );
}

function NewRoleModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { showError } = useToast();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!name.trim()) {
      setError("Rol adı zorunludur.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await createRole({ name: name.trim(), description: description.trim() || undefined });
      onCreated();
    } catch (err: any) {
      showError(err.response?.data?.detail || "Rol oluşturulurken bir hata oluştu.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-sm w-full shadow-lg flex flex-col gap-4">
        <div className="flex justify-between items-center border-b border-outline-variant pb-2">
          <h3 className="font-h3 text-on-surface">Yeni Rol Ekle</h3>
          <button onClick={onClose} className="text-secondary hover:text-on-surface">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <div className="flex flex-col gap-3">
          <div>
            <label className="font-label-sm text-secondary mb-1 block">Rol Adı</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Örn. REGIONAL_MANAGER"
              className={clsx(
                "w-full border rounded px-3 py-2 bg-surface text-body-sm focus:outline-none transition-colors",
                error ? "border-danger focus:border-danger" : "border-outline-variant focus:border-primary"
              )}
            />
            {error && <p className="font-body-sm text-danger mt-1">{error}</p>}
          </div>
          <div>
            <label className="font-label-sm text-secondary mb-1 block">Açıklama (opsiyonel)</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:outline-none focus:border-primary transition-colors resize-none"
            />
          </div>
        </div>
        <div className="flex gap-2 justify-end">
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

function NewPermissionModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const { showError } = useToast();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    if (!name.trim()) {
      setError("İzin adı zorunludur.");
      return;
    }
    setError("");
    setSubmitting(true);
    try {
      await createPermission({ name: name.trim(), description: description.trim() || undefined });
      onCreated();
    } catch (err: any) {
      showError(err.response?.data?.detail || "İzin oluşturulurken bir hata oluştu.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-sm w-full shadow-lg flex flex-col gap-4">
        <div className="flex justify-between items-center border-b border-outline-variant pb-2">
          <h3 className="font-h3 text-on-surface">Yeni İzin</h3>
          <button onClick={onClose} className="text-secondary hover:text-on-surface">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <div className="flex flex-col gap-3">
          <div>
            <label className="font-label-sm text-secondary mb-1 block">İzin Adı</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Örn. INVOICE_WRITE"
              className={clsx(
                "w-full border rounded px-3 py-2 bg-surface text-body-sm focus:outline-none transition-colors",
                error ? "border-danger focus:border-danger" : "border-outline-variant focus:border-primary"
              )}
            />
            {error && <p className="font-body-sm text-danger mt-1">{error}</p>}
          </div>
          <div>
            <label className="font-label-sm text-secondary mb-1 block">Açıklama (opsiyonel)</label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:outline-none focus:border-primary transition-colors resize-none"
            />
          </div>
        </div>
        <div className="flex gap-2 justify-end">
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

function AssignPermissionModal({
  role,
  permissions,
  onClose,
  onAssigned,
}: {
  role: RoleResponse;
  permissions: PermissionResponse[];
  onClose: () => void;
  onAssigned: () => void;
}) {
  const { showError } = useToast();
  const [selectedPermission, setSelectedPermission] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const availablePermissions = permissions.filter((p) => !role.permissions.includes(p.name));

  const handleAssign = async () => {
    if (!selectedPermission) return;
    setSubmitting(true);
    try {
      await assignPermission(role.name, selectedPermission);
      onAssigned();
    } catch (err: any) {
      showError(err.response?.data?.detail || "İzin eklenirken bir hata oluştu.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-[#0b1e3b]/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-surface border border-outline-variant rounded-lg p-stack-lg max-w-sm w-full shadow-lg flex flex-col gap-4">
        <div className="flex justify-between items-center border-b border-outline-variant pb-2">
          <h3 className="font-h3 text-on-surface">İzin Ekle</h3>
          <button onClick={onClose} className="text-secondary hover:text-on-surface">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <p className="font-body-sm text-on-surface-variant">
          <strong className="text-on-surface">{roleLabel(role.name)}</strong> rolüne yeni bir izin ekleyin.
        </p>
        {availablePermissions.length > 0 ? (
          <select
            value={selectedPermission}
            onChange={(e) => setSelectedPermission(e.target.value)}
            className="w-full border border-outline-variant rounded px-3 py-2 bg-surface text-body-sm focus:border-primary outline-none"
          >
            <option value="">İzin seçin</option>
            {availablePermissions.map((p) => (
              <option key={p.name} value={p.name}>
                {p.name}
              </option>
            ))}
          </select>
        ) : (
          <p className="font-body-sm text-outline-variant italic">Eklenebilecek başka izin yok.</p>
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
            disabled={!selectedPermission || submitting}
            className="h-10 px-4 bg-primary text-on-primary rounded font-label-md hover:bg-[#0033b3] transition-colors disabled:opacity-50"
          >
            {submitting ? "Ekleniyor..." : "Ekle"}
          </button>
        </div>
      </div>
    </div>
  );
}
