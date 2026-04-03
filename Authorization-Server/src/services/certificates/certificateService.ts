import fs from "fs/promises";
import path from "path";

import { ICertificateService } from "../../contracts/services/ICertificateService";
import { CertificateInfo } from "../../domain/certificates/CertificateInfo";

const CERTIFICATE_BLOCK_REGEX =
  /-----BEGIN CERTIFICATE-----[\s\S]*?-----END CERTIFICATE-----/g;

export function createCertificateService(
  certificatesPath: string = path.resolve(process.cwd(), "src/certificates")
): ICertificateService {
  return {
    async listDigitalCertificates(): Promise<CertificateInfo[]> {
      const files = await fs.readdir(certificatesPath, { withFileTypes: true });
      const certificates: CertificateInfo[] = [];

      for (const file of files) {
        if (!file.isFile() || !file.name.endsWith(".pem")) {
          continue;
        }

        const filePath = path.join(certificatesPath, file.name);
        const pemContent = await fs.readFile(filePath, "utf8");
        const certificateBlocks = pemContent.match(CERTIFICATE_BLOCK_REGEX) ?? [];

        for (const certificate of certificateBlocks) {
          certificates.push({
            fileName: file.name,
            certificate,
          });
        }
      }

      return certificates;
    },
  };
}
