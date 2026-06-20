import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import mpegts from 'mpegts.js';
import { AlertCircle, Play, Pause, Volume2, VolumeX, Maximize, Minimize, Settings, PictureInPicture, PictureInPicture2, Activity, Clock, QrCode, CheckCircle2, Send, Lock, MessageCircle, ChevronLeft } from 'lucide-react';

interface VideoPlayerProps {
  url: string;
  title: string;
  poster?: string;
  onPlaybackFailed?: () => void;
  onBack?: () => void;
}

type StreamKind = 'hls' | 'mpegts' | 'native';

function detectKind(url: string): StreamKind {
  const lowercaseUrl = url.toLowerCase();
  if (lowercaseUrl.includes('.m3u8')) return 'hls';
  return 'mpegts'; // Default to mpegts to treat all streams commonly.
}

export const VideoPlayer: React.FC<VideoPlayerProps> = ({ url, title, poster, onPlaybackFailed, onBack }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [buffering, setBuffering] = useState(false);
  const [bufferCountdown, setBufferCountdown] = useState(15);
  const [isBuildingBuffer, setIsBuildingBuffer] = useState(true);
  
  // Custom Controls State
  const [isPlaying, setIsPlaying] = useState(false);
  const [isMuted, setIsMuted] = useState(true);
  const [volume, setVolume] = useState(0);
  const [showControls, setShowControls] = useState(true);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [levels, setLevels] = useState<{ height: number; bitrate: number; name?: string }[]>([]);
  const [currentLevel, setCurrentLevel] = useState<number>(-1); // -1 = Auto
  const [showQualityMenu, setShowQualityMenu] = useState(false);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  
  // Stats State
  const [showStats, setShowStats] = useState(false);
  const [stats, setStats] = useState({ resolution: 'Unknown', buffer: '0.0s', bitrate: 'Unknown', codec: 'Unknown', currentData: '0 MB', totalData: '0 MB' });
  
  const [currentDataConsumed, setCurrentDataConsumed] = useState(0);
  const [totalDataConsumed, setTotalDataConsumed] = useState(() => {
    const saved = localStorage.getItem('zesty_total_data_consumed');
    return saved ? parseInt(saved, 10) : 0;
  });

  useEffect(() => {
    const timeout = setTimeout(() => {
      localStorage.setItem('zesty_total_data_consumed', totalDataConsumed.toString());
    }, 2000);
    return () => clearTimeout(timeout);
  }, [totalDataConsumed]);

  // Preview State
  const [previewSeconds, setPreviewSeconds] = useState(120);
  const [isUnlocked, setIsUnlocked] = useState(() => {
    return localStorage.getItem('zesty_unlocked') === 'true';
  });
  const [unlockSuccess, setUnlockSuccess] = useState(false);
  const [unlockCode, setUnlockCode] = useState('');

  const hlsRef = useRef<Hls | null>(null);
  const playerRef = useRef<mpegts.Player | null>(null);

  useEffect(() => {
    const initCast = (isAvailable: boolean) => {
      if (isAvailable && (window as any).cast && (window as any).chrome) {
        try {
          (window as any).cast.framework.CastContext.getInstance().setOptions({
            receiverApplicationId: (window as any).chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID,
            autoJoinPolicy: (window as any).chrome.cast.AutoJoinPolicy.ORIGIN_SCOPED
          });

          const castContext = (window as any).cast.framework.CastContext.getInstance();
          castContext.addEventListener(
            (window as any).cast.framework.CastContextEventType.SESSION_STATE_CHANGED,
            (event: any) => {
              if (event.sessionState === (window as any).cast.framework.SessionState.SESSION_STARTED) {
                const castSession = castContext.getCurrentSession();
                if (castSession) {
                  const mediaInfo = new (window as any).chrome.cast.media.MediaInfo(url, 'application/x-mpegURL');
                  mediaInfo.metadata = new (window as any).chrome.cast.media.GenericMediaMetadata();
                  mediaInfo.metadata.title = title;
                  const request = new (window as any).chrome.cast.media.LoadRequest(mediaInfo);
                  castSession.loadMedia(request).catch(console.error);
                }
              }
            }
          );
        } catch (e) {
          console.error("Cast init error", e);
        }
      }
    };

    if ((window as any).cast && (window as any).cast.framework) {
       initCast(true);
    } else {
       (window as any).__onGCastApiAvailable = initCast;
    }
  }, [url, title]);

  useEffect(() => {
    if (!isBuildingBuffer) return;
    
    if (bufferCountdown > 0) {
      const timer = setTimeout(() => {
        setBufferCountdown(prev => prev - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else {
      setIsBuildingBuffer(false);
    }
  }, [bufferCountdown, isBuildingBuffer]);

  useEffect(() => {
    let lastDataConsumedSnapshot = 0;
    const interval = setInterval(() => {
      if (!videoRef.current) return;
      const v = videoRef.current;

      const resolution = v.videoWidth && v.videoHeight ? `${v.videoWidth}x${v.videoHeight}` : 'Unknown';

      let bufferLen = 0;
      if (v.buffered.length > 0) {
        for (let i = 0; i < v.buffered.length; i++) {
          if (v.currentTime >= v.buffered.start(i) && v.currentTime <= v.buffered.end(i)) {
            bufferLen = v.buffered.end(i) - v.currentTime;
            break;
          }
        }
      }

      let bitrate = 'Unknown';
      let codec = 'Unknown';
      if (hlsRef.current) {
        const level = hlsRef.current.levels[hlsRef.current.currentLevel === -1 ? hlsRef.current.loadLevel : hlsRef.current.currentLevel];
        if (level && level.bitrate) {
          bitrate = `${(level.bitrate / 1000).toFixed(0)} kbps`;
        } else if (hlsRef.current.bandwidthEstimate) {
          bitrate = `${(hlsRef.current.bandwidthEstimate / 1000).toFixed(0)} kbps`;
        }
        if (level && level.videoCodec) codec = level.videoCodec;
      }

      let addData = 0;
      if (playerRef.current && !v.paused) {
        const info = (playerRef.current as any).statisticsInfo;
        if (info && info.speed) {
          addData = info.speed * 1024;
          if (bitrate === 'Unknown') bitrate = `${(info.speed * 8).toFixed(0)} kbps`;
        }
      }

      setCurrentDataConsumed(prevCurrent => {
        const nextCurrent = prevCurrent + addData;
        
        setTotalDataConsumed(prevTotal => {
          const nextTotal = prevTotal + addData;
          
          let formatConsumption = (bytes: number) => {
            if (bytes > 1024 * 1024 * 1024) {
              return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
            }
            return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
          };

          if (bitrate === 'Unknown' || bitrate === '0 kbps') {
            const bytesSinceLast = nextCurrent - lastDataConsumedSnapshot;
            if (bytesSinceLast > 0 && !v.paused) {
              const kbps = (bytesSinceLast * 8) / 1000;
              bitrate = `${kbps.toFixed(0)} kbps`;
            }
          }
          lastDataConsumedSnapshot = nextCurrent;

          setStats(prevStats => ({
            ...prevStats,
            resolution,
            buffer: `${bufferLen.toFixed(1)}s`,
            bitrate,
            codec,
            currentData: formatConsumption(nextCurrent),
            totalData: formatConsumption(nextTotal)
          }));
          
          return nextTotal;
        });

        return nextCurrent;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (isUnlocked || error) return;

    if (previewSeconds > 0) {
      const timer = setTimeout(() => {
        setPreviewSeconds(prev => prev - 1);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [previewSeconds, isUnlocked, error]);

  useEffect(() => {
    if (previewSeconds === 0 && !isUnlocked && videoRef.current && !videoRef.current.paused) {
      videoRef.current.pause();
      setIsPlaying(false);
    }
  }, [previewSeconds, isUnlocked, isPlaying]);

  const handleUnlock = () => {
    if (unlockCode.toLowerCase().trim() === 'nikkiboss') {
      setUnlockSuccess(true);
      setTimeout(() => {
        setIsUnlocked(true);
        localStorage.setItem('zesty_unlocked', 'true');
        setUnlockSuccess(false);
        videoRef.current?.play().catch(()=>{});
        setIsPlaying(true);
      }, 2000);
    } else {
      alert('Invalid code. Contact @SPEEDNIKK on Telegram.');
    }
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (document.activeElement?.tagName === 'INPUT') return;
      if (previewSeconds === 0 && !isUnlocked) return; // respect preview lock
      
      const v = videoRef.current;
      if (!v) return;

      switch (e.key.toLowerCase()) {
        case ' ':
          e.preventDefault();
          if (v.paused) v.play().catch(()=>{}); else v.pause();
          break;
        case 'f':
          e.preventDefault();
          if (document.fullscreenElement) {
            document.exitFullscreen().catch(console.error);
          } else {
            containerRef.current?.requestFullscreen().catch(console.error);
          }
          break;
        case 'm':
          e.preventDefault();
          v.muted = !v.muted;
          setIsMuted(v.muted);
          if (v.volume === 0 && !v.muted) {
             v.volume = 1;
             setVolume(1);
          }
          break;
        case 'arrowright':
          e.preventDefault();
          v.currentTime += 10;
          break;
        case 'arrowleft':
          e.preventDefault();
          v.currentTime -= 10;
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [previewSeconds, isUnlocked]);

  const changeQuality = (levelIndex: number) => {
    if (hlsRef.current) {
      hlsRef.current.currentLevel = levelIndex;
      setCurrentLevel(levelIndex);
    }
    setShowQualityMenu(false);
  };

  const handleMouseMove = () => {
    setShowControls(true);
    if (controlsTimeoutRef.current) clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = setTimeout(() => setShowControls(false), 3000);
  };

  const handleMouseLeave = () => {
    setShowControls(false);
  };

  const togglePlay = () => {
    if (previewSeconds === 0 && !isUnlocked) return;
    if (videoRef.current) {
      if (videoRef.current.paused) {
        videoRef.current.play().catch(() => {});
      } else {
        videoRef.current.pause();
      }
    }
  };

  const togglePiP = async () => {
    if (!videoRef.current) return;
    try {
      if (document.pictureInPictureElement) {
        await document.exitPictureInPicture();
      } else if (document.pictureInPictureEnabled) {
        await videoRef.current.requestPictureInPicture();
      }
    } catch (err) {
      console.warn("PiP failed: ", err);
    }
  };

  const toggleMute = () => {
    if (videoRef.current) {
      const newMuted = !videoRef.current.muted;
      videoRef.current.muted = newMuted;
      setIsMuted(newMuted);
      if (newMuted) {
        setVolume(0);
      } else {
        setVolume(videoRef.current.volume || 1);
      }
    }
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const vol = parseFloat(e.target.value);
    setVolume(vol);
    if (videoRef.current) {
      videoRef.current.volume = vol;
      const willMute = vol === 0;
      videoRef.current.muted = willMute;
      setIsMuted(willMute);
    }
  };

  const toggleFullscreen = async () => {
    try {
      if (!document.fullscreenElement && !(document as any).webkitFullscreenElement) {
        if (containerRef.current?.requestFullscreen) {
          await containerRef.current.requestFullscreen();
        } else if ((containerRef.current as any)?.webkitRequestFullscreen) {
          await (containerRef.current as any).webkitRequestFullscreen();
        } else {
          // iOS Safari lacks support for true container fullscreen, use CSS fake fullscreen
          setIsFullscreen(true);
        }
        try {
          if (screen.orientation && screen.orientation.unlock) {
            screen.orientation.unlock();
          }
        } catch (e) {}
      } else {
        if (document.exitFullscreen) {
          await document.exitFullscreen();
        } else if ((document as any).webkitExitFullscreen) {
          await (document as any).webkitExitFullscreen();
        } else {
          setIsFullscreen(false);
        }
        try {
          if (screen.orientation && screen.orientation.unlock) {
            screen.orientation.unlock();
          }
        } catch (e) {}
      }
    } catch (err) {
      console.warn("Fullscreen failed: ", err);
      setIsFullscreen(!isFullscreen);
    }
  };

  useEffect(() => {
    const handler = () => setIsFullscreen(!!document.fullscreenElement || !!(document as any).webkitFullscreenElement);
    document.addEventListener("fullscreenchange", handler);
    document.addEventListener("webkitfullscreenchange", handler);
    return () => {
      document.removeEventListener("fullscreenchange", handler);
      document.removeEventListener("webkitfullscreenchange", handler);
    };
  }, []);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    
    const updatePlayState = () => setIsPlaying(!video.paused);
    const updateVolumeState = () => {
      setIsMuted(video.muted);
      setVolume(video.volume);
    };
    
    video.addEventListener('play', updatePlayState);
    video.addEventListener('pause', updatePlayState);
    video.addEventListener('volumechange', updateVolumeState);
    
    return () => {
      video.removeEventListener('play', updatePlayState);
      video.removeEventListener('pause', updatePlayState);
      video.removeEventListener('volumechange', updateVolumeState);
    };
  }, []);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let active = true;
    let currentMpegtsPlayer: mpegts.Player | null = null;
    let currentHls: Hls | null = null;

    // Reset state
    setError(null);
    setLoading(true);
    setBuffering(false);
    setIsBuildingBuffer(true);
    setBufferCountdown(15);
    setLevels([]);
    setCurrentLevel(-1);
    video.muted = true;
    setIsMuted(true);
    setVolume(0);
    const hasUnlocked = localStorage.getItem('zesty_unlocked') === 'true';
    if (hasUnlocked) {
      setIsUnlocked(true);
      setPreviewSeconds(120);
    } else {
      setIsUnlocked(false);
      setPreviewSeconds(120);
    }
    setUnlockCode('');
    setCurrentDataConsumed(0);

    const kind = detectKind(url);
    const proxiedUrl = `/api/proxy?url=${encodeURIComponent(url)}`;

    // Build alternative URLs to try in order of priority:
    // If https: try direct stream first (avoids proxy datacenter IP blocks). If it fails, fallback to proxy.
    // If http: try proxy first (to avoid mixed content block). If it fails, fallback to direct.
    type LoadAttempt = {
      url: string;
      disableAudio?: boolean;
    };
    const attempts: LoadAttempt[] = [];
    if (url.startsWith('https://')) {
      attempts.push({ url });
      attempts.push({ url: proxiedUrl });
      if (kind === 'mpegts') {
        attempts.push({ url, disableAudio: true }); // Fallbacks for unsupported ac-3 audio
        attempts.push({ url: proxiedUrl, disableAudio: true });
      }
    } else {
      attempts.push({ url: proxiedUrl });
      attempts.push({ url });
      if (kind === 'mpegts') {
        attempts.push({ url: proxiedUrl, disableAudio: true });
        attempts.push({ url, disableAudio: true });
      }
    }

    const tryLoad = (attemptIndex: number) => {
      if (!active) return;

      if (attemptIndex >= attempts.length) {
        setError(`Failed to play stream after exhausting connection attempts.`);
        setLoading(false);
        if (onPlaybackFailed) onPlaybackFailed();
        return;
      }

      const activeAttempt = attempts[attemptIndex];
      const activeUrl = activeAttempt.url;
      console.log(`VideoPlayer: Loading stream (${kind}), attempt ${attemptIndex + 1}/${attempts.length} with URL: ${activeUrl} (audio: ${!activeAttempt.disableAudio})`);

      // Clean up previous attempts
      if (currentMpegtsPlayer) {
        try {
          currentMpegtsPlayer.unload();
          currentMpegtsPlayer.detachMediaElement();
          currentMpegtsPlayer.destroy();
        } catch (e) {
          console.warn('Error cleaning up previous mpegts player:', e);
        }
        currentMpegtsPlayer = null;
        playerRef.current = null;
      }
      if (currentHls) {
        try {
          currentHls.destroy();
        } catch (e) {
          console.warn('Error cleaning up previous HLS player:', e);
        }
        currentHls = null;
        hlsRef.current = null;
      }

      if (kind === 'hls') {
        if (Hls.isSupported()) {
          const hls = new Hls({
            enableWorker: false, // Disabling worker ensures iframe sandbox compatibility
            lowLatencyMode: false,
            maxBufferLength: 120, // Increased buffer
            maxMaxBufferLength: 180,
            manifestLoadingMaxRetry: 4,
            levelLoadingMaxRetry: 4,
            fragLoadingMaxRetry: 4,
            liveSyncDurationCount: 15, // Tolerate higher latency behind live edge
            liveMaxLatencyDurationCount: 30,
          });
          currentHls = hls;
          hlsRef.current = hls;

          hls.loadSource(activeUrl);
          hls.attachMedia(video);

          hls.on(Hls.Events.MANIFEST_PARSED, (e, data) => {
            if (!active) return;
            setLevels(data.levels || []);
            setCurrentLevel(hls.currentLevel);
            video.play().catch(() => {});
            setLoading(false);
          });

          hls.on(Hls.Events.LEVEL_SWITCHED, (e, data) => {
            if (!active) return;
            setCurrentLevel(data.level);
          });

          hls.on(Hls.Events.FRAG_LOADED, (e, data) => {
            if (!active) return;
            let loadedBytes = 0;
            if (data && data.frag && data.frag.stats && data.frag.stats.loaded) {
              loadedBytes = data.frag.stats.loaded;
            } else if (data && (data as any).stats && (data as any).stats.total) { // older hls.js versions
              loadedBytes = (data as any).stats.total;
            }
            if (loadedBytes > 0) {
              setCurrentDataConsumed(prev => prev + loadedBytes);
              setTotalDataConsumed(prev => prev + loadedBytes);
            }
          });

          hls.on(Hls.Events.ERROR, (_, data) => {
            if (!active) return;
            if (data.fatal) {
              console.warn(`HLS fatal error: ${data.type} (details: ${data.details}), trying next fallback...`);
              setTimeout(() => tryLoad(attemptIndex + 1), 10);
            }
          });
        } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
          // Native Safari HLS support
          video.src = activeUrl;
          
          const onLoadedMetadata = () => {
            if (!active) return;
            video.play().catch(() => {});
            setLoading(false);
          };

          const onNativeError = () => {
            if (!active) return;
            console.warn(`Native HLS playback error on attempt ${attemptIndex + 1}, trying next fallback...`);
            video.removeEventListener('loadedmetadata', onLoadedMetadata);
            video.removeEventListener('error', onNativeError);
            setTimeout(() => tryLoad(attemptIndex + 1), 10);
          };

          video.addEventListener('loadedmetadata', onLoadedMetadata);
          video.addEventListener('error', onNativeError);
        } else {
          setError('HLS is not supported in this browser.');
          setLoading(false);
          if (onPlaybackFailed) onPlaybackFailed();
        }
      } else if (kind === 'mpegts') {
        if (mpegts.getFeatureList().mseLivePlayback) {
          try {
            const player = mpegts.createPlayer({
              type: url.toLowerCase().includes('.flv') ? 'flv' : 'mpegts',
              isLive: true,
              url: activeUrl,
              hasAudio: !activeAttempt.disableAudio,
            }, {
              enableWorker: false, // Disabling worker is essential inside sandbox iframes
              lazyLoad: false,
              liveBufferLatencyChasing: false,
              stashInitialSize: 384,
            });
            currentMpegtsPlayer = player;
            playerRef.current = player;
            player.attachMediaElement(video);
            player.load();
            
            const playPromise = player.play() as any;
            if (playPromise && playPromise.catch) {
              playPromise.catch(() => {});
            }
            setLoading(false);
            
            player.on(mpegts.Events.ERROR, (type, detail) => {
              if (!active) return;
              console.warn(`MPEG-TS Player Error (type=${type}, detail=${detail}) on attempt ${attemptIndex + 1}. Trying next fallback...`);
              setTimeout(() => tryLoad(attemptIndex + 1), 10);
            });
          } catch (err: any) {
            console.error('Failed to create MPEG-TS player:', err);
            tryLoad(attemptIndex + 1);
          }
        } else {
          setError('MPEG-TS/FLV playback is not supported in this browser.');
          setLoading(false);
          if (onPlaybackFailed) onPlaybackFailed();
        }
      } else {
        // Native
        video.src = activeUrl;

        const onLoaded = () => {
          if (!active) return;
          video.play().catch(() => {});
          setLoading(false);
        };

        const onNativeError = () => {
          if (!active) return;
          console.warn(`Native playback error on attempt ${attemptIndex + 1}. Trying next fallback...`);
          video.removeEventListener('loadedmetadata', onLoaded);
          video.removeEventListener('error', onNativeError);
          setTimeout(() => tryLoad(attemptIndex + 1), 10);
        };

        video.addEventListener('loadedmetadata', onLoaded);
        video.addEventListener('error', onNativeError);
      }
    };

    // Begin Loading
    tryLoad(0);

    const handleWaiting = () => setBuffering(true);
    const handlePlaying = () => setBuffering(false);

    video.addEventListener('waiting', handleWaiting);
    video.addEventListener('playing', handlePlaying);

    return () => {
      active = false;
      video.removeEventListener('waiting', handleWaiting);
      video.removeEventListener('playing', handlePlaying);
      if (currentHls) {
        try {
          currentHls.destroy();
        } catch (e) {}
      }
      if (currentMpegtsPlayer) {
        try {
          currentMpegtsPlayer.unload();
          currentMpegtsPlayer.detachMediaElement();
          currentMpegtsPlayer.destroy();
        } catch (e) {}
      }
      if (video) {
        try {
          video.src = '';
          video.load();
        } catch (e) {}
      }
    };
  }, [url, onPlaybackFailed]);

  return (
    <div 
      ref={containerRef}
      className={`w-full bg-black overflow-hidden group shadow-2xl border border-white/10 ${isFullscreen ? 'fixed inset-0 z-[100] h-[100dvh] w-screen rounded-none' : 'relative aspect-video rounded-t-xl'}`}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      onTouchStart={handleMouseMove}
    >
      {(loading || buffering || isBuildingBuffer) && !error && (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center bg-black/95 backdrop-blur-xl font-sans">
          {isBuildingBuffer && !error ? (
             <div className="flex flex-col items-center gap-6 animate-pulse">
                <img src="/og.png" alt="ZestyySports" className="w-32 md:w-48 h-auto object-contain drop-shadow-2xl" />
                <div className="flex flex-col items-center gap-1">
                   <div className="flex items-center gap-2 text-red-500 mb-1">
                      <Clock className="w-4 h-4" />
                      <span className="text-[10px] sm:text-xs font-black uppercase tracking-widest leading-none font-sans">Your stream is right here</span>
                   </div>
                   <span className="text-2xl sm:text-4xl font-black text-white tracking-widest font-sans">{bufferCountdown}s</span>
                </div>
             </div>
          ) : (
            <div className="w-12 h-12 border-4 border-white/20 border-t-white/90 rounded-full animate-spin shadow-2xl mb-4"></div>
          )}
        </div>
      )}

      {error && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-[#1a1313] z-50 p-6 text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mb-4" />
          <h3 className="text-white font-semibold text-lg mb-2">Playback Error</h3>
          <p className="text-red-400/80 text-sm max-w-md">{error}</p>
          <button 
            onClick={() => window.location.reload()}
            className="mt-6 px-4 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-lg text-sm border border-red-500/30 transition-colors"
          >
            Refresh Player
          </button>
        </div>
      )}
      
      {previewSeconds === 0 && !isUnlocked && !error && (
        <div className="absolute inset-0 z-50 flex flex-col items-center justify-center bg-black/80 backdrop-blur-xl p-4 sm:p-6 text-center animate-in fade-in duration-500 overflow-y-auto">
          <div className="w-full max-w-sm sm:max-w-md bg-neutral-900/80 border border-white/10 rounded-2xl sm:rounded-3xl p-5 sm:p-8 shadow-2xl shadow-black/50 overflow-hidden relative my-auto">
            {/* Background Glow */}
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full h-32 bg-red-600/20 blur-[60px] pointer-events-none" />

            {unlockSuccess ? (
              <div className="flex flex-col items-center justify-center animate-in zoom-in-95 duration-500">
                <div className="w-20 h-20 bg-green-500/20 rounded-full flex items-center justify-center mb-6">
                  <CheckCircle2 className="w-12 h-12 text-green-500" />
                </div>
                <h3 className="text-white font-black text-2xl mb-2 tracking-tight">Code Accepted</h3>
                <p className="text-neutral-300 text-sm mb-2">Welcome to the family.</p>
                <p className="text-green-400 font-medium text-sm animate-pulse">Enjoy uninterrupted ad-free 4K live streams</p>
              </div>
            ) : (
              <div className="animate-in slide-in-from-bottom-4 duration-500 flex flex-col items-center">
                <img src="/og.png" alt="ZestyySports" className="w-24 h-24 sm:w-28 sm:h-28 object-contain mb-4 sm:mb-6 drop-shadow-2xl" />
                <h3 className="text-white font-black text-xl sm:text-3xl mb-2 tracking-tight">Preview Ended</h3>
                <p className="text-neutral-400 text-xs sm:text-sm mb-6 sm:mb-8 text-balance">
                  Your complimentary preview has concluded. Proceed with <span className="text-white font-bold">zestyysports subscription</span> to continue watching.
                </p>
                
                <div className="w-full flex flex-col gap-3 mb-6 sm:mb-8">
                  <div className="relative">
                    <input 
                      type="text" 
                      placeholder="Enter Unlock Code" 
                      value={unlockCode}
                      onChange={(e) => setUnlockCode(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') handleUnlock();
                      }}
                      className="w-full bg-black/60 border border-white/10 rounded-xl px-4 py-3 sm:px-5 sm:py-4 text-white placeholder-white/40 focus:outline-none focus:border-red-500 focus:ring-1 focus:ring-red-500/50 transition-all font-mono text-center text-base sm:text-lg uppercase tracking-widest"
                    />
                  </div>
                  <button 
                    onClick={handleUnlock}
                    className="w-full py-3 sm:py-4 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-500 hover:to-red-600 text-white font-black uppercase tracking-widest rounded-xl transition-all shadow-lg flex items-center justify-center gap-2 group text-sm sm:text-base"
                  >
                    <span>Proceed</span>
                    <Send className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </button>
                </div>

                <div className="w-full pt-5 sm:pt-6 border-t border-white/10 flex flex-col items-center">
                  <p className="text-[10px] sm:text-xs text-neutral-500 uppercase tracking-widest mb-3 sm:mb-4 font-bold">How to unlock?</p>
                  <div className="bg-white/5 rounded-2xl p-3 sm:p-4 flex items-center gap-3 sm:gap-4 border border-white/5 w-full">
                    <div className="bg-[#2AABEE]/20 p-2 sm:p-3 rounded-xl shrink-0">
                      <MessageCircle className="w-5 h-5 sm:w-6 sm:h-6 text-[#2AABEE]" />
                    </div>
                    <div className="flex-1 text-left min-w-0">
                      <p className="text-white font-bold text-xs sm:text-sm truncate">Contact on Telegram</p>
                      <p className="text-neutral-400 text-[10px] sm:text-xs mt-0.5 truncate">Message @SPEEDNIKK to get your access code.</p>
                    </div>
                    <a href="https://t.me/SPEEDNIKK" target="_blank" rel="noopener noreferrer" className="bg-[#2AABEE] hover:bg-[#229ED9] text-white p-2 rounded-lg transition-colors shrink-0">
                      <QrCode className="w-4 h-4 sm:w-5 sm:h-5" />
                    </a>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      <video
        ref={videoRef}
        poster={poster || undefined}
        muted={isMuted}
        className={`w-full h-full object-contain cursor-pointer ${error ? 'hidden' : ''}`}
        onClick={(e) => {
          e.stopPropagation();
          togglePlay();
          handleMouseMove();
        }}
        onDoubleClick={(e) => {
          e.stopPropagation();
          toggleFullscreen();
        }}
        playsInline
      />
      
      {!loading && !error && (
        <>
          {isFullscreen && (
            <button
              onClick={(e) => {
                 e.stopPropagation();
                 onBack?.();
              }}
              className="absolute top-2 left-2 sm:top-4 sm:left-4 z-50 p-2 sm:p-3 bg-transparent hover:bg-white/10 text-white rounded-full transition-all"
            >
              <ChevronLeft className="w-6 h-6 sm:w-8 sm:h-8" />
            </button>
          )}

          <div className="absolute top-2 right-2 z-40 flex flex-col items-end gap-1 pointer-events-none select-none">
            <img src="/og.png" alt="ZestyySports" className="h-8 sm:h-10 md:h-12 opacity-50 drop-shadow-2xl object-contain" />
            {!isUnlocked && (
              <div className="opacity-40 text-white font-black tracking-widest text-base sm:text-xl md:text-3xl font-mono drop-shadow-2xl">
                {Math.floor(previewSeconds / 60)}:{(previewSeconds % 60).toString().padStart(2, '0')}
              </div>
            )}
          </div>

          {/* Top Left Live Badge & Stats */}
          <div className={`absolute top-3 left-3 sm:top-4 sm:left-4 z-40 transition-all duration-500 ease-in-out transform ${showControls || !isPlaying || showStats ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-2'} pointer-events-none`}>
             <div className="flex flex-col gap-2 relative">
               <div className="flex items-center gap-2">
                 <div className="px-2.5 py-1 bg-red-600/90 backdrop-blur-md rounded border border-red-500/40 inline-flex items-center gap-1.5 shadow-lg shadow-red-600/20 w-fit pointer-events-auto">
                    <div className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
                    <span className="text-[10px] font-black text-white uppercase tracking-widest leading-none">LIVE</span>
                 </div>
                 {/* Countdown moved below image watermark */}
               </div>
               
               {/* Stats for Nerds Overlay */}
               {showStats && (
                 <div className="bg-black/80 backdrop-blur-md border border-white/20 p-4 rounded-xl shadow-2xl text-[10px] md:text-xs text-white/90 font-mono tracking-wider w-[260px] pointer-events-auto animate-in fade-in slide-in-from-top-2 duration-200">
                    <div className="flex items-center gap-2 border-b border-white/10 pb-2 mb-3">
                       <Activity className="w-3.5 h-3.5 text-red-500" />
                       <span className="font-bold text-white uppercase text-[10px] tracking-widest">Stats for Nerds</span>
                    </div>
                    <div className="flex flex-col gap-2">
                       <div className="flex justify-between items-center bg-white/5 px-2 py-1.5 rounded">
                          <span className="text-white/50">Resolution</span>
                          <span className="font-bold text-green-400">{stats.resolution}</span>
                       </div>
                       <div className="flex justify-between items-center bg-white/5 px-2 py-1.5 rounded">
                          <span className="text-white/50">Buffer Length</span>
                          <span className="font-bold text-amber-400">{stats.buffer}</span>
                       </div>
                       <div className="flex justify-between items-center bg-white/5 px-2 py-1.5 rounded">
                          <span className="text-white/50">Bitrate</span>
                          <span className="font-bold text-blue-400">{stats.bitrate}</span>
                       </div>
                       <div className="flex justify-between items-center bg-white/5 px-2 py-1.5 rounded">
                          <span className="text-white/50">Current Stream Data</span>
                          <span className="font-bold text-purple-400">{stats.currentData}</span>
                       </div>
                       <div className="flex justify-between items-center bg-white/5 px-2 py-1.5 rounded">
                          <span className="text-white/50">Total Data Consumed</span>
                          <span className="font-bold text-pink-400">{stats.totalData}</span>
                       </div>
                    </div>
                 </div>
               )}
             </div>
          </div>

          {/* Bottom Controls Gradient & Bar */}
          <div className={`absolute bottom-0 left-0 right-0 z-40 transition-all duration-500 ease-in-out transform bg-gradient-to-t from-black/95 via-black/50 to-transparent pt-20 px-3 pb-3 sm:px-4 sm:pb-4 ${showControls || !isPlaying ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'}`}>
            <div className="flex flex-col gap-2">
              <div className="flex justify-between items-center text-white w-full gap-2 sm:gap-4">
                <div className="flex items-center gap-2 sm:gap-4 min-w-0 flex-1">
                  <button onClick={togglePlay} className="hover:scale-110 transition-transform shrink-0 p-1">
                    {isPlaying ? <Pause className="w-4 h-4 sm:w-7 sm:h-7 fill-white" /> : <Play className="w-4 h-4 sm:w-7 sm:h-7 fill-white" />}
                  </button>
                  
                  <div className="group/volume flex items-center gap-1 sm:gap-2 shrink-0">
                    <button onClick={toggleMute} className="hover:scale-110 transition-transform p-1">
                      {isMuted || volume === 0 ? <VolumeX className="w-4 h-4 sm:w-6 sm:h-6" /> : <Volume2 className="w-4 h-4 sm:w-6 sm:h-6" />}
                    </button>
                    <input 
                      type="range" 
                      min="0" max="1" step="0.05"
                      value={isMuted ? 0 : volume}
                      onChange={handleVolumeChange}
                      className="w-0 opacity-0 group-hover/volume:w-16 sm:group-hover/volume:w-20 group-hover/volume:opacity-100 transition-all duration-300 accent-red-600 cursor-pointer hidden sm:block"
                    />
                  </div>
                  
                  <h2 className="font-bold text-xs sm:text-sm md:text-base tracking-wide truncate min-w-0 flex-1 ml-1">
                    {title}
                  </h2>
                </div>
                
                <div className="flex items-center gap-2 shrink-0">
                   <div className="hover:scale-110 transition-transform flex items-center justify-center p-1" style={{ '--connected-color': 'white', '--disconnected-color': 'white' } as React.CSSProperties}>
                     {React.createElement('google-cast-launcher', { style: { display: 'inline-block', width: '20px', height: '20px', cursor: 'pointer' }})}
                   </div>
                   <button onClick={() => setShowStats(!showStats)} className={`hover:scale-110 transition-transform p-1 ${showStats ? 'text-red-500' : 'text-white hover:text-red-400'}`}>
                     <Activity className="w-4 h-4 sm:w-6 sm:h-6" />
                   </button>
                   {levels.length > 1 && (
                     <div className="relative">
                       <button onClick={() => setShowQualityMenu(!showQualityMenu)} className="hover:scale-110 transition-transform p-1">
                         <Settings className={`w-4 h-4 sm:w-6 sm:h-6 ${showQualityMenu ? 'animate-spin-slow' : ''}`} />
                       </button>
                       {showQualityMenu && (
                         <div className="absolute bottom-full right-0 mb-4 bg-black/90 backdrop-blur-md rounded-lg border border-white/20 p-2 min-w-[120px] shadow-2xl flex flex-col gap-1 z-50 overflow-hidden text-sm animate-in fade-in zoom-in-95 duration-200">
                           <button 
                             onClick={() => changeQuality(-1)} 
                             className={`px-3 py-1.5 rounded-md text-left whitespace-nowrap transition-colors ${currentLevel === -1 ? 'bg-red-600 font-bold text-white' : 'text-neutral-300 hover:bg-white/10 hover:text-white'}`}
                           >
                             Auto
                           </button>
                           {levels.map((lvl, idx) => (
                             <button
                               key={idx}
                               onClick={() => changeQuality(idx)}
                               className={`px-3 py-1.5 rounded-md text-left whitespace-nowrap transition-colors ${currentLevel === idx ? 'bg-red-600 font-bold text-white' : 'text-neutral-300 hover:bg-white/10 hover:text-white'}`}
                             >
                               {lvl.height ? `${lvl.height}p` : (lvl.name || `Level ${idx}`)}
                             </button>
                           ))}
                         </div>
                       )}
                     </div>
                   )}
                   <button onClick={togglePiP} className="hover:scale-110 transition-transform p-1">
                     <PictureInPicture className="w-4 h-4 sm:w-6 sm:h-6" />
                   </button>
                   <button onClick={toggleFullscreen} className="hover:scale-110 transition-transform p-1">
                     {isFullscreen ? <Minimize className="w-4 h-4 sm:w-6 sm:h-6" /> : <Maximize className="w-4 h-4 sm:w-6 sm:h-6" />}
                   </button>
                </div>
              </div>
              <div className="w-full h-1 bg-white/20 rounded-full overflow-hidden mt-1 cursor-not-allowed">
                 <div className="h-full bg-red-600 w-full rounded-full flex justify-end">
                    <div className="w-1.5 h-full bg-red-400" />
                 </div>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

