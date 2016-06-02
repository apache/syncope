/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.service;

import java.util.List;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.netbeans.plugin.user.UserProperties;

/**
 *
 * @author nuwan
 */
public class MailTemplateManagerService {
    
    private MailTemplateService service;
    
    public MailTemplateManagerService() {
        String url = "http://localhost:9080/syncope/rest/";
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                setAddress(url).create(UserProperties.getUserName(), 
                        UserProperties.getPassword()); 
        service = syncopeClient.getService(MailTemplateService.class);
    }
    
    
    public List<MailTemplateTO> list(){
        return service.list();
    }
    
    public boolean create(final MailTemplateTO mailTemplateTO){
        return false;
    }
    
    public Object read(String key){
        return null;
    }
    
    public boolean delete(String key){
        return false;
    }
    
    public Object getFormat(String key, MailTemplateFormat format){
        return null;
    }
    
    public boolean setFormat(String key, MailTemplateFormat format){
        return false;
    }
    
    public boolean removeFormat(String key, MailTemplateFormat format){
        return false;
    }
    
}
