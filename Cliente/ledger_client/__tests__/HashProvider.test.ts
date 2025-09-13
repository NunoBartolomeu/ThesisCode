import { hashProvider } from '../src/services/crypto/HashProvider';

describe('HashProvider', () => {
  const testData = 'Hello, World!';

  it('should register required algorithms', () => {
    const supported = hashProvider.getSupportedAlgorithms();
    expect(supported.has('SHA-256')).toBe(true);
    expect(supported.has('SHA-512')).toBe(true);
    // SHA3 is optional if not implemented
  });

  it('should hash SHA-256 correctly', async () => {
    const hash = await hashProvider.hash(testData, 'SHA-256');
    expect(hash).toMatch(/^[0-9a-f]+$/);        // valid hex
    expect(hash.length).toBe(64);               // 32 bytes -> 64 hex chars
  });

  it('should hash SHA-512 correctly', async () => {
    const hash = await hashProvider.hash(testData, 'SHA-512');
    expect(hash).toMatch(/^[0-9a-f]+$/);
    expect(hash.length).toBe(128);              // 64 bytes -> 128 hex chars
  });


  it('should hash SHA-256 correctly', async () => {
    const expected = 'dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f'; // known SHA-256 of "Hello, World!"
    const result = await hashProvider.hash(testData, 'SHA-256');
    expect(result).toBe(expected);
  });

  it('should hash SHA-512 correctly', async () => {
    const expected = '374d794a95cdcfd8b35993185fef9ba368f160d8daf432d08ba9f1ed1e5abe6cc69291e0fa2fe0006a52570ef18c19def4e617c33ce52ef0a6e5fbe318cb0387';
    const result = await hashProvider.hash(testData, 'SHA-512');
    expect(result).toBe(expected);
  });

  it('should convert string to byte array correctly', () => {
    const bytes = hashProvider.toHashByteArray(testData);
    expect(bytes.length).toBe(testData.length);
  });

  it('should throw error for unsupported algorithm', async () => {
    await expect(hashProvider.hash(testData, 'SHA3-256')).rejects.toThrow();
  });

  it('should return default algorithm', () => {
    const defaultAlgo = hashProvider.getDefaultAlgorithm();
    expect(hashProvider.isSupported(defaultAlgo)).toBe(true);
  });
});
