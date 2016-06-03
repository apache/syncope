/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.connector;

import org.apache.syncope.netbeans.plugin.service.MailTemplateManagerService;
import org.apache.syncope.netbeans.plugin.service.ReportTemplateManagerService;

/**
 *
 * @author nuwan
 */
public class ResourceConnector {
    
    private static MailTemplateManagerService mailTemplateManagerService;
    private static ReportTemplateManagerService reportTemplateManagerService;
    
    public static MailTemplateManagerService getMailTemplateManagerService(){
        if(mailTemplateManagerService == null){
            mailTemplateManagerService = new MailTemplateManagerService();
        }
        return mailTemplateManagerService;
    }
    
    public static ReportTemplateManagerService getReportTemplateManagerService(){
        if(reportTemplateManagerService == null){
            reportTemplateManagerService = new ReportTemplateManagerService();
        }
        return reportTemplateManagerService;
    }
    
}
