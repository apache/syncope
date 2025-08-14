package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.to.PasswordModuleTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.PasswordModuleService;
import org.apache.syncope.core.logic.PasswordModuleLogic;

public class PasswordModuleServiceImpl extends AbstractService implements PasswordModuleService {

    protected final PasswordModuleLogic passwordModuleLogic;

    public PasswordModuleServiceImpl(final PasswordModuleLogic passwordModuleLogic) {
        this.passwordModuleLogic = passwordModuleLogic;
    }

    @Override public PasswordModuleTO read(String key) {
        return passwordModuleLogic.read(key);
    }

    @Override public List<PasswordModuleTO> list() {
        return passwordModuleLogic.list();
    }

    @Override public Response create(PasswordModuleTO passwordModuleTO) {
        PasswordModuleTO passwordModule = passwordModuleLogic.create(passwordModuleTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(passwordModule.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, passwordModule.getKey()).
                build();
    }

    @Override public void update(PasswordModuleTO passwordModuleTO) {
        passwordModuleLogic.update(passwordModuleTO);
    }

    @Override public void delete(String key) {
        passwordModuleLogic.delete(key);
    }
}
