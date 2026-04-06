import { IManagementHandlers } from "../../contracts/handlers/IManagementHandlers";

export function createManagementHandlers(): IManagementHandlers {
  return {
    async createClient(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "create client is not implemented yet",
      });
    },

    async listClients(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "list clients is not implemented yet",
      });
    },

    async getClientById(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "get client by id is not implemented yet",
      });
    },

    async rotateClientSecret(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "rotate client secret is not implemented yet",
      });
    },

    async deleteClient(req, res): Promise<void> {
      res.status(501).json({
        error: "not_implemented",
        error_description: "delete client is not implemented yet",
      });
    },
  };
}

