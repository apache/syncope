package org.apache.syncope.core.provisioning.api.data;

import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.core.persistence.api.entity.am.PasswordModule;

public interface PasswordModuleDataBinder {

    PasswordModule create(PasswordModuleTO passwordModuleTO);

    PasswordModule update(PasswordModule passwordModule, PasswordModuleTO passwordModuleTO);

    PasswordModuleTO getPasswordModuleTO(PasswordModule passwordModule);
}
