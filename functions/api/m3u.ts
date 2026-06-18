export async function onRequest(context: any) {
  const { request } = context;
  
  if (request.method === 'OPTIONS') {
    return new Response(null, {
      status: 204,
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, HEAD, POST, OPTIONS',
        'Access-Control-Allow-Headers': 'Content-Type, Range',
        'Access-Control-Max-Age': '86400',
      }
    });
  }

  const url = new URL(request.url);
  const streamUrl = url.searchParams.get('url') || 'https://m3u-tvb.pages.dev/ixp.m3u';

  try {
    const response = await fetch(streamUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
      },
      cf: {
        cacheTtlByStatus: { '200-299': 60, 404: 1, '500-599': 0 }
      }
    } as any);

    const text = await response.text();
    
    return new Response(text, {
      status: response.status,
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Content-Type': 'text/plain',
      }
    });
  } catch (error: any) {
    return new Response(`Error fetching M3U: ${error.message}`, { 
      status: 500,
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Content-Type': 'text/plain'
      }
    });
  }
}
