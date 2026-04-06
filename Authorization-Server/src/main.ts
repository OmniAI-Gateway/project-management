import express from 'express';
import passport from 'passport';
import dotenv from 'dotenv';
import { setupGoogleStrategy } from './infrastructure/auth/googleStrategy';
import { createGoogleLoginAuthService } from './services/identity/google/googleLoginAuthService';
import { createHandlersLoginWithGoogle } from './handlers/HandlersLoginWithGoogle';
import { createCertificateService } from './services/certificates/certificateService';
import { createCertificateHandlers } from './handlers/certificateHandlers';
import { createCommonOAuthHandlers } from './handlers/oauth/commonOAuthHandlers';
import { createClientCredentialsGrantHandlers } from './handlers/oauth/clientCredentialsGrantHandlers';
import { createApiKeysHandlers } from './handlers/management/apiKeysHandlers';
import { createManagementHandlers } from './handlers/management/clientManagementHandlers';
import { createDiscoveryHandlers } from './handlers/discoveryHandlers';
import { createCommonOAuthDomainRoutes } from './routes/domains/commonOAuth/commonOAuthDomainRoutes';
import { createGrantsDomainRoutes } from './routes/domains/grants/grantsDomainRoutes';
import { createManagementDomainRoutes } from './routes/domains/management/managementDomainRoutes';

const app = express();
dotenv.config();

setupGoogleStrategy();
app.use(passport.initialize());
app.use(express.json());

const googleService = createGoogleLoginAuthService();
const googleHandler = createHandlersLoginWithGoogle(googleService);
const certificateService = createCertificateService();
const certificateHandlers = createCertificateHandlers(certificateService);
const commonOAuthHandlers = createCommonOAuthHandlers();
const clientCredentialsGrantHandlers = createClientCredentialsGrantHandlers();
const apiKeysHandlers = createApiKeysHandlers();
const managementHandlers = createManagementHandlers();
const discoveryHandlers = createDiscoveryHandlers();
const port = Number(process.env.PORT ?? 3000);

app.use(
  '/',
  createCommonOAuthDomainRoutes({
    certificateHandlers,
    commonOAuthHandlers,
    discoveryHandlers,
  })
);
app.use(
  '/',
  createGrantsDomainRoutes({
    googleHandlers: googleHandler,
    clientCredentialsGrantHandlers,
  })
);
app.use(
  '/',
  createManagementDomainRoutes({
    apiKeysHandlers,
    managementHandlers,
  })
);

app.listen(
    port,
    () => console.log(`Servidor a correr em http://localhost:${port}`)
);
