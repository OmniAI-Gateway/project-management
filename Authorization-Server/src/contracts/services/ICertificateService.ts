import { CertificateInfo } from "../../domain/certificates/CertificateInfo";

export interface ICertificateService {
  listDigitalCertificates(): Promise<CertificateInfo[]>;
}
