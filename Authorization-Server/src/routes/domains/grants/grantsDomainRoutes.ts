import { Router } from "express";
import { IClientCredentialsGrantHandlers } from "../../../contracts/handlers/IClientCredentialsGrantHandlers";
import { IHandlersLoginWithGoogle } from "../../../contracts/handlers/IHandlersLoginWithGoogle";
import { createTokensEndpoints } from "../../credentials/clientCredentials";
import { createGoogleAuthRoutes } from "../../login/google/googleAuthRoutes";

interface GrantsDomainDeps {
  googleHandlers: IHandlersLoginWithGoogle;
  clientCredentialsGrantHandlers: IClientCredentialsGrantHandlers;
}

export function createGrantsDomainRoutes(deps: GrantsDomainDeps): Router {
  const router = Router();

  router.use("/auth", createGoogleAuthRoutes(deps.googleHandlers));
  router.use("/oauth", createTokensEndpoints(deps.clientCredentialsGrantHandlers));

  return router;
}

