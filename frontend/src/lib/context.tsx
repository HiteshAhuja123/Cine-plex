"use client";

import {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
} from "react";
import { AnimatePresence, motion } from "framer-motion";

// ── User state ──────────────────────────────────────────────────────────────

interface UserState {
  userId: number | null;
  userName: string | null;
}

interface AppContextValue extends UserState {
  setUser: (id: number, name: string) => void;
  clearUser: () => void;
  toast: (message: string, type?: ToastType) => void;
}

const AppContext = createContext<AppContextValue | null>(null);

export function useApp() {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used inside AppProvider");
  return ctx;
}

// ── Toast ───────────────────────────────────────────────────────────────────

type ToastType = "info" | "success" | "error";

interface ToastItem {
  id: string;
  message: string;
  type: ToastType;
}

function ToastList({ items, onRemove }: { items: ToastItem[]; onRemove: (id: string) => void }) {
  return (
    <div className="fixed bottom-20 left-1/2 -translate-x-1/2 z-[400] flex flex-col gap-2 items-center pointer-events-none px-4 w-full max-w-sm">
      <AnimatePresence>
        {items.map((t) => (
          <motion.div
            key={t.id}
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -10, scale: 0.95 }}
            transition={{ type: "spring", stiffness: 400, damping: 30 }}
            className={`px-4 py-3 rounded-lg text-sm font-medium shadow-xl border text-center w-full
              ${t.type === "error" ? "border-[var(--red)] text-[#ff6b7a] bg-[var(--surface)]" : ""}
              ${t.type === "success" ? "border-[var(--green)] text-[#4ade80] bg-[var(--surface)]" : ""}
              ${t.type === "info" ? "border-[var(--gold)] text-[var(--gold)] bg-[var(--surface)]" : ""}
            `}
          >
            {t.message}
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}

// ── Provider ────────────────────────────────────────────────────────────────

export function AppProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<UserState>({ userId: null, userName: null });
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const setUser = useCallback((id: number, name: string) => {
    setUserState({ userId: id, userName: name });
  }, []);

  const clearUser = useCallback(() => {
    setUserState({ userId: null, userName: null });
  }, []);

  const toast = useCallback((message: string, type: ToastType = "info") => {
    const id = Math.random().toString(36).slice(2);
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3500);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <AppContext.Provider value={{ ...user, setUser, clearUser, toast }}>
      {children}
      <ToastList items={toasts} onRemove={removeToast} />
    </AppContext.Provider>
  );
}
