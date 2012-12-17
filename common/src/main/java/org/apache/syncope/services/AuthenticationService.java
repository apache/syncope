package org.apache.syncope.services;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.syncope.to.EntitlementTO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequestMapping("/auth")
@Path("/auth")
public interface AuthenticationService {

    @RequestMapping(method = RequestMethod.GET, value = "/allentitlements")
    @GET
    @Path("allentitlements")
    public abstract Set<EntitlementTO> getAllEntitlements();

    @RequestMapping(method = RequestMethod.GET, value = "/entitlements")
    @GET
    @Path("entitlements")
    public abstract Set<EntitlementTO> getMyEntitlements();

}