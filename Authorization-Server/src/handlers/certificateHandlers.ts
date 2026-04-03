import { ICertificateHandlers } from "../contracts/handlers/ICertificateHandlers";
import { ICertificateService } from "../contracts/services/ICertificateService";

export function createCertificateHandlers(
  certificateService: ICertificateService
): ICertificateHandlers {
  return {
    async listCertificates(req, res, next): Promise<void> {
      try {
        const certificates = await certificateService.listDigitalCertificates();
        res.status(200).json({ certificates });
      } catch (error) {
        next(error);
      }
    },
  };
}

