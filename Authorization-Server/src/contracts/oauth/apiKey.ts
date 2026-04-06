export interface ApiKey {
  id: string;
  ownerId: string;
  keyPrefix: string;
  keyHash: string;
  name: string;
  scopes: string[];
  lastUsedAt?: string;
  expiresAt?: string;
  revokedAt?: string;
  createdAt: string;
}

export interface ApiKeyPublicView {
  id: string;
  keyPrefix: string;
  name: string;
  scopes: string[];
  lastUsedAt?: string;
  expiresAt?: string;
  revokedAt?: string;
  createdAt: string;
}

