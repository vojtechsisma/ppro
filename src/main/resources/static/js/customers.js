// Stránka Zákazníci – seznam s vyhledáváním (vyžaduje js/common.js).

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

document.addEventListener('DOMContentLoaded', () => {
  let debounceTimeout;
  customerSearchInput.addEventListener('input', () => {
    clearTimeout(debounceTimeout);
    debounceTimeout = setTimeout(loadCustomers, 250);
  });
  loadCustomers();
});
