import type { EventData } from "./event.ts";
import type { Interest, PlannerRequest } from "./planner.ts";

export interface DemoSubscription { status: "FREE" | "DEMO_PREMIUM"; price: number; currency: string }
export interface BuddyRequest {
  id: string; displayName: string; message: string;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELLED";
  isOwner: boolean; createdAt: string;
  /** Set on an accepted request: how to meet safely. */
  safetyNotice?: string;
}
export interface BuddyPost {
  id: string; eventId: string; displayName: string; bio: string; language: string | null;
  status: "OPEN" | "MATCHED" | "CLOSED" | "EXPIRED";
  isOwner: boolean; requests: BuddyRequest[]; createdAt: string; expiresAt: string; demo: true;
}
// The backend keeps no display name, so it is optional.
export interface PairPreferences extends Omit<PlannerRequest, "city"> { displayName?: string }
/** A suggested event; voteId is the id the backend expects in a vote when it differs from event.id. */
export interface PairSuggestion { event: EventData; reason: string; matchScore?: number; voteId?: string }
export type PairVote = "LIKE" | "MAYBE" | "PASS";
export interface PairSession {
  id: string; city: string; role: "A" | "B";
  participantA: PairPreferences; participantB: PairPreferences | null;
  status: "WAITING" | "READY" | "MATCHED" | "CLOSED" | "EXPIRED";
  recommendations: PairSuggestion[]; votes: { A: Record<string, PairVote>; B: Record<string, PairVote> };
  sharedEventId: string | null; expiresAt: string; demo: true;
  /** Why nothing fits both people, and what to relax (backend NO_MATCH). */
  conflict?: { message: string; relaxMessage: string | null };
}
export interface AlertPreferences {
  /** Backend category ids (GET /api/meta), e.g. "tech". */
  city: string; categorySlugs: string[]; budgetMax?: number; startDate?: string; endDate?: string;
  frequency: "instant" | "daily" | "weekly"; channels: ["in_app"]; paused: boolean;
}
export interface DemoNotification {
  id: string; eventId: string; title: string; categorySlug: string;
  readAt: string | null; createdAt: string; demo: true;
}
export interface NotificationFeed { items: DemoNotification[]; unreadCount: number }
