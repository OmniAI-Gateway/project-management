import { Router } from "express";

import { IHandlersLoginWithGoogle } from "../../../contracts/handlers/IHandlersLoginWithGoogle";

export function createGoogleAuthRoutes(
  googleHandlers: IHandlersLoginWithGoogle
): Router {
  const router = Router();

  router.get("/google", googleHandlers.redirectToGoogle);
  router.get("/google/callback", googleHandlers.handleGoogleCallback);

  return router;
}

