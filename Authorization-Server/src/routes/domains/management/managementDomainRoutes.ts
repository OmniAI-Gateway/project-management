import { Router } from "express";
import { IApiKeysHandlers } from "../../../contracts/handlers/IApiKeysHandlers";
import { IManagementHandlers } from "../../../contracts/handlers/IManagementHandlers";
import { createApiKeysRoutes } from "../../credentials/apiKeys";
import { createClientManagementRoutes } from "../../credentials/clientManagementRoutes";

interface ManagementDomainDeps {
  apiKeysHandlers: IApiKeysHandlers;
  managementHandlers: IManagementHandlers;
}

export function createManagementDomainRoutes(deps: ManagementDomainDeps): Router {
  const router = Router();

  router.use("/api", createApiKeysRoutes(deps.apiKeysHandlers));
  router.use("/management", createClientManagementRoutes(deps.managementHandlers));

  return router;
}

