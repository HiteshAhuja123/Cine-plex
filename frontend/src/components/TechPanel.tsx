"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, ChevronDown, ChevronRight, Trash2, Database } from "lucide-react";
import { useTechPanel } from "@/lib/techPanelContext";
import type { ApiLogEntry, SqlStep, SqlStepType } from "@/lib/techPanelBus";

// ─── SQL Step colors ──────────────────────────────────────────────────────────

const stepColors: Record<SqlStepType, { bg: string; text: string; label: string }> = {
  tx:    { bg: "bg-[#1e1e35]",           text: "text-[var(--text-3)]",  label: "TXN"   },
  lock:  { bg: "bg-[rgba(245,197,24,0.12)]", text: "text-[var(--gold)]", label: "LOCK"  },
  query: { bg: "bg-[rgba(99,179,237,0.1)]",  text: "text-[#63b3ed]",    label: "READ"  },
  write: { bg: "bg-[rgba(229,9,20,0.1)]",    text: "text-[#ff6b7a]",    label: "WRITE" },
  check: { bg: "bg-[rgba(29,185,84,0.1)]",   text: "text-[var(--green)]", label: "CHK" },
};

const methodColors: Record<string, string> = {
  GET:    "text-[#63b3ed] border-[#63b3ed]",
  POST:   "text-[#b794f4] border-[#b794f4]",
  PUT:    "text-[var(--gold)] border-[var(--gold)]",
  DELETE: "text-[var(--red)] border-[var(--red)]",
  PATCH:  "text-[var(--green)] border-[var(--green)]",
};

// ─── SQL Step block ───────────────────────────────────────────────────────────

function SqlBlock({ step }: { step: SqlStep }) {
  const c = stepColors[step.type];
  return (
    <div className={`rounded-lg overflow-hidden border border-white/5 mb-1.5 last:mb-0`}>
      <div className={`flex items-center gap-2 px-2.5 py-1 ${c.bg}`}>
        <span className={`text-[9px] font-black tracking-widest ${c.text} opacity-80`}>
          {stepColors[step.type].label}
        </span>
        <span className={`text-xs font-semibold ${c.text}`}>{step.label}</span>
      </div>
      <pre className="px-3 py-2 text-[11px] leading-relaxed font-mono text-[var(--text-2)] bg-[var(--bg)] overflow-x-auto whitespace-pre">
        {step.sql}
      </pre>
    </div>
  );
}

// ─── Log Card ─────────────────────────────────────────────────────────────────

function LogCard({ entry }: { entry: ApiLogEntry }) {
  const [expanded, setExpanded] = useState(entry.method === "POST");
  const mc = methodColors[entry.method.toUpperCase()] ?? "text-[var(--text-2)] border-[var(--border)]";
  const ts = new Date(entry.timestamp).toLocaleTimeString("en-US", {
    hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false,
  });

  const statusDot =
    entry.status === "pending" ? (
      <span className="w-2 h-2 rounded-full bg-[var(--gold)] animate-pulse flex-shrink-0" />
    ) : entry.status === "success" ? (
      <span className="w-2 h-2 rounded-full bg-[var(--green)] flex-shrink-0" />
    ) : (
      <span className="w-2 h-2 rounded-full bg-[var(--red)] flex-shrink-0" />
    );

  return (
    <div
      className={`bg-[var(--surface)] border rounded-xl overflow-hidden transition-colors
        ${entry.status === "error" ? "border-[rgba(229,9,20,0.3)]" : "border-[var(--border)]"}
        ${entry.raceLabel ? "border-l-2 border-l-[var(--gold)]" : ""}
      `}
    >
      {/* Card header */}
      <button
        className="w-full flex items-center gap-2 px-3 py-2.5 hover:bg-[var(--surface-2)] transition-colors text-left"
        onClick={() => setExpanded((v) => !v)}
      >
        {statusDot}
        <span className={`text-[10px] font-black border rounded px-1 py-0.5 flex-shrink-0 ${mc}`}>
          {entry.method}
        </span>
        {entry.raceLabel && (
          <span className="text-[9px] font-black text-[var(--gold)] bg-[rgba(245,197,24,0.12)] px-1.5 py-0.5 rounded flex-shrink-0">
            {entry.raceLabel}
          </span>
        )}
        <span className="text-xs text-[var(--text-2)] font-mono flex-1 truncate min-w-0">
          {entry.path}
        </span>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          {entry.durationMs !== undefined && (
            <span className="text-[10px] text-[var(--text-3)]">{entry.durationMs}ms</span>
          )}
          {entry.statusCode && (
            <span className={`text-[10px] font-mono ${entry.status === "error" ? "text-[var(--red)]" : "text-[var(--green)]"}`}>
              {entry.statusCode}
            </span>
          )}
          {expanded ? (
            <ChevronDown size={12} className="text-[var(--text-3)]" />
          ) : (
            <ChevronRight size={12} className="text-[var(--text-3)]" />
          )}
        </div>
      </button>

      {/* Timestamp + error row */}
      <div className="px-3 pb-1 flex items-center gap-2">
        <span className="text-[10px] text-[var(--text-3)] font-mono">{ts}</span>
        {entry.error && (
          <span className="text-[10px] text-[var(--red)] truncate">{entry.error}</span>
        )}
      </div>

      {/* SQL steps */}
      <AnimatePresence initial={false}>
        {expanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.18 }}
            className="overflow-hidden"
          >
            <div className="px-3 pb-3 border-t border-[var(--border)] pt-2.5">
              {entry.sql.map((step, i) => (
                <SqlBlock key={i} step={step} />
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Panel ────────────────────────────────────────────────────────────────────

export function TechPanel() {
  const { open, setOpen, logs, clearLogs } = useTechPanel();

  return (
    <AnimatePresence>
      {open && (
        <>
          {/* Backdrop (mobile) */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[190] bg-black/40 md:hidden"
            onClick={() => setOpen(false)}
          />

          {/* Panel */}
          <motion.aside
            initial={{ x: "100%" }}
            animate={{ x: 0 }}
            exit={{ x: "100%" }}
            transition={{ type: "spring", stiffness: 380, damping: 36 }}
            className="fixed top-14 right-0 bottom-0 z-[200]
              w-full max-w-[440px] flex flex-col
              bg-[var(--surface)] border-l border-[var(--border)]
              shadow-[-8px_0_40px_rgba(0,0,0,0.6)]"
          >
            {/* Header */}
            <div className="flex items-center gap-2 px-4 py-3 border-b border-[var(--border)] flex-shrink-0">
              <Database size={14} className="text-[var(--gold)]" />
              <span className="font-black text-sm text-[var(--text)] flex-1">
                Live Backend
                <span className="text-[var(--text-3)] font-normal ml-1.5 text-xs">
                  · SQL &amp; Lock Trace
                </span>
              </span>

              {logs.length > 0 && (
                <button
                  onClick={clearLogs}
                  title="Clear log"
                  className="text-[var(--text-3)] hover:text-[var(--red)] transition-colors p-1"
                >
                  <Trash2 size={13} />
                </button>
              )}
              <button
                onClick={() => setOpen(false)}
                className="text-[var(--text-3)] hover:text-[var(--text)] transition-colors p-1"
              >
                <X size={16} />
              </button>
            </div>

            {/* Legend */}
            <div className="flex flex-wrap gap-x-3 gap-y-1 px-4 py-2 border-b border-[var(--border)] flex-shrink-0">
              {(Object.entries(stepColors) as [SqlStepType, typeof stepColors[SqlStepType]][]).map(([, c]) => (
                <span key={c.label} className={`text-[10px] font-bold ${c.text}`}>
                  {c.label}
                </span>
              ))}
              <span className="text-[10px] text-[var(--text-3)] ml-auto">newest first</span>
            </div>

            {/* Log list */}
            <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2">
              {logs.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full gap-3 text-center">
                  <Database size={28} className="text-[var(--border)]" />
                  <p className="text-sm text-[var(--text-3)]">
                    No requests yet.
                    <br />
                    Navigate the app to see SQL queries.
                  </p>
                </div>
              ) : (
                logs.map((entry) => <LogCard key={entry.id} entry={entry} />)
              )}
            </div>

            {/* Footer count */}
            {logs.length > 0 && (
              <div className="px-4 py-2 border-t border-[var(--border)] flex-shrink-0">
                <span className="text-[10px] text-[var(--text-3)]">
                  {logs.length} request{logs.length !== 1 ? "s" : ""} captured
                </span>
              </div>
            )}
          </motion.aside>
        </>
      )}
    </AnimatePresence>
  );
}
