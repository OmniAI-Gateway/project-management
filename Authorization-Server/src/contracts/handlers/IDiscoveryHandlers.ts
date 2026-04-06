import { NextFunction, Request, Response } from "express";

export interface IDiscoveryHandlers {
  getAuthorizationServerMetadata(
    req: Request,
    res: Response,
    next: NextFunction
  ): Promise<void>;
  getOpenIdConfiguration(
    req: Request,
    res: Response,
    next: NextFunction
  ): Promise<void>;
}

