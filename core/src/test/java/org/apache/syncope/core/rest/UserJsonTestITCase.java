package org.apache.syncope.core.rest;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.controller.UserService;

public class UserJsonTestITCase extends AbstractUserTestITCase {

    @Override
    public void setupService() {
        super.userService = createServiceInstance(UserService.class);
        setupJSON(WebClient.client(super.userService));
    }
}
