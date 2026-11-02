// Stránka Rezervace – rezervace typu, výdej s přiřazením konkrétních kusů a rušení (vyžaduje js/common.js).

const RESERVATION_STATUS = {
  ACTIVE: { text: 'Čeká na výdej', badge: 'reserved' },
  FULFILLED: { text: 'Vydáno', badge: 'free' },
  EXPIRED: { text: 'Propadla', badge: 'late' },
  CANCELLED: { text: 'Zrušena', badge: 'retired' }
};

let reservations = [];
let categories = [];
let equipment = [];
let pickupReservation = null;
const pickupSelection = new Set();

const todayTableBody = document.getElementById('todayTableBody');
const upcomingTableBody = document.getElementById('upcomingTableBody');
const closedTableBody = document.getElementById('closedTableBody');

const reservationDialog = setupDialog(document.getElementById('reservationModal'));
const reservationForm = document.getElementById('reservationForm');
const reservationCustomer = document.getElementById('reservationCustomer');
const reservationCategory = document.getElementById('reservationCategory');
const reservationSize = document.getElementById('reservationSize');
const reservationQuantity = document.getElementById('reservationQuantity');
const reservationStart = document.getElementById('reservationStart');
const reservationEnd = document.getElementById('reservationEnd');
const reservationHoldHint = document.getElementById('reservationHoldHint');
const reservationSubmitBtn = document.getElementById('reservationSubmitBtn');

const pickupDialog = setupDialog(document.getElementById('pickupModal'));
const pickupForm = document.getElementById('pickupForm');
const pickupSummary = document.getElementById('pickupSummary');
const pickupItemPicker = document.getElementById('pickupItemPicker');
const pickupSubmitBtn = document.getElementById('pickupSubmitBtn');

document.addEventListener('DOMContentLoaded', async () => {
  document.getElementById('todayLabel').textContent = formatDateTime(new Date().toISOString()).replace(/ \d\d:\d\d$/, '');
  setupReservationEvents();
  await loadCatalog();
  await loadReservations();
});

async function loadCatalog() {
  const [categoriesRes, equipmentRes] = await Promise.all([fetch('/api/categories'), fetch('/api/equipment')]);
  categories = categoriesRes.ok ? await categoriesRes.json() : [];
  equipment = equipmentRes.ok ? await equipmentRes.json() : [];
}

async function loadReservations() {
  const res = await apiRequest('/api/reservations');
  if (!res) return;
  if (!res.ok) {
    const message = '<tr><td colspan="6" class="table-message">Rezervace se nepodařilo načíst.</td></tr>';
    todayTableBody.innerHTML = upcomingTableBody.innerHTML = message;
    return;
  }
  reservations = await res.json();
  renderReservations();
}

function typeLabel(reservation) {
  return `${escapeHtml(reservation.category_name)} <span class="num">${escapeHtml(reservation.size)}</span>`
    + (reservation.quantity > 1 ? ` <span class="num">× ${reservation.quantity}</span>` : '');
}

function statusBadge(reservation) {
  const status = RESERVATION_STATUS[reservation.status] || { text: reservation.status, badge: 'out' };
  return `<span class="hv-badge hv-badge--${status.badge}">${status.text}</span>`;
}

function actionButtons(reservation) {
  if (reservation.status !== 'ACTIVE') return '';
  return `
    <div class="row-actions">
      <button type="button" class="hv-btn hv-btn--primary hv-btn--sm" data-action="pickup" data-id="${reservation.id}">Vydat</button>
      <button type="button" class="hv-btn hv-btn--danger-ghost hv-btn--sm" data-action="cancel" data-id="${reservation.id}">Zrušit</button>
    </div>`;
}

function emptyRow(colspan, text) {
  return `<tr><td colspan="${colspan}" class="table-message">${text}</td></tr>`;
}

function renderReservations() {
  const today = new Date();
  const active = reservations.filter(r => r.status === 'ACTIVE');
  const todays = active.filter(r => isSameDay(new Date(r.planned_start), today) || new Date(r.planned_start) < today);
  const upcoming = active.filter(r => !todays.includes(r));
  const closed = reservations.filter(r => r.status !== 'ACTIVE').reverse().slice(0, 30);

  todayTableBody.innerHTML = todays.length === 0 ? emptyRow(6, 'Dnes nikdo nepřijede pro rezervaci.') : todays.map(r => `
    <tr>
      <td class="num cell-strong" data-label="Vyzvednout">${formatTime(r.planned_start)}</td>
      <td class="cell-strong" data-label="Zákazník">${escapeHtml(r.customer_name)}</td>
      <td data-label="Typ">${typeLabel(r)}</td>
      <td class="num cell-muted" data-label="Drží do">${formatTime(r.pickup_deadline)} <span class="hv-help">(propadne ${formatTime(r.holds_until)})</span></td>
      <td data-label="Stav">${statusBadge(r)}</td>
      <td class="col-number">${actionButtons(r)}</td>
    </tr>`).join('');

  upcomingTableBody.innerHTML = upcoming.length === 0 ? emptyRow(6, 'Žádné další rezervace.') : upcoming.map(r => `
    <tr>
      <td class="num" data-label="Od – do">${formatDateTime(r.planned_start)} → ${formatDateTime(r.planned_end)}</td>
      <td class="cell-strong" data-label="Zákazník">${escapeHtml(r.customer_name)}</td>
      <td data-label="Typ">${typeLabel(r)}</td>
      <td class="num" data-label="Cena / den">${formatMoney(r.unit_daily_price)}</td>
      <td data-label="Stav">${statusBadge(r)}</td>
      <td class="col-number">${actionButtons(r)}</td>
    </tr>`).join('');

  closedTableBody.innerHTML = closed.length === 0 ? emptyRow(4, 'Zatím žádné.') : closed.map(r => `
    <tr>
      <td class="num cell-muted" data-label="Od – do">${formatDateTime(r.planned_start)} → ${formatDateTime(r.planned_end)}</td>
      <td data-label="Zákazník">${escapeHtml(r.customer_name)}</td>
      <td data-label="Typ">${typeLabel(r)}</td>
      <td data-label="Stav">${statusBadge(r)}${r.rental_id ? ` <a href="/rentals.html" class="hv-help">výpůjčka č. ${r.rental_id}</a>` : ''}</td>
    </tr>`).join('');
}

// --- Nová rezervace ---

function sizesOfCategory(categoryId) {
  const sizes = new Map();
  equipment
    .filter(item => item.category_id === categoryId && item.status !== 'RETIRED')
    .forEach(item => sizes.set(item.size.trim().toLowerCase(), item.size.trim()));
  return [...sizes.values()].sort((a, b) => a.localeCompare(b, 'cs', { numeric: true }));
}

function renderSizeOptions() {
  const sizes = sizesOfCategory(Number(reservationCategory.value));
  reservationSize.innerHTML = sizes.length === 0
    ? '<option value="">Žádné kusy</option>'
    : sizes.map(size => `<option value="${escapeHtml(size)}">${escapeHtml(size)}</option>`).join('');
}

function updateHoldHint() {
  if (!reservationStart.value) {
    reservationHoldHint.textContent = '';
    return;
  }
  const start = new Date(reservationStart.value);
  const morningLimit = new Date(start);
  morningLimit.setHours(10, 0, 0, 0);
  const deadline = start > morningLimit ? start : morningLimit;
  const expires = new Date(deadline.getTime() + 30 * 60000);
  reservationHoldHint.textContent = `Drží do ${formatTime(deadline.toISOString())}, nevyzvednutá propadne v ${formatTime(expires.toISOString())}.`;
}

async function openReservationModal() {
  reservationDialog.alert('');
  reservationForm.reset();
  const saturday = new Date();
  saturday.setDate(saturday.getDate() + ((6 - saturday.getDay() + 7) % 7 || 7));
  saturday.setHours(8, 0, 0, 0);
  const sunday = new Date(saturday);
  sunday.setDate(sunday.getDate() + 1);
  sunday.setHours(18, 0, 0, 0);
  reservationStart.value = toDateTimeInputValue(saturday);
  reservationEnd.value = toDateTimeInputValue(sunday);
  reservationQuantity.value = 1;

  await loadCustomerOptions(reservationCustomer);
  const withItems = categories.filter(c => equipment.some(item => item.category_id === c.id));
  reservationCategory.innerHTML = withItems.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
  renderSizeOptions();
  updateHoldHint();
  reservationDialog.open();
  reservationCustomer.focus();
}

async function submitReservation(event) {
  event.preventDefault();
  reservationDialog.alert('');
  reservationSubmitBtn.disabled = true;
  try {
    const res = await apiRequest('/api/reservations', {
      method: 'POST',
      body: JSON.stringify({
        customer_id: Number(reservationCustomer.value),
        category_id: Number(reservationCategory.value),
        size: reservationSize.value,
        quantity: Number(reservationQuantity.value),
        planned_start: toApiDateTime(reservationStart.value),
        planned_end: toApiDateTime(reservationEnd.value)
      })
    });
    if (!res) return;
    if (!res.ok) {
      reservationDialog.alert(await readErrorDetail(res, 'Rezervaci se nepodařilo uložit.'));
      return;
    }
    reservationDialog.close();
    await loadReservations();
  } finally {
    reservationSubmitBtn.disabled = false;
  }
}

// --- Výdej rezervace ---

async function openPickup(reservation) {
  pickupDialog.alert('');
  pickupReservation = reservation;
  pickupSelection.clear();
  await loadCatalog();
  pickupSummary.innerHTML = `${escapeHtml(reservation.customer_name)} · ${typeLabel(reservation)}
    · vrátit do <span class="num">${formatDateTime(reservation.planned_end)}</span>.
    Vyberte ${reservation.quantity} ${reservation.quantity === 1 ? 'kus' : reservation.quantity <= 4 ? 'kusy' : 'kusů'}.`;
  renderPickupPicker();
  pickupDialog.open();
}

function renderPickupPicker() {
  const size = pickupReservation.size.trim().toLowerCase();
  const items = equipment
    .filter(item => item.category_id === pickupReservation.category_id
      && item.size.trim().toLowerCase() === size
      && item.status === 'AVAILABLE')
    .sort((a, b) => a.inventory_number.localeCompare(b.inventory_number, 'cs', { numeric: true }));
  pickupItemPicker.innerHTML = items.length === 0
    ? '<div class="board-empty">Na place teď není žádný volný kus tohoto typu.</div>'
    : items.map(item => `
      <button type="button" class="hv-tag" data-id="${item.id}" data-status="free" aria-pressed="${pickupSelection.has(item.id)}">
        <span class="hv-tag__no">${escapeHtml(item.inventory_number)}</span>
        <span class="hv-tag__meta"><span class="num">${escapeHtml(item.size)}</span> · ${escapeHtml(item.model_name)}</span>
        <span class="hv-tag__status">Volné</span>
      </button>`).join('');
  const missing = pickupReservation.quantity - pickupSelection.size;
  pickupSubmitBtn.disabled = missing !== 0;
  pickupSubmitBtn.textContent = missing > 0 ? `Vybrat ještě ${missing}` : `Vydat ${pickupSelection.size} ks`;
}

async function submitPickup(event) {
  event.preventDefault();
  pickupDialog.alert('');
  pickupSubmitBtn.disabled = true;
  const res = await apiRequest(`/api/reservations/${pickupReservation.id}/pickup`, {
    method: 'POST',
    body: JSON.stringify({ equipment_item_ids: [...pickupSelection] })
  });
  if (!res) return;
  if (!res.ok) {
    pickupDialog.alert(await readErrorDetail(res, 'Výdej se nepodařil.'));
    pickupSubmitBtn.disabled = false;
    return;
  }
  pickupDialog.close();
  await loadReservations();
}

async function cancelReservation(reservation) {
  if (!confirm(`Zrušit rezervaci: ${reservation.customer_name}, ${reservation.category_name} ${reservation.size}?`)) return;
  const res = await apiRequest(`/api/reservations/${reservation.id}/cancel`, { method: 'POST' });
  if (res && res.ok) {
    await loadReservations();
  } else if (res) {
    alert(await readErrorDetail(res, 'Rezervaci se nepodařilo zrušit.'));
  }
}

function setupReservationEvents() {
  document.getElementById('openReservationModalBtn').addEventListener('click', openReservationModal);
  reservationCategory.addEventListener('change', renderSizeOptions);
  reservationStart.addEventListener('change', updateHoldHint);
  reservationForm.addEventListener('submit', submitReservation);

  document.querySelector('main').addEventListener('click', event => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const reservation = reservations.find(r => r.id === Number(button.dataset.id));
    if (!reservation) return;
    if (button.dataset.action === 'pickup') openPickup(reservation);
    if (button.dataset.action === 'cancel') cancelReservation(reservation);
  });

  pickupItemPicker.addEventListener('click', event => {
    const tag = event.target.closest('.hv-tag');
    if (!tag) return;
    const id = Number(tag.dataset.id);
    if (pickupSelection.has(id)) {
      pickupSelection.delete(id);
    } else if (pickupSelection.size < pickupReservation.quantity) {
      pickupSelection.add(id);
    }
    renderPickupPicker();
  });
  pickupForm.addEventListener('submit', submitPickup);
}
