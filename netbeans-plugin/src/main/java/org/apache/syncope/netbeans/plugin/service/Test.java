/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.service;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.openide.util.Exceptions;

/**
 *
 * @author nuwan
 */
public class Test {
    
    public static void main(String[] args) {
        
        try {
            String url = "http://syncope-vm.apache.org:9080/syncope/rest/";
            SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                    setAddress(url).create("admin","password");
            MailTemplateService service = syncopeClient.getService(MailTemplateService.class);
            //MailTemplateTO mailTemplateTO = new MailTemplateTO();
            Response rs = service.getFormat("confirmPasswordReset", MailTemplateFormat.HTML);
            String templateData = (IOUtils.toString((InputStream) rs.getEntity()));
            System.out.println(templateData);
//service.setFormat("", MailTemplateFormat.HTML, templateIn);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }
    
}
