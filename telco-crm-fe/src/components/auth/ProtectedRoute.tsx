import type { ReactNode } from "react";
import { useAuth } from "../../context/AuthContext";
import { PageLoader } from "./PageLoader";
import Unauthorized from "../../pages/Unauthorized";

interface ProtectedRouteProps {
  allowedRoles?: string[];
  children: ReactNode;
}

// allowedRoles verilmezse sayfa tüm authenticated kullanıcılara açıktır.
export function ProtectedRoute({ allowedRoles, children }: ProtectedRouteProps) {
  const { hasAnyRole, loading } = useAuth();

  if (loading) {
    return <PageLoader />;
  }

  if (allowedRoles && allowedRoles.length > 0 && !hasAnyRole(allowedRoles)) {
    return <Unauthorized />;
  }

  return <>{children}</>;
}
