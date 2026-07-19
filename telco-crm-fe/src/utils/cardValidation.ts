export function isValidLuhn(cardNumber: string): boolean {
  const digitsOnly = cardNumber.replace(/\s+/g, "");
  if (!/^\d{13,19}$/.test(digitsOnly)) {
    return false;
  }

  let sum = 0;
  let doubleDigit = false;
  for (let i = digitsOnly.length - 1; i >= 0; i--) {
    let digit = parseInt(digitsOnly[i], 10);
    if (doubleDigit) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }
    sum += digit;
    doubleDigit = !doubleDigit;
  }
  return sum % 10 === 0;
}

export function isExpiryValid(expiryDate: string): boolean {
  const match = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(expiryDate);
  if (!match) {
    return false;
  }
  const month = parseInt(match[1], 10);
  const year = 2000 + parseInt(match[2], 10);

  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;

  if (year < currentYear) return false;
  if (year === currentYear && month < currentMonth) return false;
  return true;
}
