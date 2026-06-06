"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { Search, Film } from "lucide-react";
import { api } from "@/lib/api";
import type { Movie, Page } from "@/lib/types";
import { MovieCardSkeleton } from "@/components/Spinner";

const GRADIENT_PALETTES = [
  "linear-gradient(135deg,#1a1a3e,#2d1b5c)",
  "linear-gradient(135deg,#0e2a1a,#1a4a2e)",
  "linear-gradient(135deg,#3e1010,#5c2010)",
  "linear-gradient(135deg,#0e1e3e,#1a2e5c)",
  "linear-gradient(135deg,#2e1030,#5c1a48)",
];

function movieGradient(id: number) {
  return GRADIENT_PALETTES[(id - 1) % GRADIENT_PALETTES.length];
}

const containerVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.055 } },
};

const cardVariants = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { type: "spring" as const, stiffness: 340, damping: 28 } },
};

function MovieCard({ movie, onClick }: { movie: Movie; onClick: () => void }) {
  const [imgError, setImgError] = useState(false);
  const grad = movieGradient(movie.id);
  const initials = movie.title.substring(0, 2).toUpperCase();

  return (
    <motion.div
      variants={cardVariants}
      whileHover={{ y: -6, transition: { type: "spring", stiffness: 400, damping: 20 } }}
      onClick={onClick}
      className="group cursor-pointer bg-[var(--surface)] rounded-xl overflow-hidden border border-[var(--border)]
        hover:border-[var(--red)] hover:shadow-[0_10px_32px_rgba(229,9,20,0.2)] transition-colors"
    >
      {/* Poster */}
      <div className="relative overflow-hidden" style={{ aspectRatio: "2/3" }}>
        {movie.posterUrl && !imgError ? (
          <img
            src={movie.posterUrl}
            alt={movie.title}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
            onError={() => setImgError(true)}
          />
        ) : (
          <div
            className="w-full h-full flex items-center justify-center text-5xl font-black text-white/10"
            style={{ background: grad }}
          >
            {initials}
          </div>
        )}
        {/* Rating badge */}
        {movie.averageRating && (
          <div className="absolute top-2 right-2 bg-black/60 backdrop-blur-sm rounded-full px-2 py-0.5 text-xs font-bold text-[var(--gold)]">
            ★ {movie.averageRating.toFixed(1)}
          </div>
        )}
      </div>

      {/* Info */}
      <div className="p-3">
        <p className="text-sm font-bold leading-snug mb-2 line-clamp-2">{movie.title}</p>
        <div className="flex flex-wrap gap-1.5 items-center">
          <span className="bg-[rgba(229,9,20,0.18)] text-[#ff6b7a] text-[10px] font-bold uppercase tracking-wide px-1.5 py-0.5 rounded">
            {movie.genre}
          </span>
          <span className="bg-white/10 text-[var(--text-2)] text-[10px] font-bold uppercase tracking-wide px-1.5 py-0.5 rounded">
            {movie.language}
          </span>
          <span className="text-[var(--text-3)] text-[11px] ml-auto">{movie.durationMinutes}m</span>
        </div>
      </div>
    </motion.div>
  );
}

export default function MoviesPage() {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Movie> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (q: string, p: number) => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get<Page<Movie>>(
        `/movies?keyword=${encodeURIComponent(q)}&page=${p}&size=12&sort=averageRating,desc`
      );
      setData(res);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load movies");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(query, page);
  }, [page]); // eslint-disable-line react-hooks/exhaustive-deps

  function handleSearch() {
    setPage(0);
    load(query, 0);
  }

  return (
    <div className="max-w-6xl mx-auto px-4 md:px-6 py-8">
      {/* Hero */}
      <motion.div
        initial={{ opacity: 0, y: -16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-8"
      >
        <h1 className="text-3xl md:text-4xl font-black mb-1">
          <span className="text-[var(--red)]">Now</span> Showing
        </h1>
        <p className="text-[var(--text-2)] text-sm">Book your seats for the latest blockbusters</p>
      </motion.div>

      {/* Search */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="flex gap-2 mb-6"
      >
        <div className="relative flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-3)]" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            placeholder="Search by title or genre…"
            className="w-full bg-[var(--surface)] border border-[var(--border)] rounded-lg pl-9 pr-4 py-2.5
              text-sm text-[var(--text)] placeholder:text-[var(--text-3)]
              outline-none focus:border-[var(--gold)] transition-colors"
          />
        </div>
        <button
          onClick={handleSearch}
          className="btn-secondary px-5 py-2.5 rounded-lg text-sm font-semibold whitespace-nowrap"
        >
          Search
        </button>
      </motion.div>

      {/* Section title */}
      <div className="flex items-center gap-2 mb-5">
        <Film size={15} className="text-[var(--text-3)]" />
        <span className="text-sm font-bold text-[var(--text-2)]">
          {query ? `Results for "${query}"` : "All Movies"}
        </span>
        {data && (
          <span className="text-[var(--text-3)] text-xs">
            {data.totalElements} title{data.totalElements !== 1 ? "s" : ""}
          </span>
        )}
      </div>

      {/* Grid */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4">
          {Array.from({ length: 12 }).map((_, i) => (
            <MovieCardSkeleton key={i} />
          ))}
        </div>
      ) : error ? (
        <div className="text-center py-16">
          <p className="text-[var(--text-2)] font-medium mb-1">Failed to load movies</p>
          <p className="text-[var(--text-3)] text-sm mb-4">{error}</p>
          <button onClick={() => load(query, page)} className="btn-primary px-5 py-2 rounded-lg text-sm font-semibold">
            Retry
          </button>
        </div>
      ) : data?.content.length === 0 ? (
        <div className="text-center py-16">
          <p className="text-[var(--text-2)] font-medium mb-1">No movies found</p>
          <p className="text-[var(--text-3)] text-sm">Try a different search term.</p>
        </div>
      ) : (
        <AnimatePresence mode="wait">
          <motion.div
            key={`${query}-${page}`}
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-4"
          >
            {data?.content.map((movie) => (
              <MovieCard
                key={movie.id}
                movie={movie}
                onClick={() => router.push(`/movies/${movie.id}`)}
              />
            ))}
          </motion.div>
        </AnimatePresence>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex justify-center gap-2 mt-8 flex-wrap"
        >
          {Array.from({ length: data.totalPages }).map((_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`w-9 h-9 rounded-lg text-sm font-semibold transition-all
                ${i === data.number
                  ? "bg-[var(--red)] text-white"
                  : "btn-secondary"
                }`}
            >
              {i + 1}
            </button>
          ))}
        </motion.div>
      )}
    </div>
  );
}
