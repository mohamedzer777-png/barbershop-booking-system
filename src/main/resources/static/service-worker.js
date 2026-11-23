// גרסת מטמון — שנה את המספר כדי לאלץ רענון מטמון אחרי עדכון
const CACHE = 'barbershop-v2';

// קבצי המעטפת שנשמרים מראש (לא כולל קריאות API — אלה תמיד טריות מהשרת)
const SHELL = [
  '/index.html',
  '/manifest.json',
  '/icon-192.png',
  '/icon-512.png'
];

// התקנה: שומרים את המעטפת במטמון
self.addEventListener('install', function (event) {
  event.waitUntil(
    caches.open(CACHE).then(function (cache) { return cache.addAll(SHELL); })
  );
  self.skipWaiting();
});

// הפעלה: מנקים מטמונים ישנים מגרסאות קודמות
self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(keys.filter(function (k) { return k !== CACHE; })
                            .map(function (k) { return caches.delete(k); }));
    })
  );
  self.clients.claim();
});

// בקשות:
//  - קריאות API (גם /api/admin) -> תמיד מהרשת, אף פעם לא מהמטמון (נתונים חייבים להיות עדכניים)
//  - שאר הקבצים -> מהרשת, ובמקרה כשל (אופליין) נופלים למטמון
self.addEventListener('fetch', function (event) {
  const url = new URL(event.request.url);

  if (url.pathname.startsWith('/api/')) {
    event.respondWith(fetch(event.request));
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then(function (res) {
        const copy = res.clone();
        caches.open(CACHE).then(function (cache) { cache.put(event.request, copy); });
        return res;
      })
      .catch(function () { return caches.match(event.request); })
  );
});
