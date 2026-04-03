import { AuthTokenPayload } from "../../domain/token/AuthTokenPayload";

export interface ITokenService {
  generateToken(payload: AuthTokenPayload): Promise<string>;
}