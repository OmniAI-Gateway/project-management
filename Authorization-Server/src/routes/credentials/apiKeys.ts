import { Router } from "express";
import { IApiKeysHandlers } from "../../contracts/handlers/IApiKeysHandlers";

export function createApiKeysRoutes(
  apiKeysHandlers: IApiKeysHandlers
): Router {
  const router = Router();

  router.post("/api-keys", apiKeysHandlers.createApiKey);

  router.get("/api-keys", apiKeysHandlers.listApiKeys);

  router.delete("/api-keys/:id", apiKeysHandlers.revokeApiKey);

  return router;
}
