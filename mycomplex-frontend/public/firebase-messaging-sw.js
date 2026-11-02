self.addEventListener('message', event => {
  if (event.data?.type !== 'MYCOMPLEX_FIREBASE_CONFIG_ENDPOINT') return;

  event.waitUntil(loadPublicFirebaseConfig(event.data.apiUrl));
});

async function loadPublicFirebaseConfig(apiUrl) {
  const endpoint = new URL('/api/public/firebase-config', apiUrl);
  const localDevelopment = endpoint.hostname === 'localhost'
    || endpoint.hostname === '127.0.0.1';
  if (endpoint.protocol !== 'https:' && !localDevelopment) return;

  const response = await fetch(endpoint, {
    credentials: 'omit',
    headers: { Accept: 'application/json' },
    mode: 'cors'
  });
  if (!response.ok) return;

  const config = await response.json();
  if (config?.enabled === true) {
    self.firebasePublicConfig = config;
  }
}

self.addEventListener('push', event => {
  if (!event.data) return;
  const payload = event.data.json();
  const data = payload.data || {};
  event.waitUntil(self.registration.showNotification(
    data.title || 'MyComplex',
    {
      body: data.body || 'Tienes una nueva notificación.',
      icon: '/assets/branding/mycomplex-mark.png',
      data: { link: data.link || '/select-property' }
    }
  ));
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  const link = event.notification.data?.link || '/select-property';
  event.waitUntil(clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  }).then(windowClients => {
    const target = new URL(link, self.location.origin).href;
    const existing = windowClients.find(client => client.url === target);
    return existing ? existing.focus() : clients.openWindow(target);
  }));
});
