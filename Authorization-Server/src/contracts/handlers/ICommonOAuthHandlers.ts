import { NextFunction, Request, Response } from "express";

export interface ICommonOAuthHandlers {
  introspectToken(req: Request, res: Response, next: NextFunction): Promise<void>;
  revokeToken(req: Request, res: Response, next: NextFunction): Promise<void>;
  getUserInfo(req: Request, res: Response, next: NextFunction): Promise<void>;
}

