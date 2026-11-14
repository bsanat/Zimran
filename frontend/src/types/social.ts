import type { Interest, PlannerRequest, Recommendation } from "./planner.ts";

export interface DemoSubscription { status: "FREE" | "DEMO_PREMIUM"; price: 9.99; currency: "USD" }
export interface BuddyRequest {
  id: string; displayName: string; message: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELLED";
  isOwner: boolean; createdAt: string;
}
export interface BuddyPost {
  id: string; eventId: string; displayName: string; bio: string; language: string;
  status: "OPEN" | "MATCHED" | "CLOSED" | "EXPIRED";
  isOwner: boolean; requests: BuddyRequest[]; createdAt: string; expiresAt: string; demo: true;
}
export interface PairPreferences extends Omit<PlannerRequest, "city"> { displayName: string }
export type PairVote = "LIKE" | "MAYBE" | "PASS";
export interface PairSession {
  id: string; city: string; role: "A" | "B";
  participantA: PairPreferences; participantB: PairPreferences | null;
  status: "WAITING" | "READY" | "MATCHED" | "CLOSED" | "EXPIRED";
  recommendations: Recommendation[]; votes: { A: Record<string, PairVote>; B: Record<string, PairVote> };
  sharedEventId: string | null; expiresAt: string; demo: true;
}
export interface AlertPreferences {
  city: string; categorySlugs: Interest[]; budgetMax?: number; startDate?: string; endDate?: string;
  frequency: "instant" | "daily" | "weekly"; channels: ["in_app"]; paused: boolean;
}
export interface DemoNotification {
  id: string; eventId: string; title: string; categorySlug: Interest;
  readAt: string | null; createdAt: string; demo: true;
}
export interface NotificationFeed { items: DemoNotification[]; unreadCount: number }
