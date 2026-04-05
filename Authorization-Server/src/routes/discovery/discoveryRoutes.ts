import {Router} from "express";

export function createDiscoveryRoutes(): Router {
    const router = Router()
    router.get("/.well-known/oauth-authorization-server")
    return router
}