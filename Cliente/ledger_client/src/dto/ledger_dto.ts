export interface LedgerDTO {
  name: string;
  entriesPerPage: number;
  hashAlgorithm: string;
  cryptoAlgorithm: string;
  verifiedEntries: PageEntryDTO[];
  nonVerifiedEntries: PageEntryDTO[];
  pages: PageSummaryDTO[];
}

export interface PageSummaryDTO {
  number: number;
  timestamp: number;
  entryCount: number;
}

export interface PageDTO {
  ledgerName: string;
  number: number;
  timestamp: number;
  previousHash?: string;
  entryCount: number;
  hash: string;
  entries: PageEntryDTO[];
}

export interface PageEntryDTO {
  id: string;
  isParticipant: boolean;
}

export interface EntryDTO {
  id: string;
  timestamp: number;
  content: string;
  senders: ParticipantDTO[];
  recipients: ParticipantDTO[];
  signatures: SignatureDTO[];
  relatedEntryIds: string[];
  keywords: string[];
  ledgerName: string;
  pageNumber?: number;
  hash: string;
}

export interface ParticipantDTO {
  fullName: string;
  email: string;
}

export interface SignatureDTO {
  participant: string;
  email: string;
  publicKey: string;
  signature: string;
}
