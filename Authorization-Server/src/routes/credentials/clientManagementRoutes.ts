import { Router } from "express";
import { IManagementHandlers } from "../../contracts/handlers/IManagementHandlers";

export function createClientManagementRoutes(
  managementHandlers: IManagementHandlers
): Router {
  const router = Router();

    // POST /clients
    // Registers a new machine-to-machine (M2M) application.
    // It generates a new 'client_id' and a strong 'client_secret'.
    // The secret should be returned to the user ONLY ONCE in the response.
  router.post("/clients", managementHandlers.createClient);

    // GET /clients
    // Lists all the M2M applications created by the currently authenticated user.
    // Usually returns an array of objects containing client_id, name, and creation date.
  router.get("/clients", managementHandlers.listClients);

    // GET /clients/:clientId
    // Retrieves the details of a specific application owned by the user.
    // IMPORTANT: This endpoint must NEVER return the 'client_secret'.
  router.get("/clients/:clientId", managementHandlers.getClientById);

    // POST /clients/:clientId/rotate-secret
    // Generates a new 'client_secret' for the application and invalidates the old one.
    // This is crucial for security in case a secret is accidentally leaked or compromised.
  router.post(
    "/clients/:clientId/rotate-secret",
    managementHandlers.rotateClientSecret
  );

    // DELETE /clients/:clientId
    // Deletes the application entirely and revokes all its access.
    // Any future token requests with this client_id will be rejected.
  router.delete("/clients/:clientId", managementHandlers.deleteClient);

  return router;
}
