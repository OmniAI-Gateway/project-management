import { NextFunction, Request, Response } from "express";

export interface IClientCredentialsGrantHandlers {
  issueToken(req: Request, res: Response, next: NextFunction): Promise<void>;
}

