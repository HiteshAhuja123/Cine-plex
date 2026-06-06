export function Spinner({ size = 20 }: { size?: number }) {
  return (
    <div
      style={{ width: size, height: size }}
      className="rounded-full border-2 border-[var(--border)] border-t-[var(--red)] animate-spin flex-shrink-0"
    />
  );
}

export function PageLoader() {
  return (
    <div className="flex items-center justify-center gap-3 py-16 text-[var(--text-2)] text-sm">
      <Spinner />
      <span>Loading…</span>
    </div>
  );
}

export function MovieCardSkeleton() {
  return (
    <div className="rounded-xl overflow-hidden border border-[var(--border)] bg-[var(--surface)]">
      <div className="skeleton" style={{ aspectRatio: "2/3", width: "100%" }} />
      <div className="p-3 space-y-2">
        <div className="skeleton h-4 rounded w-3/4" />
        <div className="skeleton h-3 rounded w-1/2" />
      </div>
    </div>
  );
}
