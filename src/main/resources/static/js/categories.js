// Stránka Kategorie a ceník – přehled sazeb (vyžaduje js/common.js).

const categoryTableBody = document.getElementById('categoryTableBody');

async function loadCategoryPrices() {
  const res = await fetch('/api/categories');
  if (!res.ok) {
    categoryTableBody.innerHTML = '<tr><td colspan="5" class="table-message">Ceník se nepodařilo načíst.</td></tr>';
    return;
  }

  const categories = await res.json();
  categoryTableBody.innerHTML = categories.map(category => `
    <tr>
      <td class="cell-strong">${escapeHtml(category.name)}</td>
      <td><span class="cell-code">${escapeHtml(category.code)}</span></td>
      <td class="col-number num">${escapeHtml(formatPrice(category.base_daily_rate))} Kč</td>
      <td class="col-number num">${escapeHtml(formatPrice(category.weekend_rate))} Kč</td>
      <td class="cell-muted">${escapeHtml(category.description || '')}</td>
    </tr>`).join('');
}

document.addEventListener('DOMContentLoaded', loadCategoryPrices);
