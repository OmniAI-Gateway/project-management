import { Request, Response, NextFunction } from 'express';

export interface IHandlersLoginWithGoogle {
    /**
     * Redirects the user to Google's consent screen.
     */
    redirectToGoogle(req: Request, res: Response, next: NextFunction): Promise<void>;

    /**
     * Handles the callback code returned by Google after login.
     */
    handleGoogleCallback(req: Request, res: Response, next: NextFunction): Promise<void>;
}