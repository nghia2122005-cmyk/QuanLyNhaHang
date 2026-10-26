const currencyFormatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })

/** "45.000 ₫": same Intl output as before, with the currency symbol styled smaller and lighter. */
export function MoneyText({ amount }: { amount: number | string }) {
  const parts = currencyFormatter.formatToParts(Number(amount))
  return <span className="money-text">{parts.map((part, index) => part.type === 'currency'
    ? <span key={index} className="money-currency">{part.value}</span>
    : part.value)}</span>
}
