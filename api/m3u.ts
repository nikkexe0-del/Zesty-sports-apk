
import fetch from 'node-fetch';
import { VercelRequest, VercelResponse } from '@vercel/node';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  const url = req.query.url as string || 'https://m3u-tvb.pages.dev/ixp.m3u';
  try {
    const response = await fetch(url);
    const text = await response.text();
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Content-Type', 'text/plain');
    res.status(200).send(text);
  } catch (err) {
    console.error('M3U Fetch Error:', err);
    res.status(500).send('Error fetching M3U');
  }
}
