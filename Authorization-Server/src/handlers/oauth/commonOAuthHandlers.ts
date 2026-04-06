import { ICommonOAuthHandlers } from "../../contracts/handlers/ICommonOAuthHandlers";
import {
  IntrospectEndpointRequestBody,
  IntrospectEndpointResponseBody,
  RevokeEndpointRequestBody,
  RevokeEndpointResponseBody,
} from "../../contracts/oauth/endpointDtos";

export function createCommonOAuthHandlers(): ICommonOAuthHandlers {
  return {
    async introspectToken(req, res, next): Promise<void> {
      try {
        const body = req.body as Partial<IntrospectEndpointRequestBody>;

        if (!body.token) {
          res.status(400).json({
            error: "invalid_request",
            error_description: "token is required",
          });
          return;
        }

        const response: IntrospectEndpointResponseBody = { active: false };
        res.status(200).json(response);
      } catch (error) {
        next(error);
      }
    },

    async revokeToken(req, res, next): Promise<void> {
      try {
        const body = req.body as Partial<RevokeEndpointRequestBody>;

        if (!body.token) {
          res.status(400).json({
            error: "invalid_request",
            error_description: "token is required",
          });
          return;
        }

        const response: RevokeEndpointResponseBody = { revoked: true };
        res.status(200).json(response);
      } catch (error) {
        next(error);
      }
    },

    async getUserInfo(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "userinfo endpoint is not implemented yet",
      });
    },
  };
}

