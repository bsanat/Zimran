import type { CSSProperties } from "react";

const paths: Record<string, string> = {
  copy: "M9 9h11v12H9zM6 16H3V3h12v3",
  ticket: "M3 5h18v5a2 2 0 0 0 0 4v5H3v-5a2 2 0 0 0 0-4V5Zm12 2v2m0 2v2m0 2v2",
  bell: "M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9ZM10 21h4",
  people: "M16 21v-3a6 6 0 0 0-12 0v3M14 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0Zm6 15v-3a6 6 0 0 0-3-5m0-11a4 4 0 0 1 0 8",
  globe: "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM2 12h20M12 2c5 5 5 15 0 20-5-5-5-15 0-20Z",
  arrow: "M5 12h14m-6-6 6 6-6 6",
  chevron: "m6 9 6 6 6-6",
  grid: "M3 3h6v6H3V3Zm12 0h6v6h-6V3ZM3 15h6v6H3v-6Zm12 0h6v6h-6v-6Z",
  back: "M19 12H5m6-6-6 6 6 6",
  pin: "M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0ZM12 7a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z",
  calendar: "M8 2v4m8-4v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14H3V6a2 2 0 0 1 2-2Z",
  clock: "M12 8v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z",
  spark: "m12 2 2.7 7.3L22 12l-7.3 2.7L12 22l-2.7-7.3L2 12l7.3-2.7L12 2Z",
  check: "m5 12 4 4L19 6",
  external: "M14 3h7v7m0-7L10 14m0-10H4v16h16v-6",
  music: "M9 18V5l12-3v13M9 8l12-3M9 18c0 2-2 3-4 3s-3-1-3-2.5S4 16 6 16s3 1 3 2Zm12-3c0 2-2 3-4 3s-3-1-3-2.5 2-3.5 4-3.5 3 1 3 3Z",
  art: "M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 0-4c-2 0-2-3 1-3h3c6 0 4-11-5-11ZM7 8h.01M12 6h.01M17 8h.01M6 13h.01",
  food: "M4 2v7c0 3 6 3 6 0V2M7 2v20M20 2v20m0-20c-5 2-6 10 0 10",
  outdoors: "m12 3 8 10h-4l6 7H2l6-7H4L12 3Zm0 17v3",
  sports: "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM5 5l14 14M5 19 19 5M2 12h20M12 2v20",
  theater: "M3 4c4 3 8 3 12 0v7a6 6 0 0 1-12 0V4Zm3 5h1m4 0h1m-6 4c2 2 3 2 5 0m7-10 3-1v10a6 6 0 0 1-5 6",
  nightlife: "M4 3h16L12 13 4 3Zm8 10v8m-5 0h10",
  technology: "M7 7h10v10H7V7Zm-5 3h5m-5 4h5m10-4h5m-5 4h5M10 2v5m4-5v5m-4 10v5m4-5v5",
  comedy: "M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM7 9h1m8 0h1M7 14c2 5 8 5 10 0H7Z",
  gaming: "M7 7h10c5 0 7 13 3 13l-5-4H9l-5 4C0 20 2 7 7 7ZM5 11h6m-3-3v6m8-3h.01M19 13h.01",
  search: "m21 21-6-6M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0Z",
  alert: "M12 8v5m0 4h.01M12 2 1 21h22L12 2Z",
  user: "M20 21v-3a8 8 0 0 0-16 0v3M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z",
  family: "M3 10 12 2l9 8v11H3V10Zm5 11v-7h8v7M9 8h6",
  books: "M12 5v16M12 5C8 2 4 3 2 4v15c4-1 7-1 10 2 3-3 6-3 10-2V4c-2-1-6-2-10 1Z",
  ai: "M7 7h10v10H7V7Zm3 3v4m4-4v4M10 2v5m4-5v5m-4 10v5m4-5v5M2 10h5m-5 4h5m10-4h5m-5 4h5",
  running: "M15 3h.01M12 7l-4 5-5 1m9-6 3 5 5 1m-8-6 2 8-6 6m6-6 5 5",
  climate: "M20 3C7 1 1 11 7 17S23 17 20 3ZM4 21 16 9",
  fitness: "M7 7v10m10-10v10M3 9v6m18-6v6M7 12h10",
  wellness: "M12 21S1 14 2 7c1-6 8-6 10 0 2-6 9-6 10 0 1 7-10 14-10 14Z",
  crypto: "M9 4v16m5-18v3m0 14v3M6 5h9c7 0 6 7 0 7H9m6 0c7 0 8 7 0 7H6",
};

export function Icon({ name, size = 20, style, className }: { name: string; size?: number; style?: CSSProperties; className?: string }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={style} className={className}><path d={paths[name] ?? paths.spark} /></svg>;
}
