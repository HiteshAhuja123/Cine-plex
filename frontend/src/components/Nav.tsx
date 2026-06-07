"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { Menu, X, User, LogOut, Database } from "lucide-react";
import { useApp } from "@/lib/context";
import { useTechPanel } from "@/lib/techPanelContext";
import { AuthModal } from "./AuthModal";

export function Nav() {
  const pathname = usePathname();
  const { userId, userName, clearUser, toast } = useApp();
  const { open: techOpen, setOpen: setTechOpen } = useTechPanel();
  const [authOpen, setAuthOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  function handleSignOut() {
    if (!confirm(`Sign out as ${userName}?`)) return;
    clearUser();
    toast("Signed out", "info");
    setMenuOpen(false);
  }

  const links = [
    { href: "/", label: "Movies" },
    { href: "/bookings", label: "My Bookings" },
  ];

  return (
    <>
      <nav className="sticky top-0 z-[100] bg-[var(--surface)] border-b border-[var(--border)] px-4 md:px-6 h-14 flex items-center gap-3 overflow-hidden">
        {/* Logo */}
        <Link href="/" className="flex-shrink-0">
          <span className="text-xl font-black text-[var(--red)] tracking-tight">
            CINE<span className="text-[var(--gold)]">PLEX</span>
          </span>
        </Link>

        {/* Desktop links */}
        <div className="hidden md:flex gap-1 flex-1">
          {links.map((l) => {
            const active =
              l.href === "/"
                ? pathname === "/" || pathname.startsWith("/movies") || pathname.startsWith("/shows")
                : pathname.startsWith(l.href);
            return (
              <Link
                key={l.href}
                href={l.href}
                className={`px-3 py-1.5 rounded-full text-sm transition-all font-medium
                  ${active
                    ? "bg-[var(--surface-2)] text-[var(--text)]"
                    : "text-[var(--text-2)] hover:bg-[var(--surface-2)] hover:text-[var(--text)]"
                  }`}
              >
                {l.label}
              </Link>
            );
          })}
        </div>

        {/* Live Backend toggle */}
        <button
          onClick={() => setTechOpen((v) => !v)}
          className={`hidden md:flex items-center gap-1.5 text-xs font-semibold px-3 py-1.5 rounded-full border transition-all
            ${techOpen
              ? "bg-[rgba(245,197,24,0.12)] border-[var(--gold)] text-[var(--gold)]"
              : "border-[var(--border)] text-[var(--text-3)] hover:border-[var(--gold)] hover:text-[var(--gold)]"
            }`}
          title="Toggle Live Backend Panel"
        >
          <Database size={12} />
          Live Backend
        </button>

        {/* Desktop user chip */}
        <div className="hidden md:block">
          {userId ? (
            <div className="flex items-center gap-2">
              <span className="text-sm text-[var(--green)] border border-[var(--green)] rounded-full px-3 py-1 font-medium max-w-[120px] truncate">
                {userName}
              </span>
              <button
                onClick={handleSignOut}
                className="text-[var(--text-3)] hover:text-[var(--red)] transition-colors p-1"
                title="Sign out"
              >
                <LogOut size={16} />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setAuthOpen(true)}
              className="text-sm text-[var(--text-2)] border border-[var(--border)] rounded-full px-4 py-1.5 hover:border-[var(--gold)] hover:text-[var(--gold)] transition-all"
            >
              Sign In
            </button>
          )}
        </div>

        {/* Mobile menu toggle */}
        <button
          className="md:hidden ml-auto text-[var(--text-2)] hover:text-[var(--text)] transition-colors"
          onClick={() => setMenuOpen((v) => !v)}
        >
          {menuOpen ? <X size={22} /> : <Menu size={22} />}
        </button>
      </nav>

      {/* Mobile drawer */}
      <AnimatePresence>
        {menuOpen && (
          <motion.div
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.18 }}
            className="md:hidden fixed top-14 inset-x-0 z-[90] bg-[var(--surface)] border-b border-[var(--border)] shadow-xl"
          >
            <div className="p-4 space-y-1">
              {links.map((l) => (
                <Link
                  key={l.href}
                  href={l.href}
                  onClick={() => setMenuOpen(false)}
                  className="block px-4 py-3 rounded-lg text-sm font-medium text-[var(--text-2)] hover:bg-[var(--surface-2)] hover:text-[var(--text)] transition-all"
                >
                  {l.label}
                </Link>
              ))}
              <button
                onClick={() => { setMenuOpen(false); setTechOpen((v) => !v); }}
                className={`w-full flex items-center gap-2 px-4 py-3 rounded-lg text-sm font-medium transition-all
                  ${techOpen
                    ? "bg-[rgba(245,197,24,0.1)] text-[var(--gold)]"
                    : "text-[var(--text-2)] hover:bg-[var(--surface-2)] hover:text-[var(--gold)]"
                  }`}
              >
                <Database size={14} /> Live Backend Panel
              </button>

              <div className="border-t border-[var(--border)] pt-3 mt-2">
                {userId ? (
                  <div className="flex items-center justify-between px-4 py-2">
                    <span className="text-sm text-[var(--green)] font-medium flex items-center gap-2">
                      <User size={14} /> {userName}
                    </span>
                    <button
                      onClick={handleSignOut}
                      className="text-xs text-[var(--text-3)] hover:text-[var(--red)] transition-colors flex items-center gap-1"
                    >
                      <LogOut size={13} /> Sign out
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => { setMenuOpen(false); setAuthOpen(true); }}
                    className="w-full text-sm btn-primary py-2.5 rounded-lg font-semibold"
                  >
                    Sign In
                  </button>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} />
    </>
  );
}
