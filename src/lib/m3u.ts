export interface M3UItem {
  id: string;
  name: string;
  logo: string;
  url: string;
  group: string;
}

export function parseM3U(content: string): M3UItem[] {
  const lines = content.split('\n');
  const items: M3UItem[] = [];
  let currentItem: Partial<M3UItem> = {};

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (line.startsWith('#EXTINF:')) {
      // Parse #EXTINF:-1 tvg-id="id" tvg-name="name" tvg-logo="url" group-title="group",Channel Name
      const logoMatch = line.match(/tvg-logo=["']([^"']+)["']/i);
      const nameMatch = line.match(/tvg-name=["']([^"']+)["']/i);
      const groupMatch = line.match(/group-title=["']([^"']+)["']/i);
      const idMatch = line.match(/tvg-id=["']([^"']+)["']/i);
      
      const lastCommaIndex = line.lastIndexOf(',');
      const displayName = lastCommaIndex !== -1 ? line.substring(lastCommaIndex + 1).trim() : 'Unknown Channel';

      currentItem = {
        id: idMatch ? idMatch[1] : `channel-${items.length}-${Math.random().toString(36).substr(2, 5)}`,
        name: (nameMatch ? nameMatch[1] : displayName) || 'Unknown Channel',
        logo: logoMatch ? logoMatch[1] : '',
        group: groupMatch ? groupMatch[1] : 'General',
      };
    } else if (line && !line.startsWith('#')) {
      if (currentItem.name) {
        currentItem.url = line;
        items.push(currentItem as M3UItem);
        currentItem = {};
      }
    }
  }

  return items;
}
