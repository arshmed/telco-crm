import { Link } from "react-router-dom";
import clsx from "clsx";

const TABS = [
  { key: "users", label: "Kullanıcılar", path: "/admin" },
  { key: "roles", label: "Roller ve İzinler", path: "/admin/roles" },
  { key: "logs", label: "Erişim Günlükleri", path: "/admin/logs" },
] as const;

export type AdminTabKey = (typeof TABS)[number]["key"];

export function AdminTabs({ active }: { active: AdminTabKey }) {
  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="font-h1 text-on-surface">Kullanıcı ve Rol Yönetimi</h1>
        <p className="font-body-sm text-on-surface-variant mt-1">Sistem erişimlerini ve yetkilendirmeleri yönetin.</p>
      </div>
      <div className="flex border-b border-outline-variant">
        {TABS.map((tab) => (
          <Link
            key={tab.key}
            to={tab.path}
            className={clsx(
              "px-4 py-2 font-label-md border-b-2 transition-colors",
              active === tab.key
                ? "text-primary font-bold border-primary"
                : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low border-transparent"
            )}
          >
            {tab.label}
          </Link>
        ))}
      </div>
    </div>
  );
}
