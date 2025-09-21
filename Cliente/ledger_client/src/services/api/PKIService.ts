import { Fetcher, ApiResponse } from './Fetcher';
import {
  CreateCertificateRequest,
  VerifyCertificateRequest,
  CertificateResponse,
  SystemCertificateResponse,
  CertificateVerificationResult,
} from '@/models/pki_dto';

export class PKIService {
  private baseUrl = "/certificate";
  private fetcher = new Fetcher();

  async createCertificate(userId: string, publicKey: string, algorithm: string): Promise<ApiResponse<CertificateResponse>> {
    const request: CreateCertificateRequest = { userId, publicKey, algorithm };
    return this.fetcher.request<CertificateResponse>(`${this.baseUrl}/create`, {
      method: 'POST',
      body: request,
      requireAuth: true,
    });
  }

  async verifyCertificate(userId: string, certificate: string): Promise<ApiResponse<CertificateVerificationResult>> {
    const request: VerifyCertificateRequest = { userId, certificate };
    return this.fetcher.request<CertificateVerificationResult>(`${this.baseUrl}/verify`, {
      method: 'POST',
      body: request,
      requireAuth: true,
    });
  }

  async getUserCertificate(userId: string): Promise<ApiResponse<CertificateResponse>> {
    return this.fetcher.request<CertificateResponse>(`${this.baseUrl}/user/${userId}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getSystemCertificate(): Promise<ApiResponse<SystemCertificateResponse>> {
    return this.fetcher.request<SystemCertificateResponse>(`${this.baseUrl}/system`, {
      method: 'GET',
      requireAuth: true,
    });
  }
}
