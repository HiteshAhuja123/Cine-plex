"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { motion } from "framer-motion";
import { ArrowLeft, Star, Clock, Globe, Calendar, Ticket } from "lucide-react";
import { api } from "@/lib/api";
import type { Movie, Show, Page } from "@/lib/types";
import { PageLoader } from "@/components/Spinner";

const GRADIENT_PALETTES = [
  "linear-gradient(135deg,#1a1a3e,#2d1b5c)",
  "linear-gradient(135deg,#0e2a1a,#1a4a2e)",
  "linear-gradient(135deg,#3e1010,#5c2010)",
  "linear-gradient(135deg,#0e1e3e,#1a2e5c)",
  "linear-gradient(135deg,#2e1030,#5c1a48)",
];

const listVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.06 } },
};

const itemVariants = {
  hidden: { opacity: 0, x: -16 },
  visible: { opacity: 1, x: 0, transition: { type: "spring" as const, stiffness: 320, damping: 28 } },
};

function formatShowTime(iso: string) {
  const dt = new Date(iso);
  return {
    date: dt.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" }),
    time: dt.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true }),
  };
}

function availColor(pct: number) {
  if (pct < 20) return "bg-[var(--red)]";
  if (pct < 50) return "bg-[var(--gold)]";
  return "bg-[var(--green)]";
}

export default function MovieShowsPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [movie, setMovie] = useState<Movie | null>(null);
  const [shows, setShows] = useState<Show[]>([]);
  const [loading, setLoading] = useState(true);
  const [imgError, setImgError] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        const [m, pg] = await Promise.all([
          api.get<Movie>(`/movies/${id}`),
          api.get<Page<Show>>(`/shows/movie/${id}?size=30`),
        ]);
        setMovie(m);
        setShows(pg.content);
      } catch {
        router.push("/");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id, router]);

  if (loading) return <PageLoader />;
  if (!movie) return null;

  const grad = GRADIENT_PALETTES[(movie.id - 1) % GRADIENT_PALETTES.length];

  return (
    <div className="max-w-4xl mx-auto px-4 md:px-6 py-6">
      {/* Back */}
      <motion.button
        initial={{ opacity: 0, x: -10 }}
        animate={{ opacity: 1, x: 0 }}
        onClick={() => router.push("/")}
        className="flex items-center gap-2 text-[var(--text-2)] hover:text-[var(--gold)] transition-colors text-sm mb-5"
      >
        <ArrowLeft size={16} /> Back to Movies
      </motion.button>

      {/* Movie header */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-5 mb-7 flex gap-4"
      >
        {/* Poster */}
        <div className="w-20 min-w-20 rounded-lg overflow-hidden" style={{ aspectRatio: "2/3" }}>
          {movie.posterUrl && !imgError ? (
            <img
              src={movie.posterUrl}
              alt={movie.title}
              className="w-full h-full object-cover"
              onError={() => setImgError(true)}
            />
          ) : (
            <div
              className="w-full h-full flex items-center justify-center text-2xl font-black text-white/10"
              style={{ background: grad }}
            >
              {movie.title.substring(0, 2).toUpperCase()}
            </div>
          )}
        </div>

        {/* Info */}
        <div className="flex-1 min-w-0">
          <h1 className="text-xl md:text-2xl font-black leading-tight mb-2">{movie.title}</h1>
          <div className="flex flex-wrap gap-1.5 mb-3">
            <span className="bg-[rgba(229,9,20,0.18)] text-[#ff6b7a] text-[10px] font-bold uppercase px-2 py-0.5 rounded">
              {movie.genre}
            </span>
            <span className="bg-white/10 text-[var(--text-2)] text-[10px] font-bold uppercase px-2 py-0.5 rounded">
              {movie.language}
            </span>
            {movie.averageRating && (
              <span className="flex items-center gap-1 text-[var(--gold)] text-xs font-bold">
                <Star size={11} fill="currentColor" /> {movie.averageRating.toFixed(1)}
              </span>
            )}
            <span className="flex items-center gap-1 text-[var(--text-3)] text-xs">
              <Clock size={11} /> {movie.durationMinutes} min
            </span>
          </div>
          {movie.description && (
            <p className="text-[var(--text-2)] text-sm leading-relaxed line-clamp-3">{movie.description}</p>
          )}
        </div>
      </motion.div>

      {/* Shows */}
      <div className="flex items-center gap-2 mb-4">
        <Ticket size={14} className="text-[var(--text-3)]" />
        <span className="text-sm font-bold text-[var(--text-2)]">
          Upcoming Shows
          <span className="text-[var(--text-3)] font-normal ml-2">{shows.length} available</span>
        </span>
      </div>

      {shows.length === 0 ? (
        <div className="text-center py-16 text-[var(--text-3)]">
          <p className="font-medium text-[var(--text-2)] mb-1">No upcoming shows</p>
          <p className="text-sm">Check back soon for new screenings.</p>
        </div>
      ) : (
        <motion.div
          variants={listVariants}
          initial="hidden"
          animate="visible"
          className="space-y-3"
        >
          {shows.map((show) => {
            const { date, time } = formatShowTime(show.startTime);
            const pct = show.totalSeats > 0 ? (show.availableSeats / show.totalSeats) * 100 : 0;
            return (
              <motion.div
                key={show.id}
                variants={itemVariants}
                whileHover={{ x: 4, transition: { type: "spring", stiffness: 400, damping: 25 } }}
                onClick={() => router.push(`/shows/${show.id}`)}
                className="bg-[var(--surface)] border border-[var(--border)] hover:border-[var(--red)]
                  rounded-xl p-4 flex flex-wrap sm:flex-nowrap items-center gap-4 cursor-pointer transition-colors"
              >
                {/* Time */}
                <div className="min-w-[80px]">
                  <p className="text-xs text-[var(--text-2)] mb-0.5 flex items-center gap-1">
                    <Calendar size={10} /> {date}
                  </p>
                  <p className="text-2xl font-black text-[var(--gold)] leading-none">{time.split(":")[0]}<span className="text-base">:{time.split(":")[1]?.replace(" ", "").toLowerCase()}</span></p>
                </div>

                {/* Theater info */}
                <div className="flex-1 min-w-0">
                  <p className="font-semibold text-sm truncate">{show.theaterName}</p>
                  <p className="text-xs text-[var(--text-2)] mt-0.5 flex items-center gap-1">
                    <Globe size={10} /> {show.screenName} · {show.language}
                  </p>
                </div>

                {/* Availability */}
                <div className="text-right min-w-[80px]">
                  <div className="w-18 h-1 bg-[var(--border)] rounded-full overflow-hidden mb-1 ml-auto" style={{ width: 72 }}>
                    <div
                      className={`h-full rounded-full ${availColor(pct)} transition-all`}
                      style={{ width: `${pct.toFixed(0)}%` }}
                    />
                  </div>
                  <p className="text-xs text-[var(--text-2)]">{show.availableSeats}/{show.totalSeats}</p>
                </div>

                {/* Price */}
                <div className="text-right min-w-[60px]">
                  <p className="text-[10px] text-[var(--text-3)]">from</p>
                  <p className="text-base font-bold">₹{parseFloat(String(show.basePrice)).toFixed(0)}</p>
                </div>
              </motion.div>
            );
          })}
        </motion.div>
      )}
    </div>
  );
}
