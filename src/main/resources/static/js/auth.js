const globalSetTokens = typeof window !== 'undefined' && typeof window.setTokens === 'function'
  ? window.setTokens
  : (accessToken, refreshToken) => {
      localStorage.setItem('naujava_access_token', accessToken);
      if (refreshToken) {
        localStorage.setItem('naujava_refresh_token', refreshToken);
      }
    };

const globalShowAlert = typeof window !== 'undefined' && typeof window.showAlert === 'function'
  ? window.showAlert
  : (message, type = 'info') => {
      const container = document.querySelector('#alertContainer');
      if (!container) return;
      const alert = document.createElement('div');
      alert.className = `alert alert--${type}`;
      alert.textContent = message;
      container.innerHTML = '';
      container.append(alert);
      setTimeout(() => alert.remove(), 6000);
    };

function prepareAuthForm(formId, url) {
  const form = document.querySelector(`#${formId}`);
  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const formData = new FormData(form);
    const payload = {};
    formData.forEach((value, key) => (payload[key] = value));
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || 'Ошибка сервера');
      }
      const result = await response.json();
      globalSetTokens(result.accessToken, result.refreshToken);
      window.location.href = '/files';
    } catch (error) {
      globalShowAlert(error.message || 'Не удалось выполнить запрос', 'error');
    }
  });
}

window.addEventListener('DOMContentLoaded', () => {
  prepareAuthForm('loginForm', '/api/v1/auth/login');
  prepareAuthForm('registerForm', '/api/v1/auth/register');
});
