import {Router} from "express";

export function createClientManagementRoutes(): Router {
    const router = Router();

    // POST /clients
    // Registers a new machine-to-machine (M2M) application.
    // It generates a new 'client_id' and a strong 'client_secret'.
    // The secret should be returned to the user ONLY ONCE in the response.
    router.post("/clients")

    // GET /clients
    // Lists all the M2M applications created by the currently authenticated user.
    // Usually returns an array of objects containing client_id, name, and creation date.
    router.get("/clients")

    // GET /clients/:clientId
    // Retrieves the details of a specific application owned by the user.
    // IMPORTANT: This endpoint must NEVER return the 'client_secret'.
    router.get("/clients/:clientId")

    // POST /clients/:clientId/rotate-secret
    // Generates a new 'client_secret' for the application and invalidates the old one.
    // This is crucial for security in case a secret is accidentally leaked or compromised.
    router.post("/clients/:clientId/rotate-secret")

    // DELETE /clients/:clientId
    // Deletes the application entirely and revokes all its access.
    // Any future token requests with this client_id will be rejected.
    router.delete("/clients/:clientId")

    return router;
}
