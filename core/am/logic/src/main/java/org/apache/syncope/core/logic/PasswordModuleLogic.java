package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PasswordModuleDAO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;
import org.apache.syncope.core.provisioning.api.data.PasswordModuleDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class PasswordModuleLogic extends AbstractTransactionalLogic<PasswordModuleTO> {

    protected final PasswordModuleDataBinder passwordModuleDataBinder;

    protected final PasswordModuleDAO passwordModuleDAO;

    public PasswordModuleLogic(final PasswordModuleDataBinder passwordModuleDataBinder,
            final PasswordModuleDAO passwordModuleDAO) {
        this.passwordModuleDataBinder = passwordModuleDataBinder;
        this.passwordModuleDAO = passwordModuleDAO;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MODULE_CREATE + "')")
    public PasswordModuleTO create(final PasswordModuleTO passwordModuleTO) {
        return passwordModuleDataBinder.getPasswordModuleTO(
                passwordModuleDAO.save(passwordModuleDataBinder.create(passwordModuleTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MODULE_UPDATE + "')")
    public PasswordModuleTO update(final PasswordModuleTO passwordModuleTO) {
        PasswordModule passwordModule = passwordModuleDAO.findById(passwordModuleTO.getKey()).
                orElseThrow(() -> new NotFoundException("PasswordModule " + passwordModuleTO.getKey()));

        return passwordModuleDataBinder.getPasswordModuleTO(
                passwordModuleDAO.save(passwordModuleDataBinder.update(passwordModule, passwordModuleTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MODULE_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<PasswordModuleTO> list() {
        return passwordModuleDAO.findAll().stream()
                .map(passwordModuleDataBinder::getPasswordModuleTO).toList();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MODULE_READ + "')")
    @Transactional(readOnly = true)
    public PasswordModuleTO read(final String key) {
        PasswordModule passwordModule = passwordModuleDAO.findById(key).
                orElseThrow(() -> new NotFoundException("PasswordModule " + key));

        return passwordModuleDataBinder.getPasswordModuleTO(passwordModule);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.PASSWORD_MODULE_DELETE + "')")
    public PasswordModuleTO delete(final String key) {
        PasswordModule passwordModule = passwordModuleDAO.findById(key).
                orElseThrow(() -> new NotFoundException("PasswordModule " + key));

        PasswordModuleTO deleted = passwordModuleDataBinder.getPasswordModuleTO(passwordModule);
        passwordModuleDAO.delete(passwordModule);

        return deleted;
    }

    @Override protected PasswordModuleTO resolveReference(Method method, Object... args)
            throws UnresolvedReferenceException {
        if (ArrayUtils.isEmpty(args)) {
            throw new UnresolvedReferenceException();
        }

        final String key;

        if (args[0] instanceof String string) {
            key = string;
        } else if (args[0] instanceof AuthModuleTO authModuleTO) {
            key = authModuleTO.getKey();
        } else {
            throw new UnresolvedReferenceException();
        }

        try {
            return passwordModuleDataBinder.getPasswordModuleTO(passwordModuleDAO.findById(key).orElseThrow());
        } catch (Throwable ignore) {
            LOG.debug("Unresolved reference", ignore);
            throw new UnresolvedReferenceException(ignore);
        }
    }
}
