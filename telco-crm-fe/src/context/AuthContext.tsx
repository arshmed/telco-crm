import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { fetchCurrentUser, type CurrentUser } from "../api/authApi";

interface AuthContextValue {
  currentUser: CurrentUser | null;
  roles: string[];
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
  loading: boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(window.location.pathname !== "/login");

  useEffect(() => {
    if (window.location.pathname === "/login") {
      return;
    }
    let cancelled = false;
    fetchCurrentUser()
      .then((user) => {
        if (!cancelled) setCurrentUser(user);
      })
      .catch(() => {
        // 401 durumunda apiClient interceptor'ı zaten /login'e yönlendiriyor.
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const roles = useMemo(() => currentUser?.roles ?? [], [currentUser]);
  const hasRole = useCallback((role: string) => roles.includes(role), [roles]);
  const hasAnyRole = useCallback(
    (allowedRoles: string[]) => allowedRoles.some((role) => roles.includes(role)),
    [roles]
  );

  const value = useMemo(
    () => ({ currentUser, roles, hasRole, hasAnyRole, loading }),
    [currentUser, roles, hasRole, hasAnyRole, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth, bir AuthProvider içinde kullanılmalı");
  }
  return ctx;
}
