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
      const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
      const response = await fetch(url, {
        method: 'POST',
        headers,
        credentials: 'same-origin',
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || 'Ошибка сервера');
      }
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
