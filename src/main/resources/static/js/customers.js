// Stránka Zákazníci – seznam s vyhledáváním a přidání zákazníka (vyžaduje js/common.js).

const customerSearchInput = document.getElementById('customerSearchInput');
const customerTableBody = document.getElementById('customerTableBody');

function renderCustomerMessage(text) {
  customerTableBody.innerHTML = `<tr><td colspan="4" class="table-message">${escapeHtml(text)}</td></tr>`;
}

async function loadCustomers() {
  const params = new URLSearchParams();
  if (customerSearchInput.value.trim()) params.append('search', customerSearchInput.value.trim());

  const res = await apiRequest(`/api/customers?${params.toString()}`);
  if (!res) return;
  if (!res.ok) {
    renderCustomerMessage('Zákazníky se nepodařilo načíst. Zkuste stránku obnovit.');
    return;
  }

  const customers = await res.json();
  if (customers.length === 0) {
    renderCustomerMessage('Hledání neodpovídá žádný zákazník.');
    return;
  }

  customerTableBody.innerHTML = customers.map(customer => `
    <tr>
      <td class="cell-strong">${escapeHtml(customer.last_name)} ${escapeHtml(customer.first_name)}</td>
      <td class="num">${escapeHtml(customer.phone)}</td>
      <td class="num">${escapeHtml(customer.id_document_number)}</td>
      <td class="num cell-muted">${escapeHtml(formatDate(customer.created_at))}</td>
    </tr>`).join('');
}

const customerDialog = setupDialog(document.getElementById('customerModal'));
const customerForm = document.getElementById('customerForm');
const customerSubmitBtn = document.getElementById('customerSubmitBtn');

function openCustomerModal() {
  customerForm.reset();
  customerDialog.alert('');
  customerDialog.open();
  document.getElementById('customerFirstName').focus();
}

async function submitCustomer(event) {
  event.preventDefault();
  customerDialog.alert('');
  customerSubmitBtn.disabled = true;
  try {
    const lastName = document.getElementById('customerLastName').value.trim();
    const res = await apiRequest('/api/customers', {
      method: 'POST',
      body: JSON.stringify({
        first_name: document.getElementById('customerFirstName').value.trim(),
        last_name: lastName,
        phone: document.getElementById('customerPhone').value.trim(),
        id_document_number: document.getElementById('customerDocument').value.trim()
      })
    });
    if (!res) return;
    if (!res.ok) {
      customerDialog.alert(await readErrorDetail(res, 'Zákazníka se nepodařilo uložit. Zkontrolujte údaje.'));
      return;
    }
    customerDialog.close();
    customerSearchInput.value = lastName;
    await loadCustomers();
  } finally {
    customerSubmitBtn.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('openCustomerModalBtn').addEventListener('click', openCustomerModal);
  customerForm.addEventListener('submit', submitCustomer);

  let debounceTimeout;
  customerSearchInput.addEventListener('input', () => {
    clearTimeout(debounceTimeout);
    debounceTimeout = setTimeout(loadCustomers, 250);
  });
  loadCustomers();
});
