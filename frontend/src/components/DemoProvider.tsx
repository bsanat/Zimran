"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { demoRequest, isSubscription } from "@/lib/demo-api";
import type { DemoSubscription, NotificationFeed } from "@/types/social";

interface DemoContextValue {
  subscription: DemoSubscription | null; loading: boolean; error: string; unreadCount: number;
  refresh: () => Promise<void>; activate: () => Promise<void>; deactivate: () => Promise<void>;
  refreshNotifications: () => Promise<void>;
}
const DemoContext = createContext<DemoContextValue | null>(null);

export function DemoProvider({ children }: { children: React.ReactNode }) {
  const [subscription, setSubscription] = useState<DemoSubscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [unreadCount, setUnreadCount] = useState(0);
  const refreshNotifications = useCallback(async () => {
    const feed = await demoRequest<NotificationFeed>("notifications");
    setUnreadCount(feed.unreadCount);
  }, []);
  const refresh = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const value = await demoRequest<DemoSubscription>("subscription");
      if (!isSubscription(value)) throw new Error("The server returned an incomplete response. Please try again.");
      setSubscription(value);
      if (value.status === "DEMO_PREMIUM") await refreshNotifications();
      else setUnreadCount(0);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again.");
    } finally { setLoading(false); }
  }, [refreshNotifications]);
  const updateSubscription = useCallback(async (action: "activate" | "deactivate") => {
    const value = await demoRequest<DemoSubscription>("subscription", { method: "POST", body: JSON.stringify({ action }) });
    if (!isSubscription(value)) throw new Error("The server returned an incomplete response. Please try again.");
    setSubscription(value); setError("");
    if (value.status === "DEMO_PREMIUM") await refreshNotifications();
    else setUnreadCount(0);
  }, [refreshNotifications]);
  const activate = useCallback(() => updateSubscription("activate"), [updateSubscription]);
  const deactivate = useCallback(() => updateSubscription("deactivate"), [updateSubscription]);
  useEffect(() => { void refresh(); }, [refresh]);
  useEffect(() => {
    if (subscription?.status !== "DEMO_PREMIUM") return;
    const timer = setInterval(() => { if (!document.hidden) void refreshNotifications().catch(() => {}); }, 15_000);
    return () => clearInterval(timer);
  }, [subscription?.status, refreshNotifications]);
  return <DemoContext.Provider value={{ subscription, loading, error, unreadCount, refresh, activate, deactivate, refreshNotifications }}>{children}</DemoContext.Provider>;
}

export function useDemo() {
  const value = useContext(DemoContext);
  if (!value) throw new Error("DemoProvider is missing.");
  return value;
}
