const publicPaths = ['/', '/login', '/register', '/forbidden'];

function showValidationSummary(message, errors) {
  const summary = document.querySelector('#validationSummary');
  if (summary) {
    summary.querySelector('.validation-summary__text').textContent = message;
    const list = summary.querySelector('.validation-summary__list');
    list.innerHTML = errors.map(error => `<li>${error}</li>`).join('');
    summary.style.display = 'block';
  }
}

function hideValidationSummary() {
  const summary = document.querySelector('#validationSummary');
  if (summary) {
    summary.style.display = 'none';
  }
}

function clearAllFieldErrors(form) {
  form.querySelectorAll('.field-error').forEach(el => el.classList.remove('field-error'));
  form.querySelectorAll('.field-error-message').forEach(el => el.remove());
}

function clearFieldError(fieldId) {
  const field = document.querySelector(`#${fieldId}`);
  if (field) {
    field.classList.remove('field-error');
    const errorMsg = field.parentElement.querySelector('.field-error-message');
    if (errorMsg) errorMsg.remove();
  }
}

function showFieldError(fieldId, message) {
  const field = document.querySelector(`#${fieldId}`);
  if (field) {
    field.classList.add('field-error');
    let existingMsg = field.parentElement.querySelector('.field-error-message');
    if (existingMsg) {
      existingMsg.textContent = existingMsg.textContent + '; ' + message;
    } else {
      const errorMsg = document.createElement('p');
      errorMsg.className = 'field-error-message';
      errorMsg.textContent = message;
      field.parentElement.appendChild(errorMsg);
    }
  }
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

let refreshPromise = null;
let redirectInProgress = false;

function redirectToLogin() {
  if (redirectInProgress) return;
  redirectInProgress = true;
  window.location.href = '/login';
}

async function refreshAccessToken() {
  if (refreshPromise) {
    return refreshPromise;
  }

  refreshPromise = fetch('/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'Accept': 'application/json' },
    credentials: 'same-origin'
  }).then(response => {
    if (!response.ok) {
      throw new Error('Refresh token expired');
    }
    return true;
  }).finally(() => {
    refreshPromise = null;
  });

  return refreshPromise;
}

async function parseApiResponse(response) {
  const contentType = response.headers.get('Content-Type') || '';
  if (response.status === 204) {
    return null;
  }
  if (contentType.includes('application/json')) {
    return response.json().catch(() => null);
  }
  return response.text().catch(() => null);
}

async function performFetchWithRefresh(path, options = {}) {
  const { skipRedirectOnAuthFail = false, ...fetchOptions } = options;
  fetchOptions.headers = fetchOptions.headers || {};
  fetchOptions.headers['Accept'] = fetchOptions.headers['Accept'] || 'application/json';
  if (!(fetchOptions.body instanceof FormData) && !fetchOptions.headers['Content-Type']) {
    fetchOptions.headers['Content-Type'] = 'application/json';
  }
  fetchOptions.credentials = fetchOptions.credentials || 'same-origin';

  const executeFetch = async () => {
    const isFetchNetworkError = (error) => {
      const message = String(error?.message || '').toLowerCase();
      return error instanceof TypeError || error?.name === 'TypeError' || error?.name === 'DOMException' || message.includes('failed to fetch') || message.includes('network') || message.includes('refused');
    };

    let response;
    try {
      response = await fetch(path, fetchOptions);
    } catch (fetchError) {
      if (isFetchNetworkError(fetchError)) {
        throw new Error('Сетевая ошибка: не удалось подключиться к серверу. Проверьте подключение и попробуйте снова.');
      }
      throw fetchError;
    }

    if (response.status === 401 && path !== '/api/v1/auth/refresh') {
      try {
        await refreshAccessToken();
      } catch (refreshError) {
        if (!skipRedirectOnAuthFail) {
          redirectToLogin();
        }
        throw new Error('Unauthorized');
      }

      const retryResponse = await fetch(path, fetchOptions);
      if (retryResponse.status === 401) {
        if (!skipRedirectOnAuthFail) {
          redirectToLogin();
        }
        throw new Error('Unauthorized');
      }
      return retryResponse;
    }
    return response;
  };

  return executeFetch();
}

function apiFetch(path, options = {}) {
  return performFetchWithRefresh(path, options).then(async response => {
    if (!response.ok) {
      const data = await response.json().catch(() => null);
      if (response.status === 403 && data?.error === 'Ваш аккаунт заблокирован') {
        window.location.href = '/forbidden';
        return Promise.reject(new Error('Redirecting to forbidden page'));
      }
      const error = new Error(data?.message || response.statusText);
      error.status = response.status;
      if (data?.fieldErrors && Array.isArray(data.fieldErrors)) {
        error.details = data.fieldErrors.map(e => e.message).join(', ');
      }
      return Promise.reject(error);
    }

    return parseApiResponse(response);
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

async function initNavbar() {
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

  if (window.location.pathname === '/forbidden') {
    updateNavbar(true, false);
    return;
  }

  try {
    const user = await apiFetch('/api/v1/users/me', { method: 'GET', skipRedirectOnAuthFail: true });
    const isAdmin = user?.role === 'ADMIN';
    updateNavbar(true, isAdmin);

    if ((window.location.pathname === '/login' || window.location.pathname === '/register') && user) {
      window.location.href = '/files';
    }
  } catch (error) {
    updateNavbar(false, false);
    if (!isPublicPage()) {
      window.location.href = '/login';
    }
  }
}

function validateUpdateProfileForm(newUsername, currentPassword, newPassword, confirmNewPassword) {
  const errors = [];

  // At least one field should be provided
  if ((!newUsername || newUsername.trim().length === 0) &&
      (!newPassword || newPassword.trim().length === 0)) {
    errors.push('Укажите хотя бы новый логин или новый пароль');
  }

  // Username validation: 5-255 chars, letters (including Cyrillic), digits, dots, dashes, underscores
  if (newUsername && newUsername.trim().length > 0) {
    if (newUsername.length < 5 || newUsername.length > 255) {
      errors.push('Имя пользователя должно иметь длину от 5 до 255 символов');
    } else if (!/^[a-zA-Zа-яА-ЯЁё\d\-_\.]+$/.test(newUsername)) {
      errors.push('Имя пользователя должно содержать только буквы, цифры, точки, тире и нижние подчёркивания');
    }
  }

  // If changing password, current password is required
  if (newPassword && newPassword.trim().length > 0) {
    if (!currentPassword || currentPassword.trim().length === 0) {
      errors.push('Для смены пароля укажите текущий пароль');
    }

    // Password validation: 8-255 chars, at least one lowercase, one uppercase, one digit, one special char
    if (newPassword.length < 8 || newPassword.length > 255) {
      errors.push('Длина пароля должна быть от 8 до 255 символов');
    } else if (!/(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[?!@#$%^&*()+_\-])/.test(newPassword)) {
      errors.push('Пароль должен содержать латинские буквы верхнего и нижнего регистров, цифру и специальный символ');
    }

    // Confirm password
    if (!confirmNewPassword || confirmNewPassword.trim().length === 0) {
      errors.push('Подтвердите новый пароль');
    } else if (newPassword !== confirmNewPassword) {
      errors.push('Пароли не совпадают');
    }
  }

  return errors;
}

function loadQuotaData() {
  return apiFetch('/api/v1/quotas/my', { method: 'GET' })
    .then(quotaData => {
      return quotaData;
    })
    .catch(() => {
      return null;
    });
}

function formatQuotaDisplay(quotaData) {
  if (!quotaData) return 'Нет информации о квоте.';

  const usedBytes = parseInt(quotaData.used_storage_bytes) || 0;
  const maxBytes = parseInt(quotaData.max_storage_bytes) || 0;

  const usedFormatted = formatBytes(usedBytes);
  const maxFormatted = formatBytes(maxBytes);

  return `${usedFormatted} из ${maxFormatted}`;
}

function initProfileData() {
  const usernameNode = document.querySelector('#profileUsername');
  const quotaNode = document.querySelector('#profileQuota');
  if (!usernameNode && !quotaNode) {
    return;
  }

  // Load user data
  apiFetch('/api/v1/users/me', { method: 'GET' })
    .then(data => {
      if (usernameNode) {
        usernameNode.textContent = data?.username || data?.email || 'Неизвестный пользователь';
      }
    })
    .catch(() => {
      if (usernameNode) {
        usernameNode.textContent = 'Гость';
      }
    });

  // Load quota data
  if (quotaNode) {
    loadQuotaData().then(quotaData => {
      quotaNode.textContent = formatQuotaDisplay(quotaData);
    });
  }
}

function initProfileUpdateForm() {
  const updateProfileForm = document.querySelector('#updateProfileForm');
  if (!updateProfileForm) return;

  // Clear errors when user starts typing
  updateProfileForm.querySelectorAll('input').forEach(input => {
    input.addEventListener('input', () => {
      clearFieldError(input.id);
      hideValidationSummary();
    });
  });

  // Delete account functionality
  const deleteAccountBtn = document.querySelector('#deleteAccountBtn');
  if (deleteAccountBtn) {
    deleteAccountBtn.addEventListener('click', async () => {
      const confirmed = confirm(
        'Вы уверены, что хотите удалить аккаунт?\n\nЭто действие нельзя будет отменить. Все ваши файлы будут удалены, и вы потеряете доступ к системе.'
      );

      if (!confirmed) return;

      deleteAccountBtn.disabled = true;
      const originalText = deleteAccountBtn.textContent;
      deleteAccountBtn.textContent = 'Удаление...';

      try {
        await apiFetch('/api/v1/users/me', { method: 'DELETE' });
        showAlert('Аккаунт успешно удалён', 'success');

        // Redirect to login after a short delay
        setTimeout(() => {
          window.location.href = '/login';
        }, 2000);
      } catch (error) {
        showAlert(error.message || 'Ошибка удаления аккаунта', 'error');
        deleteAccountBtn.disabled = false;
        deleteAccountBtn.textContent = originalText;
      }
    });
  }

  updateProfileForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const newUsername = document.querySelector('#newUsername').value.trim();
    const currentPassword = document.querySelector('#currentPassword').value;
    const newPassword = document.querySelector('#newPassword').value;
    const confirmNewPassword = document.querySelector('#confirmNewPassword').value;

    hideValidationSummary();
    clearAllFieldErrors(updateProfileForm);

    // Client-side validation
    const errors = validateUpdateProfileForm(newUsername, currentPassword, newPassword, confirmNewPassword);
    if (errors.length > 0) {
      showValidationSummary('Пожалуйста, исправьте ошибки в форме', errors);

      // Highlight fields so пользователь видит, где нужно ввести данные
      if (!newUsername && !newPassword) {
        showFieldError('newUsername', 'Укажите новый логин или новый пароль');
        showFieldError('newPassword', 'Укажите новый логин или новый пароль');
      }
      if (newUsername && (newUsername.length < 5 || newUsername.length > 255 || !/^[a-zA-Zа-яА-ЯЁё\d\-_\.]+$/.test(newUsername))) {
        showFieldError('newUsername', 'Имя пользователя должно иметь длину от 5 до 255 символов и содержать только буквы верхнего и нижнего регистров, цифры, точки, тире и нижние подчёркивания');
      }
      if (newPassword && (newPassword.length < 8 || newPassword.length > 255 || !/(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[?!@#$%^&*()+_\-])/.test(newPassword))) {
        showFieldError('newPassword', 'Пароль должен содержать латинские буквы верхнего и нижнего регистров, цифру и специальный символ');
      }
      if (newPassword && (!confirmNewPassword || newPassword !== confirmNewPassword)) {
        showFieldError('confirmNewPassword', 'Пароли не совпадают');
      }
      if (newPassword && (!currentPassword || currentPassword.trim().length === 0)) {
        showFieldError('currentPassword', 'Для смены пароля укажите текущий пароль');
      }
      return;
    }

    const button = updateProfileForm.querySelector('button[type="submit"]');
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = '⏳ Обновление...';

    try {
      const payload = {};
      if (newUsername) payload.new_username = newUsername;
      if (currentPassword) payload.current_password = currentPassword;
      if (newPassword) payload.new_password = newPassword;
      if (confirmNewPassword) payload.confirm_new_password = confirmNewPassword;

      await apiFetch('/api/v1/users/me', {
        method: 'PATCH',
        body: JSON.stringify(payload)
      });

      showAlert('Профиль успешно обновлён!', 'success');

      // Clear form
      document.querySelector('#newUsername').value = '';
      document.querySelector('#currentPassword').value = '';
      document.querySelector('#newPassword').value = '';
      document.querySelector('#confirmNewPassword').value = '';

      // Refresh profile data
      initProfileData();

    } catch (error) {
      // Handle field-specific errors from server
      if (error && error.fieldErrors && Array.isArray(error.fieldErrors)) {
        clearAllFieldErrors(updateProfileForm);
        const fieldErrorsMap = {};
        error.fieldErrors.forEach(fieldError => {
          const fieldName = fieldError.field;
          const message = fieldError.message;
          const fieldIdMap = {
            'newUsername': 'newUsername',
            'currentPassword': 'currentPassword',
            'newPassword': 'newPassword',
            'confirmNewPassword': 'confirmNewPassword'
          };
          const elementId = fieldIdMap[fieldName] || fieldName;
          if (!fieldErrorsMap[elementId]) {
            fieldErrorsMap[elementId] = [];
          }
          fieldErrorsMap[elementId].push(message);
        });

        // Show field-specific errors
        Object.entries(fieldErrorsMap).forEach(([fieldId, messages]) => {
          showFieldError(fieldId, messages.join(' '));
        });

        // Show general summary
        showValidationSummary('Пожалуйста, исправьте ошибки в форме', Object.values(fieldErrorsMap).flat());
      } else {
        showAlert(error.message || 'Ошибка обновления профиля', 'error');
      }
    } finally {
      button.disabled = false;
      button.textContent = originalText;
    }
  });
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initProfileData();
  initProfileUpdateForm();
});

// ============================================
// File Management Functions
// ============================================

function formatDate(isoString) {
  if (!isoString) return 'Неизвестно';
  const date = new Date(isoString);
  return date.toLocaleDateString('ru-RU', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatBytes(bytes) {
  if (!bytes) return '0 Б';
  const k = 1024;
  const sizes = ['Б', 'КБ', 'МБ', 'ГБ', 'ТБ'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i];
}

function formatFilesCount(count) {
  if (count % 10 === 1 && count % 100 !== 11) return `${count} файл`;
  if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) return `${count} файла`;
  return `${count} файлов`;
}

function getFileStatus(file) {
  const now = new Date();
  const expireDate = new Date(file.expire_date);
  const isExpired = expireDate < now;
  const isDownloadLimitReached = file.times_downloaded >= file.max_downloads;
  return {
    isExpired,
    isDownloadLimitReached,
    isActive: !isExpired && !isDownloadLimitReached,
    status: isExpired ? 'Истёк' : isDownloadLimitReached ? 'Лимит достигнут' : 'Активен'
  };
}

function createFileCard(file) {
  const fileStatus = getFileStatus(file);
  const statusClass = fileStatus.isActive ? 'status-active' : 'status-inactive';
  const card = document.createElement('div');
  card.className = 'file-card';
  card.innerHTML = `
    <div class="file-card-header">
      <div class="file-info">
        <h3 class="file-name">${escapeHtml(file.file_name)}</h3>
        <p class="file-meta">
          Загружено: <time>${formatDate(file.upload_date)}</time>
        </p>
      </div>
      <span class="file-status ${statusClass}">${fileStatus.status}</span>
    </div>
    <div class="file-card-meta">
      <div class="meta-item">
        <span class="meta-label">Истечение:</span>
        <span class="meta-value">${formatDate(file.expire_date)}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">Скачиваний:</span>
        <span class="meta-value">${file.times_downloaded}/${file.max_downloads}</span>
      </div>
      <div class="meta-item">
        <span class="meta-label">Защита:</span>
        <span class="meta-value">${file.has_password ? '🔒 Защищён' : 'Открыт'}</span>
      </div>
    </div>
    <div class="file-card-actions">
      <button class="btn btn-small btn-secondary copy-link-btn" data-file-id="${file.id}" aria-label="Копировать ссылку на скачивание">
        📋 Копировать ссылку
      </button>
      <a href="/download/${file.id}" class="btn btn-small btn-secondary">⬇️ Скачать</a>
      <button class="btn btn-small btn-danger delete-file-btn" data-file-id="${file.id}" aria-label="Удалить файл">
        🗑️ Удалить
      </button>
    </div>
    <div class="file-card-error" aria-live="polite"></div>
  `;
  return card;
}

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

async function loadFiles() {
  const filesList = document.querySelector('#filesList');
  const quotaInfo = document.querySelector('#quotaInfo');
  const quotaMeta = document.querySelector('.quota-meta');

  if (!filesList) return;

  filesList.innerHTML = '<div class="empty-state"><p class="empty-title">Загрузка файлов...</p></div>';

  try {
    const [files, quotaData] = await Promise.all([
      apiFetch('/api/v1/files/my?page=0&page_size=100'),
      loadQuotaData()
    ]);

    if (!files || files.length === 0) {
      filesList.innerHTML = `
        <div class="empty-state">
          <p class="empty-title">У вас пока нет файлов</p>
          <p class="empty-text">Загрузите файл, чтобы увидеть его здесь.</p>
          <a href="/upload" class="btn btn-primary">⬆️ Загрузить файл</a>
        </div>
      `;
    } else {
      filesList.innerHTML = '';
      files.forEach(file => {
        const card = createFileCard(file);
        filesList.appendChild(card);
      });

      // Attach event listeners
      document.querySelectorAll('.copy-link-btn').forEach(btn => {
        btn.addEventListener('click', handleCopyLink);
      });
      document.querySelectorAll('.delete-file-btn').forEach(btn => {
        btn.addEventListener('click', handleDeleteFile);
      });
    }

    // Update quota information
    if (quotaData) {
      updateQuotaDisplay(quotaData);
    } else {
      updateQuotaDisplay(null);
    }

    const quotaFiles = document.querySelector('#quotaFiles');
    if (quotaFiles) {
      quotaFiles.textContent = formatFilesCount(files ? files.length : 0);
    }
  } catch (error) {
    showAlert(error.message || 'Ошибка загрузки данных', 'error');
    filesList.innerHTML = `
      <div class="empty-state">
        <p class="empty-title">Ошибка загрузки</p>
        <p class="empty-text">${escapeHtml(error.message)}</p>
        <button class="btn btn-secondary" onclick="(async () => { try { await loadFiles(); } catch(e) { console.error(e); } })()">Попробовать снова</button>
      </div>
    `;
  }
}

function updateFileCount(count) {
  const quotaFiles = document.querySelector('#quotaFiles');
  if (quotaFiles) {
    quotaFiles.textContent = formatFilesCount(count);
  }
}

function updateQuotaDisplay(quotaData) {
  const quotaUsed = document.querySelector('#quotaUsed');
  const quotaTotal = document.querySelector('#quotaTotal');
  if (!quotaUsed && !quotaTotal) return;

  if (quotaData) {
    const usedBytes = parseInt(quotaData.used_storage_bytes) || 0;
    const maxBytes = parseInt(quotaData.max_storage_bytes) || 0;
    if (quotaUsed) quotaUsed.textContent = formatBytes(usedBytes);
    if (quotaTotal) quotaTotal.textContent = formatBytes(maxBytes);
  } else {
    if (quotaUsed) quotaUsed.textContent = '0';
    if (quotaTotal) quotaTotal.textContent = '0';
  }
}

function showFileCardError(card, message) {
  if (!card) return;
  const errorNode = card.querySelector('.file-card-error');
  if (!errorNode) return;
  errorNode.textContent = message;
  errorNode.classList.add('file-card-error--visible');
}

function clearFileCardError(card) {
  if (!card) return;
  const errorNode = card.querySelector('.file-card-error');
  if (!errorNode) return;
  errorNode.textContent = '';
  errorNode.classList.remove('file-card-error--visible');
}

async function handleCopyLink(event) {
  const button = event.currentTarget;
  const fileId = button.dataset.fileId;
  const originalText = button.textContent;
  
  const card = button.closest('.file-card');
  clearFileCardError(card);

  try {
    const response = await apiFetch(`/api/v1/files/link/${fileId}`);
    let link = response.download_link;
    
    // If link doesn't start with http, prepend origin
    if (!link.startsWith('http')) {
      link = window.location.origin + link;
    }
    
    await navigator.clipboard.writeText(link);
    button.textContent = '✅ Скопировано!';
    
    setTimeout(() => {
      button.textContent = originalText;
    }, 2000);
    
    showAlert('Ссылка скопирована в буфер обмена', 'success');
  } catch (error) {
    const message = error.details || error.message || 'Ошибка копирования ссылки';
    showAlert(message, 'error');
    showFileCardError(card, message);
  }
}

async function handleDeleteFile(event) {
  const button = event.currentTarget;
  const fileId = button.dataset.fileId;
  const card = button.closest('.file-card');
  const fileName = card.querySelector('.file-name').textContent;
  
  const confirmed = confirm(`Вы уверены, что хотите удалить файл "${fileName}"?\nЭто действие нельзя будет отменить.`);
  
  if (!confirmed) return;

  button.disabled = true;
  const originalText = button.textContent;
  button.textContent = '⏳ Удаление...';

  try {
    await apiFetch(`/api/v1/files/${fileId}`, { method: 'DELETE' });
    card.remove();
    showAlert('Файл удалён', 'success');
    
    // Update file count
    const filesList = document.querySelector('#filesList');
    const fileCards = filesList.querySelectorAll('.file-card');
    updateFileCount(fileCards.length);

    // Refresh quota after delete
    const quotaData = await loadQuotaData();
    updateQuotaDisplay(quotaData);
    
    // If no files left, show empty state
    if (fileCards.length === 0) {
      filesList.innerHTML = `
        <div class="empty-state">
          <p class="empty-title">У вас пока нет файлов</p>
          <p class="empty-text">Загрузите файл, чтобы увидеть его здесь.</p>
          <a href="/upload" class="btn btn-primary">⬆️ Загрузить файл</a>
        </div>
      `;
    }
  } catch (error) {
    showAlert(error.message || 'Ошибка удаления файла', 'error');
    button.disabled = false;
    button.textContent = originalText;
  }
}

async function initFilesPage() {
  const filesList = document.querySelector('#filesList');
  if (!filesList) return;
  try {
    await loadFiles();
  } catch (error) {
    console.error('Error loading files page:', error);
  }
}

// ============================================
// Upload Functions
// ============================================

function validateUploadForm(file, ttl, maxDownloads, password) {
  const errors = [];
  
  if (!file) {
    errors.push('Выберите файл для загрузки');
  } else if (file.size > 100 * 1024 * 1024) {
    errors.push('Размер файла не должен превышать 100 МБ');
  }
  
  // TTL validation in days (1-14)
  if (isNaN(ttl) || ttl < 1 || ttl > 14) {
    errors.push('Срок жизни должен быть от 1 до 14 дней');
  }
  
  if (isNaN(maxDownloads) || maxDownloads < 1 || maxDownloads > 1000) {
    errors.push('Максимум скачиваний должен быть от 1 до 1000');
  }
  
  if (password && (password.length < 6 || password.length > 64)) {
    errors.push('Длина пароля должна быть от 6 до 64 символов');
  }
  
  return errors;
}

function showValidationSummary(message) {
  const summary = document.querySelector('#validationSummary');
  if (summary) {
    summary.querySelector('.validation-summary__text').textContent = message;
    summary.style.display = 'block';
  }
}

function hideValidationSummary() {
  const summary = document.querySelector('#validationSummary');
  if (summary) {
    summary.style.display = 'none';
  }
}

function clearAllFieldErrors(form) {
  form.querySelectorAll('.field-error').forEach(el => el.classList.remove('field-error'));
  form.querySelectorAll('.field-error-message').forEach(el => el.remove());
}

function clearFieldError(fieldId) {
  const field = document.querySelector(`#${fieldId}`);
  if (field) {
    field.classList.remove('field-error');
    const errorMsg = field.parentElement.querySelector('.field-error-message');
    if (errorMsg) errorMsg.remove();
  }
}

function showFieldError(fieldId, message) {
  const field = document.querySelector(`#${fieldId}`);
  if (field) {
    field.classList.add('field-error');
    let existingMsg = field.parentElement.querySelector('.field-error-message');
    if (existingMsg) {
      existingMsg.textContent = existingMsg.textContent + '; ' + message;
    } else {
      const errorMsg = document.createElement('p');
      errorMsg.className = 'field-error-message';
      errorMsg.textContent = message;
      field.parentElement.appendChild(errorMsg);
    }
  }
}

function initUploadPage() {
  const uploadForm = document.querySelector('#uploadForm');
  if (!uploadForm) return;

  const dropzone = document.querySelector('#dropzone');
  const fileInput = document.querySelector('#fileInput');
  const fileNameDisplay = document.querySelector('#fileNameDisplay');

  // Clear errors when user starts typing
  uploadForm.querySelectorAll('input').forEach(input => {
    input.addEventListener('input', () => {
      clearFieldError(input.id);
      hideValidationSummary();
    });
  });
  
  // Clear errors when user selects a file
  fileInput.addEventListener('change', () => {
    clearFieldError('dropzone');
    hideValidationSummary();
    updateFileDisplay();
  });

  // Drag & drop
  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });
  dropzone.addEventListener('dragleave', () => {
    dropzone.classList.remove('dragover');
  });
  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files.length > 0) {
      fileInput.files = e.dataTransfer.files;
      updateFileDisplay();
    }
  });

  // File input change
  fileInput.addEventListener('change', updateFileDisplay);

  function updateFileDisplay() {
    const file = fileInput.files[0];
    const span = dropzone.querySelector('span');
    
    if (file) {
      const size = formatBytes(file.size);
      span.innerHTML = `✅ Файл выбран: <strong>${escapeHtml(file.name)}</strong> (${size})`;
      hideValidationSummary();
      clearFieldError('fileInput');
      clearFieldError('dropzone'); // Also clear dropzone error as file-related validation
    } else {
      span.innerHTML = '📁 Перетащите файл или нажмите, чтобы выбрать';
    }
  }

  uploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const file = fileInput.files[0];
    const ttl = parseInt(document.querySelector('#ttl').value);
    const maxDownloads = parseInt(document.querySelector('#maxDownloads').value);
    const password = document.querySelector('#password').value || null;

    hideValidationSummary();
    clearAllFieldErrors(uploadForm);

    // Validate form
    const errors = validateUploadForm(file, ttl, maxDownloads, password);
    if (errors.length > 0) {
      showValidationSummary('Пожалуйста, исправьте ошибки в форме');
      if (!file) showFieldError('dropzone', 'Выберите файл для загрузки');
      else if (file.size > 100 * 1024 * 1024) showFieldError('dropzone', 'Размер файла не должен превышать 100 МБ');
      if (isNaN(ttl) || ttl < 1 || ttl > 14) showFieldError('ttl', 'Срок жизни должен быть от 1 до 14 дней');
      if (isNaN(maxDownloads) || maxDownloads < 1 || maxDownloads > 1000) showFieldError('maxDownloads', 'Максимум скачиваний должен быть от 1 до 1000');
      if (password && (password.length < 6 || password.length > 64)) showFieldError('password', 'Длина пароля должна быть от 6 до 64 символов');
      return;
    }

    const ttlMinutes = ttl * 24 * 60; // Convert days to minutes
    const button = uploadForm.querySelector('button[type="submit"]');
    const originalText = button.textContent;
    button.disabled = true;

    try {
      const formData = new FormData();
      formData.append('file', file);
      
      // Create payload as JSON Blob for proper multipart/form-data encoding
      const payloadBlob = new Blob([JSON.stringify({
        ttl_minutes: ttlMinutes,
        max_downloads: maxDownloads,
        password: password
      })], { type: 'application/json' });
      formData.append('payload', payloadBlob);

      const progressContainer = uploadForm.querySelector('.progress-container') || createProgressBar(uploadForm);
      button.textContent = '⏳ Загрузка...';
      progressContainer.querySelector('.progress-bar').style.width = '15%';
      progressContainer.querySelector('.progress-text').textContent = 'Начинаем загрузку...';

      try {
        await apiFetch('/api/v1/files', {
          method: 'POST',
          body: formData,
          credentials: 'same-origin'
        });

        showAlert('Файл успешно загружен!', 'success');
        fileInput.value = '';
        document.querySelector('#ttl').value = '1';
        document.querySelector('#maxDownloads').value = '10';
        document.querySelector('#password').value = '';
        hideValidationSummary();
        progressContainer.querySelector('.progress-bar').style.width = '100%';
        progressContainer.querySelector('.progress-text').textContent = 'Загрузка завершена';
        button.disabled = false;
        button.textContent = originalText;
        updateFileDisplay();

        setTimeout(() => {
          window.location.href = '/files';
        }, 1500);
      } catch (error) {
        const message = error.details || error.message || 'Ошибка загрузки файла';
        showAlert(message, 'error');
        button.disabled = false;
        button.textContent = originalText;
        progressContainer.remove();
      }

    } catch (error) {
      showAlert(error.message || 'Ошибка при подготовке к загрузке', 'error');
      button.disabled = false;
      button.textContent = originalText;
    }
  });
}

function createProgressBar(form) {
  const container = document.createElement('div');
  container.className = 'progress-container';
  container.innerHTML = `
    <div class="progress-wrapper">
      <div class="progress-bar" style="width: 0%"></div>
    </div>
    <p class="progress-text">0%</p>
  `;
  form.insertBefore(container, form.querySelector('button[type="submit"]'));
  return container;
}

// ============================================
// Download Functions
// ============================================

function initDownloadPage() {
  const downloadForm = document.querySelector('#downloadForm');
  if (!downloadForm) return;

  const fileId = document.querySelector('#fileId').value;
  const button = downloadForm.querySelector('button[type="submit"]');
  const originalText = button ? button.textContent : 'Скачать';
  const fileInfoDiv = document.getElementById('fileInfo');
  
  button.disabled = true;
  button.textContent = '⏳ Проверяю файл...';
  fileInfoDiv.innerHTML = '<p>Загрузка информации о файле...</p>';

  apiFetch(`/api/v1/files/${encodeURIComponent(fileId)}`, { method: 'GET' })
    .then((fileData) => {
      button.disabled = false;
      button.textContent = originalText;
      
      // Display file information
      const fileStatus = getFileStatus(fileData);
      const statusClass = fileStatus.isActive ? 'status-active' : 'status-inactive';
      
      fileInfoDiv.innerHTML = `
        <div class="file-info-header">
          <h3 class="file-name">${escapeHtml(fileData.file_name)}</h3>
          <span class="file-status ${statusClass}">${fileStatus.status}</span>
        </div>
        <div class="file-meta">
          <p>Размер: <span>${formatBytes(fileData.file_size_bytes)}</span></p>
          <p>Загружено: <span>${formatDate(fileData.upload_date)}</span></p>
          <p>Истекает: <span>${formatDate(fileData.expire_date)}</span></p>
          <p>Скачиваний: <span>${fileData.times_downloaded}/${fileData.max_downloads}</span></p>
          <p>Защита: <span>${fileData.has_password ? '🔒 Защищён' : 'Открыт'}</span></p>
        </div>
      `;

      downloadForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const password = document.querySelector('#downloadPassword').value || null;
            button.disabled = true;
            button.textContent = '⏳ Скачивание...';
            
            // Show progress bar
            const progressContainer = document.getElementById('downloadProgress');
            const progressBar = progressContainer.querySelector('.progress-bar');
            const progressText = progressContainer.querySelector('.progress-text');
            progressContainer.style.display = 'block';
            progressBar.style.width = '0%';
            progressText.textContent = '0%';

            try {
                // Use fetch with ReadableStream to track download progress and handle errors properly
                const response = await fetch('/api/v1/files/download', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/octet-stream'
                    },
                    body: JSON.stringify({
                        file_id: fileId,
                        password: password
                    })
                });

                if (!response.ok) {
                    const errorData = await response.json().catch(() => null);
                    const errorMessage = errorData?.message || response.statusText;
                    throw new Error(errorMessage);
                }

                const contentLength = response.headers.get('content-length');
                const contentType = response.headers.get('content-type');
                const contentDisposition = response.headers.get('content-disposition') || '';
                let filename = 'download';

                // Extract filename from content-disposition header
                if (contentDisposition) {
                    // Try filename*=UTF-8''encoded first (RFC 5987)
                    let filenameMatch = contentDisposition.match(/filename\*=UTF-8''([^;\r\n]+)/i);
                    if (filenameMatch && filenameMatch[1]) {
                        try {
                            filename = decodeURIComponent(filenameMatch[1].trim());
                        } catch (e) {
                            // If decoding fails, use the encoded version
                            filename = filenameMatch[1].trim();
                        }
                    } else {
                        // Fallback to filename="quoted" (RFC 2183)
                        filenameMatch = contentDisposition.match(/filename="([^"]+)"/i);
                        if (filenameMatch && filenameMatch[1]) {
                            filename = filenameMatch[1].trim();
                        } else {
                            // Last resort: filename without quotes
                            filenameMatch = contentDisposition.match(/filename=([^;\r\n]+)/i);
                            if (filenameMatch && filenameMatch[1]) {
                                filename = filenameMatch[1].trim();
                            }
                        }
                    }
                }

                // Read response body as stream and track progress
                const reader = response.body.getReader();
                const chunks = [];
                let loaded = 0;

                while (true) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    chunks.push(value);
                    loaded += value.length;
                    const percent = contentLength ? Math.round((loaded / contentLength) * 100) : 0;
                    progressBar.style.width = percent + '%';
                    progressText.textContent = percent + '%';
                }

                const blob = new Blob(chunks, { type: contentType });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = filename;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);

                showAlert('Файл скачан успешно! Перенаправление...', 'success');
                button.textContent = '✅ Готово';
                progressBar.style.width = '100%';
                progressText.textContent = '100%';

                setTimeout(() => {
                    window.location.href = '/files';
                }, 1200);
            } catch (error) {
                showAlert(error.message || 'Ошибка скачивания файла', 'error');
                button.disabled = false;
                button.textContent = originalText;
                progressContainer.style.display = 'none';
            }
        });
})
.catch(error => {
  if (error.status === 404) {
    const params = new URLSearchParams({
      header: 'Файл не найден',
      message: 'Запрашиваемый файл не найден или был удалён.'
    });
    window.location.href = `/notfound?${params.toString()}`;
    return;
  }
  showAlert(error.message || 'Ошибка проверки файла', 'error');
  if (button) {
    button.disabled = false;
    button.textContent = originalText;
  }
});
}

// ============================================
// Page Initialization
// ============================================

document.addEventListener('DOMContentLoaded', async () => {
  await initNavbar();
  initProfileData();
  await initFilesPage();
  initUploadPage();
  initDownloadPage();
});

window.showAlert = showAlert;
window.apiFetch = apiFetch;
window.initNavbar = initNavbar;
window.initProfileData = initProfileData;
window.loadFiles = loadFiles;
window.updateFileCount = updateFileCount;