export class PKIStorageService {
  private static PKI_PRIVATE_KEY = 'pki_private';
  private static PKI_PUBLIC_KEY = 'pki_public';
  private static PKI_USER_CERTIFICATE_PREFIX = 'pki_user_cert_';
  private static PKI_SYSTEM_CERTIFICATE = 'pki_system_cert';

  // --- Key Pair ---
  static savePKIKeyPair(privateKeyStr: string, publicKeyStr: string): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(this.PKI_PRIVATE_KEY, privateKeyStr);
      localStorage.setItem(this.PKI_PUBLIC_KEY, publicKeyStr);
    } catch (e) {
      console.error('Failed to save PKI key pair:', e);
    }
  }

  static getPKIPrivateKey(): string | null {
    if (typeof window === 'undefined') return null;
    try {
      return localStorage.getItem(this.PKI_PRIVATE_KEY);
    } catch (e) {
      console.error('Failed to get private key:', e);
      return null;
    }
  }

  static getPKIPublicKey(): string | null {
    if (typeof window === 'undefined') return null;
    try {
      return localStorage.getItem(this.PKI_PUBLIC_KEY);
    } catch (e) {
      console.error('Failed to get public key:', e);
      return null;
    }
  }

  static clearPKIKeyPair(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.removeItem(this.PKI_PRIVATE_KEY);
      localStorage.removeItem(this.PKI_PUBLIC_KEY);
    } catch (e) {
      console.error('Failed to clear PKI key pair:', e);
    }
  }

  // --- User Certificate ---
  static savePKICertificate(userId: string, certificate: object): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(this.PKI_USER_CERTIFICATE_PREFIX + userId, JSON.stringify(certificate));
    } catch (e) {
      console.error('Failed to save PKI certificate:', e);
    }
  }

  static getPKICertificate(userId: string): object | null {
    if (typeof window === 'undefined') return null;
    try {
      const stored = localStorage.getItem(this.PKI_USER_CERTIFICATE_PREFIX + userId);
      return stored ? JSON.parse(stored) : null;
    } catch (e) {
      console.error('Failed to get PKI certificate:', e);
      return null;
    }
  }

  static clearPKICertificate(userId: string): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.removeItem(this.PKI_USER_CERTIFICATE_PREFIX + userId);
    } catch (e) {
      console.error('Failed to clear PKI certificate:', e);
    }
  }

  // --- System Certificate ---
  static saveSystemCertificate(certificate: object): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(this.PKI_SYSTEM_CERTIFICATE, JSON.stringify(certificate));
    } catch (e) {
      console.error('Failed to save system certificate:', e);
    }
  }

  static getSystemCertificate(): object | null {
    if (typeof window === 'undefined') return null;
    try {
      const stored = localStorage.getItem(this.PKI_SYSTEM_CERTIFICATE);
      return stored ? JSON.parse(stored) : null;
    } catch (e) {
      console.error('Failed to get system certificate:', e);
      return null;
    }
  }

  static clearSystemCertificate(): void {
    if (typeof window === 'undefined') return;
    try {
      localStorage.removeItem(this.PKI_SYSTEM_CERTIFICATE);
    } catch (e) {
      console.error('Failed to clear system certificate:', e);
    }
  }
}
