# ZestyySports - Premium Ad-Free Live Stream Experience

Welcome to **ZestyySports**, a high-performance web-based live streaming and IPTV application built entirely with React, Vite, and Tailwind CSS. The platform is designed to provide crystal-clear, reliable, and entirely ad-free live streams through an impeccably clean and polished user interface.

## 🚀 Features

* **Advanced Media Playback**: Powered by robust native integrations of `hls.js` and `mpegts.js`. Supports everything from `.m3u8` streams and TS/FLV to raw native fallback formats.
* **Ad-Free Premium Environment**: Emphasizes a pure, uninterrupted spectator experience, marked by the iconic blue `ShieldCheck` Ad-Free badge logic.
* **Auto-Proxy Fallback System**: Built-in backend (Node/Express proxy) that automatically mitigates datacenters blocking cross-origin or mixed-content (HTTP/HTTPS) issues.
* **Smart Buffering & Fallback**: Automatically measures stream drops and implements "Connection Exhaustion" retries, with built-in cache controls configured for ultra-low latency. 
* **Dynamic Fullscreen & Picture-in-Picture (PiP)**: Watch games while multitasking. The player responds dynamically, accommodating both desktop and mobile orientations beautifully.
* **M3U Playlist Parser**: Provides the capability to drop your own valid M3U IPTV playlist URL directly into the frontend, mapping channels to a highly polished grid card format.
* **Trial/Lock Progression**: Integrated client-side preview modes leading to simple unlock constraints.

## 📦 Tech Stack

- **Frontend**: React and standard HTML5 Media APIs.
- **Styling**: Tailwind CSS via Vite plugin to deliver the dark mode, red/slate-infused cosmic visual layout entirely without monolithic CSS bundles.
- **Backend / Routing**: Local Express proxy `(server.ts)` bound dynamically to handle bypasses, bundled optimally using ESBuild on build.
- **Video Utilities**: `hls.js`, `mpegts.js`, and specialized React refs to manage the destruction and garbage collection of raw media fragments precisely.
- **Icons**: `lucide-react` for minimal, high-visibility UI iconography. 
- **Animation**: `motion` (`framer-motion`) and Tailwind's native transitions for fluid motion layout feedback.

## ⚙️ How to use 

**Clone & Install Dependencies**
```bash
npm install
```

**Start Local Development Server**
```bash
npm run dev
```

**Build for Production** 
```bash
npm run build
```

**Run Production Server**
```bash
npm run start
```

## 🛠 Project Structure

- `src/components/VideoPlayer.tsx` - The flagship component. Contains all stream instantiation fallback logic, volume bindings, full-screen polyfills, error tracking, loading animations, and lock/trial logic.
- `src/App.tsx` - App logic containing library states (such as grid item configurations). Parses initial `.m3u` resources. 
- `server.ts` - Node Express server enabling proxy utilities to prevent active viewer blocking.

## 🔑 Permissions & Architecture Note

No client side persistent databases are used (beyond simple `localStorage` progression state logic). To edit unlock parameters, refer to the client interface constraints within `VideoPlayer.tsx`.
