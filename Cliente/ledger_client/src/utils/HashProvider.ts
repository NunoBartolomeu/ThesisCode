interface HashAlgorithm {
  name: string;
  hash(data: string): Promise<Uint8Array>;
}

class SHA256Algorithm implements HashAlgorithm {
  name = "SHA-256";
  
  async hash(data: string): Promise<Uint8Array> {
    const encoder = new TextEncoder();
    const dataBuffer = encoder.encode(data);
    const hashBuffer = await crypto.subtle.digest('SHA-256', dataBuffer);
    return new Uint8Array(hashBuffer);
  }
}

class SHA512Algorithm implements HashAlgorithm {
  name = "SHA-512";
  
  async hash(data: string): Promise<Uint8Array> {
    const encoder = new TextEncoder();
    const dataBuffer = encoder.encode(data);
    const hashBuffer = await crypto.subtle.digest('SHA-512', dataBuffer);
    return new Uint8Array(hashBuffer);
  }
}

class HashProvider {
  private algorithms: Map<string, HashAlgorithm> = new Map();

  constructor() {
    const algos = [
      new SHA256Algorithm(),
      new SHA512Algorithm(),
      // SHA3 is not natively supported
    ];

    algos.forEach(algo => {
      this.algorithms.set(algo.name, algo);
      console.log(`Registered Hash Algorithm: ${algo.name}`);
    });

    if (this.algorithms.size === 0) {
      throw new Error("No HashAlgorithm implementations registered.");
    }
  }

  toHashString(byteArray: Uint8Array): string {
    return Array.from(byteArray)
      .map(byte => byte.toString(16).padStart(2, '0'))
      .join('');
  }

  toHashByteArray(string: string): Uint8Array {
    const encoder = new TextEncoder();
    return encoder.encode(string);
  }

  async hash(data: string, algorithm: string): Promise<Uint8Array> {
    const hashAlgorithm = this.algorithms.get(algorithm);
    if (!hashAlgorithm) {
      throw new Error(`Unsupported hash algorithm: ${algorithm}`);
    }
    return await hashAlgorithm.hash(data);
  }

  isSupported(algorithmName: string): boolean {
    return this.algorithms.has(algorithmName);
  }

  getSupportedAlgorithms(): Set<string> {
    return new Set(this.algorithms.keys());
  }

  getDefaultAlgorithm(): string {
    const firstKey = this.algorithms.keys().next().value;
    if (!firstKey) {
      throw new Error("No algorithms available");
    }
    return firstKey;
  }
}

export const hashProvider = new HashProvider();