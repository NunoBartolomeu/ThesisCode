'use client';

import { useState, useCallback, useEffect } from 'react';
import { PKIService } from '@/services/api/PKIService';
import { signatureProvider } from '@/services/crypto/SignatureProvider';
import { PKIStorageService } from '@/services/storage/PKIStorageService';
import { CertificateDetails } from '@/models/pki_dto';

interface KeyPairHex {
  privateKey: string;
  publicKey: string;
  algorithm: string;
}

export function usePKIViewModel() {
  const pkiService = new PKIService();

  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [keyPair, setKeyPair] = useState<KeyPairHex | null>(null);
  const [userCertificate, setUserCertificate] = useState<CertificateDetails | null>(null);
  const [systemCertificate, setSystemCertificate] = useState<CertificateDetails | null>(null);

  // Load existing key pair on mount
  useEffect(() => {
    const privateKey = PKIStorageService.getPKIPrivateKey();
    const publicKey = PKIStorageService.getPKIPublicKey();
    const algorithm = PKIStorageService.getPKIAlgorithm();
    
    if (privateKey && publicKey && algorithm) {
      setKeyPair({ privateKey, publicKey, algorithm });
    }
  }, []);

  const clearErrors = useCallback(() => setErrors({}), []);

  const ensureHexFormat = (data: string): string => {
    return data.toLowerCase().replace(/[^0-9a-f]/g, '');
  };

  // PEM conversion utilities
  const wrapPEM = (hexString: string, type: 'PUBLIC KEY' | 'PRIVATE KEY' | 'CERTIFICATE') => {
    // Convert hex to base64
    const bytes = signatureProvider.keyOrSigToByteArray(hexString);
    const binaryString = String.fromCharCode.apply(null, Array.from(bytes) as number[]);
    const base64 = btoa(binaryString);
    const lines = base64.match(/.{1,64}/g) || [];
    return `-----BEGIN ${type}-----\n${lines.join('\n')}\n-----END ${type}-----\n`;
  };

  const unwrapPEM = (pemString: string): string => {
    // Remove PEM headers and convert base64 to hex
    const base64 = pemString
      .replace(/-----BEGIN [^-]+-----/g, '')
      .replace(/-----END [^-]+-----/g, '')
      .replace(/\s/g, '');
    
    const binaryString = atob(base64);
    const bytes = new Uint8Array(binaryString.length);
    for (let i = 0; i < binaryString.length; i++) {
      bytes[i] = binaryString.charCodeAt(i);
    }
    return signatureProvider.keyOrSigToString(bytes);
  };

  // Key pair operations
  const generateKeyPair = useCallback(async () => {
    setLoading(true);
    clearErrors();
    let algorithmName = signatureProvider.getDefaultAlgorithm()

    try {
      const cryptoKeyPair = await signatureProvider.generateKeyPair(algorithmName);
      
      // Export keys to hex - ensure they're in proper hex format
      const exportedPub = await crypto.subtle.exportKey('spki', cryptoKeyPair.publicKey);
      const exportedPriv = await crypto.subtle.exportKey('pkcs8', cryptoKeyPair.privateKey);

      const publicKeyHex = ensureHexFormat(signatureProvider.keyOrSigToString(new Uint8Array(exportedPub)));
      const privateKeyHex = ensureHexFormat(signatureProvider.keyOrSigToString(new Uint8Array(exportedPriv)));

      const newKeyPair = { 
        privateKey: privateKeyHex, 
        publicKey: publicKeyHex, 
        algorithm: algorithmName 
      };

      // Save to storage with algorithm
      PKIStorageService.savePKIKeyPair(privateKeyHex, publicKeyHex, algorithmName);
      setKeyPair(newKeyPair);

      return { success: true, data: newKeyPair };
    } catch (err: any) {
      const error = { general: err.message || 'Key generation failed' };
      setErrors(error);
      return { success: false, errors: error };
    } finally {
      setLoading(false);
    }
  }, []);

  const importKeyPairFromHex = useCallback((privateKeyHex: string, publicKeyHex: string, algorithm: string) => {
    try {
      // Validate hex format
      const cleanPrivateKey = ensureHexFormat(privateKeyHex);
      const cleanPublicKey = ensureHexFormat(publicKeyHex);
      
      if (!PKIStorageService.validateHexString(cleanPrivateKey) || !PKIStorageService.validateHexString(cleanPublicKey)) {
        throw new Error('Invalid hex format');
      }

      const newKeyPair = { 
        privateKey: cleanPrivateKey, 
        publicKey: cleanPublicKey, 
        algorithm 
      };
      
      PKIStorageService.savePKIKeyPair(cleanPrivateKey, cleanPublicKey, algorithm);
      setKeyPair(newKeyPair);
      return { success: true };
    } catch (err: any) {
      const error = { general: err.message };
      setErrors(error);
      return { success: false, error: err.message };
    }
  }, []);

  const importKeyPairFromPEM = useCallback((privateKeyPEM: string, publicKeyPEM: string) => {
    try {
      const privateKeyHex = unwrapPEM(privateKeyPEM);
      const publicKeyHex = unwrapPEM(publicKeyPEM);
      let algorithm = signatureProvider.getDefaultAlgorithm() 
      return importKeyPairFromHex(privateKeyHex, publicKeyHex, algorithm);
    } catch (err: any) {
      const error = { general: 'Invalid PEM format: ' + err.message };
      setErrors(error);
      return { success: false, error: error.general };
    }
  }, [importKeyPairFromHex]);

  const exportKeyPairAsPEM = useCallback(() => {
    if (!keyPair) throw new Error('No key pair available');
    
    return {
      privateKey: wrapPEM(keyPair.privateKey, 'PRIVATE KEY'),
      publicKey: wrapPEM(keyPair.publicKey, 'PUBLIC KEY'),
      algorithm: keyPair.algorithm
    };
  }, [keyPair]);

  const exportKeyPairAsHex = useCallback(() => {
    if (!keyPair) throw new Error('No key pair available');
    return keyPair;
  }, [keyPair]);

  const clearKeyPair = useCallback(() => {
    PKIStorageService.clearPKIKeyPair();
    setKeyPair(null);
  }, []);

  // Certificate operations
  const createCertificate = useCallback(async (userId: string) => {
    if (!keyPair) {
      const error = { general: 'Generate or import a key pair first' };
      setErrors(error);
      return { success: false, errors: error };
    }

    if (!keyPair.algorithm) {
      const error = { general: 'Algorithm information missing from key pair' };
      setErrors(error);
      return { success: false, errors: error };
    }

    setLoading(true);
    clearErrors();

    try {
      // Pass the algorithm along with the public key
      const result = await pkiService.createCertificate(userId, keyPair.publicKey, keyPair.algorithm);
      
      if (result.success && result.data) {
        PKIStorageService.savePKICertificate(userId, result.data.certificate);
        setUserCertificate(result.data.certificate);
      } else {
        setErrors(result.errors || { general: result.message || 'Certificate creation failed' });
      }

      return result;
    } catch (err: any) {
      const error = { general: err.message };
      setErrors(error);
      return { success: false, errors: error };
    } finally {
      setLoading(false);
    }
  }, [keyPair]);

  const fetchUserCertificate = useCallback(async (userId: string) => {
    setLoading(true);
    clearErrors();

    try {
      // First check storage
      const stored = PKIStorageService.getPKICertificate(userId) as CertificateDetails;
      if (stored) {
        setUserCertificate(stored);
        return { success: true, data: stored };
      }

      // Fetch from server
      const result = await pkiService.getUserCertificate(userId);
      if (result.success && result.data) {
        PKIStorageService.savePKICertificate(userId, result.data.certificate);
        setUserCertificate(result.data.certificate);
      } else {
        setErrors(result.errors || { general: 'Failed to fetch user certificate' });
      }
      return result;
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSystemCertificate = useCallback(async () => {
    setLoading(true);
    clearErrors();

    try {
      // First check storage
      const stored = PKIStorageService.getSystemCertificate() as CertificateDetails;
      if (stored) {
        setSystemCertificate(stored);
        return { success: true, data: stored };
      }

      // Fetch from server
      const result = await pkiService.getSystemCertificate();
      if (result.success && result.data) {
        PKIStorageService.saveSystemCertificate(result.data.certificate);
        setSystemCertificate(result.data.certificate);
      } else {
        setErrors(result.errors || { general: 'Failed to fetch system certificate' });
      }
      return result;
    } finally {
      setLoading(false);
    }
  }, []);

  const exportCertificateAsPEM = useCallback((certificate: CertificateDetails) => {
    return wrapPEM(certificate.certificateBase64, 'CERTIFICATE');
  }, []);

  const importCertificateFromPEM = useCallback((certificatePEM: string, userId?: string) => {
    try {
      const certificateHex = unwrapPEM(certificatePEM);
      
      const certificate: CertificateDetails = {
        certificateBase64: certificateHex,
        serialNumber: '',
        issuer: '',
        subject: '',
        validFrom: '',
        validTo: '',
        publicKeyAlgorithm: '',
        signatureAlgorithm: ''
      };

      if (userId) {
        PKIStorageService.savePKICertificate(userId, certificate);
        setUserCertificate(certificate);
      } else {
        PKIStorageService.saveSystemCertificate(certificate);
        setSystemCertificate(certificate);
      }

      return { success: true };
    } catch (err: any) {
      const error = { general: 'Invalid certificate PEM: ' + err.message };
      setErrors(error);
      return { success: false, error: error.general };
    }
  }, []);

  // Verification using stored keys
  const verifyData = useCallback(async (data: string, signatureHex: string, algorithm?: string) => {
    if (!keyPair) {
      throw new Error('No key pair available for verification');
    }

    try {
      const alg = algorithm || keyPair.algorithm;
      return await signatureProvider.verifyWithHex(data, signatureHex, keyPair.publicKey, alg);
    } catch (err: any) {
      throw new Error('Verification failed: ' + err.message);
    }
  }, [keyPair]);

  const signData = useCallback(async (data: string, algorithm?: string) => {
    if (!keyPair) {
      throw new Error('No key pair available for signing');
    }

    try {
      const alg = algorithm || keyPair.algorithm;
      const signature = await signatureProvider.signWithHexKey(data, keyPair.privateKey, alg);
      return signatureProvider.keyOrSigToString(signature);
    } catch (err: any) {
      throw new Error('Signing failed: ' + err.message);
    }
  }, [keyPair]);

  // Helper functions
  const hasKeyPair = () => !!keyPair;
  const hasUserCertificate = () => !!userCertificate;
  const hasSystemCertificate = () => !!systemCertificate;
  const getKeyPairAlgorithm = () => keyPair?.algorithm;

  const getPublicKeyFromCertificate = (certificate: CertificateDetails): string => {
    return certificate.certificateBase64;
  };

  return {
    loading,
    errors,
    keyPair,
    userCertificate,
    systemCertificate,

    // Key pair operations
    generateKeyPair,
    importKeyPairFromHex,
    importKeyPairFromPEM,
    exportKeyPairAsPEM,
    exportKeyPairAsHex,
    clearKeyPair,

    // Certificate operations
    createCertificate,
    fetchUserCertificate,
    fetchSystemCertificate,
    exportCertificateAsPEM,
    importCertificateFromPEM,

    // Crypto operations
    signData,
    verifyData,

    // Helper functions
    hasKeyPair,
    hasUserCertificate,
    hasSystemCertificate,
    getKeyPairAlgorithm,
    getPublicKeyFromCertificate,
    clearErrors
  };
}