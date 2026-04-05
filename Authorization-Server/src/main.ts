import express from 'express';
import passport from 'passport';
import dotenv from 'dotenv';
import { setupGoogleStrategy } from './infrastructure/auth/googleStrategy';
import { createGoogleLoginAuthService } from './services/identity/google/googleLoginAuthService';
import { createHandlersLoginWithGoogle } from './handlers/HandlersLoginWithGoogle';
import { createGoogleAuthRoutes } from './routes/login/google/googleAuthRoutes';
import { createCertificateService } from './services/certificates/certificateService';
import { createCertificateHandlers } from './handlers/certificateHandlers';
import { createCertificateRoutes } from './routes/certificates/certificateRoutes';
import {createApiKeysRoutes} from "./routes/credentials/apiKeys";
import {createTokensEndpoints} from "./routes/credentials/clientCredentials";
import {createClientManagementRoutes} from "./routes/credentials/clientManagementRoutes";

const app = express();
dotenv.config();

setupGoogleStrategy();
app.use(passport.initialize());

const googleService = createGoogleLoginAuthService();
const googleHandler = createHandlersLoginWithGoogle(googleService);
const certificateService = createCertificateService();
const certificateHandlers = createCertificateHandlers(certificateService);
const port = Number(process.env.PORT ?? 3000);

app.use('/auth', createGoogleAuthRoutes(googleHandler));
app.use('/', createCertificateRoutes(certificateHandlers));
app.use("/api",createApiKeysRoutes)
app.use("/oauth", createTokensEndpoints)
app.use("/management", createClientManagementRoutes)

app.listen(
    port,
    () => console.log(`Servidor a correr em http://localhost:${port}`)
);
