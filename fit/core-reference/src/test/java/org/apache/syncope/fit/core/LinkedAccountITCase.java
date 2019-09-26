/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class LinkedAccountITCase extends AbstractITCase {

    @Test
    public void createWithLinkedAccount() throws NamingException {
        UserTO user = UserITCase.getSampleTO(
                "linkedAccount" + RandomStringUtils.randomNumeric(5) + "@syncope.apache.org");

        LinkedAccountTO account = new LinkedAccountTO.Builder().
                connObjectName("uid=" + user.getUsername() + ",ou=People,o=isp").
                resource(RESOURCE_NAME_LDAP).
                build();
        account.getPlainAttrs().add(attrTO("surname", "LINKED_SURNAME"));
        user.getLinkedAccounts().add(account);

        user = createUser(user).getEntity();
        assertNotNull(user.getKey());

        LdapContext ldapObj = (LdapContext) getLdapRemoteObject(
                RESOURCE_LDAP_ADMIN_DN, RESOURCE_LDAP_ADMIN_PWD, account.getConnObjectName());
        assertNotNull(ldapObj);

        Attributes ldapAttrs = ldapObj.getAttributes("");
        assertEquals(
                user.getPlainAttr("email").get().getValues().get(0),
                ldapAttrs.get("mail").getAll().next().toString());
        assertEquals("LINKED_SURNAME", ldapAttrs.get("sn").getAll().next().toString());
    }
}
