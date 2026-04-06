import { AuthTokenPayload } from "../../domain/token/AuthTokenPayload";

export interface AccessTokenPayload extends AuthTokenPayload {
  iss: string;
  aud: string | string[];
  exp: number;
  iat: number;
  scope?: string;
  client_id?: string;
  token_use: "access";
}

export interface IdTokenPayload extends AuthTokenPayload {
  iss: string;
  aud: string;
  exp: number;
  iat: number;
  auth_time?: number;
  nonce?: string;
  token_use: "id";
}

