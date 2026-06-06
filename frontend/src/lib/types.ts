export interface Movie {
  id: number;
  title: string;
  genre: string;
  language: string;
  durationMinutes: number;
  averageRating?: number;
  description?: string;
  posterUrl?: string;
}

export interface Show {
  id: number;
  movieId: number;
  movieTitle: string;
  theaterName: string;
  screenName: string;
  startTime: string;
  availableSeats: number;
  totalSeats: number;
  basePrice: number;
  language: string;
}

export interface ShowSeat {
  showSeatId: number;
  seatCode: string;
  rowLabel: string;
  columnNumber: number;
  seatType: "REGULAR" | "PREMIUM";
  status: "AVAILABLE" | "HELD" | "BOOKED";
  price: number;
}

export interface BookingSeat {
  seatCode: string;
  seatType: string;
  price: number;
}

export interface Booking {
  id: number;
  movieTitle: string;
  theaterName: string;
  showStartTime: string;
  status: "HELD" | "CONFIRMED" | "CANCELLED" | "EXPIRED";
  seats: BookingSeat[];
  totalAmount: number;
  expiresAt?: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface SelectedSeat {
  showSeatId: number;
  seatCode: string;
  price: number;
  seatType: string;
}
