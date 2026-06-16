var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));

// server.ts
var import_express = __toESM(require("express"), 1);
var import_vite = require("vite");
var import_path = __toESM(require("path"), 1);
var import_url = require("url");
var import_http = __toESM(require("http"), 1);
var import_https = __toESM(require("https"), 1);
var import_meta = {};
var __dirname = (() => {
  try {
    if (typeof import_meta !== "undefined" && import_meta.url) {
      return import_path.default.dirname((0, import_url.fileURLToPath)(import_meta.url));
    }
  } catch (e) {
  }
  return process.cwd();
})();
async function startServer() {
  const app = (0, import_express.default)();
  const PORT = 3e3;
  app.get("/api/proxy", (req, res) => {
    const streamUrl = req.query.url;
    if (!streamUrl) {
      return res.status(400).send("URL is required");
    }
    const followRedirect = (url, depth) => {
      if (depth > 5) {
        return res.status(500).send("Too many redirects");
      }
      try {
        const parsedUrl = new URL(url);
        const isHttps = parsedUrl.protocol === "https:";
        const client = isHttps ? import_https.default : import_http.default;
        const options = {
          method: req.method,
          headers: {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            "Accept": "*/*",
            "Accept-Language": "en-US,en;q=0.9",
            "Connection": "keep-alive",
            "Referer": new URL(url).origin + "/"
          }
        };
        if (req.headers.range) {
          options.headers["Range"] = req.headers.range;
        }
        const proxyReq = client.request(url, options, (proxyRes) => {
          if (proxyRes.statusCode && proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
            let location = proxyRes.headers.location;
            if (!location.startsWith("http")) {
              location = new URL(location, url).toString();
            }
            return followRedirect(location, depth + 1);
          }
          const headersToForward = {};
          for (const [key, value] of Object.entries(proxyRes.headers)) {
            if (value && key.toLowerCase() !== "access-control-allow-origin" && key.toLowerCase() !== "host") {
              headersToForward[key] = value;
            }
          }
          headersToForward["access-control-allow-origin"] = "*";
          headersToForward["access-control-expose-headers"] = "*";
          headersToForward["cache-control"] = "no-cache, no-store, must-revalidate";
          headersToForward["pragma"] = "no-cache";
          headersToForward["expires"] = "0";
          if (!headersToForward["content-type"]) {
            headersToForward["content-type"] = "video/mp2t";
          }
          res.writeHead(proxyRes.statusCode || 200, headersToForward);
          proxyRes.pipe(res);
        });
        proxyReq.on("error", (err) => {
          console.error("Proxy request error:", err);
          if (!res.headersSent) {
            res.status(500).send("Proxy error");
          }
        });
        req.on("close", () => {
          proxyReq.destroy();
        });
        proxyReq.end();
      } catch (err) {
        if (!res.headersSent) {
          res.status(400).send("Invalid URL");
        }
      }
    };
    followRedirect(streamUrl, 0);
  });
  app.get("/api/m3u", async (req, res) => {
    const url = req.query.url || "https://m3u-tvb.pages.dev/ixp.m3u";
    try {
      const response = await fetch(url);
      const text = await response.text();
      res.setHeader("Access-Control-Allow-Origin", "*");
      res.setHeader("Content-Type", "text/plain");
      res.send(text);
    } catch (err) {
      console.error("M3U Fetch Error:", err);
      res.status(500).send("Error fetching M3U");
    }
  });
  if (process.env.NODE_ENV !== "production") {
    const vite = await (0, import_vite.createServer)({
      server: { middlewareMode: true },
      appType: "spa"
    });
    app.use(vite.middlewares);
  } else {
    const distPath = import_path.default.join(process.cwd(), "dist");
    app.use(import_express.default.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(import_path.default.join(distPath, "index.html"));
    });
  }
  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}
startServer();
//# sourceMappingURL=server.cjs.map
