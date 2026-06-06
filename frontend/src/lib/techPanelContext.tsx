"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
import { setTechPanelListener, type ApiLogEntry } from "./techPanelBus";

interface TechPanelContextValue {
  open: boolean;
  setOpen: (v: boolean | ((prev: boolean) => boolean)) => void;
  logs: ApiLogEntry[];
  clearLogs: () => void;
}

const TechPanelContext = createContext<TechPanelContextValue | null>(null);

export function useTechPanel() {
  const ctx = useContext(TechPanelContext);
  if (!ctx) throw new Error("useTechPanel must be used inside TechPanelProvider");
  return ctx;
}

export function TechPanelProvider({ children }: { children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const [logs, setLogs] = useState<ApiLogEntry[]>([]);

  useEffect(() => {
    setTechPanelListener(({ type, entry }) => {
      if (type === "add") {
        setLogs((prev) => [entry, ...prev].slice(0, 60));
      } else {
        setLogs((prev) => prev.map((e) => (e.id === entry.id ? entry : e)));
      }
    });
    return () => setTechPanelListener(null);
  }, []);

  const clearLogs = useCallback(() => setLogs([]), []);

  return (
    <TechPanelContext.Provider value={{ open, setOpen, logs, clearLogs }}>
      {children}
    </TechPanelContext.Provider>
  );
}
