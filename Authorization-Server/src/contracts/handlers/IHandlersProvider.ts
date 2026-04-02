
import { IIdentityHandlers } from "./IIdentityHandlers";
import { IManagementHandlers } from "./IManagementHandlers";

export interface IHandlersProvider {
    identity: IIdentityHandlers,
    management: IManagementHandlers
}