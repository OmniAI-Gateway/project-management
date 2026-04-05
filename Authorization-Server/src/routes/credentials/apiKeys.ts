import {Router} from "express";
import {IApiKeysHandlers} from "../../contracts/handlers/IApiKeysHandlers";

export function createApiKeysRoutes(
    certificateHandlers: IApiKeysHandlers
): Router {
    const router = Router();

    router.post("/api-keys",);

    router.get("/api-keys")

    router.delete("/api-keys/:id")

    return router;
}

