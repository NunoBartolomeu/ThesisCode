interface SignatureAlgorithm {
  name: string;
  sign(data: ArrayBuffer, privateKey: CryptoKey): Promise<ArrayBuffer>;
  verify(data: ArrayBuffer, signature: ArrayBuffer, publicKey: CryptoKey): Promise<boolean>;
  generateKeyPair(): Promise<CryptoKeyPair>;
  bytesToPublicKey(encodedPublicKey: ArrayBuffer): Promise<CryptoKey>;
  bytesToPrivateKey(encodedPrivateKey: ArrayBuffer): Promise<CryptoKey>;
}

class ECSignatureAlgorithm implements SignatureAlgorithm {
  name = "ECDSA";
  
  private readonly algorithm = {
    name: "ECDSA",
    namedCurve: "P-256"
  };
  
  private readonly signAlgorithm = {
    name: "ECDSA",
    hash: { name: "SHA-256" }
  };

  async sign(data: ArrayBuffer, privateKey: CryptoKey): Promise<ArrayBuffer> {
    return await crypto.subtle.sign(this.signAlgorithm, privateKey, data);
  }

  async verify(data: ArrayBuffer, signature: ArrayBuffer, publicKey: CryptoKey): Promise<boolean> {
    return await crypto.subtle.verify(this.signAlgorithm, publicKey, signature, data);
  }

  async generateKeyPair(): Promise<CryptoKeyPair> {
    return await crypto.subtle.generateKey(
      this.algorithm,
      true,
      ["sign", "verify"]
    );
  }

  async bytesToPublicKey(encodedPublicKey: ArrayBuffer): Promise<CryptoKey> {
    return await crypto.subtle.importKey(
      "spki",
      encodedPublicKey,
      this.algorithm,
      true,
      ["verify"]
    );
  }

  async bytesToPrivateKey(encodedPrivateKey: ArrayBuffer): Promise<CryptoKey> {
    return await crypto.subtle.importKey(
      "pkcs8",
      encodedPrivateKey,
      this.algorithm,
      true,
      ["sign"]
    );
  }
}

class RSASignatureAlgorithm implements SignatureAlgorithm {
  name = "RSA";
  
  private keySize: number;
  
  private readonly algorithm = {
    name: "RSA-PSS",
    modulusLength: 2048,
    publicExponent: new Uint8Array([1, 0, 1]),
    hash: "SHA-256"
  };
  
  private readonly signAlgorithm = {
    name: "RSA-PSS",
    saltLength: 32
  };

  constructor(keySize: number = 2048) {
    this.keySize = keySize;
    this.algorithm.modulusLength = keySize;
  }

  async sign(data: ArrayBuffer, privateKey: CryptoKey): Promise<ArrayBuffer> {
    return await crypto.subtle.sign(this.signAlgorithm, privateKey, data);
  }

  async verify(data: ArrayBuffer, signature: ArrayBuffer, publicKey: CryptoKey): Promise<boolean> {
    return await crypto.subtle.verify(this.signAlgorithm, publicKey, signature, data);
  }

  async generateKeyPair(): Promise<CryptoKeyPair> {
    return await crypto.subtle.generateKey(
      this.algorithm,
      true,
      ["sign", "verify"]
    );
  }

  async bytesToPublicKey(encodedPublicKey: ArrayBuffer): Promise<CryptoKey> {
    return await crypto.subtle.importKey(
      "spki",
      encodedPublicKey,
      {
        name: "RSA-PSS",
        hash: "SHA-256"
      },
      true,
      ["verify"]
    );
  }

  async bytesToPrivateKey(encodedPrivateKey: ArrayBuffer): Promise<CryptoKey> {
    return await crypto.subtle.importKey(
      "pkcs8",
      encodedPrivateKey,
      {
        name: "RSA-PSS",
        hash: "SHA-256"
      },
      true,
      ["sign"]
    );
  }
}

class SignatureProvider {
  private algorithms: Map<string, SignatureAlgorithm> = new Map();

  constructor() {
    // Hardcoded algorithm registration
    const algos = [
      new ECSignatureAlgorithm(),
      new RSASignatureAlgorithm()
    ];

    algos.forEach(algo => {
      this.algorithms.set(algo.name, algo);
      console.log(`Registered Signature Algorithm: ${algo.name}`);
    });

    if (this.algorithms.size === 0) {
      throw new Error("No SignatureAlgorithm implementations registered.");
    }
  }

  keyOrSigToString(bytes: Uint8Array): string {
    return Array.from(bytes)
      .map(byte => byte.toString(16).padStart(2, '0'))
      .join('');
  }

  keyOrSigToByteArray(hex: string): Uint8Array {
    const bytes = hex.match(/.{1,2}/g)?.map(byte => parseInt(byte, 16)) || [];
    return new Uint8Array(bytes);
  }

  dataToString(data: Uint8Array): string {
    const decoder = new TextDecoder();
    return decoder.decode(data);
  }

  dataToByteArray(data: string): Uint8Array {
    const encoder = new TextEncoder();
    return encoder.encode(data);
  }

  private resolve(name: string): SignatureAlgorithm {
    const algorithm = this.algorithms.get(name);
    if (!algorithm) {
      throw new Error(`Unsupported signature algorithm: ${name}`);
    }
    return algorithm;
  }

  private toArrayBuffer(uint8Array: Uint8Array): ArrayBuffer {
    if (uint8Array.buffer instanceof ArrayBuffer) {
      return uint8Array.buffer.slice(uint8Array.byteOffset, uint8Array.byteOffset + uint8Array.byteLength);
    }
    const arrayBuffer = new ArrayBuffer(uint8Array.byteLength);
    new Uint8Array(arrayBuffer).set(uint8Array);
    return arrayBuffer;
  }

  async sign(data: string, privateKey: CryptoKey, algorithm: string): Promise<Uint8Array> {
    const dataArray = this.dataToByteArray(data);
    const dataBuffer = this.toArrayBuffer(dataArray);
    const result = await this.resolve(algorithm).sign(dataBuffer, privateKey);
    return new Uint8Array(result);
  }

  async signWithEncodedKey(data: string, encodedPrivateKey: Uint8Array, algorithm: string): Promise<Uint8Array> {
    const algo = this.resolve(algorithm);
    const keyBuffer = this.toArrayBuffer(encodedPrivateKey);
    const privateKey = await algo.bytesToPrivateKey(keyBuffer);
    const dataArray = this.dataToByteArray(data);
    const dataBuffer = this.toArrayBuffer(dataArray);
    const result = await algo.sign(dataBuffer, privateKey);
    return new Uint8Array(result);
  }

  async signWithHexKey(data: string, hexPrivateKey: string, algorithm: string): Promise<Uint8Array> {
    return await this.signWithEncodedKey(data, this.keyOrSigToByteArray(hexPrivateKey), algorithm);
  }

  async verify(data: string, signature: Uint8Array, publicKey: CryptoKey, algorithm: string): Promise<boolean> {
    const dataArray = this.dataToByteArray(data);
    const dataBuffer = this.toArrayBuffer(dataArray);
    const sigBuffer = this.toArrayBuffer(signature);
    return await this.resolve(algorithm).verify(dataBuffer, sigBuffer, publicKey);
  }

  async verifyWithEncodedKey(data: string, signature: Uint8Array, encodedPublicKey: Uint8Array, algorithm: string): Promise<boolean> {
    const algo = this.resolve(algorithm);
    const keyBuffer = this.toArrayBuffer(encodedPublicKey);
    const publicKey = await algo.bytesToPublicKey(keyBuffer);
    const dataArray = this.dataToByteArray(data);
    const dataBuffer = this.toArrayBuffer(dataArray);
    const sigBuffer = this.toArrayBuffer(signature);
    return await algo.verify(dataBuffer, sigBuffer, publicKey);
  }

  async verifyWithHex(data: string, hexSignature: string, hexPublicKey: string, algorithm: string): Promise<boolean> {
    return await this.verifyWithEncodedKey(
      data,
      this.keyOrSigToByteArray(hexSignature),
      this.keyOrSigToByteArray(hexPublicKey),
      algorithm
    );
  }

  async generateKeyPair(algorithm: string): Promise<CryptoKeyPair> {
    return await this.resolve(algorithm).generateKeyPair();
  }

  isSupported(name: string): boolean {
    return this.algorithms.has(name);
  }

  getSupportedAlgorithms(): Set<string> {
    return new Set(this.algorithms.keys());
  }

  getDefaultAlgorithm(): string {
    const firstKey = Array.from(this.algorithms.keys())[0];
    if (!firstKey) {
      throw new Error("No algorithms available");
    }
    return firstKey;
  }
}

export const signatureProvider = new SignatureProvider();