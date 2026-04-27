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

// ============================================
// File Management Functions
// ============================================

function formatDate(isoString) {
  if (!isoString) return 'Неизвестно';
  const date = new Date(isoString);
  return date.toLocaleDateString('ru-RU', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatBytes(bytes) {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'КБ', 'МБ', 'ГБ'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return (bytes / Math.pow(k, i)).toFixed(1) + ' ' + sizes[i];
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
    const files = await apiFetch('/api/v1/files/my?page=0&page_size=100');
    
    if (!files || files.length === 0) {
      filesList.innerHTML = `
        <div class="empty-state">
          <p class="empty-title">У вас пока нет файлов</p>
          <p class="empty-text">Загрузите файл, чтобы увидеть его здесь.</p>
          <a href="/upload" class="btn btn-secondary">Загрузить файл</a>
        </div>
      `;
      return;
    }

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

    if (quotaMeta) {
      quotaMeta.innerHTML = `
        <span id="quotaFiles">${files.length} файлов</span>
      `;
    }
  } catch (error) {
    showAlert(error.message || 'Ошибка загрузки файлов', 'error');
    filesList.innerHTML = `
      <div class="empty-state">
        <p class="empty-title">Ошибка загрузки</p>
        <p class="empty-text">${escapeHtml(error.message)}</p>
        <button class="btn btn-secondary" onclick="loadFiles()">Попробовать снова</button>
      </div>
    `;
  }
}

function updateFileCount(count) {
  const quotaFiles = document.querySelector('#quotaFiles');
  if (quotaFiles) {
    quotaFiles.textContent = `${count} файлов`;
  }
}

async function handleCopyLink(event) {
  const button = event.currentTarget;
  const fileId = button.dataset.fileId;
  const originalText = button.textContent;
  
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
    showAlert(error.message || 'Ошибка копирования ссылки', 'error');
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
    
    // If no files left, show empty state
    if (fileCards.length === 0) {
      filesList.innerHTML = `
        <div class="empty-state">
          <p class="empty-title">У вас пока нет файлов</p>
          <p class="empty-text">Загрузите файл, чтобы увидеть его здесь.</p>
          <a href="/upload" class="btn btn-secondary">Загрузить файл</a>
        </div>
      `;
    }
  } catch (error) {
    showAlert(error.message || 'Ошибка удаления файла', 'error');
    button.disabled = false;
    button.textContent = originalText;
  }
}

function initFilesPage() {
  const filesList = document.querySelector('#filesList');
  if (!filesList) return;
  loadFiles();
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

      // Create XMLHttpRequest for progress tracking
      const xhr = new XMLHttpRequest();
      const progressContainer = uploadForm.querySelector('.progress-container') || createProgressBar(uploadForm);

      xhr.upload.addEventListener('progress', (e) => {
        if (e.lengthComputable) {
          const percentComplete = (e.loaded / e.total) * 100;
          const progressBar = progressContainer.querySelector('.progress-bar');
          const progressText = progressContainer.querySelector('.progress-text');
          progressBar.style.width = percentComplete + '%';
          progressText.textContent = `${Math.round(percentComplete)}%`;
        }
      });

       xhr.addEventListener('load', async () => {
        if (xhr.status === 201 || xhr.status === 200) {
          try {
            const response = JSON.parse(xhr.responseText);
            showAlert('Файл успешно загружен!', 'success');
            fileInput.value = '';
            document.querySelector('#ttl').value = '1';
            document.querySelector('#maxDownloads').value = '10';
            document.querySelector('#password').value = '';
            hideValidationSummary();
            progressContainer.remove();
            button.disabled = false;
            button.textContent = originalText;
            updateFileDisplay();
            
            // Redirect to files page
            setTimeout(() => {
              window.location.href = '/files';
            }, 1500);
          } catch (e) {
            showAlert('Ошибка обработки ответа сервера', 'error');
            button.disabled = false;
            button.textContent = originalText;
          }
        } else {
          try {
            const error = JSON.parse(xhr.responseText);
            // Check for validation errors (fieldErrors array)
            if (error.fieldErrors && Array.isArray(error.fieldErrors)) {
              clearAllFieldErrors(uploadForm);
              const fieldErrorsMap = {};
              error.fieldErrors.forEach(fieldError => {
                const fieldName = fieldError.field;
                const message = fieldError.message;
                const fieldIdMap = {
                  'fileName': 'dropzone',
                  'fileSize': 'dropzone',
                  'contentType': 'dropzone',
                  'ttlMinutes': 'ttl',
                  'maxDownloads': 'maxDownloads',
                  'password': 'password'
                };
                const elementId = fieldIdMap[fieldName] || fieldName;
                if (!fieldErrorsMap[elementId]) {
                  fieldErrorsMap[elementId] = [];
                }
                fieldErrorsMap[elementId].push(message);
              });
              
               // Show field-specific errors
               Object.entries(fieldErrorsMap).forEach(([fieldId, messages]) => {
                 showFieldError(fieldId, messages.join('; '));
               });
               
               // Show general summary
               showValidationSummary('Пожалуйста, исправьте ошибки в форме');
            } else {
              showAlert(error.message || xhr.statusText || 'Ошибка загрузки файла', 'error');
            }
          } catch {
            showAlert('Ошибка загрузки файла: ' + xhr.statusText, 'error');
          }
          button.disabled = false;
          button.textContent = originalText;
        }
       });

       xhr.addEventListener('error', () => {
        showAlert('Ошибка сети при загрузке файла', 'error');
        button.disabled = false;
        button.textContent = originalText;
      });

      xhr.open('POST', '/api/v1/files');
      xhr.setRequestHeader('Accept', 'application/json');
      xhr.withCredentials = true;
      
      button.textContent = '⏳ Загрузка...';
      xhr.send(formData);
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

  downloadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const fileId = document.querySelector('#fileId').value;
    const password = document.querySelector('#downloadPassword').value || null;
    
    const button = downloadForm.querySelector('button[type="submit"]');
    const originalText = button.textContent;
    button.disabled = true;
    button.textContent = '⏳ Скачивание...';

    try {
      // Single fetch request to download file as blob
      const fetchResponse = await fetch('/api/v1/files/download', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/octet-stream'
        },
        credentials: 'same-origin',
        body: JSON.stringify({
          file_id: fileId,
          password: password
        })
      });

      if (!fetchResponse.ok) {
        const error = await fetchResponse.json().catch(() => null);
        throw new Error(error?.message || fetchResponse.statusText);
      }

      const blob = await fetchResponse.blob();
      const contentDisposition = fetchResponse.headers.get('content-disposition');
      let filename = 'download';

      if (contentDisposition) {
        // Try filename*=UTF-8''encoded first
        let filenameMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/);
        if (filenameMatch && filenameMatch[1]) {
          filename = decodeURIComponent(filenameMatch[1]);
        } else {
          // Fallback to filename="quoted"
          filenameMatch = contentDisposition.match(/filename="([^"]+)"/);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1];
          }
        }
      }

      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);

      showAlert('Файл скачан успешно!', 'success');
      button.disabled = false;
      button.textContent = originalText;
    } catch (error) {
      showAlert(error.message || 'Ошибка скачивания файла', 'error');
      button.disabled = false;
      button.textContent = originalText;
    }
  });
}

// ============================================
// Page Initialization
// ============================================

document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  initProfileData();
  initReportPage();
  initFilesPage();
  initUploadPage();
  initDownloadPage();
});

window.showAlert = showAlert;
window.apiFetch = apiFetch;
window.initNavbar = initNavbar;
window.initProfileData = initProfileData;
window.loadFiles = loadFiles;
window.updateFileCount = updateFileCount;