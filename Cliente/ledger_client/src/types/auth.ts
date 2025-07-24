export interface User {
  email: string;
  fullName: string;
}

export interface Token {
  accessToken: string;
  expiresAt: Date;
}

export interface AuthSession {
  user: User | null;
  token: Token | null;
}
