import { Router } from "express";

export function createTokensEndpoints(): Router {
    const router = Router()

    // POST /introspect
    // OAuth 2.0 Token Introspection Endpoint (RFC 7662).
    // Allows APIs to query the Authorization Server to determine the active state
    // of an OAuth 2.0 token and to determine meta-information about this token.
    router.post("/introspect");

    // POST /revoke
    // OAuth 2.0 Token Revocation Endpoint (RFC 7009).
    // Allows clients to notify the Authorization Server that a previously obtained
    // refresh or access token is no longer needed, so it can be invalidated.
    router.post("/revoke");

    // GET /userinfo
    // OpenID Connect UserInfo Endpoint.
    // Takes a valid Access Token (sent via Bearer header) and returns a JSON
    // object with profile information about the authenticated human user.
    router.get("/userinfo");

    // POST /token
    // The standard OAuth 2.0 Token Endpoint.
    // M2M clients will call this endpoint providing 'grant_type=client_credentials'
    // along with their 'client_id' and 'client_secret' (usually via Basic Auth header).
    // If valid, it returns a signed JWT access token.
    router.post("/token")

    return router;
}