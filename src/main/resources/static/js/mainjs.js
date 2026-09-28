'use strict';
// sessionStorage is used for this classroom demo, scoped to this browser tab.
const tokenKey = 'jwt-nimbus-token';
const feedback = document.querySelector('#feedback');
function message(text) { feedback.textContent = text; }
async function api(path, method = 'GET', body, authenticated = false) {
  const headers = { 'Accept': 'application/json' };
  if (body) headers['Content-Type'] = 'application/json';
  if (authenticated) {
    const token = sessionStorage.getItem(tokenKey);
    if (!token) { window.location.replace('/login'); throw new Error('Bạn cần đăng nhập.'); }
    headers.Authorization = 'Bearer ' + token;
  }
  const response = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (authenticated && (response.status === 401 || response.status === 403)) {
      sessionStorage.removeItem(tokenKey);
      window.location.replace('/login');
    }
    throw new Error(data.detail || 'Yêu cầu thất bại (HTTP ' + response.status + ').');
  }
  return data;
}
function bindForm(id, action) {
  const form = document.getElementById(id);
  if (!form) return;
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const button = form.querySelector('button'); button.disabled = true; message('Đang xử lý…');
    try { await action(Object.fromEntries(new FormData(form))); }
    catch (error) { message(error.message); }
    finally { button.disabled = false; }
  });
}
bindForm('signupForm', async data => {
  await api('/auth/signup', 'POST', data);
  document.querySelector('#loginForm [name=email]').value = data.email;
  document.querySelector('#signupForm').reset();
  message('Đăng ký thành công. Bạn có thể đăng nhập.');
});
bindForm('loginForm', async data => {
  const result = await api('/auth/login', 'POST', data);
  sessionStorage.setItem(tokenKey, result.token);
  window.location.assign('/user/profile');
});
if (document.querySelector('#profile')) {
  api('/users/me', 'GET', undefined, true).then(user => {
    document.querySelector('#fullName').textContent = user.fullName;
    document.querySelector('#email').textContent = user.email;
    document.querySelector('#profileJson').textContent = JSON.stringify(user, null, 2);
  }).catch(error => message(error.message));
  document.querySelector('#loadUsers').addEventListener('click', async () => {
    try {
      const users = await api('/users/', 'GET', undefined, true);
      document.querySelector('#usersJson').textContent = JSON.stringify(users, null, 2);
      document.querySelector('#usersPanel').hidden = false;
    } catch (error) { message(error.message); }
  });
  document.querySelector('#logout').addEventListener('click', () => {
    sessionStorage.removeItem(tokenKey);
    window.location.replace('/login');
  });
}
