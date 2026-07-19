import { Link, useLocation } from "react-router-dom";
import clsx from "clsx";
import { logoutFromBff } from "../../api/authApi";
import { useAuth } from "../../context/AuthContext";
import { ROLES } from "../../constants/roles";

// allowedRoles verilmeyen item herkese (tüm authenticated kullanıcılara) görünür.
const navItems = [
  { path: "/overview", icon: "dashboard", label: "Genel Bakış" },
  { path: "/customers", icon: "person", label: "Müşteriler", allowedRoles: [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER] },
  { path: "/subscriptions", icon: "sim_card", label: "Abonelikler", allowedRoles: [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER, ROLES.CUSTOMER] },
  { path: "/sales", icon: "receipt_long", label: "Siparişler", allowedRoles: [ROLES.CALL_CENTER_AGENT, ROLES.FIELD_DEALER, ROLES.CUSTOMER] },
  { path: "/finance", icon: "payments", label: "Finans", allowedRoles: [ROLES.BILLING_OPERATOR, ROLES.CUSTOMER] },
  { path: "/catalog", icon: "inventory_2", label: "Ürün Kataloğu", allowedRoles: [ROLES.MARKETING_MANAGER] },
  { path: "/support", icon: "support_agent", label: "Destek", allowedRoles: [ROLES.CALL_CENTER_AGENT, ROLES.CUSTOMER] },
  { path: "/admin", icon: "settings_suggest", label: "Yönetim", allowedRoles: [ROLES.SYSTEM_ADMIN] },
];

export function Sidebar() {
  const location = useLocation();
  const { hasAnyRole, loading } = useAuth();

  const handleLogout = async (e: React.MouseEvent) => {
    e.preventDefault();
    await logoutFromBff();
  };

  const visibleItems = navItems.filter((item) => !item.allowedRoles || hasAnyRole(item.allowedRoles));

  return (
    <aside className="fixed left-0 top-0 h-screen w-sidebar-width bg-on-secondary-fixed border-r border-outline-variant flex flex-col p-4 z-40">
      <div className="mb-stack-lg flex items-center gap-3">
        <div className="w-10 h-10 rounded bg-primary flex items-center justify-center text-surface font-h2 font-bold">
          TX
        </div>
        <div>
          <h1 className="font-h3 text-white font-bold tracking-tight">TelcoX CRM</h1>
        </div>
      </div>

      <nav className="flex-1 flex flex-col gap-2 overflow-y-auto">
        {loading
          ? Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="h-10 rounded bg-on-secondary-fixed-variant/40 animate-pulse" />
            ))
          : visibleItems.map((item) => {
              const isActive = location.pathname.startsWith(item.path);
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={clsx(
                    "flex items-center gap-3 px-3 py-2 rounded transition-colors duration-200",
                    isActive
                      ? "bg-on-secondary-fixed-variant text-surface-lowest font-bold border-l-4 border-primary"
                      : "text-outline-variant hover:text-surface-bright hover:bg-on-secondary-fixed-variant"
                  )}
                >
                  <span className={clsx("material-symbols-outlined", isActive && "icon-fill")}>{item.icon}</span>
                  <span className="font-label-md">{item.label}</span>
                </Link>
              );
            })}
      </nav>

      <div className="mt-auto flex flex-col gap-2 pt-4 border-t border-on-secondary-fixed-variant">
        <button
          onClick={handleLogout}
          className="flex items-center gap-3 px-3 py-2 text-outline-variant hover:text-error hover:bg-error-container/20 transition-colors duration-200 rounded w-full text-left"
        >
          <span className="material-symbols-outlined">logout</span>
          <span className="font-label-md">Çıkış Yap</span>
        </button>
      </div>
    </aside>
  );
}
