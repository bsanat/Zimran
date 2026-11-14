"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError, generatePlan, isPlannerResponse } from "@/lib/api";
import { createDefaultDraft, INTERESTS, isPlannerDraft, toPlannerRequest, validateDraft } from "@/lib/planner";
import type { PlannerDraft, PlannerResponse } from "@/types/planner";

// Keep the storage keys so saved drafts and plans survive the rebrand.
const DRAFT_KEY = "sxodim:draft:v1";
const PLAN_KEY = "sxodim:plan:v1";
let lastPlan: PlannerResponse | null = null;
let lastDraft: { draft: PlannerDraft; step: number } | null = null;

export function readLastPlan(): PlannerResponse | null {
  try {
    const cached: unknown = JSON.parse(sessionStorage.getItem(PLAN_KEY) ?? "null");
    if (isPlannerResponse(cached)) return cached;
  } catch { /* Storage can be unavailable in private browsing. */ }
  return lastPlan;
}

export function usePlanner() {
  const router = useRouter();
  const [draft, setDraft] = useState<PlannerDraft>(createDefaultDraft);
  const [step, setStep] = useState(0);
  const [ready, setReady] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof PlannerDraft, string>>>({});
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const pending = useRef(false);
  const request = useRef<AbortController | null>(null);

  useEffect(() => {
    let restored = lastDraft;
    try {
      const cached = JSON.parse(sessionStorage.getItem(DRAFT_KEY) ?? "null");
      if (cached && isPlannerDraft(cached.draft) && Number.isInteger(cached.step) && cached.step >= 0 && cached.step <= 3) restored = cached;
    } catch { /* Keep the in-memory draft if browser storage is unavailable. */ }
    const initialDraft = restored?.draft ?? createDefaultDraft();
    const interest = new URLSearchParams(window.location.search).get("interest");
    const selected = INTERESTS.find(item => item.id === interest);
    setDraft(selected ? { ...initialDraft, interests: [...new Set([...initialDraft.interests, selected.id])] } : initialDraft);
    setStep(selected ? 1 : restored?.step ?? 0);
    setReady(true);
    return () => request.current?.abort();
  }, []);

  useEffect(() => {
    if (!ready) return;
    lastDraft = { draft, step };
    try { sessionStorage.setItem(DRAFT_KEY, JSON.stringify(lastDraft)); } catch { /* The form remains usable without storage. */ }
  }, [draft, step, ready]);

  function update<K extends keyof PlannerDraft>(field: K, value: PlannerDraft[K]) {
    setDraft(current => ({ ...current, [field]: value }));
    setErrors(current => ({ ...current, [field]: undefined, ...(field === "datePreset" ? { customDate: undefined } : {}) }));
    setError("");
  }

  function next() {
    const all = validateDraft(draft);
    const fields: (keyof PlannerDraft)[] = step === 0 ? ["city"] : step === 1 ? ["interests"] : ["datePreset", "customDate", "budget", "timePreference", "notes"];
    const currentErrors = Object.fromEntries(Object.entries(all).filter(([key]) => fields.includes(key as keyof PlannerDraft)));
    setErrors(currentErrors);
    if (Object.keys(currentErrors).length === 0) setStep(current => Math.min(current + 1, 3));
  }

  async function submit() {
    if (pending.current) return;
    const validation = validateDraft(draft);
    setErrors(validation);
    if (Object.keys(validation).length) {
      setStep(validation.city ? 0 : validation.interests ? 1 : 2);
      return;
    }
    pending.current = true;
    setBusy(true);
    setError("");
    const controller = new AbortController();
    request.current = controller;
    try {
      const response = await generatePlan(toPlannerRequest(draft), controller.signal);
      if (controller.signal.aborted) return;
      lastPlan = response;
      try { sessionStorage.setItem(PLAN_KEY, JSON.stringify(response)); } catch { /* Client navigation can use the in-memory response. */ }
      router.push("/planner/results");
    } catch (cause) {
      if (controller.signal.aborted) return;
      setError(cause instanceof Error ? cause.message : "We couldn’t create your plan. Please try again.");
      if (cause instanceof ApiError && cause.status === 400 && cause.field) {
        const field = ["date", "startDate", "endDate"].includes(cause.field) ? "customDate" : cause.field;
        if (field in draft) {
          setErrors({ [field]: cause.message });
          setStep(field === "city" ? 0 : field === "interests" ? 1 : 2);
        }
      }
    } finally {
      pending.current = false;
      if (!controller.signal.aborted) setBusy(false);
    }
  }

  return { draft, step, setStep, ready, errors, error, busy, update, next, submit };
}
