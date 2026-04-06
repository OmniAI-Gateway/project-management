import { ApiKeyPublicView } from "./apiKey";
import { OAuthClientPublicView } from "./oauthClient";

export interface TokenEndpointRequestBody {
  grant_type: "client_credentials";
  client_id?: string;
  client_secret?: string;
  scope?: string;
}

export interface TokenEndpointResponseBody {
  access_token: string;
  token_type: "Bearer";
  expires_in: number;
  scope?: string;
}

export interface IntrospectEndpointRequestBody {
  token: string;
  token_type_hint?: "access_token" | "refresh_token";
}

export interface IntrospectEndpointResponseBody {
  active: boolean;
  scope?: string;
  client_id?: string;
  username?: string;
  token_type?: "Bearer";
  exp?: number;
  iat?: number;
  nbf?: number;
  sub?: string;
  aud?: string | string[];
  iss?: string;
  jti?: string;
}

export interface RevokeEndpointRequestBody {
  token: string;
  token_type_hint?: "access_token" | "refresh_token";
}

export interface RevokeEndpointResponseBody {
  revoked: true;
}

export interface UserInfoEndpointResponseBody {
  sub: string;
  email?: string;
  name?: string;
  picture?: string;
}

export interface CreateOAuthClientRequestBody {
  clientName: string;
  redirectUris?: string[];
  grantTypes: Array<"client_credentials" | "authorization_code" | "refresh_token">;
  scopes?: string[];
}

export interface CreateOAuthClientResponseBody {
  client: OAuthClientPublicView;
  clientSecret: string;
}

export interface RotateClientSecretResponseBody {
  clientId: string;
  clientSecret: string;
}

export interface CreateApiKeyRequestBody {
  name: string;
  scopes?: string[];
  expiresAt?: string;
}

export interface CreateApiKeyResponseBody {
  apiKey: ApiKeyPublicView;
  rawKey: string;
}

