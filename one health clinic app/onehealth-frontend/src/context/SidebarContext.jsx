import { createContext, useCallback, useContext, useMemo, useState } from "react";

const SidebarContext = createContext(null);

/**
 * Shared open/closed state for the owner/clinic-admin/doctor dashboard
 * sidebar. The toggle button lives in the top Navbar; the sidebar itself is
 * rendered deep inside each dashboard page (DashboardLayout) - they don't
 * share a DOM parent, so plain CSS :hover tricks can't bridge them. This
 * context is that bridge.
 */
export function SidebarProvider({ children }) {
  const [pinned, setPinned] = useState(false);
  const [hovering, setHovering] = useState(false);

  const toggle = useCallback(() => setPinned((p) => !p), []);
  const close = useCallback(() => {
    setPinned(false);
    setHovering(false);
  }, []);
  const setHover = useCallback((value) => setHovering(value), []);

  const value = useMemo(
    () => ({ open: pinned || hovering, pinned, toggle, close, setHover }),
    [pinned, hovering, toggle, close, setHover]
  );

  return <SidebarContext.Provider value={value}>{children}</SidebarContext.Provider>;
}

export function useSidebar() {
  const ctx = useContext(SidebarContext);
  if (!ctx) {
    throw new Error("useSidebar must be used within a SidebarProvider");
  }
  return ctx;
}
