import { Navigate, Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { TopBar } from "./TopBar";
import { useAuth } from "../../context/AuthContext";
import { PageLoader } from "../auth/PageLoader";

export function AppLayout() {
  const { currentUser, loading } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <PageLoader />
      </div>
    );
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

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
