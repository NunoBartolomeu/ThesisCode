import { Fetcher, ApiResponse } from '@/viewmodels/Fetcher';
import { LedgerDTO, PageDTO, PageSummaryDTO, EntryDTO } from '@/dto/ledger_dto';

export class LedgerService {
  private fetcher: Fetcher;

  constructor(baseUrl?: string) {
    this.fetcher = new Fetcher(baseUrl);
  }

  async getAvailableLedgers(): Promise<ApiResponse<string[]>> {
    return await this.fetcher.request<string[]>('/ledger/available', {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getLedger(ledgerName: string): Promise<ApiResponse<LedgerDTO>> {
    return await this.fetcher.request<LedgerDTO>(`/ledger/${ledgerName}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getPage(ledgerName: string, pageNumber: number): Promise<ApiResponse<PageDTO>> {
    return await this.fetcher.request<PageDTO>(`/ledger/${ledgerName}/page/${pageNumber}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async getEntry(entryId: string): Promise<ApiResponse<EntryDTO>> {
    return await this.fetcher.request<EntryDTO>(`/ledger/entry/${entryId}`, {
      method: 'GET',
      requireAuth: true,
    });
  }

  async addKeywords(entryId: string, keywords: string[]): Promise<ApiResponse<{ message: string }>> {
    return await this.fetcher.request<{ message: string }>(`/ledger/entry/${entryId}/keywords`, {
      method: 'POST',
      body: keywords,
      requireAuth: true,
    });
  }

  async removeKeyword(entryId: string, keyword: string): Promise<ApiResponse<{ message: string }>> {
    return await this.fetcher.request<{ message: string }>(`/ledger/entry/${entryId}/keyword/${keyword}`, {
      method: 'DELETE',
      requireAuth: true,
    });
  }
}