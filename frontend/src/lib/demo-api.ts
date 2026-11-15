import { ApiError } from "./api.ts";
import type { DemoSubscription } from "../types/social.ts";

const sessionKey = "outly-demo-session-v2";
let token = "";
let sessionPromise: Promise<string> | null = null;

async function send(path: string, options: RequestInit, bearer?: string): Promise<Record<string, unknown>> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body) headers.set("Content-Type", "application/json");
  if (bearer) headers.set("Authorization", `Bearer ${bearer}`);
  const signal = options.signal ? AbortSignal.any([options.signal, AbortSignal.timeout(15_000)]) : AbortSignal.timeout(15_000);
  let response: Response;
  try { response = await fetch(`/api/demo/${path}`, { ...options, headers, signal, cache: "no-store" }); }
  catch (cause) {
    if (options.signal?.aborted) throw cause;
    throw new ApiError("We could not reach the demo service. Please try again.");
  }
  let body: unknown;
  try { body = await response.json(); } catch { throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE"); }
  if (!response.ok) {
    const error = body && typeof body === "object" && "error" in body ? body.error : null;
    throw new ApiError(error && typeof error === "object" && "message" in error && typeof error.message === "string" ? error.message : "We could not complete your request. Please try again.", response.status,
      error && typeof error === "object" && "code" in error && typeof error.code === "string" ? error.code : "API_ERROR");
  }
  if (!body || typeof body !== "object" || Array.isArray(body)) throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE");
  return body as Record<string, unknown>;
}

export async function getDemoToken(): Promise<string> {
  if (token) return token;
  try { token = sessionStorage.getItem(sessionKey) || ""; } catch { /* Keep this tab's identity in memory if storage is unavailable. */ }
  if (token) return token;
  if (!sessionPromise) sessionPromise = (async () => {
    const result = await send("session", { method: "POST", body: "{}" });
    if (typeof result.token !== "string" || !/^[a-zA-Z0-9_-]{32,}$/.test(result.token)) throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE");
    token = result.token;
    try { sessionStorage.setItem(sessionKey, token); } catch { /* The in-memory session still works until reload. */ }
    return token;
  })().finally(() => { sessionPromise = null; });
  return sessionPromise;
}

export async function demoRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const bearer = await getDemoToken();
  try { return await send(path, options, bearer) as T; }
  catch (cause) {
    if (cause instanceof ApiError && cause.status === 401) {
      token = "";
      try { sessionStorage.removeItem(sessionKey); } catch { /* A fresh session can be created on retry. */ }
    }
    throw cause;
  }
}

export function isSubscription(value: unknown): value is DemoSubscription {
  return !!value && typeof value === "object" && "status" in value && ["FREE", "DEMO_PREMIUM"].includes(String(value.status)) && "price" in value && value.price === 4.99 && "currency" in value && value.currency === "USD";
}
