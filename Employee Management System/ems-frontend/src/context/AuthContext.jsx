import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import * as authApi from '../api/auth';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null); // { username, roles: [...] } | null
  const [checking, setChecking] = useState(true);

  const refresh = useCallback(async () => {
    try {
      const res = await authApi.me();
      setUser(res.data);
    } catch {
      setUser(null);
    } finally {
      setChecking(false);
    }
  }, []);

  // On first load, ask the backend if our session cookie (if any) is still valid.
  // This doubles as the request that primes the CSRF cookie for later POSTs.
  useEffect(() => {
    refresh();
  }, [refresh]);

  async function login(username, password) {
    const res = await authApi.login(username, password);
    if (res.success) {
      await refresh();
    }
    return res;
  }

  async function logout() {
    await authApi.logout();
    setUser(null);
  }

  function hasRole(role) {
    return !!user?.roles?.some((r) => r === `ROLE_${role}` || r === role);
  }

  function hasAnyRole(roles) {
    return roles.some((role) => hasRole(role));
  }

  // Employees are "ADMIN"/"MANAGER" (org-wide access) or plain "USER" -
  // isStaff distinguishes the two everywhere self-service views differ from admin views.
  const isStaff = hasAnyRole(['ADMIN', 'MANAGER']);

  return (
    <AuthContext.Provider value={{ user, checking, login, logout, hasRole, hasAnyRole, isStaff }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
