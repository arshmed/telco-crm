import { useLocation } from "react-router-dom";
import { useEffect, useState } from "react";

export function TopBar() {
  const location = useLocation();
  const [userName, setUserName] = useState("Kullanıcı");
  const [initials, setInitials] = useState("U");
  
  // Format the path to a readable title
  const pathName = location.pathname.split('/')[1];
  const title = pathName ? pathName.charAt(0).toUpperCase() + pathName.slice(1) : 'CRM Console';

  useEffect(() => {
    // JWT Token'dan kullanıcı bilgisini (preferred_username veya name) okuyalım
    const token = localStorage.getItem("access_token");
    if (token) {
      try {
        const payloadBase64 = token.split(".")[1];
        if (payloadBase64) {
          const payloadJson = atob(payloadBase64);
          const payload = JSON.parse(payloadJson);
          
          const fullName = payload.name || payload.preferred_username || "Sistem Kullanıcısı";
          setUserName(fullName);
          
          // Ad-Soyad baş harfleri (örn: "Ahmet Yılmaz" -> "AY")
          const parts = fullName.split(" ").filter((p: string) => p.length > 0);
          if (parts.length >= 2) {
            setInitials((parts[0][0] + parts[parts.length - 1][0]).toUpperCase());
          } else {
            setInitials(fullName.substring(0, 2).toUpperCase());
          }
        }
      } catch (e) {
        console.error("Token decode hatası", e);
      }
    }
  }, [location.pathname]); // Sayfa değiştikçe de check edebiliriz (token güncellendiyse)

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
        <div className="relative hidden md:flex items-center">
          <span className="material-symbols-outlined absolute left-2 text-outline text-[18px]">search</span>
          <input 
            type="text" 
            placeholder="Arama..." 
            className="h-[32px] pl-8 pr-3 rounded border border-outline-variant bg-surface-container-lowest focus:border-primary focus:ring-1 focus:ring-primary text-body-sm w-48 outline-none transition-all" 
          />
        </div>
        
        <button className="text-on-surface-variant hover:bg-surface-container-low p-1.5 rounded transition-colors active:scale-95">
          <span className="material-symbols-outlined text-[20px]">notifications</span>
        </button>
        <button className="text-on-surface-variant hover:bg-surface-container-low p-1.5 rounded transition-colors active:scale-95">
          <span className="material-symbols-outlined text-[20px]">help_outline</span>
        </button>
        <button className="text-on-surface-variant hover:bg-surface-container-low p-1.5 rounded transition-colors active:scale-95">
          <span className="material-symbols-outlined text-[20px]">apps</span>
        </button>
        
        <div className="h-6 w-px bg-outline-variant mx-1"></div>
        
        <button className="flex items-center gap-2 hover:bg-surface-container-low transition-colors cursor-pointer active:scale-95 p-1 rounded-full pl-3 ml-2 border border-outline-variant">
          <span className="font-label-md text-on-surface-variant hidden sm:block truncate max-w-[120px]">{userName}</span>
          <div className="w-8 h-8 rounded-full bg-primary-container text-on-primary-container flex items-center justify-center font-bold text-[12px] overflow-hidden shrink-0">
            {initials}
          </div>
        </button>
      </div>
    </header>
  );
}
