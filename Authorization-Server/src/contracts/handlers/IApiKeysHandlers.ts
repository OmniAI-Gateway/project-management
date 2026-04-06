import { NextFunction, Request, Response } from "express";

export interface IApiKeysHandlers {
  createApiKey(req: Request, res: Response, next: NextFunction): Promise<void>;
  listApiKeys(req: Request, res: Response, next: NextFunction): Promise<void>;
  revokeApiKey(req: Request, res: Response, next: NextFunction): Promise<void>;
}