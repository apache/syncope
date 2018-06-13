package org.apache.syncope.ide.netbeans.service;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.ImplementationType;
import org.apache.syncope.common.rest.api.service.ImplementationService;

public class ImplementationManagerService {

    private final ImplementationService service ;

    public ImplementationManagerService(final String url, final String userName, final String password) {
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().setAddress(url).create(userName, password);
        service = syncopeClient.getService(ImplementationService.class);
    }
 
    public List<ImplementationTO> list(final ImplementationType type) {
        return service.list(type);
    } 

    public ImplementationTO read(final ImplementationType type ,final String key) {
        return service.read(type,key);
    }

    public boolean create(final ImplementationTO implementationTO) {
        return Response.Status.CREATED.getStatusCode() == service.create(implementationTO).getStatus();
    }

    public boolean delete(final ImplementationType type , String key) {
        return Response.Status.NO_CONTENT.getStatusCode() == service.delete(type,key).getStatus();    
    }

    public boolean update(final ImplementationTO implementationTO ) {
       return Response.Status.NO_CONTENT.getStatusCode() == service.update(implementationTO).getStatus();
    }
}