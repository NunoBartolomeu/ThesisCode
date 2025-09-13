export class AuthValidator {
  static isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  }

  static isValidPassword(password: string): boolean {
    return password.length >= 1 && password.length <= 128;
  }

  static isValidName(name: string): boolean {
    const trimmed = name.trim();
    return trimmed.length >= 2 && trimmed.length <= 50;
  }

  static isValid2FA(code: string): boolean {
    return /^\d{6}$/.test(code.trim());
  }
}
