"use client";

import { useLanguage } from "@/components/LanguageProvider";
import { US_STATES } from "@/lib/locations";
import { US_STATE_MAP } from "@/lib/us-map";

export function UnitedStatesMap({ selectedCode, onSelect }: { selectedCode: string; onSelect: (code: string) => void }) {
  const { t } = useLanguage();
  return <div className="us-map-panel">
    <div className="us-map-scroll" tabIndex={0} role="region" aria-label={t("United States map. Scroll horizontally on smaller screens.")}>
      <svg className="us-map" viewBox="-70 0 1200 625" role="group" aria-labelledby="us-map-title us-map-description">
        <title id="us-map-title">{t("Choose a US state")}</title>
        <desc id="us-map-description">{t("All 50 states. Select a state to show its cities, or use the state filter above. Alaska and Hawaii are shown as insets.")}</desc>
        {US_STATES.map(state => {
          const { path, label, anchor } = US_STATE_MAP[state.code];
          return <g key={state.code} className={`us-map-state${anchor ? " us-map-callout" : ""}`} role="button" tabIndex={0} aria-label={t(state.name)} aria-pressed={selectedCode === state.code} onClick={() => onSelect(state.code)} onKeyDown={event => {
            if (event.key === "Enter" || event.key === " ") { event.preventDefault(); onSelect(state.code); }
          }}>
            <path d={path} vectorEffect="non-scaling-stroke" />
            {anchor && <>
              <polyline points={`${anchor[0]},${anchor[1]} 976,${label[1] - 4} ${label[0] - 12},${label[1] - 4}`} />
              <rect className="us-map-callout-target" x={label[0] - 8} y={label[1] - 20} width={138} height={30} rx={5} />
            </>}
          </g>;
        })}
        <g aria-hidden="true" className="us-map-labels">
          {US_STATES.map(state => {
            const { label, anchor } = US_STATE_MAP[state.code];
            const words = anchor ? [t(state.name)] : t(state.name).split(" ");
            return <text key={state.code} className={`us-map-label${anchor || state.code === "HI" || state.code === "AK" ? " us-map-label-outside" : ""}`} data-selected={selectedCode === state.code} x={label[0]} y={label[1] - (words.length - 1) * 6} textAnchor={anchor ? "start" : "middle"}>
              {words.map((word, index) => <tspan key={index} x={label[0]} dy={index === 0 ? 0 : 13}>{word}</tspan>)}
            </text>;
          })}
        </g>
      </svg>
    </div>
    <div className="us-map-caption"><span><span className="us-map-key" />{t("Selected state")}</span><span>{t("Click a state or use the filters above. Swipe to see the full map on mobile.")}</span></div>
  </div>;
}
