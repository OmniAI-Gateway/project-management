package org.omniai.sdk.interceptors.auth.interfaces

/**
 * Abstrai o acesso a infraestrutura de segurança (JWKS + introspecção),
 * permitindo trocar facilmente entre implementações (HTTP, cache, mocks, etc.).
 */
interface AuthSecurityInfrastructure : PublicKeysProvider, TokenIntrospector