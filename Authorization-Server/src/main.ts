import express from 'express';
import passport from 'passport';
import dotenv from 'dotenv';
import { setupGoogleStrategy } from './infrastructure/auth/googleStrategy';
import { createGoogleLoginAuthService } from './services/identity/google/googleLoginAuthService';
import { createHandlersLoginWithGoogle } from './handlers/HandlersLoginWithGoogle';
import { createGoogleAuthRoutes } from './routes/login/google/googleAuthRoutes';

const app = express();
dotenv.config();

setupGoogleStrategy();
app.use(passport.initialize());

const googleService = createGoogleLoginAuthService();
const googleHandler = createHandlersLoginWithGoogle(googleService);
const port = Number(process.env.PORT ?? 3000);

app.use('/auth', createGoogleAuthRoutes(googleHandler));

app.listen(
    port,
    () => console.log(`Servidor a correr em http://localhost:${port}`)
);
