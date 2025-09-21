// Request DTOs
export interface CreateCertificateRequest {
  userId: string;
  publicKey: string; // Hex encoded public key
  algorithm: string;
}

export interface VerifyCertificateRequest {
  userId: string;
  certificate: string; // Base64 encoded certificate
}

export interface CertificateDetails {
  certificateBase64: string;
  serialNumber: string;
  issuer: string;
  subject: string;
  validFrom: string;
  validTo: string;
  publicKeyAlgorithm: string;
  signatureAlgorithm: string;
}

// Response DTOs
export interface CertificateResponse {
  certificate: CertificateDetails;
  userId: string;
}

export interface SystemCertificateResponse {
  certificate: CertificateDetails;
  userId: string; // for consistency, system ID like "ledger_system"
}

export interface CertificateVerificationResult {
  isValid: boolean;
}
