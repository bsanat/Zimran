import { demoAction } from "../../../../lib/server/social.ts";
import { ServiceError } from "../../../../lib/server/serpapi.ts";

export const runtime = "nodejs";
async function handle(request: Request, context: { params: Promise<{ path: string[] }> }) {
  try {
    const { path } = await context.params;
    return Response.json(await demoAction(request, path), { headers: { "Cache-Control": "no-store" } });
  } catch (error) {
    const known = error instanceof ServiceError;
    return Response.json({ error: { code: known ? error.code : "DEMO_UNAVAILABLE", message: known ? error.message : "The demo service is temporarily unavailable. Please try again." } }, { status: known ? error.status : 503, headers: { "Cache-Control": "no-store" } });
  }
}
export const GET = handle;
export const POST = handle;
export const PUT = handle;
export const PATCH = handle;
