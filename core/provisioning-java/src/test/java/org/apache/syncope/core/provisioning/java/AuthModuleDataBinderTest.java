package org.apache.syncope.core.provisioning.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.UUID;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.auth.LDAPAuthModuleConf;
import org.apache.syncope.common.lib.auth.OIDCAuthModuleConf;
import org.apache.syncope.common.lib.auth.SAML2IdPAuthModuleConf;
import org.apache.syncope.common.lib.auth.StaticAuthModuleConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class AuthModuleDataBinderTest extends AbstractTest {
    @Autowired
    private AuthModuleDataBinder authModuleDataBinder;

    @Autowired
    private EntityFactory entityFactory;

    @Test
    public void verifySAML2IdPAuthConf() {
        final SAML2IdPAuthModuleConf samlAuth = new SAML2IdPAuthModuleConf();
        samlAuth.setKeystorePassword(UUID.randomUUID().toString());
        samlAuth.setPrivateKeyPassword(UUID.randomUUID().toString());
        
        final AuthModuleTO originalAuthModuleTO = new AuthModuleTO();
        originalAuthModuleTO.setConf(samlAuth);
        AuthModule authModule = authModuleDataBinder.create(originalAuthModuleTO);
        SAML2IdPAuthModuleConf samlAuthModuleConfCreated = (SAML2IdPAuthModuleConf)  authModule.getConf();
        assertNotEquals(samlAuthModuleConfCreated.getKeystorePassword(), samlAuth.getKeystorePassword());
        assertNotEquals(samlAuthModuleConfCreated.getPrivateKeyPassword(), samlAuth.getPrivateKeyPassword());

        AuthModuleTO authModuleTO = authModuleDataBinder.getAuthModuleTO(authModule);
        final SAML2IdPAuthModuleConf finalConf = (SAML2IdPAuthModuleConf) authModuleTO.getConf();
        assertEquals(finalConf.getKeystorePassword(), samlAuth.getKeystorePassword());
        assertEquals(finalConf.getPrivateKeyPassword(), samlAuth.getPrivateKeyPassword());
    }

    @Test
    public void verifyStaticAuthModuleConf() {
        final StaticAuthModuleConf staticAuth = new StaticAuthModuleConf();
        staticAuth.getUsers().put("Syncope", "P@$$w0rd");
        AuthModule authModule = getAuthModule(staticAuth);
        AuthModuleTO authModuleTO = authModuleDataBinder.getAuthModuleTO(authModule);
        final StaticAuthModuleConf finalConf = (StaticAuthModuleConf) authModuleTO.getConf();
        assertEquals(finalConf.getUsers(), staticAuth.getUsers());
    }

    @Test
    public void verifyLdapAuthConf() {
        final LDAPAuthModuleConf oidcConf = new LDAPAuthModuleConf();
        oidcConf.setBindCredential(UUID.randomUUID().toString());

        final AuthModuleTO originalAuthModuleTO = new AuthModuleTO();
        originalAuthModuleTO.setConf(oidcConf);
        AuthModule authModule = authModuleDataBinder.create(originalAuthModuleTO);
        LDAPAuthModuleConf samlAuthModuleConfCreated = (LDAPAuthModuleConf)  authModule.getConf();
        assertNotEquals(samlAuthModuleConfCreated.getBindCredential(), oidcConf.getBindCredential());

        AuthModuleTO authModuleTO = authModuleDataBinder.getAuthModuleTO(authModule);
        final LDAPAuthModuleConf finalConf = (LDAPAuthModuleConf) authModuleTO.getConf();
        assertEquals(finalConf.getBindCredential(), oidcConf.getBindCredential());
    }

    @Test
    public void verifyOidcAuthConf() {
        final OIDCAuthModuleConf oidcConf = new OIDCAuthModuleConf();
        oidcConf.setClientSecret(UUID.randomUUID().toString());
        oidcConf.setId(UUID.randomUUID().toString());

        final AuthModuleTO originalAuthModuleTO = new AuthModuleTO();
        originalAuthModuleTO.setConf(oidcConf);
        AuthModule authModule = authModuleDataBinder.create(originalAuthModuleTO);
        OIDCAuthModuleConf samlAuthModuleConfCreated = (OIDCAuthModuleConf)  authModule.getConf();
        assertNotEquals(samlAuthModuleConfCreated.getClientSecret(), oidcConf.getClientSecret());

        AuthModuleTO authModuleTO = authModuleDataBinder.getAuthModuleTO(authModule);
        final OIDCAuthModuleConf finalConf = (OIDCAuthModuleConf) authModuleTO.getConf();
        assertEquals(finalConf.getClientSecret(), oidcConf.getClientSecret());
    }

    private AuthModule getAuthModule(final AuthModuleConf authModuleConf) {
        AuthModule authModule = entityFactory.newEntity(AuthModule.class);
        authModule.setConf(authModuleConf);
        return authModule;
    }
}
