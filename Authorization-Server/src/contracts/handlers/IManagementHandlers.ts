import { NextFunction, Request, Response } from "express";

export interface IManagementHandlers {
  createClient(req: Request, res: Response, next: NextFunction): Promise<void>;
  listClients(req: Request, res: Response, next: NextFunction): Promise<void>;
  getClientById(req: Request, res: Response, next: NextFunction): Promise<void>;
  rotateClientSecret(req: Request, res: Response, next: NextFunction): Promise<void>;
  deleteClient(req: Request, res: Response, next: NextFunction): Promise<void>;
}

