import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import clsx from "clsx";
import { logoutFromBff } from "../../api/authApi";
import { useAuth } from "../../context/AuthContext";
import { ROLE_LABELS, roleLabel } from "../../constants/roles";

export function TopBar() {
  const location = useLocation();
  const { currentUser } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  // Menü dışına tıklanınca kapat.
  useEffect(() => {
    if (!menuOpen) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [menuOpen]);

  // Format the path to a readable title
  const pathName = location.pathname.split('/')[1];
  const title = pathName ? pathName.charAt(0).toUpperCase() + pathName.slice(1) : 'CRM Console';

  const userName = currentUser?.username ?? "Kullanıcı";
  const appRoles = currentUser?.roles?.filter((role) => role in ROLE_LABELS) ?? [];
  const roleText = appRoles.map(roleLabel).join(" · ");
  const initials = userName.slice(0, 2).toUpperCase();

  const handleLogout = async () => {
    setLoggingOut(true);
    await logoutFromBff();
  };

  return (
    <header className="fixed top-0 right-0 w-[calc(100%-260px)] h-[56px] bg-surface border-b border-outline-variant flex justify-between items-center px-container-padding z-30">
      <div className="flex items-center gap-4">
        <h2 className="font-h3 font-semibold text-on-surface">TelcoX CRM</h2>
        <span className="text-outline-variant">|</span>
        <span className="font-label-sm text-on-surface-variant">
          {title === 'Overview' ? 'Genel Bakış' :
           title === 'Customers' ? 'Müşteriler' :
           title === 'Sales' ? 'Satış' :
           title === 'Finance' ? 'Finans' :
           title === 'Support' ? 'Destek' :
           title === 'Admin' ? 'Yönetim' : title}
        </span>
      </div>

      <div className="flex items-center gap-4">
        <div className="relative" ref={menuRef}>
          <button
            onClick={() => setMenuOpen((v) => !v)}
            aria-expanded={menuOpen}
            className="flex items-center gap-2 hover:bg-surface-container-low transition-colors cursor-pointer active:scale-95 p-1 rounded-full pl-3 ml-2 border border-outline-variant"
          >
            <span className="hidden sm:flex flex-col items-end leading-tight max-w-[220px]">
              <span className="font-label-md text-on-surface truncate w-full text-right">{userName}</span>
              {roleText && (
                <span className="font-label-sm text-on-surface-variant truncate w-full text-right">{roleText}</span>
              )}
            </span>
            <div className="w-8 h-8 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-bold text-[12px] overflow-hidden shrink-0">
              {initials}
            </div>
            <span
              className={clsx(
                "material-symbols-outlined text-[18px] text-on-surface-variant transition-transform",
                menuOpen && "rotate-180"
              )}
            >
              expand_more
            </span>
          </button>

          {menuOpen && (
            <div className="absolute right-0 top-[calc(100%+8px)] w-60 bg-surface border border-outline-variant rounded-lg shadow-lg overflow-hidden z-50">
              <div className="px-4 py-3 border-b border-outline-variant">
                <p className="font-label-md text-on-surface truncate">{userName}</p>
                {roleText && <p className="font-label-sm text-on-surface-variant truncate mt-0.5">{roleText}</p>}
              </div>
              <button
                onClick={handleLogout}
                disabled={loggingOut}
                className="w-full flex items-center gap-2 px-4 py-3 text-danger hover:bg-danger-bg transition-colors disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[18px]">logout</span>
                <span className="font-label-md">{loggingOut ? "Çıkış yapılıyor..." : "Çıkış Yap"}</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
