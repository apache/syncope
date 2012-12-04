package org.apache.syncope.core.rest;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.controller.RoleService;

public class RoleXmlTestITCase extends AbstractRoleTestITCase {

    @Override
    public void setupService() {
        super.roleService = createServiceInstance(RoleService.class);
        setupXML(WebClient.client(this.roleService));
    }
}
