import { useState } from "react";
import { Link } from "react-router-dom";

export default function Admin() {
  const [users] = useState([
    { id: "usr_8892a1", name: "Caner Demir", role: "Sistem Admin", roleClass: "bg-special-bg text-special", lastLogin: "2023-10-24 14:32:01", status: "Aktif", statusColor: "bg-success text-success" },
    { id: "usr_3341c9", name: "Buse Erdem", role: "L1 Destek", roleClass: "bg-info-bg text-info", lastLogin: "2023-10-24 09:15:44", status: "Aktif", statusColor: "bg-success text-success" },
    { id: "usr_1198f4", name: "Ahmet Yılmaz", role: "Müşteri Temsilcisi", roleClass: "bg-primary-container text-on-primary-container", lastLogin: "2023-10-23 16:20:00", status: "Pasif", statusColor: "bg-outline-variant text-outline-variant" }
  ]);
  const [searchTerm, setSearchTerm] = useState("");

  const filteredUsers = users.filter(u => 
    u.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
    u.role.toLowerCase().includes(searchTerm.toLowerCase()) ||
    u.id.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex-1 flex flex-col gap-stack-md max-w-[1440px] mx-auto w-full">
      <div className="flex flex-col gap-4">
        <div>
          <h1 className="font-h1 text-on-surface">Kullanıcı ve Rol Yönetimi</h1>
          <p className="font-body-sm text-on-surface-variant mt-1">Sistem erişimlerini ve yetkilendirmeleri yönetin.</p>
        </div>

        {/* Tabs */}
        <div className="flex border-b border-outline-variant">
          <button className="px-4 py-2 font-label-md text-primary font-bold border-b-2 border-primary active:opacity-80 transition-opacity">Kullanıcılar</button>
          <button className="px-4 py-2 font-label-md text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low transition-colors border-b-2 border-transparent">Roller ve İzinler</button>
          <Link to="/admin/logs" className="px-4 py-2 font-label-md text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low transition-colors border-b-2 border-transparent">Erişim Günlükleri</Link>
        </div>
      </div>

      {/* Toolbar */}
      <div className="bg-surface border border-outline-variant rounded p-3 flex flex-wrap items-center justify-between gap-3 shadow-sm">
        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-64">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">search</span>
          <input 
            type="text" 
            placeholder="İsim, ID veya Rol Ara..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border-none bg-transparent focus:ring-0 p-0 text-body-sm text-on-surface w-full outline-none" 
          />
        </div>
        <button className="h-10 px-4 bg-primary hover:bg-[#0033b3] text-surface font-label-sm rounded flex items-center gap-2 transition-colors">
          <span className="material-symbols-outlined text-[16px]">add</span>
          Yeni Kullanıcı
        </button>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded flex-1 flex flex-col overflow-hidden shadow-sm">
        <div className="grid grid-cols-12 gap-4 px-4 h-11 items-center border-b border-outline-variant bg-background/50">
          <div className="col-span-4 font-label-sm text-secondary uppercase tracking-wider">Kullanıcı</div>
          <div className="col-span-3 font-label-sm text-secondary uppercase tracking-wider">Rol</div>
          <div className="col-span-3 font-label-sm text-secondary uppercase tracking-wider">Son Giriş</div>
          <div className="col-span-2 font-label-sm text-secondary uppercase tracking-wider">Durum</div>
        </div>
        
        <div className="flex-1 overflow-auto flex flex-col divide-y divide-outline-variant">
          {filteredUsers.length > 0 ? filteredUsers.map(user => (
            <div key={user.id} className="grid grid-cols-12 gap-4 px-4 h-row-height-std items-center hover:bg-surface-container-low transition-colors cursor-pointer">
              <div className="col-span-4 flex items-center gap-3">
                <div className="flex flex-col">
                  <span className="font-body-sm text-on-surface font-medium">{user.name}</span>
                  <span className="font-mono-id text-outline-variant">{user.id}</span>
                </div>
              </div>
              <div className="col-span-3">
                <span className={`px-2 py-0.5 rounded-sm font-mono-label uppercase ${user.roleClass}`}>{user.role}</span>
              </div>
              <div className="col-span-3 font-body-sm text-on-surface-variant">{user.lastLogin}</div>
              <div className="col-span-2 flex items-center gap-2">
                <div className={`w-2 h-2 rounded-full ${user.statusColor.split(' ')[0]}`}></div>
                <span className={`font-body-sm font-medium ${user.statusColor.split(' ')[1]}`}>{user.status}</span>
              </div>
            </div>
          )) : (
            <div className="p-8 text-center text-secondary font-body-md">Aranan kriterlere uygun kullanıcı bulunamadı.</div>
          )}
        </div>
      </div>
    </div>
  );
}
