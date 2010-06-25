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
package org.syncope.identityconnectors.bundles.staticwebservice.wstarget;

import java.util.HashSet;
import java.util.Set;
import javax.jws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttribute;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttributeValue;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSChange;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSUser;
import org.syncope.identityconnectors.bundles.staticwebservice.exceptions.ProvisioningException;
import org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;

@WebService(endpointInterface = "org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning",
serviceName = "Provisioning")
public class ProvisioningImpl implements Provisioning {

    private static final Logger log =
            LoggerFactory.getLogger(Provisioning.class);

    /**
     * Returns true if authentication is supported false otherwise.
     * @return true if authentication is supported false otherwise.
     */
    @Override
    public Boolean isAuthenticationSupported() {
        return Boolean.TRUE;
    }

    /**
     * Returns true if synchronization is supported false otherwise.
     * @return true if synchronization is supported false otherwise.
     */
    @Override
    public Boolean isSyncSupported() {
        return Boolean.TRUE;
    }

    /**
     * Verify user creentials
     * @param username
     * @param password
     * @return 
     * the accountid of the first account that match username and password.
     * @throws
     * ProvisioningException in case of authentication failed.
     */
    @Override
    public String authenticate(final String username, final String password)
            throws ProvisioningException {

        if (log.isDebugEnabled()) {
            log.debug(
                    "\nUsername: " + username +
                    "\nPassword: " + password);
        }

        return "TESTUSER";
    }

    /**
     * Returns "OK" if the resource is available.
     * @return the string "OK" in case of availability of the resource.
     */
    @Override
    public String checkAlive() {
        return "OK";
    }

    /**
     * Returns the schema.
     * @return a set of attributes.
     */
    @Override
    public Set<WSAttribute> schema() {
        Set<WSAttribute> attrs = new HashSet<WSAttribute>();

        WSAttribute attr = null;

        attr = new WSAttribute("nome");
        attrs.add(attr);

        attr = new WSAttribute("cognome");
        attr.setNullable(false);
        attrs.add(attr);

        attr = new WSAttribute("username");
        attr.setKey(true);
        attrs.add(attr);

        attr = new WSAttribute("password");
        attr.setPassword(true);
        attrs.add(attr);

        attr = new WSAttribute("data di nascita", "Date", false);
        attrs.add(attr);

        attr = new WSAttribute("privacy", "Boolean", false);
        attrs.add(attr);

        attr = new WSAttribute("altezza", "Double");
        attrs.add(attr);

        attr = new WSAttribute("eta", "Long");
        attrs.add(attr);

        return attrs;
    }

    /**
     * Creates user account.
     * @param a set of account attributes.
     * @return accountid of the account created.
     * @throws ProvisioningException in case of failure.
     */
    @Override
    public String create(final Set<WSAttributeValue> data)
            throws ProvisioningException {

        String res = null;


        for (WSAttributeValue value : data) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "\nName: " + value.getName() +
                        "\nType: " + value.getType() +
                        "\nNullable: " + value.isNullable() +
                        "\nKey: " + value.isKey() +
                        "\nPassword: " + value.isPassword() +
                        "\nValue: " + value.getValue().toString());
            }

            if (value.isKey())
                res = value.getValue().toString();
        }

        return res;
    }

    /**
     * Deletes user account.
     * @param accountid.
     * @return accountid.
     * @throws ProvisioningException in case of failure.
     */
    @Override
    public String delete(final String accountid) throws ProvisioningException {
        if (log.isDebugEnabled()) {
            log.debug("Account name: " + accountid);
        }

        return accountid;
    }

    /**
     * Updates user account.
     * @param accountid.
     * @param a set of attributes to be updated.
     * @return accountid.
     * @throws ProvisioningException in case of failure
     */
    @Override
    public String update(
            final String accountid,
            final Set<WSAttributeValue> data) throws ProvisioningException {

        for (WSAttributeValue value : data) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "\nName: " + value.getName() +
                        "\nType: " + value.getType() +
                        "\nNullable: " + value.isNullable() +
                        "\nKey: " + value.isKey() +
                        "\nPassword: " + value.isPassword() +
                        "\nValue: " + value.getValue().toString());
            }
        }

        return accountid;
    }

    /**
     * Searches for user accounts.
     * @param query filter
     * @return a set of user accounts.
     */
    @Override
    public Set<WSUser> query(Operand query) {

        Set<WSUser> resultSet = new HashSet<WSUser>();

        WSUser user = null;
        WSAttributeValue attr = null;

        for (int i = 0; i < 5; i++) {
            user = new WSUser("test" + i, new HashSet<WSAttributeValue>());

            attr = new WSAttributeValue();
            attr.setName("username");
            attr.setKey(true);
            attr.setValue("test" + i);

            user.addAttribute(attr);

            attr = new WSAttributeValue();
            attr.setName("nome");
            attr.setValue("ntest" + i);

            user.addAttribute(attr);

            attr = new WSAttributeValue();
            attr.setName("cognome");
            attr.setValue("ctest" + i);

            user.addAttribute(attr);

            resultSet.add(user);
        }

        return resultSet;
    }

    /**
     * Returns accountid related to the specified username.
     * @param username.
     * @return accountid.
     * @throws ProvisioningException in case of failure or username not found.
     */
    @Override
    public String resolve(final String username) throws ProvisioningException {
        return "TESTUSER";
    }

    /**
     * Gets the latest change id.
     * @return change id.
     * @throws ProvisioningException in case of failure.
     */
    @Override
    public int getLatestChangeNumber() throws ProvisioningException {
        return 1;
    }

    /**
     * Returns changes to be synchronized.
     * @return a set of changes
     * @throws ProvisioningException in case of failure
     */
    @Override
    public Set<WSChange> sync() throws ProvisioningException {

        WSChange change = new WSChange();
        
        // specify the change id
        change.setId(1);
        change.setType("CREATE_OR_UPDATE");

        // specify the account id
        WSAttributeValue uid = new WSAttributeValue();
        uid.setName("username");
        uid.setValue("test1");
        uid.setKey(true);

        // specify the attributes changed
        WSAttributeValue attr = new WSAttributeValue();
        attr.setName("name");

        Set<WSAttributeValue> attrs = new HashSet<WSAttributeValue>();
        attrs.add(uid);
        attrs.add(attr);

        change.setAttributes(attrs);

        Set<WSChange> changes = new HashSet<WSChange>();
        changes.add(change);

        return changes;
    }
}
