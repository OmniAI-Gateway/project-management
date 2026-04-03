import * as jose from 'jose';
import fs from 'fs/promises';
import path from 'path';
import { createPrivateKey } from 'crypto';
import {ITokenService} from '../../contracts/services/ITokenService';

export class JoseTokenService implements ITokenService {
    private signingCertificatePath = process.env.JWT_SIGNING_CERT_PATH
        ? path.resolve(process.env.JWT_SIGNING_CERT_PATH)
        : path.resolve(process.cwd(), 'src/certificates/signing-certificate.pem');

    private certificatePassphrase = process.env.JWT_SIGNING_CERT_PASSPHRASE;
    private issuer = process.env.JWT_ISSUER ?? 'teu-servidor-auth';

    async generateToken(payload: any): Promise<string> {
        let certificatePem: string;
        try {
            certificatePem = await fs.readFile(this.signingCertificatePath, 'utf8');
        } catch {
            throw new Error(`Signing certificate not found: ${this.signingCertificatePath}`);
        }

        // The certificate file must include a private key block to sign JWTs.
        const privateKey = createPrivateKey({
            key: certificatePem,
            format: 'pem',
            passphrase: this.certificatePassphrase,
        });

        return await new jose.SignJWT(payload)
            .setProtectedHeader({alg: 'RS256'})
            .setIssuedAt()
            .setIssuer(this.issuer)
            .setExpirationTime('2h')
            .sign(privateKey);
    }
}