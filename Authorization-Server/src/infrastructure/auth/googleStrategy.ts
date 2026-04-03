import passport from 'passport';
import { Strategy as GoogleStrategy } from 'passport-google-oauth20';
import { GoogleUser } from '../../domain/identity/GoogleUser';

function getRequiredEnv(name: string): string {
    const value = process.env[name];
    if (!value) {
        throw new Error(`Missing required environment variable: ${name}`);
    }
    return value;
}

export const setupGoogleStrategy = () => {
    const clientID = getRequiredEnv('GOOGLE_CLIENT_ID');
    const clientSecret = getRequiredEnv('GOOGLE_CLIENT_SECRET');
    const callbackURL = getRequiredEnv('GOOGLE_CALLBACK_URL');

    passport.use(
        new GoogleStrategy(
            {
                clientID,
                clientSecret,
                callbackURL,
            },
            (_accessToken, _refreshToken, profile, done) => {
                console.log('Perfil recebido do Google:', profile.id);

                const user: GoogleUser = {
                    id: profile.id,
                    email: profile.emails?.[0]?.value,
                    name: profile.displayName
                };

                return done(null, user);
            }
        )
    );
};