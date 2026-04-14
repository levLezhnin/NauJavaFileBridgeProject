const publicPaths = ['/', '/login', '/register', '/forbidden'];

function showAlert(message, type = 'info') {
  const container = document.querySelector('#alertContainer');
  if (!container) return;
  const alert = document.createElement('div');
  alert.className = `alert alert--${type}`;
  alert.textContent = message;
  container.innerHTML = '';
  container.append(alert);
  setTimeout(() => {
    alert.remove();
  }, 6000);
}

function apiFetch(path, options = {}) {
  options.headers = options.headers || {};
  options.headers['Accept'] = 'application/json';
  if (!(options.body instanceof FormData)) {
    options.headers['Content-Type'] = 'application/json';
  }
  options.credentials = options.credentials || 'same-origin';
  return fetch(path, options).then(async response => {
    if (response.status === 401) {
      window.location.href = '/login';
      return Promise.reject(new Error('Unauthorized'));
    }
    if (!response.ok) {
      const data = await response.json().catch(() => null);
      return Promise.reject(new Error(data?.message || response.statusText));
    }
    return response.json().catch(() => null);
  });
}

function updateNavbar(isAuthenticated) {
  const authLinks = document.querySelectorAll('.auth-only');
  const guestLinks = document.querySelectorAll('.guest-only');
  authLinks.forEach(el => el.style.display = isAuthenticated ? 'inline-flex' : 'none');
  guestLinks.forEach(el => el.style.display = isAuthenticated ? 'none' : 'inline-flex');
}

function isPublicPage() {
  return publicPaths.includes(window.location.pathname);
}

function checkAuthStatus() {
  return fetch('/api/v1/users/me', {
    method: 'GET',
    headers: { 'Accept': 'application/json' },
    credentials: 'same-origin'
  }).then(response => response.ok).catch(() => false);
}

function initNavbar() {
  updateNavbar(false);
  const logout = document.querySelector('#logoutButton');
  if (logout) {
    logout.addEventListener('click', async () => {
      try {
        await apiFetch('/api/v1/auth/logout', { method: 'POST' });
      } catch (error) {
        // ignore network/logout errors
      }
      window.location.href = '/login';
    });
  }

  checkAuthStatus().then(isAuthenticated => {
    updateNavbar(isAuthenticated);
    if (!isAuthenticated && !isPublicPage()) {
      window.location.href = '/login';
    }
  });
}

function initProfileData() {
  const usernameNode = document.querySelector('#profileUsername');
  const quotaNode = document.querySelector('#profileQuota');
  if (!usernameNode && !quotaNode) {
    return;
  }
  apiFetch('/api/v1/users/me', { method: 'GET' })
    .then(data => {
      if (usernameNode) {
        usernameNode.textContent = data?.username || data?.email || 'Неизвестный пользователь';
      }
      if (quotaNode) {
        quotaNode.textContent = data ? 'Квотные данные загружены.' : 'Нет информации о квоте.';
      }
    })
    .catch(() => {
      if (usernameNode) {
        usernameNode.textContent = 'Гость';
      }
      if (quotaNode) {
        quotaNode.textContent = 'Нет информации о квоте.';
      }
    });
}

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initProfileData();
});

window.showAlert = showAlert;
window.apiFetch = apiFetch;
window.initNavbar = initNavbar;
window.initProfileData = initProfileData;