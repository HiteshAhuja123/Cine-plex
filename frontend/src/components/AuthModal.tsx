"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X } from "lucide-react";
import { api } from "@/lib/api";
import { useApp } from "@/lib/context";
import type { User } from "@/lib/types";

interface AuthModalProps {
  open: boolean;
  onClose: () => void;
}

export function AuthModal({ open, onClose }: AuthModalProps) {
  const { setUser, toast } = useApp();
  const [tab, setTab] = useState<"register" | "existing">("register");
  const [loading, setLoading] = useState(false);

  // Register form
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");

  // Existing user form
  const [userId, setUserId] = useState("");

  async function handleRegister() {
    if (!name.trim() || !email.trim()) {
      toast("Name and email are required", "error");
      return;
    }
    setLoading(true);
    try {
      const user = await api.post<User>("/users", {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim() || null,
      });
      setUser(user.id, user.name);
      toast(`Welcome, ${user.name}!`, "success");
      onClose();
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Registration failed", "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin() {
    const id = parseInt(userId, 10);
    if (!id || id < 1) {
      toast("Enter a valid user ID", "error");
      return;
    }
    setLoading(true);
    try {
      const user = await api.get<User>(`/users/${id}`);
      setUser(user.id, user.name);
      toast(`Welcome back, ${user.name}!`, "success");
      onClose();
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Login failed", "error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 bg-black/70 backdrop-blur-sm z-[200] flex items-center justify-center p-4"
          onClick={onClose}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.92, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.92, y: 20 }}
            transition={{ type: "spring", stiffness: 380, damping: 30 }}
            className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-7 w-full max-w-sm"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-start justify-between mb-1">
              <div>
                <h2 className="text-xl font-extrabold">Welcome to CinePlex</h2>
                <p className="text-[var(--text-2)] text-sm mt-1">Sign in to hold and confirm seats</p>
              </div>
              <button
                onClick={onClose}
                className="text-[var(--text-3)] hover:text-[var(--text)] transition-colors p-1 -mt-1 -mr-1"
              >
                <X size={18} />
              </button>
            </div>

            {/* Tabs */}
            <div className="flex bg-[var(--bg)] rounded-lg p-1 mt-5 mb-5">
              {(["register", "existing"] as const).map((t) => (
                <button
                  key={t}
                  onClick={() => setTab(t)}
                  className={`flex-1 py-2 rounded-md text-sm font-semibold transition-all
                    ${tab === t ? "bg-[var(--surface)] text-[var(--text)]" : "text-[var(--text-2)] hover:text-[var(--text)]"}`}
                >
                  {t === "register" ? "Register" : "Have an ID?"}
                </button>
              ))}
            </div>

            <AnimatePresence mode="wait">
              {tab === "register" ? (
                <motion.div
                  key="register"
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 10 }}
                  transition={{ duration: 0.15 }}
                  className="space-y-3"
                >
                  <Field label="Name *" type="text" value={name} onChange={setName} placeholder="Your full name" />
                  <Field label="Email *" type="email" value={email} onChange={setEmail} placeholder="your@email.com" />
                  <Field label="Phone" type="tel" value={phone} onChange={setPhone} placeholder="+1-555-0000" />
                  <div className="flex gap-2 pt-2">
                    <button onClick={onClose} className="btn-secondary flex-1 py-2 rounded-lg text-sm font-semibold">Cancel</button>
                    <button
                      onClick={handleRegister}
                      disabled={loading}
                      className="btn-primary flex-1 py-2 rounded-lg text-sm font-semibold disabled:opacity-40"
                    >
                      {loading ? "Creating…" : "Create Account"}
                    </button>
                  </div>
                </motion.div>
              ) : (
                <motion.div
                  key="existing"
                  initial={{ opacity: 0, x: 10 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -10 }}
                  transition={{ duration: 0.15 }}
                  className="space-y-3"
                >
                  <Field label="User ID" type="number" value={userId} onChange={setUserId} placeholder="e.g. 1, 2, or 3" />
                  <p className="text-xs text-[var(--text-3)]">
                    Seeded users: Alice = 1 · Bob = 2 · Carol = 3
                  </p>
                  <div className="flex gap-2 pt-2">
                    <button onClick={onClose} className="btn-secondary flex-1 py-2 rounded-lg text-sm font-semibold">Cancel</button>
                    <button
                      onClick={handleLogin}
                      disabled={loading}
                      className="btn-primary flex-1 py-2 rounded-lg text-sm font-semibold disabled:opacity-40"
                    >
                      {loading ? "Signing in…" : "Sign In"}
                    </button>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function Field({
  label,
  type,
  value,
  onChange,
  placeholder,
}: {
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  placeholder: string;
}) {
  return (
    <div>
      <label className="block text-xs font-semibold text-[var(--text-2)] mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full bg-[var(--bg)] border border-[var(--border)] rounded-lg px-3 py-2 text-sm text-[var(--text)] placeholder:text-[var(--text-3)] outline-none focus:border-[var(--gold)] transition-colors"
      />
    </div>
  );
}
