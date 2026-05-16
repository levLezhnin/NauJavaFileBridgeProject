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

function validateRegisterForm(username, email, password) {
  const errors = [];

  // Username validation: 5-255 chars, letters (including Cyrillic), digits, dots, dashes, underscores
  if (!username || username.trim().length === 0) {
    errors.push('Имя пользователя не может быть пустым');
  } else if (username.length < 5 || username.length > 255) {
    errors.push('Имя пользователя должно иметь длину от 5 до 255 символов');
  } else if (!/^[a-zA-Zа-яА-ЯЁё\d\-_\.]+$/.test(username)) {
    errors.push('Имя пользователя должно содержать только буквы, цифры, точки, тире и нижние подчёркивания');
  }

  // Email validation
  if (!email || email.trim().length === 0) {
    errors.push('Email не может быть пустым');
  } else {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      errors.push('Введите корректный email адрес');
    }
  }

  // Password validation: 8-255 chars, at least one lowercase, one uppercase, one digit, one special char
  if (!password || password.trim().length === 0) {
    errors.push('Пароль не может быть пустым');
  } else if (password.length < 8 || password.length > 255) {
    errors.push('Длина пароля должна быть от 8 до 255 символов');
  } else if (!/(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[?!@#$%^&*()+_\-])/.test(password)) {
    errors.push('Пароль должен содержать латинские буквы верхнего и нижнего регистров, цифру и специальный символ');
  }

  return errors;
}

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

function prepareAuthForm(formId, url) {
  const form = document.querySelector(`#${formId}`);
  if (!form) return;

  // Clear errors when user starts typing
  form.querySelectorAll('input').forEach(input => {
    input.addEventListener('input', () => {
      clearFieldError(input.id);
      hideValidationSummary();
    });
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();

    const formData = new FormData(form);
    const payload = {};
    formData.forEach((value, key) => (payload[key] = value));

    // Client-side validation for registration form
    if (formId === 'registerForm') {
      const username = payload.username?.trim() || '';
      const email = payload.email?.trim() || '';
      const password = payload.password || '';

      hideValidationSummary();
      clearAllFieldErrors(form);

      const errors = validateRegisterForm(username, email, password);
      if (errors.length > 0) {
        showValidationSummary('Пожалуйста, исправьте ошибки в форме', errors);
        // Highlight specific fields
        if (!username || username.length < 5 || username.length > 255 || !/^[a-zA-Zа-яА-ЯЁё\d\-_\.]+$/.test(username)) {
          showFieldError('registerUsername', 'Имя пользователя должно иметь длину от 5 до 255 символов и содержать только буквы верхнего и нижнего регистров, цифры, точки, тире и нижние подчёркивания');
        }
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
          showFieldError('registerEmail', 'Введите корректный email адрес');
        }
        if (!password || password.length < 8 || password.length > 255 || !/(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[?!@#$%^&*()+_\-])/.test(password)) {
          showFieldError('registerPassword', 'Пароль должен содержать латинские буквы верхнего и нижнего регистров, цифру и специальный символ');
        }
        return;
      }
    }

    try {
      const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
      const response = await fetch(url, {
        method: 'POST',
        headers,
        credentials: 'same-origin',
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        let error = null;
        try {
          const text = await response.text();
          error = JSON.parse(text);
        } catch {
          // Response is not JSON or parsing failed
          throw new Error('Ошибка сервера');
        }

        // Handle field-specific errors from server
        if (error && error.fieldErrors && Array.isArray(error.fieldErrors)) {
          clearAllFieldErrors(form);
          const fieldErrorsMap = {};
          error.fieldErrors.forEach(fieldError => {
            const fieldName = fieldError.field;
            const message = fieldError.message;
            const fieldIdMap = {
              'username': 'registerUsername',
              'email': 'registerEmail',
              'password': 'registerPassword'
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
          return;
        } else {
          throw new Error(error?.message || 'Ошибка сервера');
        }
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
