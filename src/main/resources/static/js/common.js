// Společné pro všechny stránky za přihlášením: ověření tokenu, API klient, horní lišta.

const token = localStorage.getItem('access_token');
if (!token) {
  window.location.href = '/login.html';
}

const ROLE_LABELS = {
  ADMIN: 'Správce',
  STAFF: 'Obsluha pultu'
};

function authHeaders() {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  };
}

function logout() {
  localStorage.removeItem('access_token');
  localStorage.removeItem('user_info');
  window.location.href = '/login.html';
}

async function apiRequest(url, options = {}) {
  const headers = Object.assign({}, authHeaders(), options.headers || {});
  const res = await fetch(url, Object.assign({}, options, { headers }));

  if (res.status === 401) {
    logout();
    return null;
  }
  return res;
}

// Všechna data z API se do HTML vkládají escapovaná
function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatPrice(value) {
  return Number(value).toLocaleString('cs-CZ', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function formatDate(isoDateTime) {
  return new Date(isoDateTime).toLocaleDateString('cs-CZ');
}

function formatMoney(value) {
  return `${formatPrice(value)} Kč`;
}

function pad2(number) {
  return String(number).padStart(2, '0');
}

// Čas z API je místní čas bez zóny („2027-01-23T08:30:00“)
function formatTime(isoDateTime) {
  const date = new Date(isoDateTime);
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

// Formát z pultu: „So 23. 1. 08:30“
function formatDateTime(isoDateTime) {
  const date = new Date(isoDateTime);
  return `${WEEKDAYS[date.getDay()]} ${date.getDate()}. ${date.getMonth() + 1}. ${formatTime(isoDateTime)}`;
}

function isSameDay(first, second) {
  return first.getFullYear() === second.getFullYear()
    && first.getMonth() === second.getMonth()
    && first.getDate() === second.getDate();
}

// Hodnota pro <input type="datetime-local">
function toDateTimeInputValue(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}T${pad2(date.getHours())}:${pad2(date.getMinutes())}`;
}

function toApiDateTime(inputValue) {
  return inputValue.length === 16 ? `${inputValue}:00` : inputValue;
}

// Počet započatých dní po termínu (stejné pravidlo jako penále na serveru)
function startedDaysLate(plannedEnd, now = new Date()) {
  const lateMs = now - new Date(plannedEnd);
  return lateMs > 0 ? Math.ceil(lateMs / 86400000) : 0;
}

function daysWord(count) {
  if (count === 1) return 'den';
  if (count >= 2 && count <= 4) return 'dny';
  return 'dní';
}

async function readErrorDetail(res, fallback) {
  try {
    const data = await res.json();
    return data.detail || fallback;
  } catch {
    return fallback;
  }
}

// Dialog: otevření, zavření tlačítky, klepnutím mimo a klávesou Esc
function setupDialog(modal) {
  const close = () => modal.classList.remove('active');
  modal.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', close));
  modal.addEventListener('click', event => {
    if (event.target === modal) close();
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && modal.classList.contains('active')) close();
  });
  return {
    open: () => modal.classList.add('active'),
    close,
    alert: message => {
      const box = modal.querySelector('.alert');
      box.textContent = message;
      box.style.display = message ? 'block' : 'none';
    }
  };
}

async function loadCustomerOptions(select) {
  const res = await apiRequest('/api/customers');
  if (!res || !res.ok) return [];
  const customers = await res.json();
  select.innerHTML = '<option value="">Vyberte zákazníka</option>' + customers
    .map(c => `<option value="${c.id}">${escapeHtml(c.last_name)} ${escapeHtml(c.first_name)} · ${escapeHtml(c.phone)}</option>`)
    .join('');
  return customers;
}

const WEEKDAYS = ['Ne', 'Po', 'Út', 'St', 'Čt', 'Pá', 'So'];

// Formát z pultu: „Po 20. 10. 2026 · 08:12“
function formatClock(date) {
  const time = date.toLocaleTimeString('cs-CZ', { hour: '2-digit', minute: '2-digit' });
  return `${WEEKDAYS[date.getDay()]} ${date.getDate()}. ${date.getMonth() + 1}. ${date.getFullYear()} · ${time}`;
}

function startTopbarClock() {
  const clock = document.getElementById('topbarClock');
  if (!clock) return;
  const tick = () => { clock.textContent = formatClock(new Date()); };
  tick();
  setInterval(tick, 15000);
}

async function loadTopbarUser() {
  const res = await apiRequest('/api/auth/me');
  if (res && res.ok) {
    const user = await res.json();
    const userName = document.getElementById('userNameDisplay');
    userName.textContent = user.full_name;
    userName.title = ROLE_LABELS[user.role] || user.role;
  }
}

// Na telefonu se navigace schová pod tlačítko Menu
function setupMobileMenu() {
  const toggle = document.getElementById('menuToggle');
  const menu = document.getElementById('topbarMenu');
  if (!toggle || !menu) return;

  const setOpen = open => {
    menu.classList.toggle('is-open', open);
    toggle.setAttribute('aria-expanded', String(open));
  };

  toggle.addEventListener('click', () => setOpen(!menu.classList.contains('is-open')));
  menu.addEventListener('click', event => {
    if (event.target.closest('a')) setOpen(false);
  });
  document.addEventListener('click', event => {
    if (!event.target.closest('.topbar')) setOpen(false);
  });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && menu.classList.contains('is-open')) {
      setOpen(false);
      toggle.focus();
    }
  });
  window.matchMedia('(min-width: 751px)').addEventListener('change', event => {
    if (event.matches) setOpen(false);
  });
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('logoutBtn').addEventListener('click', logout);
  setupMobileMenu();
  startTopbarClock();
  loadTopbarUser();
});
