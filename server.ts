
import express from 'express';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';
import http from 'http';
import https from 'https';

// Safe resolution for __dirname across CJS / ESM environments
const __dirname = (() => {
  try {
    if (typeof import.meta !== 'undefined' && import.meta.url) {
      return path.dirname(fileURLToPath(import.meta.url));
    }
  } catch (e) {
    // Fallback to process.cwd() if anything fails
  }
  return process.cwd();
})();

async function startServer() {
  const app = express();
  const PORT = 3000;

  // Simple Proxy to bypass CORS and Mixed Content issues
  app.get('/api/proxy', (req, res) => {
    const streamUrl = req.query.url as string;
    if (!streamUrl) {
      return res.status(400).send('URL is required');
    }

    const followRedirect = (url: string, depth: number) => {
      if (depth > 5) {
        return res.status(500).send('Too many redirects');
      }

      try {
        const parsedUrl = new URL(url);
        const isHttps = parsedUrl.protocol === 'https:';
        const client = isHttps ? https : http;

        const options = {
          method: req.method,
          headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
            'Accept': '*/*',
            'Accept-Language': 'en-US,en;q=0.9',
            'Connection': 'keep-alive',
            'Referer': new URL(url).origin + '/',
          }
        };

        if (req.headers.range) {
          options.headers['Range'] = req.headers.range as string;
        }

        const proxyReq = client.request(url, options, (proxyRes) => {
          if (proxyRes.statusCode && proxyRes.statusCode >= 300 && proxyRes.statusCode < 400 && proxyRes.headers.location) {
            let location = proxyRes.headers.location;
            if (!location.startsWith('http')) {
              location = new URL(location, url).toString();
            }
            return followRedirect(location, depth + 1);
          }

          // Forward headers
          const headersToForward: Record<string, string | string[]> = {};
          
          for (const [key, value] of Object.entries(proxyRes.headers)) {
              if (value && key.toLowerCase() !== 'access-control-allow-origin' && key.toLowerCase() !== 'host') {
                  headersToForward[key] = value;
              }
          }
          
          headersToForward['access-control-allow-origin'] = '*';
          headersToForward['access-control-expose-headers'] = '*';
          headersToForward['cache-control'] = 'no-cache, no-store, must-revalidate';
          headersToForward['pragma'] = 'no-cache';
          headersToForward['expires'] = '0';
          if (!headersToForward['content-type']) {
              headersToForward['content-type'] = 'video/mp2t';
          }

          res.writeHead(proxyRes.statusCode || 200, headersToForward);

          // Pipe the stream
          proxyRes.pipe(res);
        });

        proxyReq.on('error', (err) => {
          console.error('Proxy request error:', err);
          if (!res.headersSent) {
            res.status(500).send('Proxy error');
          }
        });

        req.on('close', () => {
          proxyReq.destroy();
        });

        proxyReq.end();
      } catch (err) {
        if (!res.headersSent) {
          res.status(400).send('Invalid URL');
        }
      }
    };

    followRedirect(streamUrl, 0);
  });

  // Proxy the M3U to avoid CORS on initial fetch
  app.get('/api/m3u', async (req, res) => {
    const url = req.query.url as string || 'https://m3u-tvb.pages.dev/ixp.m3u';
    try {
      const response = await fetch(url);
      const text = await response.text();
      res.setHeader('Access-Control-Allow-Origin', '*');
      res.setHeader('Content-Type', 'text/plain');
      res.send(text);
    } catch (err) {
      console.error('M3U Fetch Error:', err);
      res.status(500).send('Error fetching M3U');
    }
  });

  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
