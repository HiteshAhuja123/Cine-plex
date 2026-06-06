"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { ArrowLeft, CheckCircle2, Clock, MapPin, AlertCircle, Zap } from "lucide-react";
import { api, rawPost } from "@/lib/api";
import { useApp } from "@/lib/context";
import { useTechPanel } from "@/lib/techPanelContext";
import type { Show, ShowSeat, Booking, SelectedSeat } from "@/lib/types";
import { PageLoader } from "@/components/Spinner";
import { AuthModal } from "@/components/AuthModal";

// ─── Types ───────────────────────────────────────────────────────────────────

type FlowView = "seats" | "hold" | "confirm";

// ─── Seat Grid ───────────────────────────────────────────────────────────────

function SeatGrid({
  seats,
  selected,
  onToggle,
}: {
  seats: ShowSeat[];
  selected: SelectedSeat[];
  onToggle: (seat: ShowSeat) => void;
}) {
  const rows: Record<string, ShowSeat[]> = {};
  for (const s of seats) {
    if (!rows[s.rowLabel]) rows[s.rowLabel] = [];
    rows[s.rowLabel].push(s);
  }
  const sortedRows = Object.keys(rows).sort();

  const isSelected = (id: number) => selected.some((s) => s.showSeatId === id);

  return (
    <div>
      <div className="text-center bg-gradient-to-b from-[rgba(229,9,20,0.12)] to-transparent
        border border-b-0 border-[rgba(229,9,20,0.25)] rounded-t-xl py-2
        text-[10px] font-black tracking-[5px] text-[rgba(229,9,20,0.7)]">
        S C R E E N
      </div>
      <div className="bg-[var(--surface)] border border-t-0 border-[var(--border)] rounded-b-xl p-4 overflow-x-auto">
        <div className="inline-flex flex-col gap-1 min-w-max">
          {sortedRows.map((rowLabel) => (
            <div key={rowLabel} className="flex items-center gap-1">
              <span className="w-5 text-center text-[10px] text-[var(--text-3)] font-bold">{rowLabel}</span>
              {rows[rowLabel]
                .sort((a, b) => a.columnNumber - b.columnNumber)
                .map((seat) => {
                  const sel = isSelected(seat.showSeatId);
                  const status = seat.status.toLowerCase();
                  const cls = [
                    "seat",
                    sel ? "selected" : status,
                    !sel && seat.seatType === "PREMIUM" && status === "available" ? "premium" : "",
                  ]
                    .filter(Boolean)
                    .join(" ");
                  const label = `${seat.seatCode} · ${seat.seatType === "PREMIUM" ? "Premium" : "Regular"} · $${parseFloat(String(seat.price)).toFixed(2)}`;
                  return (
                    <motion.div
                      key={seat.showSeatId}
                      title={label}
                      className={cls}
                      onClick={() => status === "available" && onToggle(seat)}
                      animate={sel ? { scale: [1, 1.25, 1] } : { scale: 1 }}
                      transition={{ duration: 0.25 }}
                    />
                  );
                })}
            </div>
          ))}
        </div>

        {/* Legend */}
        <div className="flex flex-wrap gap-3 mt-4 pt-3 border-t border-[var(--border)]">
          {[
            { color: "#1e4d27", label: "Available" },
            { color: "#3d1a5c", label: "Premium" },
            { color: "var(--gold)", label: "Selected" },
            { color: "#3a2800", label: "Held" },
            { color: "#1e1e1e", label: "Booked" },
          ].map(({ color, label }) => (
            <div key={label} className="flex items-center gap-1.5 text-xs text-[var(--text-2)]">
              <div className="w-3.5 h-3 rounded-sm" style={{ background: color }} />
              {label}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Booking Sidebar ──────────────────────────────────────────────────────────

function BookingSidebar({
  selected,
  onHold,
  loading,
}: {
  selected: SelectedSeat[];
  onHold: () => void;
  loading: boolean;
}) {
  const total = selected.reduce((s, seat) => s + parseFloat(String(seat.price)), 0);

  return (
    <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-4">
      <p className="text-xs font-bold text-[var(--text-3)] uppercase tracking-wider mb-3">
        Your Selection
      </p>
      <div className="min-h-14 mb-3">
        {selected.length === 0 ? (
          <p className="text-xs text-[var(--text-3)] text-center py-4">No seats selected</p>
        ) : (
          <div className="space-y-0.5">
            {selected.map((s) => (
              <div key={s.showSeatId} className="flex justify-between py-1.5 border-b border-[var(--border)] last:border-0 text-sm">
                <span className="font-mono text-xs">{s.seatCode}</span>
                <span className="text-[var(--text-2)] text-xs">${parseFloat(String(s.price)).toFixed(2)}</span>
              </div>
            ))}
          </div>
        )}
      </div>
      <div className="flex justify-between items-baseline pt-2 border-t border-[var(--border)] mb-3">
        <span className="text-sm font-bold">Total</span>
        <motion.span
          key={total}
          initial={{ scale: 1.1 }}
          animate={{ scale: 1 }}
          className="text-xl font-black text-[var(--gold)]"
        >
          ${total.toFixed(2)}
        </motion.span>
      </div>
      <button
        onClick={onHold}
        disabled={selected.length === 0 || loading}
        className="w-full btn-primary py-2.5 rounded-lg text-sm font-bold disabled:opacity-40 transition-all"
      >
        {loading ? "Reserving…" : "Hold Seats"}
      </button>
    </div>
  );
}

// ─── Hold View ────────────────────────────────────────────────────────────────

function HoldView({
  booking,
  onCancel,
  onConfirm,
  loading,
}: {
  booking: Booking;
  onCancel: () => void;
  onConfirm: () => void;
  loading: boolean;
}) {
  const [remaining, setRemaining] = useState(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    const expStr = /[Z+]/.test(booking.expiresAt ?? "") ? booking.expiresAt! : (booking.expiresAt ?? "") + "Z";
    const expiry = new Date(expStr).getTime();

    function tick() {
      const r = Math.max(0, expiry - Date.now());
      setRemaining(r);
      if (r === 0 && intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    }
    tick();
    intervalRef.current = setInterval(tick, 1000);
    return () => { if (intervalRef.current) clearInterval(intervalRef.current); };
  }, [booking.expiresAt]);

  const min = Math.floor(remaining / 60000);
  const sec = Math.floor((remaining % 60000) / 1000);
  const urgent = remaining < 60000 && remaining > 0;
  const expired = remaining === 0;

  const dt = new Date(booking.showStartTime);
  const fmt = dt.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })
    + " at " + dt.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true });

  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: "spring", stiffness: 340, damping: 28 }}
      className="max-w-lg mx-auto"
    >
      <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-6">
        <h2 className="text-lg font-black text-center mb-4">Seats Reserved!</h2>

        {/* Countdown */}
        <div className="text-center mb-5">
          <p className="text-xs text-[var(--text-2)] mb-1">Complete booking before hold expires</p>
          <motion.div
            animate={urgent ? { scale: [1, 1.04, 1] } : {}}
            transition={urgent ? { repeat: Infinity, duration: 1 } : {}}
            className={`text-5xl font-black font-mono tabular-nums ${urgent ? "text-[var(--red)]" : "text-[var(--gold)]"}`}
          >
            {expired ? "00:00" : `${String(min).padStart(2, "0")}:${String(sec).padStart(2, "0")}`}
          </motion.div>
        </div>

        {/* Show info */}
        <div className="bg-[var(--surface-2)] rounded-lg p-3 mb-3">
          <p className="font-bold text-sm mb-0.5">{booking.movieTitle}</p>
          <p className="text-xs text-[var(--text-2)]">{booking.theaterName} · {fmt}</p>
        </div>

        {/* Seats */}
        <div className="mb-4">
          {booking.seats.map((s) => (
            <div key={s.seatCode} className="flex justify-between py-2 border-b border-[var(--border)] last:border-0 text-sm text-[var(--text-2)]">
              <span className="font-mono text-xs">{s.seatCode}</span>
              <span>{s.seatType === "PREMIUM" ? "★ Premium" : "Regular"}</span>
              <span>${parseFloat(String(s.price)).toFixed(2)}</span>
            </div>
          ))}
        </div>

        {/* Total */}
        <div className="flex justify-between font-bold text-base pb-4 border-b border-[var(--border)] mb-4">
          <span>Total</span>
          <span className="text-[var(--gold)] text-xl">${parseFloat(String(booking.totalAmount)).toFixed(2)}</span>
        </div>

        {/* Actions */}
        <div className="flex gap-3">
          <button onClick={onCancel} className="btn-secondary flex-1 py-2.5 rounded-lg text-sm font-semibold">
            Release Seats
          </button>
          <button
            onClick={onConfirm}
            disabled={loading || expired}
            className="btn-gold flex-1 py-2.5 rounded-lg text-sm font-bold disabled:opacity-40 transition-all"
          >
            {loading ? "Confirming…" : "Confirm Booking"}
          </button>
        </div>

        {expired && (
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center text-[var(--red)] text-xs mt-3 flex items-center justify-center gap-1"
          >
            <AlertCircle size={12} /> Hold expired. Please select seats again.
          </motion.p>
        )}
      </div>
    </motion.div>
  );
}

// ─── Confirmation View ────────────────────────────────────────────────────────

function ConfirmationView({ booking, onBack }: { booking: Booking; onBack: () => void }) {
  const router = useRouter();
  const dt = new Date(booking.showStartTime);
  const fmt = dt.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })
    + " at " + dt.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true });
  const seats = booking.seats.map((s) => s.seatCode).join(", ");

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.96 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: "spring", stiffness: 340, damping: 28 }}
      className="max-w-lg mx-auto text-center"
    >
      {/* Checkmark */}
      <motion.div
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: "spring", stiffness: 400, damping: 20, delay: 0.1 }}
        className="w-18 h-18 rounded-full border-2 border-[var(--green)] bg-[rgba(29,185,84,0.15)]
          flex items-center justify-center mx-auto mb-5"
        style={{ width: 72, height: 72 }}
      >
        <CheckCircle2 size={36} className="text-[var(--green)]" />
      </motion.div>

      <h2 className="text-3xl font-black mb-1">Booking Confirmed!</h2>
      <p className="text-[var(--text-2)] text-sm mb-6">Your tickets have been booked. Enjoy the show!</p>

      {/* Ticket */}
      <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-5 text-left mb-5">
        <p className="font-black text-base pb-3 mb-3 border-b border-dashed border-[var(--border)]">
          {booking.movieTitle}
        </p>
        {[
          { label: "Theater", value: booking.theaterName },
          { label: "Date & Time", value: fmt },
          { label: "Seats", value: seats, mono: true },
          { label: "Total", value: `$${parseFloat(String(booking.totalAmount)).toFixed(2)}`, gold: true },
        ].map(({ label, value, mono, gold }) => (
          <div key={label} className="flex justify-between mb-2 text-sm">
            <span className="text-[var(--text-2)]">{label}</span>
            <span className={`${mono ? "font-mono text-xs" : ""} ${gold ? "text-[var(--gold)] font-bold" : ""}`}>{value}</span>
          </div>
        ))}
        <p className="text-center text-xs text-[var(--text-3)] pt-3 mt-1 border-t border-dashed border-[var(--border)]">
          Booking #{booking.id}
        </p>
      </div>

      <div className="flex gap-3">
        <button onClick={() => router.push("/bookings")} className="btn-secondary flex-1 py-2.5 rounded-lg text-sm font-semibold">
          My Bookings
        </button>
        <button onClick={onBack} className="btn-primary flex-1 py-2.5 rounded-lg text-sm font-bold">
          Book More
        </button>
      </div>
    </motion.div>
  );
}

// ─── Race Demo ────────────────────────────────────────────────────────────────

type RaceResult = { label: string; won: boolean; msg: string };

function RaceDemo({ showId, seats }: { showId: number; seats: ShowSeat[] }) {
  const { setOpen } = useTechPanel();
  const [running, setRunning] = useState(false);
  const [results, setResults] = useState<RaceResult[] | null>(null);

  const availableSeat = seats.find((s) => s.status.toLowerCase() === "available");

  async function runRace() {
    if (!availableSeat) return;
    setResults(null);
    setRunning(true);
    setOpen(true);

    // Fire both simultaneously — Alice (user 1) vs Bob (user 2)
    const [resA, resB] = await Promise.allSettled([
      rawPost<Booking>("/bookings/hold", {
        userId: 1,
        showId,
        showSeatIds: [availableSeat.showSeatId],
      }, "Alice #1"),
      rawPost<Booking>("/bookings/hold", {
        userId: 2,
        showId,
        showSeatIds: [availableSeat.showSeatId],
      }, "Bob #2"),
    ]);

    setResults([
      {
        label: "Alice (user 1)",
        won: resA.status === "fulfilled",
        msg: resA.status === "fulfilled"
          ? `Won! Booking #${(resA.value as Booking).id}`
          : (resA.reason as Error).message,
      },
      {
        label: "Bob (user 2)",
        won: resB.status === "fulfilled",
        msg: resB.status === "fulfilled"
          ? `Won! Booking #${(resB.value as Booking).id}`
          : (resB.reason as Error).message,
      },
    ]);
    setRunning(false);
  }

  return (
    <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-4 mt-5">
      <div className="flex items-center gap-2 mb-1">
        <Zap size={14} className="text-[var(--gold)]" />
        <span className="text-sm font-black text-[var(--text)]">Race Condition Demo</span>
      </div>
      <p className="text-xs text-[var(--text-3)] mb-3">
        Fires 2 simultaneous{" "}
        <code className="font-mono text-[var(--gold)]">POST /bookings/hold</code> for seat{" "}
        <code className="font-mono text-[var(--text-2)]">{availableSeat?.seatCode ?? "—"}</code>.
        Exactly 1 wins due to{" "}
        <code className="font-mono text-[var(--gold)]">SELECT FOR UPDATE</code>.
      </p>

      <button
        onClick={runRace}
        disabled={running || !availableSeat}
        className="btn-gold w-full py-2 rounded-lg text-xs font-bold flex items-center justify-center gap-2 disabled:opacity-40"
      >
        <Zap size={13} />
        {running ? "Racing…" : availableSeat ? "Fire Race Demo" : "No available seats"}
      </button>

      <AnimatePresence>
        {results && (
          <motion.div
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            className="mt-3 space-y-2"
          >
            {results.map((r) => (
              <div
                key={r.label}
                className={`flex items-start gap-2 rounded-lg px-3 py-2 text-xs
                  ${r.won
                    ? "bg-[rgba(29,185,84,0.1)] border border-[rgba(29,185,84,0.25)]"
                    : "bg-[rgba(229,9,20,0.08)] border border-[rgba(229,9,20,0.2)]"
                  }`}
              >
                <span className={`font-black mt-0.5 flex-shrink-0 ${r.won ? "text-[var(--green)]" : "text-[var(--red)]"}`}>
                  {r.won ? "✓" : "✗"}
                </span>
                <div>
                  <span className="font-semibold text-[var(--text)]">{r.label}</span>
                  <span className={`ml-2 ${r.won ? "text-[var(--green)]" : "text-[var(--text-3)]"}`}>{r.msg}</span>
                </div>
              </div>
            ))}
            <p className="text-[10px] text-[var(--text-3)] text-center pt-1">
              Check the Live Backend panel for the full SQL + lock sequence
            </p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function SeatsPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const { userId, toast } = useApp();

  const [show, setShow] = useState<Show | null>(null);
  const [seats, setSeats] = useState<ShowSeat[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [selected, setSelected] = useState<SelectedSeat[]>([]);
  const [view, setView] = useState<FlowView>("seats");
  const [holdBooking, setHoldBooking] = useState<Booking | null>(null);
  const [confirmedBooking, setConfirmedBooking] = useState<Booking | null>(null);
  const [authOpen, setAuthOpen] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const [s, seatList] = await Promise.all([
          api.get<Show>(`/shows/${id}`),
          api.get<ShowSeat[]>(`/shows/${id}/seats`),
        ]);
        setShow(s);
        setSeats(seatList);
      } catch {
        toast("Failed to load show", "error");
        router.push("/");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]); // eslint-disable-line react-hooks/exhaustive-deps

  function toggleSeat(seat: ShowSeat) {
    setSelected((prev) => {
      const exists = prev.find((s) => s.showSeatId === seat.showSeatId);
      if (exists) return prev.filter((s) => s.showSeatId !== seat.showSeatId);
      return [...prev, {
        showSeatId: seat.showSeatId,
        seatCode: seat.seatCode,
        price: seat.price,
        seatType: seat.seatType,
      }];
    });
  }

  async function holdSeats() {
    if (!userId) { setAuthOpen(true); return; }
    if (selected.length === 0) return;
    setActionLoading(true);
    try {
      const booking = await api.post<Booking>("/bookings/hold", {
        userId,
        showId: Number(id),
        showSeatIds: selected.map((s) => s.showSeatId),
      });
      setHoldBooking(booking);
      setView("hold");
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Hold failed", "error");
    } finally {
      setActionLoading(false);
    }
  }

  async function cancelHold() {
    if (!holdBooking) return;
    try {
      await api.post(`/bookings/${holdBooking.id}/cancel?userId=${userId}`);
      toast("Hold cancelled. Seats released.", "info");
      setHoldBooking(null);
      setSelected([]);
      setView("seats");
      // Refresh seats
      const seatList = await api.get<ShowSeat[]>(`/shows/${id}/seats`);
      setSeats(seatList);
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Cancel failed", "error");
    }
  }

  async function confirmBooking() {
    if (!holdBooking) return;
    setActionLoading(true);
    try {
      const booking = await api.post<Booking>(`/bookings/${holdBooking.id}/confirm`, { userId });
      setConfirmedBooking(booking);
      setView("confirm");
      toast("Booking confirmed!", "success");
    } catch (e: unknown) {
      toast(e instanceof Error ? e.message : "Confirm failed", "error");
    } finally {
      setActionLoading(false);
    }
  }

  if (loading) return <PageLoader />;
  if (!show) return null;

  const dt = new Date(show.startTime);
  const showFmt = dt.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })
    + " at " + dt.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true });

  return (
    <div className="max-w-5xl mx-auto px-4 md:px-6 py-6">
      {/* Back */}
      <button
        onClick={() => {
          if (view === "hold") cancelHold();
          else if (view === "confirm") router.push("/");
          else router.push(`/movies/${show.movieId}`);
        }}
        className="flex items-center gap-2 text-[var(--text-2)] hover:text-[var(--gold)] transition-colors text-sm mb-5"
      >
        <ArrowLeft size={16} />
        {view === "seats" ? "Back to Shows" : view === "hold" ? "Release & Back" : "Back to Movies"}
      </button>

      <AnimatePresence mode="wait">
        {view === "seats" && (
          <motion.div
            key="seats"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.2 }}
          >
            {/* Show info bar */}
            <div className="bg-[var(--surface)] border border-[var(--border)] rounded-xl px-4 py-3 mb-5 flex justify-between items-center gap-3 flex-wrap">
              <div>
                <h2 className="font-black text-base">{show.movieTitle}</h2>
                <p className="text-xs text-[var(--text-2)] mt-0.5 flex items-center gap-2">
                  <MapPin size={10} /> {show.theaterName} · {show.screenName}
                  <Clock size={10} /> {showFmt}
                </p>
              </div>
              <p className="text-sm text-[var(--text-2)]">{show.availableSeats} seats available</p>
            </div>

            {/* Two-column layout */}
            <div className="grid grid-cols-1 lg:grid-cols-[1fr_260px] gap-5">
              <div>
                <SeatGrid seats={seats} selected={selected} onToggle={toggleSeat} />
                <RaceDemo showId={Number(id)} seats={seats} />
              </div>
              <div className="lg:sticky lg:top-20">
                <BookingSidebar selected={selected} onHold={holdSeats} loading={actionLoading} />
              </div>
            </div>
          </motion.div>
        )}

        {view === "hold" && holdBooking && (
          <motion.div
            key="hold"
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.2 }}
          >
            <HoldView
              booking={holdBooking}
              onCancel={cancelHold}
              onConfirm={confirmBooking}
              loading={actionLoading}
            />
          </motion.div>
        )}

        {view === "confirm" && confirmedBooking && (
          <motion.div
            key="confirm"
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
          >
            <ConfirmationView booking={confirmedBooking} onBack={() => router.push("/")} />
          </motion.div>
        )}
      </AnimatePresence>

      <AuthModal open={authOpen} onClose={() => setAuthOpen(false)} />
    </div>
  );
}
