// Request DTOs
export interface CreateCertificateRequest {
  userId: string;
  publicKey: string; // Base64 encoded public key
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
  details: CertificateDetails;
  userId: string;
}

export interface SystemCertificateResponse {
  details: CertificateDetails;
  userId: string; // for consistency, system ID like "ledger_system"
}

export interface CertificateVerificationResult {
  isValid: boolean;
}
