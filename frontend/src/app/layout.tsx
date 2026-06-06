import type { Metadata } from "next";
import "./globals.css";
import { AppProvider } from "@/lib/context";
import { TechPanelProvider } from "@/lib/techPanelContext";
import { Nav } from "@/components/Nav";
import { AiPanel } from "@/components/AiPanel";
import { TechPanel } from "@/components/TechPanel";

export const metadata: Metadata = {
  title: "CinePlex — Movie Booking",
  description: "Book movie tickets online. Fast, easy, and mobile-friendly.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="h-full">
      <body className="min-h-full flex flex-col bg-[var(--bg)] text-white antialiased">
        <AppProvider>
          <TechPanelProvider>
            <Nav />
            <main className="flex-1">{children}</main>
            <AiPanel />
            <TechPanel />
          </TechPanelProvider>
        </AppProvider>
      </body>
    </html>
  );
}
