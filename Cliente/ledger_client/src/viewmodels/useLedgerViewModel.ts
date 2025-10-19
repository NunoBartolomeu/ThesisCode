'use client';

import { useState, useCallback } from 'react';
import { LedgerService } from '@/services/api/LedgerService';
import { LedgerDTO, PageDTO, EntryDTO } from '@/models/ledger_dto';
import { AuthStorageService } from '@/services/storage/AuthStorageService';

interface LedgerState {
  ledgers: string[];
  currentLedger: LedgerDTO | null;
  currentPage: PageDTO | null;
  currentEntry: EntryDTO | null;
  loading: boolean;
  error: string | null;
  keywordOperationInProgress: boolean;
  userMessage: { type: 'success' | 'error'; text: string } | null;
}

export function useLedgerViewModel() {
  const ledgerService = new LedgerService();

  const [state, setState] = useState<LedgerState>({
    ledgers: [],
    currentLedger: null,
    currentPage: null,
    currentEntry: null,
    loading: false,
    error: null,
    keywordOperationInProgress: false,
    userMessage: null,
  });

  const clearError = useCallback(() => {
    setState(prev => ({ ...prev, error: null }));
  }, []);

  const clearUserMessage = useCallback(() => {
    setState(prev => ({ ...prev, userMessage: null }));
  }, []);

  const setLoading = useCallback((loading: boolean) => {
    setState(prev => ({ ...prev, loading }));
  }, []);

  const setError = useCallback((error: string | null) => {
    setState(prev => ({ ...prev, error }));
  }, []);

  const setUserMessage = useCallback((message: { type: 'success' | 'error'; text: string } | null) => {
    setState(prev => ({ ...prev, userMessage: message }));
  }, []);

  // ---- Load Available Ledgers ----
  const loadLedgers = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await ledgerService.getAvailableLedgers();

      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          ledgers: response.data || [],
          loading: false,
          error: null,
        }));
        return { success: true, data: response.data };
      } else {
        setState(prev => ({
          ...prev,
          ledgers: [],
          loading: false,
          error: response.message || 'Failed to load ledgers',
        }));
        return { success: false, error: response.message || 'Failed to load ledgers' };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState(prev => ({
        ...prev,
        ledgers: [],
        loading: false,
        error: errorMessage,
      }));
      return { success: false, error: errorMessage };
    }
  }, []);

  // ---- Load Specific Ledger ----
  const loadLedger = useCallback(async (ledgerName: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await ledgerService.getLedger(ledgerName);

      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          currentLedger: response.data,
          loading: false,
          error: null,
        }));
        return { success: true, data: response.data };
      } else {
        setState(prev => ({
          ...prev,
          currentLedger: null,
          loading: false,
          error: response.message || 'Failed to load ledger',
        }));
        return { success: false, error: response.message || 'Failed to load ledger' };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState(prev => ({
        ...prev,
        currentLedger: null,
        loading: false,
        error: errorMessage,
      }));
      return { success: false, error: errorMessage };
    }
  }, []);

  // ---- Load Specific Page ----
  const loadPage = useCallback(async (ledgerName: string, pageNumber: number) => {
    setLoading(true);
    setError(null);

    try {
      const response = await ledgerService.getPage(ledgerName, pageNumber);

      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          currentPage: response.data,
          loading: false,
          error: null,
        }));
        return { success: true, data: response.data };
      } else {
        setState(prev => ({
          ...prev,
          currentPage: null,
          loading: false,
          error: response.message || 'Failed to load page',
        }));
        return { success: false, error: response.message || 'Failed to load page' };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState(prev => ({
        ...prev,
        currentPage: null,
        loading: false,
        error: errorMessage,
      }));
      return { success: false, error: errorMessage };
    }
  }, []);

  // ---- Load Specific Entry ----
  const loadEntry = useCallback(async (entryId: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await ledgerService.getEntry(entryId);

      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          currentEntry: response.data,
          loading: false,
          error: null,
        }));
        return { success: true, data: response.data };
      } else {
        setState(prev => ({
          ...prev,
          currentEntry: null,
          loading: false,
          error: response.message || 'Failed to load entry',
        }));
        return { success: false, error: response.message || 'Failed to load entry' };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setState(prev => ({
        ...prev,
        currentEntry: null,
        loading: false,
        error: errorMessage,
      }));
      return { success: false, error: errorMessage };
    }
  }, []);

const signEntry = useCallback(async (entryId: string, signatureData: string, publicKey: string, algorithm: string) => {
    const user = AuthStorageService.getUser();
    if (!user) {
      throw new Error('User not authenticated');
    }
    
    const result = await ledgerService.signEntry(entryId, user.id, signatureData, publicKey, algorithm);
    if (result.success) {
      await loadEntry(entryId);
    }
    return result;
  }, [loadEntry]);

  // ---- Handle Add Keywords (Business Logic) ----
  const handleAddKeywords = useCallback(async (entryId: string, keywordsInput: string) => {
    if (!keywordsInput.trim()) {
      setUserMessage({ type: 'error', text: 'Please enter keywords to add' });
      return { success: false };
    }

    setState(prev => ({ ...prev, keywordOperationInProgress: true }));
    setUserMessage(null);

    try {
      const keywords = keywordsInput.split(',').map(k => k.trim()).filter(k => k);
      
      if (keywords.length === 0) {
        setUserMessage({ type: 'error', text: 'Please enter valid keywords' });
        setState(prev => ({ ...prev, keywordOperationInProgress: false }));
        return { success: false };
      }

      const response = await ledgerService.addKeywords(entryId, keywords);

      if (response.success) {
        // Reload entry to get updated data
        await loadEntry(entryId);
        setUserMessage({ type: 'success', text: 'Keywords added successfully' });
        setState(prev => ({ ...prev, keywordOperationInProgress: false }));
        return { success: true };
      } else {
        setUserMessage({ type: 'error', text: response.message || 'Failed to add keywords' });
        setState(prev => ({ ...prev, keywordOperationInProgress: false }));
        return { success: false };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setUserMessage({ type: 'error', text: errorMessage });
      setState(prev => ({ ...prev, keywordOperationInProgress: false }));
      return { success: false };
    }
  }, [loadEntry]);

  // ---- Handle Remove Keyword (Business Logic) ----
  const handleRemoveKeyword = useCallback(async (entryId: string, keyword: string) => {
    setState(prev => ({ ...prev, keywordOperationInProgress: true }));
    setUserMessage(null);

    try {
      const response = await ledgerService.removeKeyword(entryId, keyword);

      if (response.success) {
        // Reload entry to get updated data
        await loadEntry(entryId);
        setUserMessage({ type: 'success', text: `Keyword "${keyword}" removed successfully` });
        setState(prev => ({ ...prev, keywordOperationInProgress: false }));
        return { success: true };
      } else {
        setUserMessage({ type: 'error', text: response.message || 'Failed to remove keyword' });
        setState(prev => ({ ...prev, keywordOperationInProgress: false }));
        return { success: false };
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
      setUserMessage({ type: 'error', text: errorMessage });
      setState(prev => ({ ...prev, keywordOperationInProgress: false }));
      return { success: false };
    }
  }, [loadEntry]);

  // ---- Retry Logic ----
  const retryLastOperation = useCallback(async () => {
    // This could be enhanced to remember what the last operation was
    // For now, it's a simple retry that views can use
    setError(null);
    setUserMessage(null);
  }, []);

  // ---- Clear Current Data ----
  const clearCurrentLedger = useCallback(() => {
    setState(prev => ({ ...prev, currentLedger: null }));
  }, []);

  const clearCurrentPage = useCallback(() => {
    setState(prev => ({ ...prev, currentPage: null }));
  }, []);

  const clearCurrentEntry = useCallback(() => {
    setState(prev => ({ ...prev, currentEntry: null }));
  }, []);

  return {
    // State
    ledgers: state.ledgers,
    currentLedger: state.currentLedger,
    currentPage: state.currentPage,
    currentEntry: state.currentEntry,
    loading: state.loading,
    error: state.error,
    keywordOperationInProgress: state.keywordOperationInProgress,
    userMessage: state.userMessage,

    // Actions
    loadLedgers,
    loadLedger,
    loadPage,
    loadEntry,
    signEntry,
    handleAddKeywords,   
    handleRemoveKeyword,  
    retryLastOperation,
    clearError,
    clearUserMessage,
    clearCurrentLedger,
    clearCurrentPage,
    clearCurrentEntry,
  };
}