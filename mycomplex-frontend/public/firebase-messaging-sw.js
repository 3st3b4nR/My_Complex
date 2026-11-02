importScripts(
  'https://www.gstatic.com/firebasejs/12.9.0/firebase-app-compat.js'
);
importScripts(
  'https://www.gstatic.com/firebasejs/12.9.0/firebase-messaging-compat.js'
);

firebase.initializeApp({
  apiKey: 'AIzaSyBQ2ElsLcRtWdeSHAhsWM04CBKq5bdCzZs',
  authDomain: 'mycomplex-aba6e.firebaseapp.com',
  projectId: 'mycomplex-aba6e',
  storageBucket: 'mycomplex-aba6e.firebasestorage.app',
  messagingSenderId: '202019758767',
  appId: '1:202019758767:web:dd5f64f099833f50ef6a5a'
});

firebase.messaging();
