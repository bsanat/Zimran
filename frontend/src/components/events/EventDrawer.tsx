"use client";

import { useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EventDetails } from "./EventDetails";
import { useEventOrder } from "./EventLink";
import "./event-drawer.css";

export function EventDrawer({ id }: { id: string }) {
  const { t } = useLanguage();
  const router = useRouter();
  const dialog = useRef<HTMLDialogElement>(null);
  const order = useEventOrder();
  const active = order.indexOf(id);
  useEffect(() => {
    const panel = dialog.current!;
    panel.showModal();
    return () => panel.close();
  }, []);

  useEffect(() => { if (dialog.current) dialog.current.scrollTop = 0; }, [id]);
  const close = () => router.back();
  const steps = active >= 0 && order.length > 1 && <div className="event-drawer-steps">
    {[-1, 1].map(step => {
      const target = order[active + step];
      const label = t(step === -1 ? "Previous event" : "Next event");
      const chevron = <Icon name="chevron" size={17} className={step === -1 ? "drawer-up" : undefined} />;
      return target
        ? <Link key={step} className="drawer-icon-button" href={`/events/${encodeURIComponent(target)}`} replace scroll={false} aria-label={label} title={label}>{chevron}</Link>
        : <button key={step} className="drawer-icon-button" type="button" disabled aria-label={label}>{chevron}</button>;
    })}
  </div>;
  return <dialog className="event-drawer" aria-label={t("Event details")} ref={dialog}
    onCancel={event => { event.preventDefault(); close(); }}
    onClick={event => {
      if (event.target !== event.currentTarget) return;
      const rect = event.currentTarget.getBoundingClientRect();
      if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) close();
    }}>
    <EventDetails key={id} id={id} closeControl={
      <button type="button" autoFocus onClick={close} className="drawer-icon-button" aria-label={t("Close details")} title={t("Close details")}>
        <Icon name="chevron" className="drawer-close-chevron" size={20} />
      </button>
    } navigation={steps} />
  </dialog>;
}
