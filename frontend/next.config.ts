import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    const origin = process.env.API_PROXY_ORIGIN;
    return { beforeFiles: origin ? [{ source: "/api/:path*", destination: `${origin.replace(/\/$/, "")}/api/:path*` }] : [] };
  },
};

export default nextConfig;
