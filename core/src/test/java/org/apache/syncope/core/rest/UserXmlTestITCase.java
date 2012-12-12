package org.apache.syncope.core.rest;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.services.UserService;

public class UserXmlTestITCase extends AbstractUserTestITCase {

    @Override
    public void setupService() {
        super.userService = createServiceInstance(UserService.class);
        setupXML(WebClient.client(super.userService));
    }
}
