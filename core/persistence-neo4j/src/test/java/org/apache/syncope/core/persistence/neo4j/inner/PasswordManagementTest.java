package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.password.JDBCPasswordManagementConf;
import org.apache.syncope.common.lib.password.LDAPPasswordManagementConf;
import org.apache.syncope.common.lib.password.PasswordManagementConf;
import org.apache.syncope.common.lib.password.SyncopePasswordManagementConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PasswordManagementTest extends AbstractTest {

    private static boolean isSpecificConf(
            final PasswordManagementConf conf,
            final Class<? extends PasswordManagementConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Autowired
    private PasswordManagementDAO passwordManagementDAO;

    @Test
    public void findAll() {
        List<? extends PasswordManagement> modules = passwordManagementDAO.findAll();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
        assertEquals(3, modules.size());
    }

    @Test
    public void find() {
        PasswordManagement passwordManagement = passwordManagementDAO.findById("DefaultSyncopePasswordManagement")
                .orElseThrow();
        assertTrue(passwordManagement.getConf() instanceof SyncopePasswordManagementConf);

        passwordManagement = passwordManagementDAO.findById("DefaultLDAPPasswordManagement").orElseThrow();
        assertTrue(passwordManagement.getConf() instanceof LDAPPasswordManagementConf);

        passwordManagement = passwordManagementDAO.findById("DefaultJDBCPasswordManagement").orElseThrow();
        assertTrue(passwordManagement.getConf() instanceof JDBCPasswordManagementConf);
    }

    @Test
    public void findByType() {
        List<? extends PasswordManagement> passwordManagements = passwordManagementDAO.findAll();
        assertTrue(passwordManagements.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        SyncopePasswordManagementConf.class)
                        && passwordManagement.getKey().equals("DefaultSyncopePasswordManagement")));
        assertTrue(passwordManagements.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        LDAPPasswordManagementConf.class)
                        && passwordManagement.getKey().equals("DefaultLDAPPasswordManagement")));
        assertTrue(passwordManagements.stream().anyMatch(
                passwordManagement -> isSpecificConf(passwordManagement.getConf(),
                        JDBCPasswordManagementConf.class)
                        && passwordManagement.getKey().equals("DefaultJDBCPasswordManagement")));
    }

    private void savePasswordManagement(final String key, final PasswordManagementConf conf) {
        PasswordManagement module = entityFactory.newEntity(PasswordManagement.class);
        module.setKey(key);
        module.setDescription("A password management module");
        module.setConf(conf);

        Item keyMapping = new Item();
        keyMapping.setIntAttrName("uid");
        keyMapping.setExtAttrName("username");
        module.getItems().add(keyMapping);

        Item fullnameMapping = new Item();
        fullnameMapping.setIntAttrName("cn");
        fullnameMapping.setExtAttrName("fullname");
        module.getItems().add(fullnameMapping);

        module = passwordManagementDAO.save(module);

        assertNotNull(module);
        assertNotNull(module.getKey());
        assertEquals(module, passwordManagementDAO.findById(module.getKey()).orElseThrow());
        assertEquals(2, module.getItems().size());
    }

    @Test
    public void saveWithSyncopeModule() {
        SyncopePasswordManagementConf conf = new SyncopePasswordManagementConf();
        conf.setDomain("Master");

        savePasswordManagement("SyncopePasswordManagementTest", conf);
    }

    @Test
    public void saveWithLdapModule() {
        LDAPPasswordManagementConf conf = new LDAPPasswordManagementConf();
        conf.setBaseDn("dc=example,dc=org");
        conf.setSearchFilter("cn={user}");
        conf.setSubtreeSearch(true);
        conf.setLdapUrl("ldap://localhost:1389");
        conf.setUsernameAttribute("uid");
        conf.setBindCredential("Password");

        savePasswordManagement("LDAPPasswordManagementTest", conf);
    }

    @Test
    public void saveWithJdbcModule() {
        JDBCPasswordManagementConf conf = new JDBCPasswordManagementConf();
        conf.setSqlFindEmail("SELECT email from users_table where name=?");
        conf.setSqlFindPhone("SELECT phoneNumber from users_table where name=?");
        conf.setSqlFindUser("SELECT * from users_table where name=?");
        conf.setSqlChangePassword("UPDATE users_table SET password=? WHERE name=?");

        savePasswordManagement("JDBCPasswordManagementTest", conf);
    }

    @Test
    public void updateWithSyncopeModule() {
        PasswordManagement module = passwordManagementDAO.findById("DefaultSyncopePasswordManagement").orElseThrow();

        PasswordManagementConf conf = module.getConf();
        SyncopePasswordManagementConf.class.cast(conf).setDomain("Two");
        module.setConf(conf);

        module = passwordManagementDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        PasswordManagement found = passwordManagementDAO.findById(module.getKey()).orElseThrow();
        assertEquals("Two", SyncopePasswordManagementConf.class.cast(found.getConf()).getDomain());
    }

    @Test
    public void updateWithLdapModule() {
        PasswordManagement module = passwordManagementDAO.findById("DefaultLDAPPasswordManagement").orElseThrow();

        PasswordManagementConf conf = module.getConf();
        LDAPPasswordManagementConf.class.cast(conf).setBaseDn("dc=example2,dc=org");
        LDAPPasswordManagementConf.class.cast(conf).setSearchFilter("cn={user2}");
        module.setConf(conf);

        module = passwordManagementDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        PasswordManagement found = passwordManagementDAO.findById(module.getKey()).orElseThrow();
        assertEquals("dc=example2,dc=org", LDAPPasswordManagementConf.class.cast(found.getConf()).getBaseDn());
        assertEquals("cn={user2}", LDAPPasswordManagementConf.class.cast(found.getConf()).getSearchFilter());
    }

    @Test
    public void updateWithJDBCModule() {
        PasswordManagement module = passwordManagementDAO.findById("DefaultJDBCPasswordManagement").orElseThrow();

        PasswordManagementConf conf = module.getConf();
        JDBCPasswordManagementConf.class.cast(conf).setSqlFindUser("SELECT * from other_table where name=?");
        module.setConf(conf);

        module = passwordManagementDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        PasswordManagement found = passwordManagementDAO.findById(module.getKey()).orElseThrow();
        assertEquals("SELECT * from other_table where name=?",
                JDBCPasswordManagementConf.class.cast(found.getConf()).getSqlFindUser());
    }

    @Test
    public void delete() {
        assertTrue(passwordManagementDAO.findById("DefaultSyncopePasswordManagement").isPresent());

        passwordManagementDAO.deleteById("DefaultSyncopePasswordManagement");

        assertTrue(passwordManagementDAO.findById("DefaultSyncopePasswordManagement").isEmpty());
    }
}
