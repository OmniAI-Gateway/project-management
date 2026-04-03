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
app.use('/auth', createCertificateRoutes(certificateHandlers));

app.listen(
    port,
    () => console.log(`Servidor a correr em http://localhost:${port}`)
);
