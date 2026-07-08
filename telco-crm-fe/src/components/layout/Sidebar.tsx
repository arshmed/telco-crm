import { Link, useLocation, useNavigate } from "react-router-dom";
import clsx from "clsx";

const navItems = [
  { path: "/overview", icon: "dashboard", label: "Genel Bakış" },
  { path: "/customers", icon: "person", label: "Müşteriler" },
  { path: "/sales", icon: "trending_up", label: "Satış" },
  { path: "/finance", icon: "payments", label: "Finans" },
  { path: "/support", icon: "support_agent", label: "Destek" },
  { path: "/admin", icon: "settings_suggest", label: "Yönetim" },
];

export function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = (e: React.MouseEvent) => {
    e.preventDefault();
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
    navigate("/login");
  };

  return (
    <aside className="fixed left-0 top-0 h-screen w-sidebar-width bg-on-secondary-fixed border-r border-outline-variant flex flex-col p-4 z-40">
      <div className="mb-stack-lg flex items-center gap-3">
        <div className="w-10 h-10 rounded bg-primary flex items-center justify-center text-surface font-h2 font-bold">
          TX
        </div>
        <div>
          <h1 className="font-h3 text-surface-lowest font-semibold tracking-tight">TelcoX CRM</h1>
          <p className="font-label-sm text-outline-variant">Ops Console v2.4</p>
        </div>
      </div>

      <nav className="flex-1 flex flex-col gap-2 overflow-y-auto">
        {navItems.map((item) => {
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
        <Link
          to="/settings"
          className="flex items-center gap-3 px-3 py-2 text-outline-variant hover:text-surface-bright hover:bg-on-secondary-fixed-variant transition-colors duration-200 rounded"
        >
          <span className="material-symbols-outlined">settings</span>
          <span className="font-label-md">Ayarlar</span>
        </Link>
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
