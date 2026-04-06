import { IDiscoveryHandlers } from "../contracts/handlers/IDiscoveryHandlers";

export function createDiscoveryHandlers(): IDiscoveryHandlers {
  return {
    async getAuthorizationServerMetadata(req, res): Promise<void> {
      const issuer = process.env.ISSUER_URL ?? "http://localhost:3000";

      res.status(200).json({
        issuer,
        token_endpoint: `${issuer}/oauth/token`,
        revocation_endpoint: `${issuer}/oauth/revoke`,
        introspection_endpoint: `${issuer}/oauth/introspect`,
        jwks_uri: `${issuer}/jwks`,
      });
    },

    async getOpenIdConfiguration(req, res): Promise<void> {
      const issuer = process.env.ISSUER_URL ?? "http://localhost:3000";

      res.status(200).json({
        issuer,
        authorization_endpoint: `${issuer}/auth/google`,
        token_endpoint: `${issuer}/oauth/token`,
        userinfo_endpoint: `${issuer}/oauth/userinfo`,
        jwks_uri: `${issuer}/jwks`,
        response_types_supported: ["code", "token"],
        subject_types_supported: ["public"],
        id_token_signing_alg_values_supported: ["RS256"],
      });
    },
  };
}

