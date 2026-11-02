// Stránka Výpůjčky – otevřené výpůjčky, vracení po kusech a sjednání nové výpůjčky (vyžaduje js/common.js).

let rentals = [];
let rentalFilter = 'open';
let pickerItems = [];
const pickedItemIds = new Set();

const rentalList = document.getElementById('rentalList');
const rentalsSummary = document.getElementById('rentalsSummary');
const rentalFilterGroup = document.getElementById('rentalFilter');
const rentalSearchInput = document.getElementById('rentalSearchInput');

const rentalDialog = setupDialog(document.getElementById('rentalModal'));
const rentalForm = document.getElementById('rentalForm');
const rentalCustomer = document.getElementById('rentalCustomer');
const rentalStart = document.getElementById('rentalStart');
const rentalEnd = document.getElementById('rentalEnd');
const rentalCategoryFilter = document.getElementById('rentalCategoryFilter');
const rentalDiscount = document.getElementById('rentalDiscount');
const rentalItemPicker = document.getElementById('rentalItemPicker');
const rentalPickedCount = document.getElementById('rentalPickedCount');
const rentalSubmitBtn = document.getElementById('rentalSubmitBtn');

const ITEM_STATUS_KEYS = { AVAILABLE: 'free', RENTED: 'out', IN_SERVICE: 'service', RETIRED: 'retired' };

document.addEventListener('DOMContentLoaded', () => {
  setupRentalEvents();
  loadRentals();
});

async function loadRentals() {
  const res = await apiRequest('/api/rentals');
  if (!res) return;
  if (!res.ok) {
    rentalList.innerHTML = '<div class="board-empty">Výpůjčky se nepodařilo načíst. Zkuste stránku obnovit.</div>';
    return;
  }
  rentals = await res.json();
  renderRentals();
}

function isOpen(rental) {
  return rental.status === 'ACTIVE' || rental.status === 'RETURNED_PARTIAL';
}

function renderRentals() {
  const open = rentals.filter(isOpen);
  const overdue = rentals.filter(r => r.overdue);
  document.getElementById('countOpen').textContent = open.length;
  document.getElementById('countOverdue').textContent = overdue.length;
  document.getElementById('countAll').textContent = rentals.length;
  const itemsOut = open.reduce((sum, r) => sum + r.items.filter(i => i.status === 'RENTED').length, 0);
  rentalsSummary.textContent = `Venku ${itemsOut} ${itemsOut === 1 ? 'kus' : itemsOut >= 2 && itemsOut <= 4 ? 'kusy' : 'kusů'} · po termínu ${overdue.length}`;

  const query = rentalSearchInput.value.trim().toLowerCase();
  const shown = (rentalFilter === 'open' ? open : rentalFilter === 'overdue' ? overdue : rentals)
    .filter(r => !query
      || r.customer_name.toLowerCase().includes(query)
      || r.items.some(i => i.inventory_number.toLowerCase().includes(query)));

  if (shown.length === 0) {
    const message = rentals.length === 0 ? 'Zatím tu není žádná výpůjčka.' : 'Filtru neodpovídá žádná výpůjčka.';
    rentalList.innerHTML = `<div class="board-empty">${message}</div>`;
    return;
  }
  // Po termínu nahoru, pak podle termínu vrácení
  const sorted = [...shown].sort((a, b) => (b.overdue - a.overdue)
    || (isOpen(b) - isOpen(a))
    || (isOpen(a) ? new Date(a.planned_end) - new Date(b.planned_end) : new Date(b.planned_end) - new Date(a.planned_end)));
  rentalList.innerHTML = sorted.map(renderRentalCard).join('');
}

function rentalBadge(rental) {
  const returned = rental.items.filter(i => i.status !== 'RENTED').length;
  const total = rental.items.length;
  if (rental.overdue) return `<span class="hv-badge hv-badge--late">Po termínu · ${returned} z ${total} vráceno</span>`;
  if (rental.status === 'COMPLETED') return '<span class="hv-badge hv-badge--free">Uzavřená · vše vráceno</span>';
  if (rental.status === 'CANCELLED') return '<span class="hv-badge hv-badge--retired">Zrušená</span>';
  if (new Date(rental.planned_start) > new Date()) return '<span class="hv-badge hv-badge--reserved">Čeká na výdej</span>';
  return `<span class="hv-badge hv-badge--out">Otevřená · ${returned} z ${total} vráceno</span>`;
}

function rentalItemRow(rental, item) {
  const name = `${escapeHtml(item.category_name)} <span class="num">${escapeHtml(item.size)}</span> · ${escapeHtml(item.model_name)}`;
  let statusText;
  let statusClass = '';
  let action;

  if (item.status === 'RENTED' && new Date(rental.planned_start) > new Date()) {
    statusText = `Připraveno · výdej ${formatDateTime(rental.planned_start)}`;
    action = '';
  } else if (item.status === 'RENTED') {
    const daysLate = startedDaysLate(rental.planned_end);
    if (daysLate > 0) {
      statusText = `Venku · po termínu ${daysLate} ${daysWord(daysLate)} · penále zatím ${formatMoney(daysLate * item.unit_daily_price)}`;
      statusClass = 'is-late';
    } else {
      statusText = `Venku · vrátit do ${formatDateTime(rental.planned_end)}`;
    }
    action = `<button type="button" class="hv-btn hv-btn--primary" data-action="return" data-rental="${rental.id}" data-item="${item.id}">Vrátit teď</button>`;
  } else {
    const daysLate = startedDaysLate(rental.planned_end, new Date(item.actual_returned_at));
    statusText = `Vráceno ${formatDateTime(item.actual_returned_at)} · ${daysLate ? `${daysLate} započatý ${daysWord(daysLate)} po termínu` : 'včas'}`;
    action = `<span class="return-fee${Number(item.late_fee) > 0 ? ' is-late' : ''}">${Number(item.late_fee) > 0 ? '+' : ''}${formatMoney(item.late_fee)}</span>`;
  }

  return `
    <div class="return-row">
      <span class="return-no">${escapeHtml(item.inventory_number)}</span>
      <div class="return-main">
        <span class="return-name">${name}</span>
        <span class="return-status ${statusClass}">${statusText}</span>
      </div>
      ${action}
    </div>`;
}

// Penále vrácených kusů + to, co zatím naběhlo kusům, které jsou pořád venku
function runningLateFee(rental) {
  const daysLate = startedDaysLate(rental.planned_end);
  return rental.items
    .filter(i => i.status === 'RENTED')
    .reduce((sum, i) => sum + daysLate * Number(i.unit_daily_price), Number(rental.total_late_fee));
}

function renderRentalCard(rental) {
  const stillAccruing = rental.overdue;
  const feeLabel = stillAccruing ? 'Penále zatím' : 'Penále celkem';
  const feeValue = stillAccruing ? runningLateFee(rental) : rental.total_late_fee;
  const discount = Number(rental.discount_amount) > 0
    ? ` · sleva ${formatMoney(rental.discount_amount)}` : '';
  return `
    <article class="hv-card rental-card">
      <header class="rental-head">
        <div class="rental-head-main">
          <span class="rental-id num">Výpůjčka č. ${rental.id}</span>
          <span class="rental-customer">${escapeHtml(rental.customer_name)}</span>
          <span class="rental-term num">${formatDateTime(rental.planned_start)} → ${formatDateTime(rental.planned_end)}${discount}</span>
        </div>
        ${rentalBadge(rental)}
      </header>
      ${rental.items.map(item => rentalItemRow(rental, item)).join('')}
      <footer class="rental-foot">
        <span>Výpůjčka zůstává otevřená do vrácení posledního kusu. Penále se počítá po kusech.</span>
        <span class="rental-fee${stillAccruing ? ' is-late' : ''}">${feeLabel} <span class="num">${formatMoney(feeValue)}</span></span>
      </footer>
    </article>`;
}

async function returnItem(rentalId, itemId, button) {
  button.disabled = true;
  const res = await apiRequest(`/api/rentals/${rentalId}/items/${itemId}/return`, { method: 'POST', body: '{}' });
  if (res && res.ok) {
    await loadRentals();
  } else if (res) {
    alert(await readErrorDetail(res, 'Kus se nepodařilo vrátit.'));
    button.disabled = false;
  }
}

// --- Nová výpůjčka ---

async function openRentalModal() {
  rentalDialog.alert('');
  rentalForm.reset();
  pickedItemIds.clear();
  const now = new Date();
  now.setSeconds(0, 0);
  const end = new Date(now);
  end.setDate(end.getDate() + 1);
  end.setHours(18, 0, 0, 0);
  rentalStart.value = toDateTimeInputValue(now);
  rentalEnd.value = toDateTimeInputValue(end);
  rentalDiscount.value = 0;

  const [, categoriesRes, equipmentRes] = await Promise.all([
    loadCustomerOptions(rentalCustomer),
    fetch('/api/categories'),
    fetch('/api/equipment?status=AVAILABLE')
  ]);
  const categories = categoriesRes.ok ? await categoriesRes.json() : [];
  pickerItems = equipmentRes.ok ? await equipmentRes.json() : [];
  rentalCategoryFilter.innerHTML = '<option value="">Všechny kategorie</option>'
    + categories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
  renderPicker();
  updateSubmitLabel();
  rentalDialog.open();
  rentalCustomer.focus();
}

function renderPicker() {
  const categoryId = rentalCategoryFilter.value ? Number(rentalCategoryFilter.value) : null;
  const items = pickerItems
    .filter(item => categoryId === null || item.category_id === categoryId || pickedItemIds.has(item.id))
    .sort((a, b) => a.inventory_number.localeCompare(b.inventory_number, 'cs', { numeric: true }));
  rentalItemPicker.innerHTML = items.length === 0
    ? '<div class="board-empty">Na place není žádný volný kus.</div>'
    : items.map(item => `
      <button type="button" class="hv-tag" data-id="${item.id}" data-status="${ITEM_STATUS_KEYS[item.status] || 'free'}"
              aria-pressed="${pickedItemIds.has(item.id)}">
        <span class="hv-tag__no">${escapeHtml(item.inventory_number)}</span>
        <span class="hv-tag__meta">${escapeHtml(item.category ? item.category.name : '')} · <span class="num">${escapeHtml(item.size)}</span></span>
        <span class="hv-tag__model">${escapeHtml(item.model_name)}</span>
      </button>`).join('');
  rentalPickedCount.textContent = `vybráno ${pickedItemIds.size}`;
}

function updateSubmitLabel() {
  const startsLater = rentalStart.value && new Date(rentalStart.value) > new Date();
  rentalSubmitBtn.textContent = startsLater ? 'Uložit výpůjčku' : `Vydat${pickedItemIds.size ? ` ${pickedItemIds.size} ks` : ''}`;
}

async function submitRental(event) {
  event.preventDefault();
  rentalDialog.alert('');
  if (pickedItemIds.size === 0) {
    rentalDialog.alert('Vyberte aspoň jeden kus.');
    return;
  }
  rentalSubmitBtn.disabled = true;
  try {
    const res = await apiRequest('/api/rentals', {
      method: 'POST',
      body: JSON.stringify({
        customer_id: Number(rentalCustomer.value),
        planned_start: toApiDateTime(rentalStart.value),
        planned_end: toApiDateTime(rentalEnd.value),
        equipment_item_ids: [...pickedItemIds],
        discount_amount: Number(rentalDiscount.value) || 0
      })
    });
    if (!res) return;
    if (!res.ok) {
      rentalDialog.alert(await readErrorDetail(res, 'Výpůjčku se nepodařilo uložit.'));
      return;
    }
    rentalDialog.close();
    rentalFilter = 'open';
    syncFilterButtons();
    await loadRentals();
  } finally {
    rentalSubmitBtn.disabled = false;
  }
}

function syncFilterButtons() {
  rentalFilterGroup.querySelectorAll('[data-filter]').forEach(button => {
    button.setAttribute('aria-pressed', String(button.dataset.filter === rentalFilter));
  });
}

function setupRentalEvents() {
  rentalFilterGroup.addEventListener('click', event => {
    const button = event.target.closest('[data-filter]');
    if (!button) return;
    rentalFilter = button.dataset.filter;
    syncFilterButtons();
    renderRentals();
  });

  let debounceTimeout;
  rentalSearchInput.addEventListener('input', () => {
    clearTimeout(debounceTimeout);
    debounceTimeout = setTimeout(renderRentals, 150);
  });

  rentalList.addEventListener('click', event => {
    const button = event.target.closest('button[data-action="return"]');
    if (button) returnItem(button.dataset.rental, button.dataset.item, button);
  });

  document.getElementById('openRentalModalBtn').addEventListener('click', openRentalModal);
  rentalCategoryFilter.addEventListener('change', renderPicker);
  rentalStart.addEventListener('change', updateSubmitLabel);
  rentalItemPicker.addEventListener('click', event => {
    const tag = event.target.closest('.hv-tag');
    if (!tag) return;
    const id = Number(tag.dataset.id);
    if (pickedItemIds.has(id)) pickedItemIds.delete(id); else pickedItemIds.add(id);
    renderPicker();
    updateSubmitLabel();
  });
  rentalForm.addEventListener('submit', submitRental);
}
