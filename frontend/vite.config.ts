import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    allowedHosts: ["0.0.0.0", "piserver01", "nucbox01"],
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.VITE_API_PROXY_TARGET ?? "http://backend:5174",
        changeOrigin: true
      }
    }
  }
});
