import { createPendingEntry, emitTechPanel } from "./techPanelBus";

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const entry = createPendingEntry(method, path);
  emitTechPanel({ type: "add", entry });

  const opts: RequestInit = { method, headers: {} };
  if (body) {
    (opts.headers as Record<string, string>)["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }

  const start = Date.now();
  let res: Response;

  try {
    res = await fetch("/api" + path, opts);
  } catch (e) {
    emitTechPanel({
      type: "update",
      entry: {
        ...entry,
        status: "error",
        durationMs: Date.now() - start,
        error: e instanceof Error ? e.message : "Network error",
      },
    });
    throw e;
  }

  const durationMs = Date.now() - start;
  const text = await res.text();

  // Parse body — handle non-JSON gracefully (HTML error pages, plain-text proxy errors, etc.)
  let json: { success: boolean; data?: unknown; message?: string };
  try {
    json = JSON.parse(text);
  } catch {
    const snippet = text.replace(/<[^>]+>/g, "").trim().slice(0, 160) || `HTTP ${res.status}`;
    emitTechPanel({
      type: "update",
      entry: { ...entry, status: "error", statusCode: res.status, durationMs, error: snippet },
    });
    throw new Error(snippet);
  }

  if (!res.ok || !json.success) {
    emitTechPanel({
      type: "update",
      entry: { ...entry, status: "error", statusCode: res.status, durationMs, error: json.message },
    });
    throw new Error(json.message || `Request failed (${res.status})`);
  }

  emitTechPanel({
    type: "update",
    entry: { ...entry, status: "success", statusCode: res.status, durationMs },
  });
  return json.data as T;
}

// Raw fetch that bypasses the standard ApiResponse wrapper (used by race demo)
export async function rawPost<T>(path: string, body: unknown, raceLabel?: string): Promise<T> {
  const entry = createPendingEntry("POST", path, raceLabel);
  emitTechPanel({ type: "add", entry });

  const start = Date.now();
  let res: Response;

  try {
    res = await fetch("/api" + path, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
  } catch (e) {
    emitTechPanel({
      type: "update",
      entry: {
        ...entry,
        status: "error",
        durationMs: Date.now() - start,
        error: e instanceof Error ? e.message : "Network error",
      },
    });
    throw e;
  }

  const durationMs = Date.now() - start;
  const text = await res.text();

  let json: { success: boolean; data?: unknown; message?: string };
  try {
    json = JSON.parse(text);
  } catch {
    const snippet = text.replace(/<[^>]+>/g, "").trim().slice(0, 160) || `HTTP ${res.status}`;
    emitTechPanel({
      type: "update",
      entry: { ...entry, status: "error", statusCode: res.status, durationMs, error: snippet },
    });
    throw new Error(snippet);
  }

  if (!res.ok || !json.success) {
    emitTechPanel({
      type: "update",
      entry: { ...entry, status: "error", statusCode: res.status, durationMs, error: json.message },
    });
    throw new Error(json.message || `Request failed (${res.status})`);
  }

  emitTechPanel({
    type: "update",
    entry: { ...entry, status: "success", statusCode: res.status, durationMs },
  });
  return json.data as T;
}

export const api = {
  get: <T>(path: string) => request<T>("GET", path),
  post: <T>(path: string, body?: unknown) => request<T>("POST", path, body),
};
