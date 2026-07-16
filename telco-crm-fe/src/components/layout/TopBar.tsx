import { useLocation } from "react-router-dom";
import { useEffect, useState } from "react";
import { logoutFromBff } from "../../api/authApi";

export function TopBar() {
  const location = useLocation();
  const [userName, setUserName] = useState("Kullanıcı");
  const [initials, setInitials] = useState("U");
  
  // Format the path to a readable title
  const pathName = location.pathname.split('/')[1];
  const title = pathName ? pathName.charAt(0).toUpperCase() + pathName.slice(1) : 'CRM Console';

  useEffect(() => {
    // BFF OAuth2 flow'da token session'da tutulur, tarayıcıda JWT yok
    // Basit bir varsayılan kullanıcı adı göster
    setUserName("TelcoX Kullanıcısı");
    setInitials("TX");
  }, [location.pathname]);

  const handleLogout = async () => {
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
        <button 
          onClick={handleLogout}
          className="flex items-center gap-2 hover:bg-surface-container-low transition-colors cursor-pointer active:scale-95 p-1 rounded-full pl-3 ml-2 border border-outline-variant"
        >
          <span className="font-label-md text-on-surface-variant hidden sm:block truncate max-w-[120px]">{userName}</span>
          <div className="w-8 h-8 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-bold text-[12px] overflow-hidden shrink-0">
            {initials}
          </div>
        </button>
      </div>
    </header>
  );
}
