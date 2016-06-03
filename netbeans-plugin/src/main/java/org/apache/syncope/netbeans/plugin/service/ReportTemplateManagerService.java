/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.service;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.netbeans.plugin.user.UserProperties;

/**
 *
 * @author nuwan
 */
public class ReportTemplateManagerService {
    
    ReportTemplateService service;
    
    public ReportTemplateManagerService() {
        String url = "http://syncope-vm.apache.org:9080/syncope/rest/";
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                setAddress(url).create(UserProperties.getUserName(), 
                        UserProperties.getPassword()); 
        service = syncopeClient.getService(ReportTemplateService.class);
        
    }
    
 
    public List<ReportTemplateTO> list(){
        return service.list();
    }
    
    public boolean create(final ReportTemplateTO reportTemplateTO){
        return Response.Status.CREATED.getStatusCode() == 
                service.create(reportTemplateTO).getStatus();
    }
    
    public ReportTemplateTO read(String key){
        return service.read(key);
    }
        
    public boolean delete(String key){
        service.delete(key);
        return true;
    }
    
    public Object getFormat(String key, ReportTemplateFormat format){
        return null;
    }
    
    public boolean setFormat(String key, ReportTemplateFormat format){
        return false;
    }
    
    public boolean removeFormat(String key, ReportTemplateFormat format){
        return false;
    }
    
}
