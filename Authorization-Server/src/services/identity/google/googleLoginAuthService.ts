import { NextFunction, Request, Response } from "express";
import passport from "passport";

import { IGoogleLoginAuthService } from "./IGoogleLoginAuthService";

type PassportAdapter = Pick<typeof passport, "authenticate">;

export function createGoogleLoginAuthService(
  passportAdapter: PassportAdapter = passport
): IGoogleLoginAuthService {
  return {
    async redirectToGoogle(
      req: Request,
      res: Response,
      next: NextFunction
    ): Promise<void> {
      passportAdapter.authenticate("google", {
        scope: ["profile", "email"],
      })(req, res, next);
    },

    async handleGoogleCallback(
      req: Request,
      res: Response,
      next: NextFunction
    ): Promise<void> {
      passportAdapter.authenticate("google", {
        session: false,
      })(req, res, next);
    },
  };
}

