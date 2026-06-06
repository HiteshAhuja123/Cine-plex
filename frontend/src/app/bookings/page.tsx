"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { Ticket, Calendar, MapPin, LogIn } from "lucide-react";
import { api } from "@/lib/api";
import { useApp } from "@/lib/context";
import type { Booking, Page } from "@/lib/types";
import { PageLoader } from "@/components/Spinner";

const STATUS_STYLES: Record<string, string> = {
  CONFIRMED: "bg-[rgba(29,185,84,0.15)] text-[#1db954]",
  HELD: "bg-[rgba(245,197,24,0.15)] text-[var(--gold)]",
  CANCELLED: "bg-white/5 text-[var(--text-3)]",
  EXPIRED: "bg-white/5 text-[var(--text-3)]",
};

const listVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.07 } },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { type: "spring" as const, stiffness: 320, damping: 28 } },
};

export default function BookingsPage() {
  const router = useRouter();
  const { userId, toast } = useApp();
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(false);
  const [cancellingId, setCancellingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const page = await api.get<Page<Booking>>(`/bookings/user/${userId}?size=30`);
      setBookings(page.content);
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Failed to load bookings", "error");
    } finally {
      setLoading(false);
    }
  }, [userId, toast]);

  useEffect(() => {
    load();
  }, [load]);

  async function cancelBooking(bookingId: number) {
    if (!confirm("Cancel this booking?")) return;
    setCancellingId(bookingId);
    try {
      await api.post(`/bookings/${bookingId}/cancel?userId=${userId}`);
      toast("Booking cancelled", "success");
      load();
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Cancel failed", "error");
    } finally {
      setCancellingId(null);
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 md:px-6 py-8">
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center gap-2 mb-6"
      >
        <Ticket size={20} className="text-[var(--red)]" />
        <h1 className="text-2xl font-black">My Bookings</h1>
      </motion.div>

      {!userId ? (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center py-16"
        >
          <div className="w-16 h-16 rounded-full bg-[var(--surface)] border border-[var(--border)] flex items-center justify-center mx-auto mb-4">
            <LogIn size={24} className="text-[var(--text-3)]" />
          </div>
          <p className="text-[var(--text-2)] font-medium mb-1">Sign in to view bookings</p>
          <p className="text-[var(--text-3)] text-sm mb-5">Create an account or use a seeded user ID to see booking history.</p>
          <button onClick={() => router.push("/")} className="btn-primary px-6 py-2.5 rounded-lg text-sm font-bold">
            Go to Movies
          </button>
        </motion.div>
      ) : loading ? (
        <PageLoader />
      ) : bookings.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center py-16"
        >
          <p className="text-[var(--text-2)] font-medium mb-1">No bookings yet</p>
          <p className="text-[var(--text-3)] text-sm mb-5">Browse movies and book your first ticket!</p>
          <button onClick={() => router.push("/")} className="btn-primary px-6 py-2.5 rounded-lg text-sm font-bold">
            Browse Movies
          </button>
        </motion.div>
      ) : (
        <motion.div variants={listVariants} initial="hidden" animate="visible" className="space-y-4">
          {bookings.map((b) => {
            const dt = new Date(b.showStartTime);
            const fmt = dt.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" })
              + " · " + dt.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true });
            const canCancel = b.status === "HELD" || b.status === "CONFIRMED";

            return (
              <motion.div
                key={b.id}
                variants={itemVariants}
                className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-4"
              >
                {/* Header */}
                <div className="flex items-start justify-between gap-2 mb-3">
                  <div>
                    <p className="font-bold text-base">{b.movieTitle}</p>
                    <p className="text-xs text-[var(--text-2)] mt-0.5 flex items-center gap-1">
                      <MapPin size={10} /> {b.theaterName}
                      <span className="mx-1">·</span>
                      <Calendar size={10} /> {fmt}
                    </p>
                  </div>
                  <span className={`text-[10px] font-bold uppercase tracking-wide px-2 py-1 rounded-full whitespace-nowrap ${STATUS_STYLES[b.status] ?? STATUS_STYLES.EXPIRED}`}>
                    {b.status}
                  </span>
                </div>

                {/* Seat tags */}
                <div className="flex flex-wrap gap-1.5 mb-3">
                  {b.seats.map((s) => (
                    <span key={s.seatCode} className="bg-[var(--surface-2)] border border-[var(--border)] rounded px-2 py-0.5 text-xs font-mono">
                      {s.seatCode}
                    </span>
                  ))}
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between">
                  <span className="font-bold text-[var(--gold)]">
                    ${parseFloat(String(b.totalAmount)).toFixed(2)}
                  </span>
                  {canCancel && (
                    <button
                      onClick={() => cancelBooking(b.id)}
                      disabled={cancellingId === b.id}
                      className="btn-secondary text-xs px-3 py-1.5 rounded-lg disabled:opacity-40"
                    >
                      {cancellingId === b.id ? "Cancelling…" : "Cancel"}
                    </button>
                  )}
                </div>
              </motion.div>
            );
          })}
        </motion.div>
      )}
    </div>
  );
}
