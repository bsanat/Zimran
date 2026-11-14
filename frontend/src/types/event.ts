export type EventStatus = "scheduled" | "cancelled" | "postponed";

export interface EventData {
  id: string;
  source: string;
  sourceEventId: string;
  title: string;
  category: string;
  city: string;
  startAt: string | null;
  dateText?: string | null;
  timezone: string;
  venue: string | null;
  address: string | null;
  description: string | null;
  imageUrl: string | null;
  priceMin: number | null;
  priceMax: number | null;
  priceKnown: boolean;
  priceText?: string | null;
  currency: string | null;
  sourceUrl: string | null;
  status: EventStatus;
  isMajor?: boolean;
}

export interface EventsResponse {
  events: EventData[];
  pagination: { page: number; pageSize: number; hasMore: boolean };
}
