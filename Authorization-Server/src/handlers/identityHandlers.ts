import { IIdentityHandlers } from "../contracts/handlers/IIdentityHandlers";
import { IHandlersLoginWithGoogle } from "../contracts/handlers/IHandlersLoginWithGoogle";

export function createIdentityHandlers(
  loginGoogle: IHandlersLoginWithGoogle
): IIdentityHandlers {
  return {
    loginGoogle,
  };
}

