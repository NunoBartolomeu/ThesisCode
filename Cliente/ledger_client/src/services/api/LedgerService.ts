import { Fetcher, ApiResponse } from './Fetcher';
import { LedgerDTO, PageDTO, PageSummaryDTO, EntryDTO } from '@/models/ledger_dto';

export class LedgerService {
  private baseUrl = "/ledger";
  private fetcher = new Fetcher();

  async getAvailableLedgers(): Promise<ApiResponse<string[]>> {
    return await this.fetcher.request<string[]>(`${this.baseUrl}/available`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getLedger(ledgerName: string): Promise<ApiResponse<LedgerDTO>> {
    return await this.fetcher.request<LedgerDTO>(`${this.baseUrl}/${ledgerName}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getPage(ledgerName: string, pageNumber: number): Promise<ApiResponse<PageDTO>> {
    return await this.fetcher.request<PageDTO>(`${this.baseUrl}/${ledgerName}/page/${pageNumber}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getEntry(entryId: string): Promise<ApiResponse<EntryDTO>> {
    return await this.fetcher.request<EntryDTO>(`${this.baseUrl}/entry/${entryId}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async addKeywords(entryId: string, keywords: string[]): Promise<ApiResponse<{ message: string }>> {
    return await this.fetcher.request<{ message: string }>(`${this.baseUrl}/entry/${entryId}/keywords`, {
      method: 'POST',
      body: keywords,
      requireAuth: true,
    });
  }

  async removeKeyword(entryId: string, keyword: string): Promise<ApiResponse<{ message: string }>> {
    return await this.fetcher.request<{ message: string }>(`${this.baseUrl}/entry/${entryId}/keyword/${keyword}`, {
      method: 'DELETE',
      requireAuth: true,
    });
  }
}