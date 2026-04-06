import { Router } from "express";
import { ICertificateHandlers } from "../../../contracts/handlers/ICertificateHandlers";
import { ICommonOAuthHandlers } from "../../../contracts/handlers/ICommonOAuthHandlers";
import { IDiscoveryHandlers } from "../../../contracts/handlers/IDiscoveryHandlers";
import { createCertificateRoutes } from "../../certificates/certificateRoutes";
import { createDiscoveryRoutes } from "../../discovery/discoveryRoutes";
import { createCommonOAuthRoutes } from "../../oauth/commonOAuthRoutes";

interface CommonOAuthDomainDeps {
  certificateHandlers: ICertificateHandlers;
  commonOAuthHandlers: ICommonOAuthHandlers;
  discoveryHandlers: IDiscoveryHandlers;
}

export function createCommonOAuthDomainRoutes(
  deps: CommonOAuthDomainDeps
): Router {
  const router = Router();

  // Keep shared protocol endpoints centralized without changing public URLs.
  router.use("/", createCertificateRoutes(deps.certificateHandlers));
  router.use("/", createDiscoveryRoutes(deps.discoveryHandlers));
  router.use("/oauth", createCommonOAuthRoutes(deps.commonOAuthHandlers));

  return router;
}

