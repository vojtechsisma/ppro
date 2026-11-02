// Stránka Přehled – visačky kusů podle kategorie, volné velikosti a detail kusu (vyžaduje js/common.js).

// --- Texty pro zobrazení ---
const STATUS_LABELS = {
  AVAILABLE: { text: 'Volné', key: 'free' },
  RENTED: { text: 'Venku', key: 'out' },
  IN_SERVICE: { text: 'Servis', key: 'service' },
  RETIRED: { text: 'Vyřazeno', key: 'retired' }
};

const STATUS_NOTES = {
  AVAILABLE: 'Na place, připraveno k vydání.',
  RENTED: 'Je půjčený zákazníkovi.',
  IN_SERVICE: 'Nepůjčuje se, dokud se nevrátí ze servisu.',
  RETIRED: 'Vyřazeno z půjčování, zůstává v evidenci.'
};

const WEAR_LABELS = {
  NEW: 'Nový',
  EXCELLENT: 'Výborný',
  GOOD: 'Dobrý',
  FAIR: 'Opotřebený',
  DAMAGED: 'Poškozený'
};

// --- Stav stránky ---
let categories = [];
let equipmentList = [];
let selectedCategoryId = null;
let selectedItemId = null;
let onlyFree = false;

const categoryTabs = document.getElementById('categoryTabs');
const searchInput = document.getElementById('searchInput');
const onlyFreeBtn = document.getElementById('onlyFreeBtn');
const sizeStrip = document.getElementById('sizeStrip');
const tagGrid = document.getElementById('tagGrid');
const boardSummary = document.getElementById('boardSummary');
const itemDetail = document.getElementById('itemDetail');
const railHint = document.getElementById('railHint');
const statusSummary = document.getElementById('statusSummary');

// Dialog
const modal = document.getElementById('equipmentModal');
const modalTitle = document.getElementById('modalTitle');
const modalAlert = document.getElementById('modalAlert');
const openAddModalBtn = document.getElementById('openAddModalBtn');
const closeModalBtn = document.getElementById('closeModalBtn');
const cancelModalBtn = document.getElementById('cancelModalBtn');
const saveItemBtn = document.getElementById('saveItemBtn');
const equipmentForm = document.getElementById('equipmentForm');
const editItemIdInput = document.getElementById('editItemId');

const formCategory = document.getElementById('formCategory');
const formInvNumber = document.getElementById('formInvNumber');
const formModelName = document.getElementById('formModelName');
const formSize = document.getElementById('formSize');
const formYear = document.getElementById('formYear');
const formWear = document.getElementById('formWear');
const formStatus = document.getElementById('formStatus');

// --- Inicializace ---
document.addEventListener('DOMContentLoaded', async () => {
  setupEventListeners();
  loadCounterAgenda();
  await loadCategories();
  await loadEquipment();
});

// --- Pult: kdo dnes přijde a kdo nevrátil ---

async function loadCounterAgenda() {
  const [reservationsRes, rentalsRes] = await Promise.all([
    apiRequest('/api/reservations'),
    apiRequest('/api/rentals')
  ]);
  if (reservationsRes && reservationsRes.ok) renderTodayPickups(await reservationsRes.json());
  if (rentalsRes && rentalsRes.ok) renderOverdueRentals(await rentalsRes.json());
}

function renderTodayPickups(reservations) {
  const now = new Date();
  const today = reservations
    .filter(r => r.status === 'ACTIVE' && (isSameDay(new Date(r.planned_start), now) || new Date(r.planned_start) < now));
  document.getElementById('todayPickups').innerHTML = today.length === 0
    ? '<span class="rail-hint">Dnes nikdo nepřijede pro rezervaci.</span>'
    : today.map(r => `
      <a class="rail-row" href="/reservations.html">
        <span class="rail-row-time num">${formatTime(r.planned_start)}</span>
        <span class="rail-row-main">
          <span class="rail-row-name">${escapeHtml(r.customer_name)}</span>
          <span class="rail-row-meta">${escapeHtml(r.category_name)} <span class="num">${escapeHtml(r.size)}</span>${r.quantity > 1 ? ` <span class="num">× ${r.quantity}</span>` : ''}</span>
        </span>
      </a>`).join('');
}

function renderOverdueRentals(rentals) {
  const overdue = rentals.filter(r => r.overdue);
  document.getElementById('overdueRentals').innerHTML = overdue.length === 0
    ? '<span class="rail-hint">Všechno je vráceno včas.</span>'
    : overdue.map(r => {
      const out = r.items.filter(i => i.status === 'RENTED');
      const daysLate = startedDaysLate(r.planned_end);
      const fee = out.reduce((sum, i) => sum + daysLate * Number(i.unit_daily_price), Number(r.total_late_fee));
      return `
      <a class="rail-row is-late" href="/rentals.html">
        <span class="rail-row-time num">+${daysLate} d</span>
        <span class="rail-row-main">
          <span class="rail-row-name">${escapeHtml(r.customer_name)}</span>
          <span class="rail-row-meta">venku ${out.map(i => escapeHtml(i.inventory_number)).join(', ')} · penále zatím <span class="num">${formatMoney(fee)}</span></span>
        </span>
      </a>`;
    }).join('');
}

async function loadCategories() {
  const res = await fetch('/api/categories');
  if (!res.ok) return;
  categories = await res.json();

  formCategory.innerHTML = '';
  categories.forEach(cat => {
    formCategory.appendChild(new Option(`${cat.name} · ${formatPrice(cat.base_daily_rate)} Kč/den`, cat.id));
  });
}

// Fond půjčovny je malý, proto se načte celý a filtruje se na místě
async function loadEquipment() {
  const res = await fetch('/api/equipment');
  if (!res.ok) {
    tagGrid.innerHTML = '<div class="board-empty">Vybavení se nepodařilo načíst. Zkuste stránku obnovit.</div>';
    boardSummary.textContent = '';
    return;
  }
  equipmentList = await res.json();
  if (selectedItemId && !equipmentList.some(i => i.id === selectedItemId)) selectedItemId = null;
  render();
}

// --- Vykreslení ---
function render() {
  renderCategoryTabs();
  renderSizeStrip();
  renderTagGrid();
  renderDetail();
  renderStatusSummary();
  renderBoardSummary();
}

function statusOf(item) {
  return STATUS_LABELS[item.status] || { text: item.status, key: 'retired' };
}

function countFree(items) {
  return items.filter(i => i.status === 'AVAILABLE').length;
}

function itemsInCategory() {
  return selectedCategoryId === null
    ? equipmentList
    : equipmentList.filter(i => i.category_id === selectedCategoryId);
}

function visibleItems() {
  const query = searchInput.value.trim().toLowerCase();
  return itemsInCategory()
    .filter(i => !onlyFree || i.status === 'AVAILABLE')
    .filter(i => !query
      || i.inventory_number.toLowerCase().includes(query)
      || i.model_name.toLowerCase().includes(query))
    .sort((a, b) => a.inventory_number.localeCompare(b.inventory_number, 'cs', { numeric: true }));
}

function renderCategoryTabs() {
  const tab = (id, name, items) => `
    <button type="button" class="cat-tab" data-category="${id ?? ''}" aria-pressed="${selectedCategoryId === id}">
      ${escapeHtml(name)}<span class="cat-tab-count">${countFree(items)}/${items.length}</span>
    </button>`;

  // Kategorie bez jediného kusu se na přehledu nezobrazují
  const withItems = categories
    .map(cat => ({ cat, items: equipmentList.filter(i => i.category_id === cat.id) }))
    .filter(entry => entry.items.length > 0);

  categoryTabs.innerHTML = tab(null, 'Vše', equipmentList)
    + withItems.map(entry => tab(entry.cat.id, entry.cat.name, entry.items)).join('');
}

function renderSizeStrip() {
  if (selectedCategoryId === null) {
    sizeStrip.hidden = true;
    return;
  }
  const bySize = new Map();
  itemsInCategory().forEach(item => {
    const entry = bySize.get(item.size) || { total: 0, free: 0 };
    entry.total += 1;
    if (item.status === 'AVAILABLE') entry.free += 1;
    bySize.set(item.size, entry);
  });

  const sizes = [...bySize.keys()].sort((a, b) => a.localeCompare(b, 'cs', { numeric: true }));
  sizeStrip.hidden = sizes.length === 0;
  sizeStrip.innerHTML = sizes.map(size => {
    const { free, total } = bySize.get(size);
    return `
      <div class="size-chip">
        <span class="size-chip-size">${escapeHtml(size)}</span>
        <span class="size-chip-free${free ? '' : ' is-empty'}">${free} ${freeWord(free)} z ${total}</span>
      </div>`;
  }).join('');
}

// 1 volný, 2–4 volné, 0 a 5+ volných
function freeWord(count) {
  if (count === 1) return 'volný';
  if (count >= 2 && count <= 4) return 'volné';
  return 'volných';
}

function renderTagGrid() {
  const items = visibleItems();
  if (items.length === 0) {
    const message = equipmentList.length === 0
      ? 'V evidenci zatím není žádný kus.'
      : 'Filtru neodpovídá žádný kus.';
    tagGrid.innerHTML = `<div class="board-empty">${message}</div>`;
    return;
  }

  tagGrid.innerHTML = items.map(item => {
    const status = statusOf(item);
    return `
      <button type="button" class="hv-tag" data-id="${item.id}" data-status="${status.key}"
              aria-pressed="${item.id === selectedItemId}">
        <span class="hv-tag__no">${escapeHtml(item.inventory_number)}</span>
        <span class="hv-tag__meta"><span class="num">${escapeHtml(item.size)}</span> · ${escapeHtml(WEAR_LABELS[item.wear_condition] || item.wear_condition)}</span>
        <span class="hv-tag__model">${escapeHtml(item.model_name)}</span>
        <span class="hv-tag__status">${status.text}</span>
      </button>`;
  }).join('');
}

function renderDetail() {
  const item = equipmentList.find(i => i.id === selectedItemId);
  railHint.hidden = Boolean(item);
  if (!item) {
    itemDetail.hidden = true;
    itemDetail.innerHTML = '';
    return;
  }

  const status = statusOf(item);
  const inService = item.status === 'IN_SERVICE';
  const serviceButton = inService
    ? `<button type="button" class="hv-btn hv-btn--primary" data-action="return-from-service">Vrátit ze servisu</button>`
    : `<button type="button" class="hv-btn hv-btn--secondary" data-action="send-to-service"${item.status === 'RENTED' ? ' disabled title="Kus je venku"' : ''}>Do servisu</button>`;

  itemDetail.innerHTML = `
    <div class="rail-head">
      <span class="detail-no">${escapeHtml(item.inventory_number)}</span>
      <button type="button" class="hv-btn hv-btn--ghost hv-btn--sm" data-action="close">Zavřít</button>
    </div>
    <span class="detail-model">${escapeHtml(item.model_name)}</span>
    <span class="detail-meta">
      ${escapeHtml(item.category ? item.category.name : '–')} · <span class="num">${escapeHtml(item.size)}</span>
      · pořízeno <span class="num">${escapeHtml(item.year_acquired)}</span>
      · stav ${escapeHtml(WEAR_LABELS[item.wear_condition] || item.wear_condition)}
    </span>
    <span class="hv-badge hv-badge--${status.key}" style="align-self:flex-start">${status.text}</span>
    <span class="detail-note">${STATUS_NOTES[item.status] || ''}</span>
    ${item.category ? `<span class="detail-note">Ceník: <span class="num">${escapeHtml(formatPrice(item.category.base_daily_rate))} Kč</span> den · <span class="num">${escapeHtml(formatPrice(item.category.weekend_rate))} Kč</span> víkend</span>` : ''}
    <div class="detail-actions">
      ${serviceButton}
      <button type="button" class="hv-btn hv-btn--secondary" data-action="edit">Upravit</button>
      <button type="button" class="hv-btn hv-btn--danger-ghost" data-action="delete">Smazat</button>
    </div>`;
  itemDetail.hidden = false;
}

function renderStatusSummary() {
  statusSummary.innerHTML = Object.entries(STATUS_LABELS).map(([status, label]) => {
    const count = equipmentList.filter(i => i.status === status).length;
    return `
      <div class="summary-row">
        <span class="hv-badge hv-badge--${label.key}">${label.text}</span>
        <span class="summary-count">${count}</span>
      </div>`;
  }).join('') + `
    <div class="summary-total">
      <span>Kusů v evidenci</span>
      <span class="summary-count">${equipmentList.length}</span>
    </div>`;
}

function renderBoardSummary() {
  const free = countFree(equipmentList);
  boardSummary.textContent = `Volné ${free} z ${equipmentList.length} kusů`;
}

// --- Akce s kusem ---

function selectedItem() {
  return equipmentList.find(i => i.id === selectedItemId);
}

async function sendToService(item) {
  if (!confirm(`Poslat ${item.inventory_number} do servisu? Do vrácení ho nepůjde půjčit.`)) return;
  const res = await apiRequest(`/api/equipment/${item.id}/service`, { method: 'POST' });
  if (res && res.ok) {
    await loadEquipment();
  } else if (res) {
    const data = await res.json();
    alert(data.detail || 'Kus nelze poslat do servisu.');
  }
}

async function returnFromService(item) {
  const res = await apiRequest(`/api/equipment/${item.id}`, {
    method: 'PUT',
    body: JSON.stringify({ status: 'AVAILABLE', wear_condition: 'GOOD' })
  });
  if (res && res.ok) {
    await loadEquipment();
  }
}

async function deleteItem(item) {
  if (!confirm(`Smazat ${item.inventory_number} z evidence?`)) return;
  const res = await apiRequest(`/api/equipment/${item.id}`, { method: 'DELETE' });
  if (res && res.status === 204) {
    selectedItemId = null;
    await loadEquipment();
  } else if (res) {
    const data = await res.json();
    alert(data.detail || 'Kus nelze smazat.');
  }
}

function openEditModal(item) {
  hideModalAlert();
  modalTitle.textContent = `Upravit ${item.inventory_number}`;
  editItemIdInput.value = item.id;

  // Kategorie, inventární číslo a rok pořízení se po zaevidování nemění
  document.getElementById('formGroupCategory').style.display = 'none';
  document.getElementById('formGroupInvNumber').style.display = 'none';
  document.getElementById('formGroupYear').style.display = 'none';
  formCategory.required = false;
  formInvNumber.required = false;
  formYear.required = false;

  formModelName.value = item.model_name;
  formSize.value = item.size;
  formWear.value = item.wear_condition;
  formStatus.value = item.status;

  showModal();
  formModelName.focus();
}

function openCreateModal() {
  hideModalAlert();
  modalTitle.textContent = 'Nový kus';
  editItemIdInput.value = '';

  document.getElementById('formGroupCategory').style.display = '';
  document.getElementById('formGroupInvNumber').style.display = '';
  document.getElementById('formGroupYear').style.display = '';
  formCategory.required = true;
  formInvNumber.required = true;
  formYear.required = true;

  equipmentForm.reset();
  if (selectedCategoryId !== null) formCategory.value = selectedCategoryId;
  formYear.value = new Date().getFullYear();
  formWear.value = 'GOOD';
  formStatus.value = 'AVAILABLE';

  showModal();
  formCategory.focus();
}

function showModal() {
  modal.classList.add('active');
}

function closeModal() {
  modal.classList.remove('active');
}

function hideModalAlert() {
  modalAlert.style.display = 'none';
}

function showModalAlert(message) {
  modalAlert.textContent = message;
  modalAlert.style.display = 'block';
}

function setupEventListeners() {
  // Kategorie
  categoryTabs.addEventListener('click', event => {
    const tab = event.target.closest('button[data-category]');
    if (!tab) return;
    selectedCategoryId = tab.dataset.category === '' ? null : Number(tab.dataset.category);
    render();
  });

  // Filtry
  onlyFreeBtn.addEventListener('click', () => {
    onlyFree = !onlyFree;
    onlyFreeBtn.setAttribute('aria-pressed', String(onlyFree));
    renderTagGrid();
  });

  let debounceTimeout;
  searchInput.addEventListener('input', () => {
    clearTimeout(debounceTimeout);
    debounceTimeout = setTimeout(renderTagGrid, 150);
  });

  // Výběr visačky
  tagGrid.addEventListener('click', event => {
    const tag = event.target.closest('.hv-tag');
    if (!tag) return;
    const id = Number(tag.dataset.id);
    selectedItemId = selectedItemId === id ? null : id;
    renderTagGrid();
    renderDetail();
    // Na úzké obrazovce je panel pod mřížkou, proto k detailu sjedeme
    if (selectedItemId && window.matchMedia('(max-width: 1080px)').matches) {
      itemDetail.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  });

  // Akce v detailu
  itemDetail.addEventListener('click', event => {
    const button = event.target.closest('button[data-action]');
    if (!button) return;
    const item = selectedItem();
    if (!item) return;

    switch (button.dataset.action) {
      case 'close':
        selectedItemId = null;
        renderTagGrid();
        renderDetail();
        break;
      case 'send-to-service': sendToService(item); break;
      case 'return-from-service': returnFromService(item); break;
      case 'edit': openEditModal(item); break;
      case 'delete': deleteItem(item); break;
    }
  });

  // Dialog
  openAddModalBtn.addEventListener('click', openCreateModal);
  closeModalBtn.addEventListener('click', closeModal);
  cancelModalBtn.addEventListener('click', closeModal);
  modal.addEventListener('click', event => {
    if (event.target === modal) closeModal();
  });
  document.addEventListener('keydown', event => {
    if (event.key !== 'Escape') return;
    if (modal.classList.contains('active')) {
      closeModal();
    } else if (selectedItemId) {
      selectedItemId = null;
      renderTagGrid();
      renderDetail();
    }
  });

  // Odeslání formuláře (vytvoření / úprava)
  equipmentForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    hideModalAlert();
    saveItemBtn.disabled = true;

    const isEdit = Boolean(editItemIdInput.value);

    try {
      let res;
      if (isEdit) {
        res = await apiRequest(`/api/equipment/${editItemIdInput.value}`, {
          method: 'PUT',
          body: JSON.stringify({
            model_name: formModelName.value,
            size: formSize.value,
            wear_condition: formWear.value,
            status: formStatus.value
          })
        });
      } else {
        res = await apiRequest('/api/equipment', {
          method: 'POST',
          body: JSON.stringify({
            category_id: parseInt(formCategory.value, 10),
            inventory_number: formInvNumber.value,
            model_name: formModelName.value,
            size: formSize.value,
            year_acquired: parseInt(formYear.value, 10),
            wear_condition: formWear.value,
            status: formStatus.value
          })
        });
      }

      if (!res) return;
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.detail || (isEdit ? 'Úprava se nepovedla.' : 'Kus se nepodařilo uložit.'));
      }

      const saved = await res.json();
      closeModal();
      selectedItemId = saved.id ?? selectedItemId;
      await loadEquipment();
    } catch (err) {
      showModalAlert(err.message);
    } finally {
      saveItemBtn.disabled = false;
    }
  });
}
