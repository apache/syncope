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
import org.apache.syncope.common.lib.password.PasswordModuleConf;
import org.apache.syncope.common.lib.password.SyncopePasswordModuleConf;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.common.rest.api.service.PasswordModuleService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

public class PasswordModuleITCase extends AbstractITCase {

    private enum PasswordModuleSupportedType {
        SYNCOPE
    };

    private static PasswordModuleTO createAuthModule(final PasswordModuleTO passwordModuleTO) {
        Response response = PASSWORD_MODULE_SERVICE.create(passwordModuleTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), PasswordModuleService.class, passwordModuleTO.getClass());
    }

    private static PasswordModuleTO buildPasswordModuleTO(final PasswordModuleSupportedType type) {
        PasswordModuleTO passwordModuleTO = new PasswordModuleTO();
        passwordModuleTO.setKey("Test" + type + "PasswordModule" + getUUIDString());
        passwordModuleTO.setDescription("A test " + type + " Password Module");

        PasswordModuleConf conf;
        switch (type) {
            case SYNCOPE:
                conf = new SyncopePasswordModuleConf();
                SyncopePasswordModuleConf.class.cast(conf).setDomain(SyncopeConstants.MASTER_DOMAIN);
                passwordModuleTO.setConf(conf);
                break;
        }

        return passwordModuleTO;
    }

    private static boolean isSpecificConf(final PasswordModuleConf conf, final Class<? extends PasswordModuleConf> clazz) {
        return ClassUtils.isAssignable(clazz, conf.getClass());
    }

    @Test
    public void list() {
        List<PasswordModuleTO> passwordModuleTOS = PASSWORD_MODULE_SERVICE.list();
        assertNotNull(passwordModuleTOS);
        assertFalse(passwordModuleTOS.isEmpty());

        assertTrue(passwordModuleTOS.stream().anyMatch(
                authModule -> isSpecificConf(authModule.getConf(), SyncopePasswordModuleConf.class)
                        && authModule.getKey().equals("DefaultSyncopePasswordModule")));
    }

    @Test
    public void getSyncopePasswordModule() {
        PasswordModuleTO passwordModuleTO = PASSWORD_MODULE_SERVICE.read("DefaultSyncopePasswordModule");

        assertNotNull(passwordModuleTO);
        assertTrue(StringUtils.isNotBlank(passwordModuleTO.getDescription()));
        assertTrue(isSpecificConf(passwordModuleTO.getConf(), SyncopePasswordModuleConf.class));
    }

    @Test
    public void create() {
        EnumSet.allOf(PasswordModuleSupportedType.class).forEach(type -> {
            PasswordModuleTO passwordModuleTO = createAuthModule(buildPasswordModuleTO(type));
            assertNotNull(passwordModuleTO);
            assertTrue(passwordModuleTO.getDescription().contains("A test " + type + " Password Module"));
            assertTrue(passwordModuleTO.getItems().isEmpty());
        });
    }

    @Test
    public void updateSyncopePasswordModule() {
        PasswordModuleTO syncopePasswordModuleTO = PASSWORD_MODULE_SERVICE.read("DefaultSyncopePasswordModule");
        assertNotNull(syncopePasswordModuleTO);

        PasswordModuleTO newSyncopePasswordModuleTO = buildPasswordModuleTO(PasswordModuleSupportedType.SYNCOPE);
        newSyncopePasswordModuleTO = createAuthModule(newSyncopePasswordModuleTO);
        assertNotNull(newSyncopePasswordModuleTO);

        PasswordModuleConf conf = syncopePasswordModuleTO.getConf();
        assertNotNull(conf);
        SyncopePasswordModuleConf.class.cast(conf).setDomain("Two");
        newSyncopePasswordModuleTO.setConf(conf);

        // update new password module
        PASSWORD_MODULE_SERVICE.update(newSyncopePasswordModuleTO);
        newSyncopePasswordModuleTO = PASSWORD_MODULE_SERVICE.read(newSyncopePasswordModuleTO.getKey());
        assertNotNull(newSyncopePasswordModuleTO);

        conf = newSyncopePasswordModuleTO.getConf();
        assertEquals("Two", SyncopePasswordModuleConf.class.cast(conf).getDomain());
    }

    @Test
    public void delete() throws IOException {
        EnumSet.allOf(PasswordModuleSupportedType.class).forEach(type -> {
            PasswordModuleTO read = createAuthModule(buildPasswordModuleTO(type));
            assertNotNull(read);

            PASSWORD_MODULE_SERVICE.delete(read.getKey());

            try {
                PASSWORD_MODULE_SERVICE.read(read.getKey());
                fail("This should not happen");
            } catch (SyncopeClientException e) {
                assertNotNull(e);
            }
        });
    }
}
