import { NextFunction, Request, Response } from "express";

export interface ICertificateHandlers {
  listCertificates(req: Request, res: Response, next: NextFunction): Promise<void>;
}

