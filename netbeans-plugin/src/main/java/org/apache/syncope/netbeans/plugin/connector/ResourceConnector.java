/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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

public class ResourceConnector {
    
    private static MailTemplateManagerService mailTemplateManagerService;
    private static ReportTemplateManagerService reportTemplateManagerService;
    
    public static MailTemplateManagerService getMailTemplateManagerService() 
            throws IOException{
        if(mailTemplateManagerService == null){
            UserProperties userProperties = getUserProperties();
            mailTemplateManagerService = new MailTemplateManagerService(
                    userProperties.getUrl(),userProperties.getUserName(),
                    userProperties.getPassword());
        }
        return mailTemplateManagerService;
    }
    
    public static ReportTemplateManagerService getReportTemplateManagerService()
            throws IOException{
        if(reportTemplateManagerService == null){
            UserProperties userProperties = getUserProperties();
            reportTemplateManagerService = new ReportTemplateManagerService(
                    userProperties.getUrl(), userProperties.getUserName(),
                    userProperties.getPassword());
        }
        return reportTemplateManagerService;
    }
    
    private static UserProperties getUserProperties() 
            throws FileNotFoundException, IOException{       
        File file = new File("UserData.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String url = bufferedReader.readLine();
        String userName = bufferedReader.readLine();
        String password = bufferedReader.readLine();
        
        UserProperties userProperties = new UserProperties(url, userName, 
                password);
        return userProperties;  
    }
    
}
