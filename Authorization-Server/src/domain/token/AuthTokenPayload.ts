export interface AuthTokenPayload extends Record<string, unknown> {
  sub: string;
  email?: string;
  name?: string;
  provider?: string;
}

