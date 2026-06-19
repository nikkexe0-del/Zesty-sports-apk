import React, { useEffect, useState, useMemo, useRef, useTransition, memo } from 'react';
import { 
  BrowserRouter as Router, 
  Routes, 
  Route, 
  useNavigate, 
  useParams,
  Link,
  Navigate
} from 'react-router-dom';
import { 
  Search, 
  Loader2, 
  ExternalLink,
  ChevronLeft,
  ChevronRight,
  Play,
  MonitorPlay,
  Tv,
  X,
  Info,
  Send,
  ShieldCheck,
  Heart,
  Home
} from 'lucide-react';
import { parseM3U, M3UItem } from './lib/m3u';
import { VideoPlayer } from './components/VideoPlayer';

const SOURCES_INDIA = [
  { id: 'adl', name: 'ADL Playlist', url: 'https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/adl.m3u' }
];

const SOURCES_USA = [
  { id: 'adl', name: 'ADL Playlist', url: 'https://raw.githubusercontent.com/nikkexe0-del/alexplaylist/refs/heads/main/adl.m3u' }
];

/* ─── Error Boundary ─────────────────────────────── */
class ErrorBoundary extends React.Component<{ children: React.ReactNode }, { hasError: boolean }> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() { return { hasError: true }; }
  componentDidCatch(error: any, errorInfo: any) { console.error("Error Boundary caught:", error, errorInfo); }
  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-neutral-100 dark:bg-black flex flex-col items-center justify-center p-6 text-center transition-colors">
          <Tv className="w-16 h-16 text-red-600 mb-6" />
          <h2 className="text-2xl font-black text-neutral-900 dark:text-white uppercase italic tracking-tighter mb-4">Something went wrong</h2>
          <p className="text-neutral-600 dark:text-neutral-400 text-sm max-w-sm mb-8 font-medium">The application encountered an error. Please try refreshing.</p>
          <button 
            onClick={() => window.location.reload()} 
            className="px-8 py-3 bg-red-600 text-white font-black rounded-xl hover:bg-neutral-800 transition-all uppercase tracking-widest text-xs shadow-lg shadow-red-600/30"
          >
            Refresh App
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

/* ─── channel mini card (grid) ─────────────────────────────── */
const ChannelMiniCard = memo(({
  channel,
  onPlay,
  isFavorite,
  onToggleFavorite
}: {
  channel: M3UItem;
  onPlay: (c: M3UItem) => void;
  isFavorite?: boolean;
  onToggleFavorite?: (id: string, e: React.MouseEvent) => void;
}) => {
  return (
    <button
      onClick={() => onPlay(channel)}
      className="flex flex-col w-full text-left group bg-white dark:bg-neutral-900/40 rounded-lg border border-neutral-200 dark:border-white/5 hover:border-red-500/30 transition-all overflow-hidden p-3 relative shadow-sm hover:shadow-md"
    >
      {onToggleFavorite && (
        <div 
          onClick={(e) => onToggleFavorite(channel.id, e)}
          className="absolute top-2 left-2 z-20 p-1.5 rounded-full bg-black/40 backdrop-blur-sm border border-white/10 hover:bg-black/60 transition-colors"
        >
          <Heart className={`w-3.5 h-3.5 ${isFavorite ? 'fill-red-500 text-red-500' : 'text-white'}`} />
        </div>
      )}
      <div className="w-full aspect-video bg-neutral-100 dark:bg-neutral-950 rounded-md overflow-hidden relative mb-3 mt-1">
        <div className="absolute inset-0 flex items-center justify-center p-3 bg-black/5 dark:bg-black/40 backdrop-blur-sm">
          {channel.logo ? (
            <img
              src={channel.logo}
              alt={channel.name}
              className="max-w-full max-h-[70%] object-contain group-hover:scale-110 transition-transform duration-500"
              referrerPolicy="no-referrer"
              loading="lazy"
              onError={(e) => {
                const target = e.currentTarget;
                target.onerror = null;
                target.src = 'https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=200&auto=format&fit=crop';
              }}
            />
          ) : (
            <div className="font-black text-neutral-600 text-[10px] uppercase text-center leading-tight px-2">
              {channel.name}
            </div>
          )}
        </div>
        <span className="badge-live absolute top-1.5 right-1.5 z-10 text-[7px]">
          <span className="live-dot" />
          LIVE
        </span>
      </div>
      <h5 className="text-[11px] font-bold truncate text-neutral-800 dark:text-white leading-tight flex items-center gap-1.5 mt-2">
        {channel.name} 
        <span className="shrink-0 bg-blue-500/10 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 text-[7px] px-1 py-0.5 rounded uppercase tracking-wider font-bold flex items-center gap-0.5">
           <ShieldCheck className="w-2 h-2" /> Ad-Free
        </span>
      </h5>
      <p className="text-[8px] font-bold text-neutral-500 dark:text-neutral-600 uppercase tracking-widest mt-0.5">
        {channel.group || ""}
      </p>
    </button>
  );
});

/* ─── Main View ─────────────────────────────── */
const MainView = memo(({ mainChannels, allChannels, loading, loadError, playlistId }: { mainChannels: M3UItem[], allChannels: M3UItem[], loading: boolean, loadError: string | null, playlistId: string }) => {
  const navigate = useNavigate();
  const [query, setQuery] = useState(() => localStorage.getItem('zesty_search_query') || "");
  const [debouncedQuery, setDebouncedQuery] = useState(query);
  const [activeGroup, setActiveGroup] = useState<string>("All");
  const [displayLimit, setDisplayLimit] = useState(40);
  const [activeTab, setActiveTab] = useState<'home' | 'favorites' | 'search'>(() => (localStorage.getItem('zesty_activeTab') as any) || 'home');
  const [favorites, setFavorites] = useState<string[]>(() => {
    const saved = localStorage.getItem('zesty_favorites');
    return saved ? JSON.parse(saved) : [];
  });

  useEffect(() => {
    localStorage.setItem('zesty_activeTab', activeTab);
  }, [activeTab]);

  const toggleFavorite = React.useCallback((id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    e.preventDefault();
    setFavorites(prev => {
      const next = prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id];
      localStorage.setItem('zesty_favorites', JSON.stringify(next));
      return next;
    });
  }, []);

  // Debounce search query
  useEffect(() => {
    localStorage.setItem('zesty_search_query', query);
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
      setDisplayLimit(40); // Reset limit on search
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  const groupedData = useMemo(() => {
    const map = new Map<string, M3UItem[]>();
    map.set("All", mainChannels);
    for (let i = 0; i < mainChannels.length; i++) {
        const c = mainChannels[i];
        if (!c.group) continue;
        if (!map.has(c.group)) map.set(c.group, []);
        map.get(c.group)!.push(c);
    }
    return map;
  }, [mainChannels]);

  const groups = useMemo(() => {
    const allGroups = Array.from(groupedData.keys()).filter(g => g !== "All").sort();
    
    const priority = ["FIFA World Cup 2026", "Cricket", "Football"];
    const ordered: string[] = [];
    
    priority.forEach(p => {
      const match = allGroups.find(g => g.toLowerCase() === p.toLowerCase());
      if (match) ordered.push(match);
    });
    
    allGroups.forEach(g => {
      if (!priority.some(p => p.toLowerCase() === g.toLowerCase())) {
        ordered.push(g);
      }
    });
    
    return ["All", ...ordered];
  }, [groupedData]);

  const filtered = useMemo(() => {
    if (activeTab === 'favorites') {
      return allChannels.filter(c => favorites.includes(c.id));
    }
    
    if (activeTab === 'search') {
      const q = debouncedQuery.trim().toLowerCase();
      if (q) return allChannels.filter((c) => c.name.toLowerCase().includes(q));
      return [];
    }

    return groupedData.get(activeGroup) || mainChannels;
  }, [groupedData, mainChannels, allChannels, debouncedQuery, activeGroup, activeTab, favorites]);

  const displayedChannels = useMemo(() => {
    return filtered.slice(0, displayLimit);
  }, [filtered, displayLimit]);

  const isSearching = activeTab === 'search' && query.trim().length > 0;

  const handlePlay = React.useCallback((ch: M3UItem) => {
    // Navigate via ID so cross-playlist items resolve correctly
    navigate(`/${playlistId}/${encodeURIComponent(ch.id)}`);
  }, [navigate, playlistId]);

  const handleLoadMore = React.useCallback(() => {
    setDisplayLimit(prev => prev + 40);
  }, []);

  useEffect(() => {
    setDisplayLimit(40); // Reset limit on group change
  }, [activeGroup]);

  return (
    <div className="min-h-screen bg-neutral-100 dark:bg-neutral-950 text-neutral-900 dark:text-white font-sans flex flex-col selection:bg-red-600 selection:text-white transition-colors duration-300">
      <main className="pt-8 px-4 sm:px-6 lg:px-12 pb-16 flex-1 flex flex-col max-w-[1800px] w-full mx-auto">
        {/* Hero */}
        <section className="relative min-h-[180px] sm:min-h-[240px] md:h-[300px] w-full rounded-2xl overflow-hidden mb-6 bg-gradient-to-br from-neutral-900 to-black border border-neutral-200 dark:border-white/5 flex flex-col justify-end p-4 sm:p-8 md:p-12 shadow-xl transition-colors duration-300">
          {/* Subtle grid texture */}
          <div className="absolute inset-0 z-0 bg-[linear-gradient(to_right,#ffffff05_1px,transparent_1px),linear-gradient(to_bottom,#ffffff05_1px,transparent_1px)] bg-[size:24px_24px]" />
          
          <div className="absolute inset-0 bg-gradient-to-t from-black/90 via-black/40 to-transparent z-10" />
          <div className="absolute inset-0 bg-gradient-to-r from-black/80 via-transparent to-transparent z-10" />
          
          <div className="relative z-20 flex flex-col md:flex-row md:items-end md:justify-between w-full gap-6">
            <div className="max-w-xl text-left">
              <div className="flex flex-wrap items-center gap-2 md:gap-3 mb-2 md:mb-4">
                <span className="badge-live text-[9px] md:text-[10px] px-2 py-1">
                  <span className="live-dot" /> LIVE NOW
                </span>
                <span className="text-[10px] md:text-xs font-black text-white/80 bg-white/10 px-2 py-1 rounded">ULTRA HD</span>
                <span className="text-[10px] md:text-xs font-black text-green-400 bg-green-500/20 px-2 py-1 rounded">₹0 COST</span>
              </div>
              
              <h2 className="text-2xl sm:text-5xl md:text-6xl font-black text-white tracking-tighter leading-none mb-2 md:mb-4">
                zestyysports
              </h2>
              
              <p className="text-neutral-300 text-xs md:text-base font-bold">
                Worldwide channels in HD, Ad-free, 4K — for free. Access {allChannels.length > 0 ? allChannels.length : 'premium'} channels instantly.
              </p>
            </div>

            <div className="shrink-0 self-start md:self-end flex flex-col sm:flex-row gap-3">
              <a 
                href="https://t.me/+0sACDI0bSDI2Njg9" 
                target="_blank" 
                rel="noreferrer" 
                className="inline-flex items-center justify-center gap-2 px-5 py-2.5 sm:px-6 sm:py-3 bg-gradient-to-r from-blue-500 to-sky-400 hover:from-blue-400 hover:to-sky-300 text-neutral-950 font-black text-[10px] sm:text-[11px] md:text-sm uppercase tracking-widest rounded-lg transition-all shadow-[0_0_20px_rgba(59,130,246,0.3)] hover:shadow-[0_0_30px_rgba(59,130,246,0.5)] hover:-translate-y-0.5"
              >
                <Send className="w-3.5 h-3.5 sm:w-4 sm:h-4 md:w-5 md:h-5 -mt-0.5" />
                Telegram
              </a>
              <a 
                href="https://instagram.com/nikkk.exe" 
                target="_blank" 
                rel="noreferrer" 
                className="inline-flex items-center justify-center gap-2 px-5 py-2.5 sm:px-6 sm:py-3 bg-gradient-to-r from-emerald-400 to-cyan-500 hover:from-emerald-300 hover:to-cyan-400 text-neutral-950 font-black text-[10px] sm:text-[11px] md:text-sm uppercase tracking-widest rounded-lg transition-all shadow-[0_0_20px_rgba(52,211,153,0.3)] hover:shadow-[0_0_30px_rgba(52,211,153,0.5)] hover:-translate-y-0.5"
              >
                <ExternalLink className="w-3.5 h-3.5 sm:w-4 sm:h-4 md:w-5 md:h-5" />
                @nikkk.exe
              </a>
            </div>
          </div>
        </section>

        <div className="flex flex-col gap-10 mt-2">
          {/* Search */}
          <section>
            <div className={`relative max-w-2xl mx-auto mb-6 ${activeTab === 'search' ? 'block' : 'hidden'}`}>
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-neutral-500" />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search channels..."
                className="w-full bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-white/10 rounded-xl py-3 sm:py-4 pl-12 pr-5 text-xs sm:text-sm text-neutral-900 dark:text-white placeholder-neutral-400 dark:placeholder-neutral-500 focus:outline-none focus:border-red-500 transition-all focus:ring-2 focus:ring-red-500/10 shadow-sm"
              />
            </div>

            {/* Group chips */}
            <div className={`gap-2 overflow-x-auto pb-2 no-scrollbar -mx-1 px-1 sm:flex ${activeTab === 'home' ? 'flex' : 'hidden'}`}>
              <button
                onClick={() => setActiveTab('favorites')}
                className={`hidden sm:block shrink-0 px-4 py-1.5 rounded-full text-[10px] font-black uppercase tracking-wider border transition-all ${
                  (activeTab === 'favorites')
                    ? "bg-red-600 text-white border-transparent shadow-lg shadow-red-600/25"
                    : "bg-white dark:bg-neutral-900/60 border-neutral-200 dark:border-white/10 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white hover:border-neutral-300 dark:hover:border-white/20 shadow-sm"
                }`}
              >
                FAVORITES
              </button>
              {groups.map((g) => (
                <button
                  key={g}
                  onClick={() => {
                    setActiveTab('home');
                    setActiveGroup(g);
                  }}
                  className={`shrink-0 px-4 py-1.5 rounded-full text-[10px] font-black uppercase tracking-wider border transition-all ${
                    (activeTab === 'home' && activeGroup === g)
                      ? "bg-red-600 text-white border-transparent shadow-lg shadow-red-600/25"
                      : "bg-white dark:bg-neutral-900/60 border-neutral-200 dark:border-white/10 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white hover:border-neutral-300 dark:hover:border-white/20 shadow-sm"
                  }`}
                >
                  {g}
                </button>
              ))}
            </div>
          </section>

          {/* Content */}
          {loading ? (
            <div className="flex flex-col items-center justify-center py-28 gap-4">
               <Loader2 className="w-10 h-10 text-red-600 animate-spin" />
            </div>
          ) : loadError ? (
            <div className="text-center py-20 text-red-500 font-bold">
              {loadError}
            </div>
          ) : (
            <section>
              {isSearching && (
                <div className="flex items-center gap-4 mb-5">
                  <h4 className="text-base sm:text-xl font-extrabold tracking-tighter text-red-500 uppercase italic">
                    Search Results
                  </h4>
                  <div className="h-px flex-1 bg-red-500/10" />
                  <span className="text-[9px] font-bold text-neutral-600 tracking-widest">
                    {filtered.length} FOUND
                  </span>
                </div>
              )}
              {filtered.length === 0 ? (
                <div className="py-16 text-center border-4 border-dashed border-neutral-300 dark:border-white/5 rounded-[32px]">
                  <p className="text-neutral-500 dark:text-neutral-600 font-black uppercase tracking-widest text-xs">
                    No matching channels.
                  </p>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3 sm:gap-4">
                    {displayedChannels.map((c) => (
                      <ChannelMiniCard
                        key={c.id}
                        channel={c}
                        onPlay={handlePlay}
                        isFavorite={favorites.includes(c.id)}
                        onToggleFavorite={toggleFavorite}
                      />
                    ))}
                  </div>
                  
                  {filtered.length > displayLimit && (
                    <div className="mt-12 flex justify-center">
                       <button 
                        onClick={handleLoadMore}
                        className="px-10 py-3 bg-neutral-200 dark:bg-white/5 border border-neutral-300 dark:border-white/10 rounded-full text-[11px] font-black uppercase tracking-[0.2em] hover:bg-neutral-300 dark:hover:bg-white/10 transition-colors text-neutral-700 dark:text-white"
                       >
                          Load More Channels
                       </button>
                    </div>
                  )}
                </>
              )}
            </section>
          )}
        </div>
      </main>

      {/* Mobile Bottom Navigation Bar */}
      <div className="fixed bottom-0 left-0 right-0 sm:hidden z-50 bg-white/80 dark:bg-neutral-950/90 backdrop-blur-xl border-t border-neutral-200 dark:border-white/10 flex justify-around items-center pb-safe pt-2 pb-4 px-2">
        {/* Connection Status Indicator */}
        <div className="absolute -top-1.5 left-1/2 -translate-x-1/2 w-8 h-1 flex justify-center">
           <div className={`w-4 h-1 rounded-full shadow-md transition-colors duration-500 animate-pulse ${loading ? 'bg-yellow-400 shadow-yellow-400/50' : 'bg-green-500 shadow-green-500/50'}`}></div>
        </div>
        
        <button 
          onClick={() => setActiveTab('home')}
          className={`flex flex-col items-center justify-center w-full py-2 gap-1 transition-colors ${activeTab === 'home' ? 'text-red-500' : 'text-neutral-500 hover:text-neutral-700 dark:hover:text-neutral-300'}`}
        >
          <Home className="w-5 h-5" />
          <span className="text-[10px] font-bold uppercase tracking-wider">Home</span>
        </button>
        <button 
          onClick={() => setActiveTab('favorites')}
          className={`flex flex-col items-center justify-center w-full py-2 gap-1 transition-colors ${activeTab === 'favorites' ? 'text-red-500' : 'text-neutral-500 hover:text-neutral-300'}`}
        >
          <Heart className={`w-5 h-5 ${activeTab === 'favorites' ? 'fill-red-500' : ''}`} />
          <span className="text-[10px] font-bold uppercase tracking-wider">Favorites</span>
        </button>
        <button 
          onClick={() => setActiveTab('search')}
          className={`flex flex-col items-center justify-center w-full py-2 gap-1 transition-colors ${activeTab === 'search' ? 'text-red-500' : 'text-neutral-500 hover:text-neutral-300'}`}
        >
          <Search className="w-5 h-5" />
          <span className="text-[10px] font-bold uppercase tracking-wider">Search</span>
        </button>
      </div>

      <Footer />
    </div>
  );
});

/* ─── Player View ─────────────────────────────── */
const ChannelPlayerView = ({ channels, allChannels, loading, playlistId }: { channels: M3UItem[], allChannels: M3UItem[], loading: boolean, playlistId: string }) => {
  const { channelId } = useParams();
  const navigate = useNavigate();
  const [showAdOverlay, setShowAdOverlay] = useState(true);

  const playing = useMemo(() => {
    if (!channelId) return undefined;
    const idx = parseInt(channelId, 10);
    if (!isNaN(idx) && channels[idx]) return channels[idx];
    return allChannels.find(c => c.id === decodeURIComponent(channelId)); // fallback
  }, [channels, allChannels, channelId]);

  const scrollableChannels = useMemo(() => {
    if (!playing) return [];
    let list = channels.length > 0 ? channels : allChannels;
    if (list.length === 0) return [];
    
    // We can just return the entire list for scrolling
    return list;
  }, [playing, channels, allChannels]);

  const handlePlaybackFailed = React.useCallback(() => {
    if (channels.length > 0 && playing) {
      const idx = channels.findIndex(c => c.id === playing.id);
      if(idx !== -1) {
          const nextIdx = (idx + 1) % channels.length;
          navigate(`/${playlistId}/${nextIdx}`);
      } else {
          // If playing a search result from another playlist, try to find next in allChannels
          const globalIdx = allChannels.findIndex(c => c.id === playing.id);
          if (globalIdx !== -1) {
              const nextIdx = (globalIdx + 1) % allChannels.length;
              navigate(`/${playlistId}/${encodeURIComponent(allChannels[nextIdx].id)}`);
          }
      }
    }
  }, [channels, allChannels, playing, navigate, playlistId]);

  if (loading) {
    return (
      <div className="min-h-screen bg-neutral-100 dark:bg-neutral-950 flex flex-col items-center justify-center p-6 text-center transition-colors duration-300">
         <div className="w-10 h-10 border-4 border-neutral-300 dark:border-white/20 border-t-red-600 dark:border-t-red-600 rounded-full animate-spin mb-4" />
      </div>
    );
  }

  if (!playing) {
    return (
      <div className="min-h-screen bg-neutral-100 dark:bg-black flex flex-col items-center justify-center p-6 gap-6 transition-colors duration-300">
        <p className="text-red-500 font-bold uppercase tracking-widest">Channel not found</p>
        <button onClick={() => navigate(`/${playlistId}`)} className="px-6 py-2 bg-neutral-900 dark:bg-white text-white dark:text-black font-black rounded-lg shadow-xl hover:scale-105 transition-transform">BACK HOME</button>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-neutral-100 dark:bg-neutral-950 text-neutral-900 dark:text-white font-sans flex flex-col relative transition-colors duration-300">
       {showAdOverlay && (
         <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 dark:bg-black/60 backdrop-blur-sm animate-in fade-in duration-300">
           <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-white/10 p-6 sm:p-8 rounded-2xl max-w-sm w-full shadow-2xl flex flex-col items-center text-center animate-in zoom-in-95 duration-500">
             <div className="w-16 h-16 bg-blue-500/10 rounded-full flex items-center justify-center mb-4">
               <Send className="w-8 h-8 text-blue-500 -mt-1" />
             </div>
             <h3 className="text-xl sm:text-2xl font-black uppercase tracking-tight mb-2 text-neutral-900 dark:text-white">Join Telegram</h3>
             <p className="text-neutral-600 dark:text-neutral-400 text-xs sm:text-sm mb-6">
               Join our official Telegram group for updates, support, and to request channels. Also, visit ZestyyFlix for movies!
             </p>
             <div className="w-full flex flex-col gap-3">
               <a 
                 href="https://t.me/+0sACDI0bSDI2Njg9" 
                 target="_blank" 
                 rel="noreferrer"
                 className="w-full py-3 bg-gradient-to-r from-blue-500 to-sky-400 hover:from-blue-400 hover:to-sky-300 text-neutral-950 font-black text-xs uppercase tracking-widest rounded-lg transition-all shadow-[0_0_20px_rgba(59,130,246,0.3)] flex items-center justify-center gap-2"
                 onClick={() => setShowAdOverlay(false)}
               >
                 <Send className="w-4 h-4 -mt-0.5" />
                 OPEN TELEGRAM
               </a>
               <a 
                 href="https://zestyyflix.vercel.app" 
                 target="_blank" 
                 rel="noreferrer"
                 className="w-full py-3 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 text-neutral-900 dark:text-white border border-neutral-300 dark:border-white/5 font-black text-xs uppercase tracking-widest rounded-lg transition-colors flex items-center justify-center gap-2"
                 onClick={() => setShowAdOverlay(false)}
               >
                 <Play className="w-4 h-4" />
                 VISIT ZESTYYFLIX
               </a>
               <button 
                 onClick={() => setShowAdOverlay(false)}
                 className="mt-2 text-[10px] sm:text-xs text-neutral-500 hover:text-neutral-900 dark:hover:text-white uppercase tracking-widest font-bold transition-colors"
               >
                 Close & Watch Stream
               </button>
             </div>
           </div>
         </div>
       )}
       <main className="flex-1 flex flex-col p-4 sm:p-8 max-w-7xl mx-auto w-full gap-6">
          <div className="flex flex-col gap-4">
             <div className="flex items-center gap-4 mb-2">
                <button 
                  onClick={() => navigate(-1)}
                  className="p-2 rounded-lg bg-neutral-200 dark:bg-white/5 border border-neutral-300 dark:border-white/5 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white transition-colors"
                >
                   <ChevronLeft className="w-5 h-5" />
                </button>
                <div className="h-4 w-px bg-neutral-300 dark:bg-white/10" />
                <span className="text-[10px] font-black tracking-widest text-neutral-500 uppercase">Return to Dashboard</span>
             </div>

             <div className="w-full flex flex-col shadow-2xl">
               <VideoPlayer 
                 url={playing.url} 
                 title={playing.name} 
                 onPlaybackFailed={handlePlaybackFailed} 
                 onBack={() => {
                   if (document.fullscreenElement || (document as any).webkitFullscreenElement) {
                     if (document.exitFullscreen) document.exitFullscreen();
                     else if ((document as any).webkitExitFullscreen) (document as any).webkitExitFullscreen();
                   } else {
                     navigate(-1);
                   }
                 }}
               />
               <PlayerFooter />
             </div>

             <div className="mt-4 flex justify-center">
                <div className="bg-white dark:bg-neutral-900/60 border border-amber-500/20 rounded-full px-4 py-2 flex items-center gap-2 shadow-sm">
                   <Info className="w-3.5 h-3.5 text-amber-500" />
                   <span className="text-[10px] sm:text-xs text-neutral-700 dark:text-neutral-300 font-medium">
                     If streams are not working please visit{' '}
                     <a href="https://nikkitv.vercel.app" target="_blank" rel="noreferrer" className="text-amber-500 dark:text-amber-400 font-bold hover:underline">
                       nikkitv.vercel.app
                     </a>
                   </span>
                </div>
             </div>

             <div className="flex flex-col gap-4 mt-2">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
                   <div className="text-left">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="badge-live">
                          <span className="live-dot" /> LIVE
                        </span>
                        <span className="text-[9px] font-black text-neutral-500 uppercase tracking-widest">
                          {playing.group} · ULTRA HD
                        </span>
                      </div>
                      <h1 className="text-2xl sm:text-4xl font-black uppercase leading-[0.85] tracking-tighter flex items-center gap-3">
                        {playing.name}
                        <span className="shrink-0 bg-blue-500/10 dark:bg-blue-500/20 text-blue-600 dark:text-blue-400 text-xs sm:text-sm px-2 py-0.5 rounded uppercase tracking-widest font-bold flex items-center gap-1">
                           <ShieldCheck className="w-3 h-3 sm:w-4 sm:h-4" /> Ad-Free
                        </span>
                      </h1>
                   </div>
                </div>

                {scrollableChannels.length > 0 && (
                   <div className="w-full bg-white dark:bg-neutral-900/40 border border-neutral-200 dark:border-white/5 p-2 rounded-xl flex items-center justify-start overflow-x-auto no-scrollbar gap-2 sm:gap-3 mt-2 scroll-smooth shadow-sm">
                      {scrollableChannels.map((channel, i) => {
                         const isPlaying = channel.id === playing.id;
                         return (
                         <button 
                            key={`${channel.id}-${i}`}
                            ref={(el) => {
                               if (isPlaying && el) {
                                  setTimeout(() => {
                                      el.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
                                  }, 100);
                               }
                            }}
                            onClick={() => {
                                let targetIdx = channels.findIndex(c => c.id === channel.id);
                                if (targetIdx !== -1) {
                                  navigate(`/${playlistId}/${targetIdx}`);
                                } else {
                                  navigate(`/${playlistId}/${encodeURIComponent(channel.id)}`);
                                }
                            }}
                            className={`flex flex-col relative shrink-0 transition-all bg-neutral-100 dark:bg-neutral-950/60 border rounded-lg overflow-hidden group shadow-sm ${isPlaying ? 'w-28 sm:w-40 border-red-500 shadow-lg shadow-red-500/20 ring-1 ring-red-500 ring-offset-2 ring-offset-neutral-100 dark:ring-offset-neutral-950 scale-100 z-10' : 'w-20 sm:w-28 border-neutral-300 dark:border-white/10 opacity-70 dark:opacity-60 hover:opacity-100 scale-95 hover:border-neutral-400 dark:hover:border-white/30'}`}
                         >
                            <div className="w-full aspect-video flex items-center justify-center p-2 relative">
                               {channel.logo ? (
                                  <img src={channel.logo} alt={channel.name} className="max-w-full max-h-full object-contain" onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = 'https://images.unsplash.com/photo-1611162617213-7d7a39e9b1d7?q=80&w=200&auto=format&fit=crop'; }} />
                               ) : (
                                  <span className="text-[7px] sm:text-[9px] font-black text-neutral-500 dark:text-neutral-600 uppercase text-center">{channel.name}</span>
                               )}
                               {isPlaying && <div className="absolute top-1 right-1 flex items-center gap-1"><span className="live-dot" /></div>}
                            </div>
                            <div className="px-2 py-1.5 w-full bg-black/80 flex flex-col justify-center items-center">
                               <span className="text-[8px] sm:text-[10px] font-bold truncate w-full text-center text-white">{channel.name}</span>
                               {isPlaying && <span className="hidden sm:block text-[7px] text-red-500 font-bold uppercase tracking-widest mt-0.5">Playing</span>}
                            </div>
                         </button>
                      )})}
                   </div>
                )}
             </div>
          </div>
       </main>
       <Footer />
    </div>
  );
};

/* ─── Player Footer ─────────────────────────────── */
const PlayerFooter = () => (
  <div className="w-full bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-white/10 px-3 py-2.5 sm:px-4 sm:py-3 flex items-center justify-between gap-3 rounded-b-xl relative z-10 text-[10px] sm:text-xs shadow-sm">
    <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5">
       <a href="https://t.me/+0sACDI0bSDI2Njg9" target="_blank" rel="noreferrer" className="flex items-center gap-1.5 text-[#60a5fa] hover:text-[#3b82f6] transition-colors font-black uppercase tracking-widest text-[9px] sm:text-[11px]">
         <Send className="w-3 h-3 sm:w-3.5 sm:h-3.5 shrink-0 -mt-0.5" />
         <span className="inline sm:hidden">Telegram</span>
         <span className="hidden sm:inline">Join Telegram</span>
       </a>
       <a href="https://instagram.com/nikkk.exe" target="_blank" rel="noreferrer" className="flex items-center gap-1.5 text-[#4ade80] hover:text-[#22c55e] transition-colors font-black uppercase tracking-widest text-[9px] sm:text-[11px]">
         <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" className="shrink-0"><rect width="20" height="20" x="2" y="2" rx="5" ry="5"/><path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/><line x1="17.5" x2="17.51" y1="6.5" y2="6.5"/></svg>
         <span className="inline sm:hidden">@nikkk.exe</span>
         <span className="hidden sm:inline">Follow on Instagram @nikkk.exe</span>
       </a>
       <a href="https://zestyyflix.vercel.app" target="_blank" rel="noreferrer" className="flex items-center gap-1 text-amber-500 hover:text-amber-400 transition-colors font-black uppercase tracking-widest text-[9px] sm:text-[11px]">
         <span className="inline sm:hidden">zestyyflix</span>
         <span className="hidden sm:inline">movies: zestyyflix.vercel.app</span>
         <ExternalLink className="w-2.5 h-2.5 shrink-0" />
       </a>
    </div>
    <span className="hidden md:block text-neutral-500 text-[9px] sm:text-[10px] font-black uppercase tracking-widest leading-none">
      adfree by Nikshep
    </span>
  </div>
);

/* ─── Footer ─────────────────────────────── */
const Footer = () => (
  <footer className="w-full border-t border-neutral-200 dark:border-white/10 py-12 px-6 flex flex-col items-center justify-center gap-6 text-center text-neutral-600 dark:text-neutral-400 bg-neutral-100 dark:bg-neutral-950 transition-colors">
    <img src="/og.png" alt="ZestyySports" className="h-10 md:h-12 object-contain" />
    <p className="text-[11px] md:text-sm font-bold uppercase tracking-widest max-w-md leading-relaxed text-neutral-700 dark:text-neutral-400">
      Worldwide channels in HD, Ad-free, 4K — for free.
    </p>
    <div className="flex flex-wrap items-center justify-center gap-4">
      <a
        href="https://t.me/+0sACDI0bSDI2Njg9"
        target="_blank"
        rel="noreferrer"
        className="flex items-center gap-2 bg-gradient-to-r from-blue-500 to-sky-400 hover:from-blue-400 hover:to-sky-300 text-neutral-950 text-[10px] md:text-xs font-black px-6 py-3 rounded-lg uppercase tracking-widest transition-all shadow-[0_0_20px_rgba(59,130,246,0.3)]"
      >
        <Send className="w-4 h-4 -mt-0.5" />
        TELEGRAM
      </a>
      <a
        href="https://instagram.com/nikkk.exe"
        target="_blank"
        rel="noreferrer"
        className="flex items-center gap-2 bg-gradient-to-r from-emerald-400 to-cyan-500 hover:from-emerald-300 hover:to-cyan-400 text-neutral-950 text-[10px] md:text-xs font-black px-6 py-3 rounded-lg uppercase tracking-widest transition-all shadow-[0_0_20px_rgba(52,211,153,0.3)]"
      >
        <ExternalLink className="w-4 h-4" />
        @nikkk.exe
      </a>
      <a
        href="https://zestyyflix.vercel.app"
        target="_blank"
        rel="noreferrer"
        className="flex items-center gap-2 bg-white dark:bg-neutral-900 border border-neutral-300 dark:border-white/10 hover:bg-neutral-100 dark:hover:bg-neutral-800 text-neutral-900 dark:text-white text-[10px] md:text-xs font-black px-6 py-3 rounded-lg uppercase tracking-widest transition-colors shadow-sm"
      >
        <Play className="w-4 h-4" />
        MORE FROM ZESTYY
      </a>
    </div>
  </footer>
);

const PlaylistContainer = () => {
  const { playlistId } = useParams();
  const pid = playlistId === "usa" ? "usa" : "india";
  const [channels, setChannels] = useState<M3UItem[]>([]);
  const [allChannels, setAllChannels] = useState<M3UItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    const fetchAllChannels = async (initialLoad = false) => {
      if (initialLoad) setLoading(true);
      try {
        const MAX_TOTAL_CHANNELS = 20000;
        
        // Fetch all sources concurrently using Promise.allSettled
        const fetchSource = async (source: any) => {
          try {
            const response = await fetch(`/api/m3u?url=${encodeURIComponent(source.url)}`);
            if (!response.ok) throw new Error(`HTTP error ${response.status}`);
            
            let items: M3UItem[] = [];
            if (source.url.endsWith('.json')) {
              const data = await response.json();
              items = Array.isArray(data) ? data.map((item: any, index: number) => ({
                id: `${source.id}-json-${index}-${item.id || 'no-id'}`,
                name: item.name || item.title || 'Unknown',
                url: item.url || item.link || '',
                logo: item.logo || item.image || item.thumb || '',
                group: item.group || item.category || source.name,
                sourceId: source.id
              })) : [];
            } else {
              const text = await response.text();
              let parsed = parseM3U(text);
              if (source.id === 'sonur') {
                parsed = parsed.filter(channel => 
                  channel.name.toLowerCase().includes('star sports')
                );
              }
              if (source.id === 'premium') {
                parsed = parsed.filter(channel => {
                  const nameLower = channel.name.toLowerCase();
                  const groupLower = (channel.group || '').toLowerCase();
                  return nameLower.includes('sports') || 
                         nameLower.includes('cricket') || 
                         groupLower.includes('sports') || 
                         groupLower.includes('cricket');
                });
              }
              items = parsed.map((c, index) => ({
                ...c,
                id: `${source.id}-${c.id}-${index}`,
                group: c.group && c.group !== 'All' ? c.group : source.name,
                sourceId: source.id // Track origin
              }));
            }

            // High efficiency filter inside the concurrent thread
            return items.filter(c => {
               if (!c.url) return false;
               const g = (c.group || "").toLowerCase();
               return g !== 'bangla' && g !== 'bangladeshi' && g !== 'bd channels' && g !== 'xxx' && g !== 'adult';
            });
          } catch (e) {
            console.warn(`Source ${source.name} failed:`, e);
            return [];
          }
        };

        const usaPromises = SOURCES_USA.map(fetchSource);
        const indiaPromises = SOURCES_INDIA.map(fetchSource);
        
        const [usaResults, indiaResults] = await Promise.all([
          Promise.allSettled(usaPromises),
          Promise.allSettled(indiaPromises)
        ]);
        
        if (!active) return;

        const processResults = (results: PromiseSettledResult<M3UItem[]>[]) => {
          const finalChannels: M3UItem[] = [];
          let totalCount = 0;
          for (const res of results) {
            if (res.status === 'fulfilled' && res.value.length > 0) {
              const items = res.value;
              const remainingSpace = MAX_TOTAL_CHANNELS - totalCount;
              if (remainingSpace <= 0) break;
              const slice = items.slice(0, remainingSpace);
              finalChannels.push(...slice);
              totalCount += slice.length;
            }
          }
          return finalChannels;
        };

        const usaChannels = processResults(usaResults);
        const indiaChannels = processResults(indiaResults);

        if (pid === "usa") {
           setChannels(usaChannels);
        } else {
           setChannels(indiaChannels);
        }
        
        setAllChannels([...indiaChannels, ...usaChannels]);

      } catch (error) {
        console.error('Failed to load channels:', error);
        if (active) setLoadError('Failed to load channels');
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchAllChannels(true);
    const interval = setInterval(() => {
      fetchAllChannels(false);
    }, 60000);

    return () => {
      active = false;
      clearInterval(interval);
    };
  }, [pid]);

  return (
    <Routes>
      <Route path="/" element={<MainView mainChannels={channels} allChannels={allChannels} loading={loading} loadError={loadError} playlistId={pid} />} />
      <Route path=":channelId" element={<ChannelPlayerView channels={channels} allChannels={allChannels} loading={loading} playlistId={pid} />} />
    </Routes>
  );
}

export default function App() {
  const [showPopup, setShowPopup] = useState(true);
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    return (localStorage.getItem('zesty_theme') as 'dark' | 'light') || 'dark';
  });

  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => {
      const next = prev === 'dark' ? 'light' : 'dark';
      localStorage.setItem('zesty_theme', next);
      return next;
    });
  };

  return (
    <ErrorBoundary>
      <Router>
        {/* Global Theme Toggler */}
        <div className="fixed top-2 sm:top-4 right-2 sm:right-4 z-[110]">
          <button 
            onClick={toggleTheme}
            className="p-2 sm:px-4 sm:py-2 bg-neutral-900/60 dark:bg-white/10 backdrop-blur-md border border-neutral-700 dark:border-white/10 rounded-full text-[10px] font-black uppercase tracking-wider text-neutral-300 hover:text-white transition-all shadow-lg flex items-center gap-2"
          >
            {theme === 'dark' ? 'Light Mode' : 'Dark Mode'}
          </button>
        </div>
        
        {showPopup && (

          <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] animate-in fade-in slide-in-from-top-5 duration-500">
            <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-white/10 shadow-2xl rounded-full px-4 py-2 sm:px-6 sm:py-3 flex items-center gap-3 max-w-[90vw] sm:max-w-md">
              <div className="bg-amber-500/10 p-1.5 rounded-full shrink-0">
                <Info className="w-4 h-4 sm:w-5 sm:h-5 text-amber-500" />
              </div>
              <p className="text-[10px] sm:text-xs font-bold text-neutral-800 dark:text-neutral-200">
                If streams are not working please visit{' '}
                <a href="https://nikkitv.vercel.app" target="_blank" rel="noreferrer" className="text-amber-500 dark:text-amber-400 hover:text-amber-600 dark:hover:text-amber-300 underline underline-offset-2">
                  nikkitv.vercel.app
                </a>
              </p>
              <button 
                onClick={() => setShowPopup(false)}
                className="shrink-0 p-1 hover:bg-neutral-200 dark:hover:bg-white/10 rounded-full transition-colors focus:outline-none"
              >
                <X className="w-3.5 h-3.5 sm:w-4 sm:h-4 text-neutral-500 dark:text-neutral-400" />
              </button>
            </div>
          </div>
        )}
        <Routes>
          <Route path="/" element={<Navigate to="/india" replace />} />
          <Route path="/:playlistId/*" element={<PlaylistContainer />} />
        </Routes>
        <style>{`
          .shimmer-text {
            background: linear-gradient(90deg, #333 0%, #fff 50%, #333 100%);
            background-size: 200% 100%;
            -webkit-background-clip: text;
            background-clip: text;
            color: transparent;
            animation: shimmer 1.5s infinite;
          }
          @keyframes shimmer {
            0% { background-position: 100% 0; }
            100% { background-position: -100% 0; }
          }
          .no-scrollbar::-webkit-scrollbar { display: none; }
          .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }
          
          .badge-live {
            background: rgba(220, 38, 38, 0.1);
            color: #ef4444;
            padding: 2px 6px;
            border-radius: 4px;
            font-weight: 800;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            border: 1px solid rgba(220, 38, 38, 0.2);
          }
          .live-dot {
            width: 4px;
            height: 4px;
            background: #ef4444;
            border-radius: 50%;
            animation: pulse-dot 1s infinite alternate;
          }
          @keyframes pulse-dot {
            from { opacity: 0.4; transform: scale(0.8); }
            to { opacity: 1; transform: scale(1.2); }
          }
        `}</style>
      </Router>
    </ErrorBoundary>
  );
}
