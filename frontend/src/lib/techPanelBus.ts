export type SqlStepType = "query" | "lock" | "tx" | "write" | "check";

export interface SqlStep {
  label: string;
  sql: string;
  type: SqlStepType;
}

export interface ApiLogEntry {
  id: string;
  method: string;
  path: string;
  status: "pending" | "success" | "error";
  statusCode?: number;
  durationMs?: number;
  sql: SqlStep[];
  timestamp: number;
  error?: string;
  raceLabel?: string;
}

type Listener = (event: { type: "add" | "update"; entry: ApiLogEntry }) => void;

let listener: Listener | null = null;

export function setTechPanelListener(fn: Listener | null) {
  listener = fn;
}

export function emitTechPanel(event: { type: "add" | "update"; entry: ApiLogEntry }) {
  listener?.(event);
}

function getSqlSteps(method: string, path: string): SqlStep[] {
  const m = method.toUpperCase();

  if (m === "GET" && /^\/shows\/\d+\/seats$/.test(path)) {
    return [
      {
        label: "Fetch seat grid",
        type: "query",
        sql: `SELECT ss.show_seat_id, ss.status, ss.price,\n       s.seat_code, s.row_label, s.column_number, s.seat_type\nFROM show_seats ss\nJOIN seats s ON ss.seat_id = s.seat_id\nWHERE ss.show_id = ?`,
      },
    ];
  }

  if (m === "GET" && /^\/shows\/\d+$/.test(path)) {
    return [
      {
        label: "Fetch show details",
        type: "query",
        sql: `SELECT s.*, m.title AS movie_title,\n       th.name AS theater_name, sc.name AS screen_name\nFROM shows s\nJOIN movies m    ON s.movie_id = m.id\nJOIN screens sc  ON s.screen_id = sc.id\nJOIN theaters th ON sc.theater_id = th.id\nWHERE s.id = ?`,
      },
    ];
  }

  if (m === "GET" && /^\/movies\/\d+\/shows/.test(path)) {
    return [
      {
        label: "Fetch shows for movie",
        type: "query",
        sql: `SELECT s.id, s.start_time, s.available_seats,\n       th.name AS theater_name, sc.name AS screen_name\nFROM shows s\nJOIN screens sc  ON s.screen_id = sc.id\nJOIN theaters th ON sc.theater_id = th.id\nWHERE s.movie_id = ?\n  AND s.start_time > NOW()\n  AND s.status = 'ACTIVE'\nORDER BY s.start_time`,
      },
    ];
  }

  if (m === "GET" && /^\/movies\/\d+$/.test(path)) {
    return [
      {
        label: "Fetch movie",
        type: "query",
        sql: `SELECT id, title, genre, language, duration_minutes,\n       average_rating, poster_url, description\nFROM movies\nWHERE id = ?`,
      },
    ];
  }

  if (m === "GET" && path.startsWith("/movies")) {
    return [
      {
        label: "Fetch movies list",
        type: "query",
        sql: `SELECT id, title, genre, language,\n       duration_minutes, average_rating, poster_url\nFROM movies\nORDER BY title\nLIMIT ? OFFSET ?`,
      },
    ];
  }

  if (m === "POST" && path === "/bookings/hold") {
    return [
      { label: "① BEGIN", type: "tx", sql: "BEGIN" },
      {
        label: "② SELECT FOR UPDATE",
        type: "lock",
        sql: `SELECT * FROM show_seats\nWHERE show_seat_id IN (?)\nFOR UPDATE\n-- blocks concurrent txns until COMMIT`,
      },
      {
        label: "③ Validate AVAILABLE",
        type: "check",
        sql: `-- assert every row has status = 'AVAILABLE'\n-- throws 409 CONFLICT if any seat is HELD or BOOKED`,
      },
      {
        label: "④ Hold seats",
        type: "write",
        sql: `UPDATE show_seats\nSET status = 'HELD'\nWHERE show_seat_id IN (?)`,
      },
      {
        label: "⑤ Create booking",
        type: "write",
        sql: `INSERT INTO bookings\n  (user_id, show_id, status, expires_at, total_amount)\nVALUES\n  (?, ?, 'HELD', NOW() + INTERVAL '10 minutes', ?)`,
      },
      {
        label: "⑥ Link seats",
        type: "write",
        sql: `INSERT INTO booking_seats\n  (booking_id, show_seat_id, price)\nVALUES\n  (?, ?, ?)  -- repeated per seat`,
      },
      {
        label: "⑦ COMMIT",
        type: "tx",
        sql: "COMMIT\n-- lock released; waiting transactions unblock",
      },
    ];
  }

  if (m === "POST" && /^\/bookings\/\d+\/confirm$/.test(path)) {
    return [
      { label: "① BEGIN", type: "tx", sql: "BEGIN" },
      {
        label: "② Lock booking row",
        type: "lock",
        sql: `SELECT * FROM bookings\nWHERE id = ?\nFOR UPDATE\n-- prevents double-confirm`,
      },
      {
        label: "③ Validate hold",
        type: "check",
        sql: `-- assert status = 'HELD'\n-- assert expires_at > NOW()\n-- throws 409 if expired or already confirmed`,
      },
      {
        label: "④ Confirm booking",
        type: "write",
        sql: `UPDATE bookings\nSET status = 'CONFIRMED',\n    confirmed_at = NOW()\nWHERE id = ?`,
      },
      {
        label: "⑤ Book seats",
        type: "write",
        sql: `UPDATE show_seats SET status = 'BOOKED'\nWHERE show_seat_id IN (\n  SELECT show_seat_id FROM booking_seats\n  WHERE booking_id = ?\n)`,
      },
      { label: "⑥ COMMIT", type: "tx", sql: "COMMIT" },
    ];
  }

  if (m === "POST" && /^\/bookings\/\d+\/cancel/.test(path)) {
    return [
      { label: "① BEGIN", type: "tx", sql: "BEGIN" },
      {
        label: "② Lock booking row",
        type: "lock",
        sql: `SELECT * FROM bookings WHERE id = ? FOR UPDATE`,
      },
      {
        label: "③ Cancel booking",
        type: "write",
        sql: `UPDATE bookings SET status = 'CANCELLED' WHERE id = ?`,
      },
      {
        label: "④ Release seats",
        type: "write",
        sql: `UPDATE show_seats SET status = 'AVAILABLE'\nWHERE show_seat_id IN (\n  SELECT show_seat_id FROM booking_seats\n  WHERE booking_id = ?\n)`,
      },
      { label: "⑤ COMMIT", type: "tx", sql: "COMMIT" },
    ];
  }

  if (m === "GET" && path.startsWith("/bookings")) {
    return [
      {
        label: "Fetch user bookings",
        type: "query",
        sql: `SELECT b.id, b.status, b.total_amount, b.expires_at,\n       m.title AS movie_title,\n       th.name AS theater_name,\n       s.start_time AS show_start_time\nFROM bookings b\nJOIN shows s     ON b.show_id = s.id\nJOIN movies m    ON s.movie_id = m.id\nJOIN screens sc  ON s.screen_id = sc.id\nJOIN theaters th ON sc.theater_id = th.id\nWHERE b.user_id = ?\nORDER BY b.created_at DESC`,
      },
    ];
  }

  if (m === "POST" && path === "/users/login") {
    return [
      {
        label: "Authenticate user",
        type: "query",
        sql: `SELECT id, name, email, phone\nFROM users\nWHERE email = ?\nLIMIT 1`,
      },
    ];
  }

  if (m === "POST" && path === "/users/register") {
    return [
      {
        label: "Create user",
        type: "write",
        sql: `INSERT INTO users (name, email, phone)\nVALUES (?, ?, ?)\nRETURNING id, name, email`,
      },
    ];
  }

  if (m === "POST" && path === "/assistant") {
    return [
      {
        label: "AI query (external)",
        type: "query",
        sql: `-- Gemini API call — no direct SQL\n-- may trigger internal booking queries`,
      },
    ];
  }

  return [
    {
      label: "Query",
      type: "query",
      sql: `-- ${m} ${path}`,
    },
  ];
}

export function createPendingEntry(
  method: string,
  path: string,
  raceLabel?: string
): ApiLogEntry {
  return {
    id: Math.random().toString(36).slice(2),
    method,
    path,
    status: "pending",
    sql: getSqlSteps(method, path),
    timestamp: Date.now(),
    raceLabel,
  };
}
