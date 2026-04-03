export interface ITokenService {
    generateToken(payload: any): Promise<string>;
}