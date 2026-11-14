"use client";

import { useEffect, useState } from "react";
import { useLanguage } from "@/components/LanguageProvider";
import { Icon } from "@/components/Icon";

export function CityClock({ timezone, initialTime }: { timezone: string; initialTime: string }) {
  const { locale, t } = useLanguage();
  const [label, setLabel] = useState(initialTime);

  useEffect(() => {
    const clock = new Intl.DateTimeFormat(locale, { timeZone: timezone, hour: "2-digit", minute: "2-digit", timeZoneName: "shortOffset" });
    const tick = () => setLabel(clock.format(new Date()));
    tick();
    const timer = window.setInterval(tick, 1000);
    return () => window.clearInterval(timer);
  }, [timezone, locale]);

  return <span className="city-guide-time" title={t("Local time")}>
    <Icon name="clock" size={16} /><span className="sr-only">{t("Local time")} </span>{label}
  </span>;
}
