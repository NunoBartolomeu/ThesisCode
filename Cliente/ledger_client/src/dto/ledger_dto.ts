export interface LedgerDTO {
  name: string;
  entriesPerPage: number;
  hashAlgorithm: string;
  pages: PageSummaryDTO[];
  unverifiedEntries: PageEntryDTO[];
  verifiedEntries: PageEntryDTO[];
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
  merkleRoot: string;
  hash: string;
  entryCount: number;
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
  hash: string;
  signatures: SignatureDTO[];
  ledgerName: string;
  pageNumber?: number;
  relatedEntryIds: string[];
  keywords: string[];
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
  algorithm: string;
}
