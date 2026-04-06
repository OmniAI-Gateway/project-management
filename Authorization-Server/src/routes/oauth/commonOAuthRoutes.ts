import { Router } from "express";
import { ICommonOAuthHandlers } from "../../contracts/handlers/ICommonOAuthHandlers";

export function createCommonOAuthRoutes(
  commonOAuthHandlers: ICommonOAuthHandlers
): Router {
  const router = Router();

  // RFC 7662 token introspection endpoint.
  router.post("/introspect", commonOAuthHandlers.introspectToken);

  // RFC 7009 token revocation endpoint.
  router.post("/revoke", commonOAuthHandlers.revokeToken);

  // OIDC userinfo endpoint.
  router.get("/userinfo", commonOAuthHandlers.getUserInfo);

  return router;
}

