package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.PasswordManagementTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.PasswordManagementService;
import org.apache.syncope.core.logic.PasswordManagementLogic;

public class PasswordManagementServiceImpl extends AbstractService implements PasswordManagementService {

    protected final PasswordManagementLogic passwordManagementLogic;

    public PasswordManagementServiceImpl(final PasswordManagementLogic passwordManagementLogic) {
        this.passwordManagementLogic = passwordManagementLogic;
    }

    @Override
    public PasswordManagementTO read(final String key) {
        return passwordManagementLogic.read(key);
    }

    @Override
    public List<PasswordManagementTO> list() {
        return passwordManagementLogic.list();
    }

    @Override
    public Response create(final PasswordManagementTO passwordManagementTO) {
        PasswordManagementTO passwordManagement = passwordManagementLogic.create(passwordManagementTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(passwordManagement.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, passwordManagement.getKey()).
                build();
    }

    @Override
    public void update(final PasswordManagementTO passwordManagementTO) {
        passwordManagementLogic.update(passwordManagementTO);
    }

    @Override
    public void delete(final String key) {
        passwordManagementLogic.delete(key);
    }
}
