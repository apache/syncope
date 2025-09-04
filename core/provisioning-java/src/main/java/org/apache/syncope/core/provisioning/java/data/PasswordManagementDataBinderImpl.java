package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.PasswordManagement;
import org.apache.syncope.core.provisioning.api.data.PasswordManagementDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordManagementDataBinderImpl implements PasswordManagementDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(PasswordManagementDataBinder.class);

    protected final EntityFactory entityFactory;

    public PasswordManagementDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    @Override
    public PasswordManagement create(final PasswordManagementTO passwordManagementTO) {
        PasswordManagement passwordManagement = entityFactory.newEntity(PasswordManagement.class);
        passwordManagement.setKey(passwordManagementTO.getKey());
        return update(passwordManagement, passwordManagementTO);
    }

    @Override
    public PasswordManagement update(final PasswordManagement passwordManagement,
            final PasswordManagementTO passwordManagementTO) {
        passwordManagement.setDescription(passwordManagementTO.getDescription());
        passwordManagement.setEnabled(passwordManagementTO.getEnabled());
        passwordManagement.setConf(passwordManagementTO.getConf());

        return passwordManagement;
    }

    @Override
    public PasswordManagementTO getPasswordManagementTO(final PasswordManagement passwordManagement) {
        PasswordManagementTO passwordManagementTO = new PasswordManagementTO();

        passwordManagementTO.setKey(passwordManagement.getKey());
        passwordManagementTO.setDescription(passwordManagement.getDescription());
        passwordManagementTO.setEnabled(passwordManagement.getEnabled());
        passwordManagementTO.setConf(passwordManagement.getConf());

        return passwordManagementTO;
    }
}
