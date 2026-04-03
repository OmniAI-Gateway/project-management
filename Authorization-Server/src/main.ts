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

app.get('/health', (_req, res) => res.status(200).json({ status: 'ok' }));
app.get('/auth/success', (req, res) => {
  res.status(200).json({
    message: 'Google login successful',
    token: req.query.token,
    provider: req.query.provider,
  });
});
app.get('/auth/error', (req, res) => {
  res.status(401).json({
    message: 'Google login failed',
    error: req.query.error,
  });
});
app.use('/auth', createGoogleAuthRoutes(googleHandler));
app.use((err: unknown, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  const message = err instanceof Error ? err.message : 'Unexpected error';
  res.status(500).json({ message });
});

app.listen(port, () => console.log(`Servidor a correr em http://localhost:${port}`));
