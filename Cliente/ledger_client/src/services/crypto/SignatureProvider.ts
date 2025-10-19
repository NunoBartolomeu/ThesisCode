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
    const rawSig = await crypto.subtle.sign(this.signAlgorithm, privateKey, data);
    return this.rawToDER(rawSig);
  }

  async verify(data: ArrayBuffer, signature: ArrayBuffer, publicKey: CryptoKey): Promise<boolean> {
    const rawSig = this.derToRaw(signature);
    return await crypto.subtle.verify(this.signAlgorithm, publicKey, rawSig, data);
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

  private rawToDER(rawSig: ArrayBuffer): ArrayBuffer {
    const view = new Uint8Array(rawSig);
    const r = view.slice(0, 32);
    const s = view.slice(32, 64);

    const rDER = this.integerToDER(r);
    const sDER = this.integerToDER(s);

    const sequence = new Uint8Array(2 + rDER.length + sDER.length);
    sequence[0] = 0x30; // SEQUENCE tag
    sequence[1] = rDER.length + sDER.length;
    sequence.set(rDER, 2);
    sequence.set(sDER, 2 + rDER.length);

    return sequence.buffer.slice(sequence.byteOffset, sequence.byteOffset + sequence.byteLength);
  }

  private derToRaw(derSig: ArrayBuffer): ArrayBuffer {
    const view = new Uint8Array(derSig);
    let offset = 0;

    if (view[offset] !== 0x30) throw new Error("Invalid DER signature");
    offset++;
    const seqLen = view[offset];
    offset++;

    const [r, rLen] = this.derToInteger(view, offset);
    offset += 2 + rLen;

    const [s, sLen] = this.derToInteger(view, offset);

    const raw = new Uint8Array(64);
    raw.set(r.slice(-32), 0);
    raw.set(s.slice(-32), 32);

    return raw.buffer.slice(raw.byteOffset, raw.byteOffset + raw.byteLength);
  }

  private integerToDER(bytes: Uint8Array): Uint8Array {
    let value = bytes;
    if (bytes[0] & 0x80) {
      value = new Uint8Array(bytes.length + 1);
      value[0] = 0x00;
      value.set(bytes, 1);
    }
    const result = new Uint8Array(2 + value.length);
    result[0] = 0x02; // INTEGER tag
    result[1] = value.length;
    result.set(value, 2);
    return result;
  }

  private derToInteger(view: Uint8Array, offset: number): [Uint8Array, number] {
    if (view[offset] !== 0x02) throw new Error("Invalid DER integer");
    const len = view[offset + 1];
    const value = view.slice(offset + 2, offset + 2 + len);
    return [value, len];
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