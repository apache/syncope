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
import org.apache.syncope.common.lib.password.SyncopePasswordManagementConf;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.rest.api.service.PasswordManagementService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class PasswordManagementITCase extends AbstractITCase {

    private enum PasswordManagementSupportedType {
        SYNCOPE,
        LDAP,
        JDBC
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
                passwordManagementTO.setConf(conf);
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
            default:
                break;
        }

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
    public void getLdapPasswordManagement() {
        PasswordManagementTO passwordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultLDAPPasswordManagement");

        assertNotNull(passwordManagementTO);
        assertTrue(StringUtils.isNotBlank(passwordManagementTO.getDescription()));
        assertTrue(isSpecificConf(passwordManagementTO.getConf(), LDAPPasswordManagementConf.class));
    }

    @Test
    public void getJdbcPasswordManagement() {
        PasswordManagementTO passwordManagementTO =
                PASSWORD_MANAGEMENT_SERVICE.read("DefaultJDBCPasswordManagement");

        assertNotNull(passwordManagementTO);
        assertTrue(StringUtils.isNotBlank(passwordManagementTO.getDescription()));
        assertTrue(isSpecificConf(passwordManagementTO.getConf(), JDBCPasswordManagementConf.class));
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
    public void updateLdapPasswordManagement() {
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
    public void updateJdbcPasswordManagement() {
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
