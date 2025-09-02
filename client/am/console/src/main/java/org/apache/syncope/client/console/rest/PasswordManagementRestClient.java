package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.rest.api.service.PasswordManagementService;

public class PasswordManagementRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3961377654788868099L;

    public List<PasswordManagementTO> list() {
        return getService(PasswordManagementService.class).list();
    }

    public void create(final PasswordManagementTO passwordManagementTO) {
        getService(PasswordManagementService.class).create(passwordManagementTO);
    }

    public PasswordManagementTO read(final String key) {
        return getService(PasswordManagementService.class).read(key);
    }

    public void update(final PasswordManagementTO passwordManagementTO) {
        getService(PasswordManagementService.class).update(passwordManagementTO);
    }

    public void delete(final String key) {
        getService(PasswordManagementService.class).delete(key);
    }
}
