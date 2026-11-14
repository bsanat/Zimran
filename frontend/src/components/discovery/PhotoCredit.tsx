"use client";

import { useLanguage } from "@/components/LanguageProvider";
import type { CityPhoto } from "@/lib/city-photos";

export function PhotoCredit({ photo }: { photo: CityPhoto }) {
  const { t } = useLanguage();
  return <small className="city-photo-credit" title={t("Photo resized for the banner")}>
    {t("Photo")} · <a href={photo.source} target="_blank" rel="noreferrer">{photo.author}</a>
    <span aria-hidden="true"> · </span><a href={photo.licenseUrl} target="_blank" rel="noreferrer">{photo.license}</a>
  </small>;
}
