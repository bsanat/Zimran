import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const origin = process.env.API_PROXY_ORIGIN;
    // Every guest feature runs on Spring; only demo/premium and demo/check-alerts of /api/demo/* exist there.
    // Vercel adds a trailing slash for an empty wildcard; Spring collection routes require exact paths.
    const backendPaths = ["events", "events/:path*", "plan", "health", "meta", "guest/session", "pair/:path*", "ai/:path*", "alerts/:path*", "notifications", "notifications/:path*", "subscription", "demo/premium", "demo/check-alerts", "buddy/:path*", "reports", "blocks", "blocks/:path*"];
    return { beforeFiles: origin ? backendPaths.map(path => ({ source: `/api/${path}`, destination: `${origin.replace(/\/$/, "")}/api/${path}` })) : [] };
  },
};

export default nextConfig;
