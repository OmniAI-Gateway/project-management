import { NextFunction, Request, Response } from "express";
import passport from "passport";

import { IGoogleLoginAuthService } from "./IGoogleLoginAuthService";
import { JoseTokenService } from "../../../infrastructure/auth/JoseTokenService";
import { GoogleUser } from "../../../domain/identity/GoogleUser";
import { AuthTokenPayload } from "../../../domain/token/AuthTokenPayload";

type PassportAdapter = Pick<typeof passport, "authenticate">;
type TokenService = Pick<JoseTokenService, "generateToken">;

export function createGoogleLoginAuthService(
  passportAdapter: PassportAdapter = passport,
  tokenService: TokenService = new JoseTokenService()
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
      passportAdapter.authenticate(
        "google",
        { session: false },
        async (err: unknown, user?: GoogleUser) => {
          if (err) {
            next(err);
            return;
          }

          if (!user) {
            res.status(401).json({ message: "Google authentication failed" });
            return;
          }

          try {
            const payload: AuthTokenPayload = {
              sub: user.id,
              email: user.email,
              name: user.name,
              provider: "google",
            };

            const token = await tokenService.generateToken(payload);

            res.status(200).json({
              message: "Google login successful",
              token,
              user,
            });
          } catch (tokenError) {
            next(tokenError);
          }
        }
      )(req, res, next);
    },
  };
}
