package org.apache.syncope.core.persistence.neo4j.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.apache.syncope.common.lib.password.JDBCPasswordModuleConf;
import org.apache.syncope.common.lib.password.LDAPPasswordModuleConf;
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.password.SyncopePasswordModuleConf;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.core.persistence.api.dao.PasswordModuleDAO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PasswordModuleTest extends AbstractTest {

    private static boolean isSpecificConf(
            final PasswordModuleConf conf,
            final Class<? extends PasswordModuleConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Autowired
    private PasswordModuleDAO passwordModuleDAO;

    @Test
    public void findAll() {
        List<? extends PasswordModule> modules = passwordModuleDAO.findAll();
        assertNotNull(modules);
        assertFalse(modules.isEmpty());
        assertEquals(3, modules.size());
    }

    @Test
    public void find() {
        PasswordModule passwordModule = passwordModuleDAO.findById("DefaultSyncopePasswordModule").orElseThrow();
        assertTrue(passwordModule.getConf() instanceof SyncopePasswordModuleConf);

        passwordModule = passwordModuleDAO.findById("DefaultLDAPPasswordModule").orElseThrow();
        assertTrue(passwordModule.getConf() instanceof LDAPPasswordModuleConf);

        passwordModule = passwordModuleDAO.findById("DefaultJDBCPasswordModule").orElseThrow();
        assertTrue(passwordModule.getConf() instanceof JDBCPasswordModuleConf);
    }

    @Test
    public void findByType() {
        List<? extends PasswordModule> passwordModules = passwordModuleDAO.findAll();
        assertTrue(passwordModules.stream().anyMatch(
                passwordModule -> isSpecificConf(passwordModule.getConf(),
                        SyncopePasswordModuleConf.class)
                        && passwordModule.getKey().equals("DefaultSyncopePasswordModule")));
        assertTrue(passwordModules.stream().anyMatch(
                passwordModule -> isSpecificConf(passwordModule.getConf(),
                        LDAPPasswordModuleConf.class)
                        && passwordModule.getKey().equals("DefaultLDAPPasswordModule")));
        assertTrue(passwordModules.stream().anyMatch(
                passwordModule -> isSpecificConf(passwordModule.getConf(),
                        JDBCPasswordModuleConf.class)
                        && passwordModule.getKey().equals("DefaultJDBCPasswordModule")));
    }

    private void savePasswordModule(final String key, final PasswordModuleConf conf) {
        PasswordModule module = entityFactory.newEntity(PasswordModule.class);
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

        module = passwordModuleDAO.save(module);

        assertNotNull(module);
        assertNotNull(module.getKey());
        assertEquals(module, passwordModuleDAO.findById(module.getKey()).orElseThrow());
        assertEquals(2, module.getItems().size());
    }

    @Test
    public void saveWithSyncopeModule() {
        SyncopePasswordModuleConf conf = new SyncopePasswordModuleConf();
        conf.setDomain("Master");

        savePasswordModule("SyncopePasswordModuleTest", conf);
    }

    @Test
    public void saveWithLdapModule() {
        LDAPPasswordModuleConf conf = new LDAPPasswordModuleConf();
        conf.setBaseDn("dc=example,dc=org");
        conf.setSearchFilter("cn={user}");
        conf.setSubtreeSearch(true);
        conf.setLdapUrl("ldap://localhost:1389");
        conf.setUsernameAttribute("uid");
        conf.setBindCredential("Password");

        savePasswordModule("LDAPPasswordModuleTest", conf);
    }

    @Test
    public void saveWithJdbcModule() {
        JDBCPasswordModuleConf conf = new JDBCPasswordModuleConf();
        conf.setSqlFindEmail("SELECT email from users_table where name=?");
        conf.setSqlFindPhone("SELECT phoneNumber from users_table where name=?");
        conf.setSqlFindUser("SELECT * from users_table where name=?");
        conf.setSqlChangePassword("UPDATE users_table SET password=? WHERE name=?");

        savePasswordModule("JDBCPasswordModuleTest", conf);
    }

    @Test
    public void updateWithSyncopeModule() {
        PasswordModule module = passwordModuleDAO.findById("DefaultSyncopePasswordModule").orElseThrow();

        PasswordModuleConf conf = module.getConf();
        SyncopePasswordModuleConf.class.cast(conf).setDomain("Two");
        module.setConf(conf);

        module = passwordModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        PasswordModule found = passwordModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("Two", SyncopePasswordModuleConf.class.cast(found.getConf()).getDomain());
    }

    @Test
    public void updateWithLdapModule() {
        PasswordModule module = passwordModuleDAO.findById("DefaultLDAPPasswordModule").orElseThrow();

        PasswordModuleConf conf = module.getConf();
        LDAPPasswordModuleConf.class.cast(conf).setBaseDn("dc=example2,dc=org");
        LDAPPasswordModuleConf.class.cast(conf).setSearchFilter("cn={user2}");
        module.setConf(conf);

        module = passwordModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        PasswordModule found = passwordModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("dc=example2,dc=org", LDAPPasswordModuleConf.class.cast(found.getConf()).getBaseDn());
        assertEquals("cn={user2}", LDAPPasswordModuleConf.class.cast(found.getConf()).getSearchFilter());
    }

    @Test
    public void updateWithJDBCModule() {
        PasswordModule module = passwordModuleDAO.findById("DefaultJDBCPasswordModule").orElseThrow();

        PasswordModuleConf conf = module.getConf();
        JDBCPasswordModuleConf.class.cast(conf).setSqlFindUser("SELECT * from other_table where name=?");
        module.setConf(conf);

        module = passwordModuleDAO.save(module);
        assertNotNull(module);
        assertNotNull(module.getKey());

        PasswordModule found = passwordModuleDAO.findById(module.getKey()).orElseThrow();
        assertEquals("SELECT * from other_table where name=?",
                JDBCPasswordModuleConf.class.cast(found.getConf()).getSqlFindUser());
    }

    @Test
    public void delete() {
        assertTrue(passwordModuleDAO.findById("DefaultSyncopePasswordModule").isPresent());

        passwordModuleDAO.deleteById("DefaultSyncopePasswordModule");

        assertTrue(passwordModuleDAO.findById("DefaultSyncopePasswordModule").isEmpty());
    }
}
