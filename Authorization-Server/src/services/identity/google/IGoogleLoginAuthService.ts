import { NextFunction, Request, Response } from "express";

export interface IGoogleLoginAuthService {
  redirectToGoogle(req: Request, res: Response, next: NextFunction): Promise<void>;
  handleGoogleCallback(req: Request, res: Response, next: NextFunction): Promise<void>;
}

