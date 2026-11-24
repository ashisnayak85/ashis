import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem("user");
    const accessToken = localStorage.getItem("accessToken");
    const refreshToken = localStorage.getItem("refreshToken");
    // "user" alone is not proof of being logged in — it's just a display
    // cache. If either token is missing (e.g. a previous session was
    // cleared, or storage got out of sync), don't show a logged-in name;
    // that's what caused the screen to look "logged in" with no data.
    if (!stored || !accessToken || !refreshToken) {
      localStorage.removeItem("user");
      return null;
    }
    return JSON.parse(stored);
  });

  function login(authResponse) {
    const { accessToken, refreshToken, role, organizationId, organizationName, userId, name } = authResponse;
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
    const userInfo = { role, organizationId, organizationName, userId, name };
    localStorage.setItem("user", JSON.stringify(userInfo));
    setUser(userInfo);
  }

  function logout() {
    localStorage.clear();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
