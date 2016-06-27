/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.service;

import java.io.InputStream;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.rest.api.service.MailTemplateService;

/**
 *
 * @author nuwan
 */
public class MailTemplateManagerService {
    
    private MailTemplateService service;
    
    public MailTemplateManagerService(String url, String userName, String password) {
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                setAddress(url).create(userName,password); 
        service = syncopeClient.getService(MailTemplateService.class);
    }
     
    public List<MailTemplateTO> list(){
        return service.list();
    }
    
    public boolean create(final MailTemplateTO mailTemplateTO){
        return Response.Status.CREATED.getStatusCode() == 
                service.create(mailTemplateTO).getStatus();
    }
    
    public MailTemplateTO read(String key){
        return service.read(key);
    }
    
    public boolean delete(String key){
        service.delete(key);
        return true;
    }
    
    public Object getFormat(String key, MailTemplateFormat format){
        return service.getFormat(key, format).getEntity();
    }
    
    public void setFormat(String key, MailTemplateFormat format, InputStream templateIn){
        service.setFormat(key, format, templateIn);
    }
    
    public boolean removeFormat(String key, MailTemplateFormat format){
        return false;
    }
    
}
