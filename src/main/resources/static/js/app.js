const StorageKeys = {
  accessToken: 'naujava_access_token',
  refreshToken: 'naujava_refresh_token'
};

function getAccessToken() {
  return localStorage.getItem(StorageKeys.accessToken);
}

function getRefreshToken() {
  return localStorage.getItem(StorageKeys.refreshToken);
}

function setTokens(accessToken, refreshToken) {
  localStorage.setItem(StorageKeys.accessToken, accessToken);
  if (refreshToken) {
    localStorage.setItem(StorageKeys.refreshToken, refreshToken);
  }
}

function clearTokens() {
  localStorage.removeItem(StorageKeys.accessToken);
  localStorage.removeItem(StorageKeys.refreshToken);
}

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
  const token = getAccessToken();
  options.headers = options.headers || {};
  options.headers['Accept'] = 'application/json';
  if (!(options.body instanceof FormData)) {
    options.headers['Content-Type'] = 'application/json';
  }
  if (token) {
    options.headers['Authorization'] = `Bearer ${token}`;
  }
  return fetch(path, options).then(async response => {
    if (response.status === 401) {
      clearTokens();
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

function decodeJwt(token) {
  if (!token) return null;
  const payload = token.split('.')[1];
  if (!payload) return null;
  try {
    const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(decodeURIComponent(escape(decoded)));
  } catch (error) {
    return null;
  }
}

function initNavbar() {
  const token = getAccessToken();
  const authLinks = document.querySelectorAll('.auth-only');
  const guestLinks = document.querySelectorAll('.guest-only');
  authLinks.forEach(el => el.style.display = token ? 'inline-flex' : 'none');
  guestLinks.forEach(el => el.style.display = token ? 'none' : 'inline-flex');
  const logout = document.querySelector('#logoutButton');
  if (logout) {
    logout.addEventListener('click', () => {
      clearTokens();
      window.location.href = '/login';
    });
  }
}

function initProfileData() {
  const token = getAccessToken();
  const payload = decodeJwt(token);
  const usernameNode = document.querySelector('#profileUsername');
  const quotaNode = document.querySelector('#profileQuota');
  if (usernameNode) {
    usernameNode.textContent = payload?.username || payload?.sub || 'Неизвестный пользователь';
  }
  if (quotaNode) {
    quotaNode.textContent = 'Квотные данные будут доступны после подключения серверного API.';
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initProfileData();
});

window.showAlert = showAlert;
window.setTokens = setTokens;
window.getAccessToken = getAccessToken;
window.getRefreshToken = getRefreshToken;
window.clearTokens = clearTokens;
window.apiFetch = apiFetch;
window.decodeJwt = decodeJwt;
window.initNavbar = initNavbar;
window.initProfileData = initProfileData;