import { signatureProvider } from '../src/services/crypto/SignatureProvider';

describe('SignatureProvider', () => {
  const testData = 'Hello, World!';
  const expectedAlgorithms = new Set(['ECDSA', 'RSA']);

  it('should register required algorithms', () => {
    const supportedAlgorithms = signatureProvider.getSupportedAlgorithms();
    expect(supportedAlgorithms.size).toBeGreaterThanOrEqual(expectedAlgorithms.size);

    expectedAlgorithms.forEach(algo => {
      expect(supportedAlgorithms.has(algo)).toBe(true);
    });
  });

  it('should sign and verify for all supported algorithms', async () => {
    const algorithms = signatureProvider.getSupportedAlgorithms();
    expect(algorithms.size).toBeGreaterThan(0);

    for (const algorithm of Array.from(algorithms)) {
      const keyPair = await signatureProvider.generateKeyPair(algorithm);
      expect(keyPair).toHaveProperty('privateKey');
      expect(keyPair).toHaveProperty('publicKey');

      // Sign
      const signature = await signatureProvider.sign(testData, keyPair.privateKey, algorithm);
      expect(signature).toBeInstanceOf(Uint8Array);
      expect(signature.length).toBeGreaterThan(0);

      // Verify correct signature
      const isValid = await signatureProvider.verify(testData, signature, keyPair.publicKey, algorithm);
      expect(isValid).toBe(true);

      // Verify with different public key
      const differentKeyPair = await signatureProvider.generateKeyPair(algorithm);
      const isInvalid = await signatureProvider.verify(testData, signature, differentKeyPair.publicKey, algorithm);
      expect(isInvalid).toBe(false);
    }
  });

  it('should convert keys to hex and back', async () => {
    const algorithm = 'ECDSA';
    const keyPair = await signatureProvider.generateKeyPair(algorithm);

    // Convert private key
    const privateKeyEncoded = await crypto.subtle.exportKey('pkcs8', keyPair.privateKey);
    const privateKeyHex = signatureProvider.keyOrSigToString(new Uint8Array(privateKeyEncoded));
    expect(privateKeyHex).toMatch(/^[0-9a-f]+$/);
    expect(privateKeyHex.length % 2).toBe(0);

    const privateKeyBytes = signatureProvider.keyOrSigToByteArray(privateKeyHex);
    expect(privateKeyBytes).toEqual(new Uint8Array(privateKeyEncoded));

    // Convert public key
    const publicKeyEncoded = await crypto.subtle.exportKey('spki', keyPair.publicKey);
    const publicKeyHex = signatureProvider.keyOrSigToString(new Uint8Array(publicKeyEncoded));
    expect(publicKeyHex).toMatch(/^[0-9a-f]+$/);
    expect(publicKeyHex.length % 2).toBe(0);

    const publicKeyBytes = signatureProvider.keyOrSigToByteArray(publicKeyHex);
    expect(publicKeyBytes).toEqual(new Uint8Array(publicKeyEncoded));
  });

  it('should convert string to byte array and back', () => {
    const dataBytes = signatureProvider.dataToByteArray(testData);
    expect(dataBytes.length).toBe(testData.length);

    const dataString = signatureProvider.dataToString(dataBytes);
    expect(dataString).toBe(testData);
  });

  it('should sign and verify using hex keys', async () => {
    const algorithm = 'ECDSA';
    const keyPair = await signatureProvider.generateKeyPair(algorithm);
    
    const privateKeyEncoded = await crypto.subtle.exportKey('pkcs8', keyPair.privateKey);
    const publicKeyEncoded = await crypto.subtle.exportKey('spki', keyPair.publicKey);

    const privateKeyHex = signatureProvider.keyOrSigToString(new Uint8Array(privateKeyEncoded));
    const publicKeyHex = signatureProvider.keyOrSigToString(new Uint8Array(publicKeyEncoded));

    const signature = await signatureProvider.signWithHexKey(testData, privateKeyHex, algorithm);
    expect(signature).toBeInstanceOf(Uint8Array);

    const signatureHex = signatureProvider.keyOrSigToString(signature);
    const isValid = await signatureProvider.verifyWithHex(testData, signatureHex, publicKeyHex, algorithm);
    expect(isValid).toBe(true);
  });
});
