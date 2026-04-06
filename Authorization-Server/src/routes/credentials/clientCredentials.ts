import { Router } from "express";
import { IClientCredentialsGrantHandlers } from "../../contracts/handlers/IClientCredentialsGrantHandlers";

export function createTokensEndpoints(
  grantHandlers: IClientCredentialsGrantHandlers
): Router {
  const router = Router();

  // POST /token
  // OAuth 2.0 token endpoint for grant_type=client_credentials.
  router.post("/token", grantHandlers.issueToken);

  return router;
}

