import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const origin = process.env.API_PROXY_ORIGIN;
    // Every guest feature runs on Spring; only demo/premium and demo/check-alerts of /api/demo/* exist there.
    const backendPaths = ["events/:path*", "plan", "health", "meta", "guest/session", "pair/:path*", "ai/:path*", "alerts/:path*", "notifications/:path*", "subscription", "demo/premium", "demo/check-alerts", "buddy/:path*", "reports", "blocks/:path*"];
    return { beforeFiles: origin ? backendPaths.map(path => ({ source: `/api/${path}`, destination: `${origin.replace(/\/$/, "")}/api/${path}` })) : [] };
  },
};

export default nextConfig;
