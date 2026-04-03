import { Router } from "express";

import { ICertificateHandlers } from "../../contracts/handlers/ICertificateHandlers";

export function createCertificateRoutes(
  certificateHandlers: ICertificateHandlers
): Router {
  const router = Router();

  router.get("/certificates", certificateHandlers.listCertificates);

  return router;
}

