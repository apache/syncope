/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.syncope.netbeans.plugin.connector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.syncope.netbeans.plugin.entity.UserProperties;
import org.apache.syncope.netbeans.plugin.service.MailTemplateManagerService;
import org.apache.syncope.netbeans.plugin.service.ReportTemplateManagerService;

/**
 *
 * @author nuwan
 */
public class ResourceConnector {
    
    private static MailTemplateManagerService mailTemplateManagerService;
    private static ReportTemplateManagerService reportTemplateManagerService;
    
    public static MailTemplateManagerService getMailTemplateManagerService() throws IOException{
        if(mailTemplateManagerService == null){
            UserProperties userProperties = getUserProperties();
            mailTemplateManagerService = new MailTemplateManagerService(
                    userProperties.getUrl(),userProperties.getUserName(),
                    userProperties.getPassword());
        }
        return mailTemplateManagerService;
    }
    
    public static ReportTemplateManagerService getReportTemplateManagerService() throws IOException{
        //if(reportTemplateManagerService == null){
            UserProperties userProperties = getUserProperties();
            reportTemplateManagerService = new ReportTemplateManagerService(
                    userProperties.getUrl(), userProperties.getUserName(),
                    userProperties.getPassword());
        //}
        return reportTemplateManagerService;
    }
    
    private static UserProperties getUserProperties() throws FileNotFoundException, IOException{       
        File file = new File("UserData.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String url = bufferedReader.readLine();
        String userName = bufferedReader.readLine();
        String password = bufferedReader.readLine();
        
        UserProperties userProperties = new UserProperties(url, userName, password);
        return userProperties;  
    }
    
}
