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

function updateNavbar(isAuthenticated, isAdmin = false) {
  const authLinks = document.querySelectorAll('.auth-only');
  const guestLinks = document.querySelectorAll('.guest-only');
  const adminLinks = document.querySelectorAll('.admin-only');
  authLinks.forEach(el => el.style.display = isAuthenticated ? 'inline-flex' : 'none');
  guestLinks.forEach(el => el.style.display = isAuthenticated ? 'none' : 'inline-flex');
  adminLinks.forEach(el => el.style.display = isAuthenticated && isAdmin ? 'inline-flex' : 'none');
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
  updateNavbar(false, false);
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

  if (isPublicPage()) {
    return;
  }

  apiFetch('/api/v1/users/me', { method: 'GET' })
    .then(data => {
      const isAdmin = data?.role === 'ADMIN';
      updateNavbar(true, isAdmin);
    })
    .catch(() => {
      updateNavbar(false, false);
      if (!isPublicPage()) {
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

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function initReportPage() {
  const reportShell = document.querySelector('#reportShell');
  const generateButton = document.querySelector('#generateReportButton');
  const statusText = document.querySelector('#reportStatusText');
  const reportActions = document.querySelector('#reportActions');
  const reportContent = document.querySelector('#reportContent');
  if (!reportShell || !generateButton || !statusText || !reportContent) {
    return;
  }

  const reportId = reportShell.dataset.reportId;
  const setStatus = (text, type = 'info') => {
    statusText.textContent = text;
    if (type === 'error') {
      statusText.style.color = 'var(--danger)';
    } else {
      statusText.style.color = 'var(--text)';
    }
  };

  const renderRawHtml = html => {
    const bodyMatch = html.match(/<body[^>]*>([\s\S]*)<\/body>/i);
    const bodyContent = bodyMatch ? bodyMatch[1] : html;
    reportContent.innerHTML = `<div style="padding: 0; margin: 0;">${bodyContent}</div>`;
  };

  const pollReport = async (id) => {
    setStatus('Отчёт формируется, подождите...');
    reportActions.innerHTML = `<span class="pill pill-muted">ID отчёта: ${id}</span>`;
    reportContent.innerHTML = '<div class="empty-state"><p class="empty-title">Ожидание отчёта</p><p class="empty-text">Проверяем готовность данных...</p></div>';

    while (true) {
      try {
        const response = await fetch(`/api/v1/report/${id}`, {
          method: 'GET',
          credentials: 'same-origin',
          headers: { Accept: 'text/plain' }
        });

        if (response.ok) {
          const html = await response.text();
          setStatus('Отчёт готов');
          reportActions.innerHTML = `<a href="/api/v1/report/${id}" target="_blank" class="btn btn-secondary">Открыть исходный HTML</a>`;
          renderRawHtml(html);
          return;
        }

        const textBody = await response.text().catch(() => '');
        let message = response.statusText || 'Не удалось получить статус отчёта';

        try {
          const parsed = JSON.parse(textBody);
          message = parsed?.message || parsed?.error || message;
        } catch {
          if (textBody && textBody.trim()) {
            message = textBody.trim();
          }
        }

        // Если отчёт ещё создаётся, продолжаем опрос
        if (message.includes('Отчёт ещё создаётся')) {
          await sleep(1400);
          continue;
        }

        setStatus('Ошибка загрузки отчёта', 'error');
        reportContent.innerHTML = `<div class="empty-state"><p class="empty-title">Ошибка</p><p class="empty-text">${message}</p></div>`;
        return;
      } catch (err) {
        setStatus('Ошибка сети при проверке статуса', 'error');
        reportContent.innerHTML = `<div class="empty-state"><p class="empty-title">Ошибка сети</p><p class="empty-text">${err.message}</p></div>`;
        return;
      }
    }
  };

  generateButton.addEventListener('click', async () => {
    generateButton.disabled = true;
    const originalText = generateButton.textContent;
    generateButton.textContent = 'Запуск...';
    setStatus('Запрос на формирование отчёта отправлен...');
    reportActions.innerHTML = '';
    reportContent.innerHTML = '<div class="empty-state"><p class="empty-title">Запуск</p><p class="empty-text">Формируем запрос на сервер...</p></div>';

    try {
      const id = await apiFetch('/api/v1/report/generate', { method: 'POST' });
      if (!id) {
        throw new Error('Не получен идентификатор отчёта');
      }
      reportActions.innerHTML = `<span class="pill pill-muted">ID отчёта: ${id}</span>`;
      await pollReport(id);
    } catch (err) {
      showAlert(err.message || 'Не удалось сформировать отчёт', 'error');
      setStatus('Не удалось запустить формирование', 'error');
      reportContent.innerHTML = '<div class="empty-state"><p class="empty-title">Ошибка</p><p class="empty-text">Проверьте соединение и повторите.</p></div>';
    } finally {
      generateButton.disabled = false;
      generateButton.textContent = originalText;
    }
  });

  if (reportId) {
    generateButton.disabled = true;
    pollReport(reportId).finally(() => {
      generateButton.disabled = false;
    });
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initProfileData();
  initReportPage();
});

window.showAlert = showAlert;
window.apiFetch = apiFetch;
window.initNavbar = initNavbar;
window.initProfileData = initProfileData;