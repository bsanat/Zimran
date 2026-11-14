import sources from "../data/city-photos.json" with { type: "json" };
import { citySlug } from "./city-path.ts";

export type CityPhoto = {
  src: string;
  title: string;
  author: string;
  source: string;
  license: string;
  licenseUrl: string;
};

export function cityPhoto(state: string, city: string): CityPhoto {
  const key = `${state.toLowerCase()}-${citySlug(city)}`;
  const credit = sources[key as keyof typeof sources];
  if (!credit) throw new Error(`Missing city photograph for ${city}, ${state}`);
  return { ...credit, src: key === "ny-new-york" ? "/photos/manhattan-panorama.webp" : `/cities/${key}.webp` };
}
