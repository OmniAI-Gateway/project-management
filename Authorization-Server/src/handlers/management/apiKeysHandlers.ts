import { IApiKeysHandlers } from "../../contracts/handlers/IApiKeysHandlers";

export function createApiKeysHandlers(): IApiKeysHandlers {
  return {
    async createApiKey(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "create api key is not implemented yet",
      });
    },

    async listApiKeys(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "list api keys is not implemented yet",
      });
    },

    async revokeApiKey(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "revoke api key is not implemented yet",
      });
    },
  };
}

