import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";

export function AppLayout() {
  return (
    <div className="min-h-screen flex bg-background">
      <Sidebar />
      <div className="flex-1 ml-sidebar-width flex flex-col min-h-screen">
        <TopBar />
        <main className="flex-1 mt-[56px] p-container-padding overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
