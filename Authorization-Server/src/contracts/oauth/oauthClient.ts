export interface OAuthClient {
  id: string;
  ownerId: string;
  clientId: string;
  clientName: string;
  clientSecretHash: string;
  redirectUris: string[];
  grantTypes: Array<"client_credentials" | "authorization_code" | "refresh_token">;
  scopes: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface OAuthClientPublicView {
  id: string;
  clientId: string;
  clientName: string;
  grantTypes: OAuthClient["grantTypes"];
  scopes: string[];
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

