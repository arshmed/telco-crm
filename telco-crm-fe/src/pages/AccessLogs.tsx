import { useState } from "react";
import { Link } from "react-router-dom";

export default function AccessLogs() {
  const [logs] = useState([
    { id: 1, date: "2023-10-25 14:32:01", initials: "AT", name: "Ahmet Yılmaz", uid: "USR-8821", action: "Giriş Yapıldı", ip: "192.168.1.45", status: "Başarılı", statusColor: "bg-[#e6f4ea] text-[#137333]", type: "login" },
    { id: 2, date: "2023-10-25 13:58:10", initials: "?", name: "Bilinmeyen Kullanıcı", uid: "Bilinmiyor", action: "Hatalı Şifre Denemesi", ip: "203.0.113.42", status: "Başarısız", statusColor: "bg-error-container text-on-error-container", type: "login" },
    { id: 3, date: "2023-10-25 10:12:45", initials: "CD", name: "Caner Demir", uid: "usr_8892a1", action: "Kullanıcı Güncellendi", ip: "10.0.0.15", status: "Başarılı", statusColor: "bg-[#e6f4ea] text-[#137333]", type: "update" },
    { id: 4, date: "2023-10-24 16:40:22", initials: "BE", name: "Buse Erdem", uid: "usr_3341c9", action: "Şifre Değiştirildi", ip: "192.168.1.112", status: "Başarılı", statusColor: "bg-[#e6f4ea] text-[#137333]", type: "update" },
  ]);

  const [searchTerm, setSearchTerm] = useState("");
  const [filterType, setFilterType] = useState("all");

  const filteredLogs = logs.filter(log => {
    const matchesSearch = log.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          log.uid.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          log.ip.includes(searchTerm);
    const matchesType = filterType === "all" || log.type === filterType;
    return matchesSearch && matchesType;
  });

  return (
    <div className="flex-1 flex flex-col space-y-stack-lg max-w-[1440px] mx-auto w-full">
      {/* Page Header & Tabs */}
      <div className="flex flex-col gap-4">
        <div>
          <h1 className="font-h1 text-on-surface">Kullanıcı ve Rol Yönetimi</h1>
          <p className="font-body-sm text-on-surface-variant mt-1">Sistem erişimlerini ve yetkilendirmeleri yönetin.</p>
        </div>
        
        {/* Sub-tabs */}
        <div className="flex border-b border-outline-variant">
          <Link to="/admin" className="px-4 py-2 font-label-md text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low transition-colors border-b-2 border-transparent">Kullanıcılar</Link>
          <button className="px-4 py-2 font-label-md text-on-surface-variant hover:text-on-surface hover:bg-surface-container-low transition-colors border-b-2 border-transparent">Roller ve İzinler</button>
          <button className="px-4 py-2 font-label-md text-primary font-bold border-b-2 border-primary active:opacity-80 transition-opacity">Erişim Günlükleri</button>
        </div>
      </div>

      {/* Toolbar / Filters */}
      <div className="bg-surface border border-outline-variant rounded p-3 flex flex-wrap items-center gap-3 shadow-sm">
        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-auto">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">calendar_today</span>
          <input 
            type="text" 
            defaultValue="2023-10-24 - 2023-10-25" 
            className="border-none bg-transparent outline-none focus:ring-0 p-0 text-body-sm text-on-surface w-44" 
            placeholder="YYYY-MM-DD - YYYY-MM-DD" 
          />
        </div>
        
        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-auto">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">person</span>
          <input 
            type="text" 
            placeholder="Kullanıcı veya IP Ara..." 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="border-none bg-transparent outline-none focus:ring-0 p-0 text-body-sm text-on-surface w-40" 
          />
        </div>

        <div className="flex items-center gap-2 border border-outline-variant rounded px-2 py-1 bg-surface-container-lowest h-10 w-full sm:w-auto relative">
          <span className="material-symbols-outlined text-outline-variant text-[18px]">filter_list</span>
          <select 
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="border-none bg-transparent outline-none focus:ring-0 p-0 pr-6 text-body-sm text-on-surface appearance-none w-32 cursor-pointer"
          >
            <option value="all">Tüm İşlemler</option>
            <option value="login">Giriş Yapıldı</option>
            <option value="update">Güncellendi</option>
          </select>
          <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 pointer-events-none text-outline-variant text-[16px]">arrow_drop_down</span>
        </div>

        <div className="flex-1"></div>
        <button className="h-10 px-4 bg-surface border border-outline-variant rounded text-on-surface font-label-md flex items-center gap-2 hover:bg-surface-container-highest transition-colors">
          <span className="material-symbols-outlined text-[18px]">download</span>
          Dışa Aktar
        </button>
      </div>

      {/* Data Table */}
      <div className="bg-surface border border-outline-variant rounded flex-1 flex flex-col overflow-hidden shadow-sm">
        <div className="overflow-x-auto flex-1">
          <table className="w-full text-left border-collapse">
            <thead className="bg-background border-b border-outline-variant">
              <tr>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-48">TARİH</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-64">KULLANICI</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3">İŞLEM</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-40">IP ADRESİ</th>
                <th className="font-label-sm text-on-surface-variant font-semibold p-3 w-32">DURUM</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant">
              {filteredLogs.length > 0 ? filteredLogs.map((log) => (
                <tr key={log.id} className="h-row-height-std hover:bg-surface-container-low transition-colors group">
                  <td className="p-3 font-mono-id text-on-surface whitespace-nowrap">{log.date}</td>
                  <td className="p-3">
                    <div className="flex items-center gap-2">
                      <div className={`w-6 h-6 rounded-full flex items-center justify-center font-label-sm text-[10px] flex-shrink-0 ${log.initials === '?' ? 'bg-surface-variant text-on-surface-variant' : 'bg-primary-container text-on-primary-container'}`}>
                        {log.initials === '?' ? <span className="material-symbols-outlined text-[14px]">question_mark</span> : log.initials}
                      </div>
                      <div className="flex flex-col">
                        <span className={`font-body-sm ${log.initials === '?' ? 'italic text-on-surface-variant' : 'text-on-surface'}`}>{log.name}</span>
                        {log.initials !== '?' && <span className="font-mono-label text-on-surface-variant">{log.uid}</span>}
                      </div>
                    </div>
                  </td>
                  <td className="p-3 font-body-sm text-on-surface">{log.action}</td>
                  <td className={`p-3 font-mono-id ${log.status === 'Başarısız' ? 'text-error' : 'text-on-surface'}`}>{log.ip}</td>
                  <td className="p-3">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-medium ${log.statusColor}`}>{log.status}</span>
                  </td>
                </tr>
              )) : (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-secondary font-body-md">Kriterlere uygun kayıt bulunamadı.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        
        {/* Pagination */}
        {filteredLogs.length > 0 && (
          <div className="bg-background border-t border-outline-variant p-3 flex items-center justify-between">
            <span className="font-body-sm text-on-surface-variant">Showing 1 to {filteredLogs.length} of {logs.length} entries</span>
            <div className="flex items-center gap-1">
              <button className="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface-variant hover:bg-surface-container-highest transition-colors disabled:opacity-50" disabled>
                <span className="material-symbols-outlined text-[18px]">chevron_left</span>
              </button>
              <button className="w-8 h-8 flex items-center justify-center rounded bg-primary-container text-surface-lowest text-on-primary-container font-label-sm">1</button>
              <button className="w-8 h-8 flex items-center justify-center rounded border border-outline-variant text-on-surface hover:bg-surface-container-highest transition-colors disabled:opacity-50" disabled>
                <span className="material-symbols-outlined text-[18px]">chevron_right</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
