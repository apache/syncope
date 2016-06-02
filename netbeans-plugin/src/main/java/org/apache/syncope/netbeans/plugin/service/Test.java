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
import org.apache.syncope.common.lib.to.ReportTemplateTO;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.netbeans.plugin.user.UserProperties;

/**
 *
 * @author nuwan
 */
public class Test {
    
    public static void main(String[] args) {
        
        String url = "http://localhost:9080/syncope/rest/";
        SyncopeClient syncopeClient = new SyncopeClientFactoryBean().
                setAddress(url).create(UserProperties.getUserName(), 
                        UserProperties.getPassword()); 
        MailTemplateService service = syncopeClient.getService(MailTemplateService.class);
        
//        MailTemplateTO mailTemplateTO = new MailTemplateTO();
//        mailTemplateTO.setKey("Mail 1");
//        System.out.println(service.create(mailTemplateTO).getStatus());
        List<MailTemplateTO> list = service.list();
        for (MailTemplateTO mailTemplateTO : list) {
            System.out.println(mailTemplateTO.getKey());
        }
        System.out.println("Report ...............................");
        ReportTemplateService service1 = syncopeClient.getService(ReportTemplateService.class);
        
        List<ReportTemplateTO> list1 = service1.list();
        
        for (ReportTemplateTO reportTemplateTO : list1) {
            System.out.println(reportTemplateTO.getKey());
        }
        
    }
}
