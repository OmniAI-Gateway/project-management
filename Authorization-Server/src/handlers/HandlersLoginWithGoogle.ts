import { IHandlersLoginWithGoogle } from "../contracts/handlers/IHandlersLoginWithGoogle";
import { IGoogleLoginAuthService } from "../services/identity/google/IGoogleLoginAuthService";

export function createHandlersLoginWithGoogle(
  googleLoginAuthService: IGoogleLoginAuthService
): IHandlersLoginWithGoogle {
  return {
    redirectToGoogle: (req, res, next) =>
      googleLoginAuthService.redirectToGoogle(req, res, next),
    handleGoogleCallback: (req, res, next) =>
      googleLoginAuthService.handleGoogleCallback(req, res, next),
  };
}