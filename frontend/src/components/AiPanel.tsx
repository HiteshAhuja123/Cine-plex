"use client";

import { useState, useRef, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Bot, X, Send } from "lucide-react";
import { api } from "@/lib/api";
import { useApp } from "@/lib/context";

interface Message {
  role: "user" | "assistant" | "thinking";
  text: string;
}

export function AiPanel() {
  const { userId, toast } = useApp();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      text: 'Hi! I can help you book seats. Try: "Book 2 seats for Inception" or "Find a sci-fi show".\n\nRequires sign-in + GEMINI_API_KEY on the server.',
    },
  ]);
  const [loading, setLoading] = useState(false);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages]);

  async function send() {
    const msg = input.trim();
    if (!msg) return;
    if (!userId) {
      toast("Please sign in to use the AI assistant", "info");
      return;
    }
    setInput("");
    setMessages((prev) => [...prev, { role: "user", text: msg }]);
    setLoading(true);
    setMessages((prev) => [...prev, { role: "thinking", text: "Thinking…" }]);
    try {
      const res = await api.post<{ reply: string }>("/assistant", {
        userId,
        message: msg,
      });
      setMessages((prev) => [
        ...prev.filter((m) => m.role !== "thinking"),
        { role: "assistant", text: res.reply || "Done." },
      ]);
    } catch (e: unknown) {
      setMessages((prev) => [
        ...prev.filter((m) => m.role !== "thinking"),
        {
          role: "assistant",
          text: `Error: ${e instanceof Error ? e.message : "Something went wrong"}`,
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      {/* FAB */}
      <motion.button
        onClick={() => setOpen((v) => !v)}
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.95 }}
        className="fixed bottom-6 right-6 z-[150] w-14 h-14 rounded-full
          bg-gradient-to-br from-[#b00010] to-[var(--red)]
          shadow-[0_4px_24px_rgba(229,9,20,0.45)]
          flex items-center justify-center text-white"
        title="AI Booking Assistant"
      >
        <AnimatePresence mode="wait">
          {open ? (
            <motion.span key="x" initial={{ rotate: -90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: 90, opacity: 0 }}>
              <X size={22} />
            </motion.span>
          ) : (
            <motion.span key="bot" initial={{ rotate: 90, opacity: 0 }} animate={{ rotate: 0, opacity: 1 }} exit={{ rotate: -90, opacity: 0 }}>
              <Bot size={22} />
            </motion.span>
          )}
        </AnimatePresence>
      </motion.button>

      {/* Panel */}
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            transition={{ type: "spring", stiffness: 380, damping: 30 }}
            className="fixed bottom-24 right-6 z-[150] w-80 max-w-[calc(100vw-48px)]
              bg-[var(--surface)] border border-[var(--border)] rounded-xl
              shadow-[0_8px_40px_rgba(0,0,0,0.55)] overflow-hidden"
          >
            {/* Header */}
            <div className="bg-gradient-to-r from-[#b00010] to-[var(--red)] px-4 py-3 flex items-center justify-between">
              <span className="flex items-center gap-2 text-sm font-bold text-white">
                <Bot size={16} /> AI Booking Assistant
              </span>
              <button onClick={() => setOpen(false)} className="text-white/80 hover:text-white transition-colors">
                <X size={16} />
              </button>
            </div>

            {/* Messages */}
            <div ref={scrollRef} className="h-56 overflow-y-auto p-3 flex flex-col gap-2">
              {messages.map((m, i) => (
                <div
                  key={i}
                  className={`max-w-[88%] px-3 py-2 rounded-xl text-xs leading-relaxed whitespace-pre-wrap
                    ${m.role === "user"
                      ? "bg-[var(--red)] text-white self-end rounded-tr-sm"
                      : m.role === "thinking"
                      ? "bg-[var(--surface-2)] text-[var(--text-3)] self-start italic rounded-tl-sm"
                      : "bg-[var(--surface-2)] text-[var(--text-2)] self-start rounded-tl-sm"
                    }`}
                >
                  {m.text}
                </div>
              ))}
            </div>

            {/* Input */}
            <div className="flex gap-2 p-3 border-t border-[var(--border)]">
              <input
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === "Enter" && !loading) send(); }}
                placeholder="Ask anything…"
                className="flex-1 bg-[var(--bg)] border border-[var(--border)] rounded-lg px-3 py-2 text-xs text-[var(--text)] placeholder:text-[var(--text-3)] outline-none focus:border-[var(--red)] transition-colors"
              />
              <button
                onClick={send}
                disabled={loading || !input.trim()}
                className="bg-[var(--red)] hover:bg-[#c10812] disabled:opacity-40 text-white rounded-lg px-3 py-2 transition-colors"
              >
                <Send size={14} />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
