import { createContext, useCallback, useContext, useMemo, useState } from "react";

const SidebarContext = createContext(null);

/**
 * Shared open/closed state for the doctor/admin dashboard sidebar. The toggle
 * button lives in the top Navbar; the sidebar itself is rendered deep inside
 * each dashboard page (DashboardLayout) - they don't share a DOM parent, so
 * plain CSS :hover tricks can't bridge them. This context is that bridge.
 *
 * - `pinned`: true once the hamburger is clicked, stays open until clicked
 *   again or a nav link/overlay is used to close it.
 * - `hovering`: true only while the mouse is over the hamburger button or the
 *   sidebar panel itself, and reverts automatically when the mouse leaves both.
 * - `open` = pinned || hovering - what actually drives the visible transform.
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
