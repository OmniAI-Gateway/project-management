import { Router } from "express";
import { IDiscoveryHandlers } from "../../contracts/handlers/IDiscoveryHandlers";

export function createDiscoveryRoutes(discoveryHandlers: IDiscoveryHandlers): Router {
  const router = Router();

  router.get(
    "/.well-known/oauth-authorization-server",
    discoveryHandlers.getAuthorizationServerMetadata
  );

  router.get(
    "/.well-known/openid-configuration",
    discoveryHandlers.getOpenIdConfiguration
  );

  return router;
}