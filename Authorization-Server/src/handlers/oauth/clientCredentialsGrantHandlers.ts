import { IClientCredentialsGrantHandlers } from "../../contracts/handlers/IClientCredentialsGrantHandlers";
import { TokenEndpointRequestBody } from "../../contracts/oauth/endpointDtos";

export function createClientCredentialsGrantHandlers(): IClientCredentialsGrantHandlers {
  return {
    async issueToken(req, res, next): Promise<void> {
      try {
        const body = req.body as Partial<TokenEndpointRequestBody>;

        if (body.grant_type !== "client_credentials") {
          res.status(400).json({
            error: "unsupported_grant_type",
            error_description: "only client_credentials is supported",
          });
          return;
        }

        res.status(501).json({
          error: "not_implemented",
          error_description: "client credentials token issuance is not implemented yet",
        });
      } catch (error) {
        next(error);
      }
    },
  };
}

