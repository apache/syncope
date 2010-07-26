/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttribute;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttributeValue;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSChange;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSUser;
import org.syncope.identityconnectors.bundles.staticwebservice.exceptions.ProvisioningException;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;

@WebService
public interface Provisioning {

    /**
     * Checks if authentication is supported.
     * @return true if the resource support authentication.
     */
    @WebMethod(operationName = "isAuthenticationSupported")
    public Boolean isAuthenticationSupported();

    /**
     * Checks if synchronization is supported.
     * @return true if the resource support synchronization.
     */
    @WebMethod(operationName = "isSyncSupported")
    public Boolean isSyncSupported();

    /**
     * Verify user creentials
     * @param username
     * @param password
     * @return
     * the accountid of the first account that match username and password.
     * @throws
     * ProvisioningException in case of authentication failed.
     */
    @WebMethod(operationName = "authenticate")
    public String authenticate(
            @WebParam(name = "username") final String username,
            @WebParam(name = "password") final String password)
            throws ProvisioningException;

    /**
     * Returns "OK" if the resource is available.
     * @return the string "OK" in case of availability of the resource.
     */
    @WebMethod(operationName = "checkAlive")
    public String checkAlive();

    /**
     * Returns the schema.
     * @return a set of attributes.
     */
    @WebMethod(operationName = "schema")
    public List<WSAttribute> schema();

    /**
     * Creates user account.
     * @param a set of account attributes.
     * @return accountid of the account created.
     * @throws ProvisioningException in case of failure.
     */
    @WebMethod(operationName = "create")
    public String create(
            @WebParam(name = "data") final List<WSAttributeValue> data)
            throws ProvisioningException;

    /**
     * Updates user account.
     * @param accountid.
     * @param a set of attributes to be updated.
     * @return accountid.
     * @throws ProvisioningException in case of failure
     */
    @WebMethod(operationName = "update")
    public String update(
            @WebParam(name = "accountid") final String accountid,
            @WebParam(name = "data") final List<WSAttributeValue> data)
            throws ProvisioningException;

    /**
     * Deletes user account.
     * @param accountid.
     * @return accountid.
     * @throws ProvisioningException in case of failure.
     */
    @WebMethod(operationName = "delete")
    public String delete(@WebParam(name = "accountid") final String accountid)
            throws ProvisioningException;

    /**
     * Searches for user accounts.
     * @param query filter
     * @return a set of user accounts.
     */
    @WebMethod(operationName = "query")
    public List<WSUser> query(@WebParam(name = "query") final Operand query);

    /**
     * Returns accountid related to the specified username.
     * @param username.
     * @return accountid.
     * @throws ProvisioningException in case of failure or username not found.
     */
    @WebMethod(operationName = "resolve")
    public String resolve(@WebParam(name = "username") final String username)
            throws ProvisioningException;

    /**
     * Gets the latest change id.
     * @return change id.
     * @throws ProvisioningException in case of failure.
     */
    @WebMethod(operationName = "getLatestChangeNumber")
    public int getLatestChangeNumber()
            throws ProvisioningException;

    /**
     * Returns changes to be synchronized.
     * @return a set of changes
     * @throws ProvisioningException in case of failure
     */
    @WebMethod(operationName = "sync")
    public List<WSChange> sync()
            throws ProvisioningException;
}
