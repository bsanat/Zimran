"use client";

import Link from "next/link";
import { createContext, useContext, useState, type ComponentProps, type ReactNode } from "react";

const EventOrder = createContext<readonly string[]>([]);
const SetEventOrder = createContext<(ids: readonly string[]) => void>(() => {});

export function EventOrderProvider({ children }: { children: ReactNode }) {
  const [order, setOrder] = useState<readonly string[]>([]);
  return <SetEventOrder.Provider value={setOrder}><EventOrder.Provider value={order}>
    {children}
  </EventOrder.Provider></SetEventOrder.Provider>;
}

export function useEventOrder() {
  return useContext(EventOrder);
}

export function EventLink({ eventId, eventIds, ...props }: Omit<ComponentProps<typeof Link>, "href" | "onNavigate"> & {
  eventId: string;
  eventIds?: readonly string[];
}) {
  const setOrder = useContext(SetEventOrder);
  return <Link {...props} href={`/events/${encodeURIComponent(eventId)}`} scroll={false}
    onNavigate={() => setOrder(eventIds ?? [eventId])} />;
}
