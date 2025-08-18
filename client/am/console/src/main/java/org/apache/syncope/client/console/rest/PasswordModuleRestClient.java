package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.common.rest.api.service.PasswordModuleService;

public class PasswordModuleRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3961377654788868099L;

    public List<PasswordModuleTO> list() {
        return getService(PasswordModuleService.class).list();
    }

    public void create(final PasswordModuleTO passwordModuleTO) {
        getService(PasswordModuleService.class).create(passwordModuleTO);
    }

    public PasswordModuleTO read(final String key) {
        return getService(PasswordModuleService.class).read(key);
    }

    public void update(final PasswordModuleTO passwordModuleTO) {
        getService(PasswordModuleService.class).update(passwordModuleTO);
    }

    public void delete(final String key) {
        getService(PasswordModuleService.class).delete(key);
    }
}
