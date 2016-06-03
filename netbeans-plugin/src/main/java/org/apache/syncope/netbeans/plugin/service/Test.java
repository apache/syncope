/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.service;

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
public class Test {
    
    public static void main(String[] args) {
        
        String url = "http://syncope-vm.apache.org:9080/syncope/rest/";
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                setAddress(url).create(UserProperties.getUserName(), 
                        UserProperties.getPassword()); 
        MailTemplateService service = syncopeClient.getService(MailTemplateService.class);
        MailTemplateTO mailTemplateTO = new MailTemplateTO();
        
//service.setFormat("", MailTemplateFormat.HTML, templateIn);
        
    }
    
}
