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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.password.JDBCPasswordManagementConf;
import org.apache.syncope.common.lib.password.LDAPPasswordManagementConf;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.common.lib.password.RESTPasswordManagementConf;
import org.apache.syncope.common.lib.password.SyncopePasswordManagementConf;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.rest.api.service.PasswordManagementService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class PasswordManagementITCase extends AbstractITCase {

    private enum PasswordManagementSupportedType {
        SYNCOPE,
        LDAP,
        JDBC,
        REST
    };

    private static PasswordManagementTO createPasswordManagement(final PasswordManagementTO passwordManagementTO) {
        Response response = PASSWORD_MANAGEMENT_SERVICE.create(passwordManagementTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), PasswordManagementService.class, passwordManagementTO.getClass());
    }

    private static PasswordManagementTO buildPasswordManagementTO(final PasswordManagementSupportedType type) {
        PasswordManagementTO passwordManagementTO = new PasswordManagementTO();
        passwordManagementTO.setKey("Test" + type + "PasswordManagement" + getUUIDString());
        passwordManagementTO.setDescription("A test " + type + " Password Management");

        PasswordManagementConf conf;
        switch (type) {
            case SYNCOPE:
                conf = new SyncopePasswordManagementConf();
                SyncopePasswordManagementConf.class.cast(conf).setDomain(SyncopeConstants.MASTER_DOMAIN);
                break;
            case LDAP:
                conf = new LDAPPasswordManagementConf();
                LDAPPasswordManagementConf.class.cast(conf).setBaseDn("dc=example,dc=org");
                LDAPPasswordManagementConf.class.cast(conf).setSearchFilter("cn={user}");
                LDAPPasswordManagementConf.class.cast(conf).setSubtreeSearch(true);
                LDAPPasswordManagementConf.class.cast(conf).setLdapUrl("ldap://localhost:1389");
                LDAPPasswordManagementConf.class.cast(conf).setUsernameAttribute("uid");
                LDAPPasswordManagementConf.class.cast(conf).setBaseDn("cn=Directory Manager,dc=example,dc=org");
                LDAPPasswordManagementConf.class.cast(conf).setBindCredential("Password");
                break;
            case JDBC:
                conf = new JDBCPasswordManagementConf();
                JDBCPasswordManagementConf.class.cast(conf)
                        .setSqlFindEmail("SELECT email from users_table where name=?");
                JDBCPasswordManagementConf.class.cast(conf)
                        .setSqlFindPhone("SELECT phoneNumber from users_table where name=?");
                JDBCPasswordManagementConf.class.cast(conf)
                        .setSqlFindUser("SELECT * from users_table where name=?");
                JDBCPasswordManagementConf.class.cast(conf)
                        .setSqlChangePassword("UPDATE users_table SET password=? WHERE name=?");
                break;
            case REST:
                conf = new RESTPasswordManagementConf();
                RESTPasswordManagementConf.class.cast(conf).setEndpointPassword("password");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUrlAccountUnlock(
                        "http://localhost:9443/syncope-fit-build-tools/cxf/rest/unlockAccount");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUrlChange(
                        "http://localhost:9443/syncope-fit-build-tools/cxf/rest/changePassword");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUrlEmail(
                        "http://localhost:9443/syncope-fit-build-tools/cxf/rest/findEmail");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUrlPhone(
                        "http://localhost:9443/syncope-fit-build-tools/cxf/rest/findPhone");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUrlSecurityQuestions(
                        "http://localhost:9443/syncope-fit-build-tools/cxf/rest/securityQuestions");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUsername(
                        "http://localhost:9443/syncope-fit-build-tools/cxf/rest/findUser");
                RESTPasswordManagementConf.class.cast(conf).setFieldNamePasswordOld("oldPassword");
                RESTPasswordManagementConf.class.cast(conf).setFieldNamePassword("password");
                RESTPasswordManagementConf.class.cast(conf).setEndpointUsername("username");
            default:
                conf = null;
                break;
        }
        passwordManagementTO.setConf(conf);

        return passwordManagementTO;
    }

    private static boolean isSpecificConf(
            final PasswordManagementConf conf,
            final Class<? extends PasswordManagementConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Test
    public void list() {
        List<PasswordManagementTO> passwordManagementTOS = PASSWORD_MANAGEMENT_SERVICE.list();
        assertNotNull(passwordManagementTOS);
        assertFalse(passwordManagementTOS.isEmpty());

        assertTrue(passwordManagementTOS.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        SyncopePasswordManagementConf.class) && passwordManagement.getKey()
                        .equals("DefaultSyncopePasswordManagement")));
        assertTrue(passwordManagementTOS.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        LDAPPasswordManagementConf.class) && passwordManagement.getKey()
                        .equals("DefaultLDAPPasswordManagement")));
        assertTrue(passwordManagementTOS.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        JDBCPasswordManagementConf.class) && passwordManagement.getKey()
                        .equals("DefaultJDBCPasswordManagement")));
        assertTrue(passwordManagementTOS.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        RESTPasswordManagementConf.class) && passwordManagement.getKey()
                        .equals("DefaultRESTPasswordManagement")));
    }

    @Test
    public void getSyncopePasswordManagement() {
        PasswordManagementTO passwordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultSyncopePasswordManagement");

        assertNotNull(passwordManagementTO);
        assertTrue(StringUtils.isNotBlank(passwordManagementTO.getDescription()));
        assertTrue(isSpecificConf(passwordManagementTO.getConf(), SyncopePasswordManagementConf.class));
    }

    @Test
    public void getLDAPPasswordManagement() {
        PasswordManagementTO passwordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultLDAPPasswordManagement");

        assertNotNull(passwordManagementTO);
        assertTrue(StringUtils.isNotBlank(passwordManagementTO.getDescription()));
        assertTrue(isSpecificConf(passwordManagementTO.getConf(), LDAPPasswordManagementConf.class));
    }

    @Test
    public void getJDBCPasswordManagement() {
        PasswordManagementTO passwordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultJDBCPasswordManagement");

        assertNotNull(passwordManagementTO);
        assertTrue(StringUtils.isNotBlank(passwordManagementTO.getDescription()));
        assertTrue(isSpecificConf(passwordManagementTO.getConf(), JDBCPasswordManagementConf.class));
    }

    @Test
    public void getRESTPasswordManagement() {
        PasswordManagementTO passwordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultRESTPasswordManagement");

        assertNotNull(passwordManagementTO);
        assertTrue(StringUtils.isNotBlank(passwordManagementTO.getDescription()));
        assertTrue(isSpecificConf(passwordManagementTO.getConf(), RESTPasswordManagementConf.class));
    }

    @Test
    public void create() {
        EnumSet.allOf(PasswordManagementSupportedType.class).forEach(type -> {
            PasswordManagementTO passwordManagementTO = createPasswordManagement(buildPasswordManagementTO(type));
            assertNotNull(passwordManagementTO);
            assertTrue(passwordManagementTO.getDescription().contains("A test " + type + " Password Management"));
        });
    }

    @Test
    public void updateSyncopePasswordManagement() {
        PasswordManagementTO syncopePasswordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultSyncopePasswordManagement");
        assertNotNull(syncopePasswordManagementTO);

        PasswordManagementTO newSyncopePasswordManagementTO =
                buildPasswordManagementTO(PasswordManagementSupportedType.SYNCOPE);
        newSyncopePasswordManagementTO = createPasswordManagement(newSyncopePasswordManagementTO);
        assertNotNull(newSyncopePasswordManagementTO);

        PasswordManagementConf conf = syncopePasswordManagementTO.getConf();
        assertNotNull(conf);
        SyncopePasswordManagementConf.class.cast(conf).setDomain("Two");
        newSyncopePasswordManagementTO.setConf(conf);

        // update new password management
        PASSWORD_MANAGEMENT_SERVICE.update(newSyncopePasswordManagementTO);
        newSyncopePasswordManagementTO = PASSWORD_MANAGEMENT_SERVICE.read(newSyncopePasswordManagementTO.getKey());
        assertNotNull(newSyncopePasswordManagementTO);

        conf = newSyncopePasswordManagementTO.getConf();
        assertEquals("Two", SyncopePasswordManagementConf.class.cast(conf).getDomain());
    }

    @Test
    public void updateLDAPPasswordManagement() {
        PasswordManagementTO ldapPasswordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultLDAPPasswordManagement");
        assertNotNull(ldapPasswordManagementTO);

        PasswordManagementTO newLdapPasswordManagementTO =
                buildPasswordManagementTO(PasswordManagementSupportedType.LDAP);
        newLdapPasswordManagementTO = createPasswordManagement(newLdapPasswordManagementTO);
        assertNotNull(newLdapPasswordManagementTO);

        PasswordManagementConf conf = ldapPasswordManagementTO.getConf();
        assertNotNull(conf);
        LDAPPasswordManagementConf.class.cast(conf).setSubtreeSearch(false);
        newLdapPasswordManagementTO.setConf(conf);

        // update new password management
        PASSWORD_MANAGEMENT_SERVICE.update(newLdapPasswordManagementTO);
        newLdapPasswordManagementTO = PASSWORD_MANAGEMENT_SERVICE.read(newLdapPasswordManagementTO.getKey());
        assertNotNull(newLdapPasswordManagementTO);

        conf = newLdapPasswordManagementTO.getConf();
        assertFalse(LDAPPasswordManagementConf.class.cast(conf).isSubtreeSearch());
    }

    @Test
    public void updateJDBCPasswordManagement() {
        PasswordManagementTO jdbcPasswordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultJDBCPasswordManagement");
        assertNotNull(jdbcPasswordManagementTO);

        PasswordManagementTO newJdbcPasswordManagementTO =
                buildPasswordManagementTO(PasswordManagementSupportedType.JDBC);
        newJdbcPasswordManagementTO = createPasswordManagement(newJdbcPasswordManagementTO);
        assertNotNull(newJdbcPasswordManagementTO);

        PasswordManagementConf conf = jdbcPasswordManagementTO.getConf();
        assertNotNull(conf);
        JDBCPasswordManagementConf.class.cast(conf).setSqlFindUser("SELECT * from other_table where name=?");
        newJdbcPasswordManagementTO.setConf(conf);

        // update new password management
        PASSWORD_MANAGEMENT_SERVICE.update(newJdbcPasswordManagementTO);
        newJdbcPasswordManagementTO = PASSWORD_MANAGEMENT_SERVICE.read(newJdbcPasswordManagementTO.getKey());
        assertNotNull(newJdbcPasswordManagementTO);

        conf = newJdbcPasswordManagementTO.getConf();
        assertEquals("SELECT * from other_table where name=?",
                JDBCPasswordManagementConf.class.cast(conf).getSqlFindUser());
    }

    @Test
    public void updateRESTPasswordManagement() {
        PasswordManagementTO restPasswordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultRESTPasswordManagement");
        assertNotNull(restPasswordManagementTO);

        PasswordManagementTO newRestPasswordManagementTO =
                buildPasswordManagementTO(PasswordManagementSupportedType.REST);
        newRestPasswordManagementTO = createPasswordManagement(newRestPasswordManagementTO);
        assertNotNull(newRestPasswordManagementTO);

        PasswordManagementConf conf = restPasswordManagementTO.getConf();
        assertNotNull(conf);
        RESTPasswordManagementConf.class.cast(conf).setFieldNamePasswordOld("changedOldPassword");
        newRestPasswordManagementTO.setConf(conf);

        // update new password management
        PASSWORD_MANAGEMENT_SERVICE.update(newRestPasswordManagementTO);
        newRestPasswordManagementTO = PASSWORD_MANAGEMENT_SERVICE.read(newRestPasswordManagementTO.getKey());
        assertNotNull(newRestPasswordManagementTO);

        conf = newRestPasswordManagementTO.getConf();
        assertEquals("changedOldPassword",
                RESTPasswordManagementConf.class.cast(conf).getFieldNamePasswordOld());
    }

    @Test
    public void delete() throws IOException {
        EnumSet.allOf(PasswordManagementSupportedType.class).forEach(type -> {
            PasswordManagementTO read = createPasswordManagement(buildPasswordManagementTO(type));
            assertNotNull(read);

            PASSWORD_MANAGEMENT_SERVICE.delete(read.getKey());

            try {
                PASSWORD_MANAGEMENT_SERVICE.read(read.getKey());
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertNotNull(e);
            }
        });
    }
}
