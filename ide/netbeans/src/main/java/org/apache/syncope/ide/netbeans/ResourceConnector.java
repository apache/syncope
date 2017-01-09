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
package org.apache.syncope.ide.netbeans;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import org.apache.syncope.ide.netbeans.service.MailTemplateManagerService;
import org.apache.syncope.ide.netbeans.service.ReportTemplateManagerService;

public final class ResourceConnector {

    private static MailTemplateManagerService MAIL_TEMPLATE_MANAGER_SERVICE;

    private static ReportTemplateManagerService REPORT_TEMPLATE_MANAGER_SERVICE;

    private static final Object MAIL_TEMPLATE_MONITOR = new Object();

    private static final Object REPORT_TEMPLATE_MONITOR = new Object();

    private ResourceConnector() {
    }

    public static MailTemplateManagerService getMailTemplateManagerService() throws IOException {
        synchronized (MAIL_TEMPLATE_MONITOR) {
            if (MAIL_TEMPLATE_MANAGER_SERVICE == null) {
                UserProperties userProperties = getUserProperties();
                MAIL_TEMPLATE_MANAGER_SERVICE = new MailTemplateManagerService(
                        userProperties.getUrl(), userProperties.getUserName(),
                        userProperties.getPassword());
            }
        }
        return MAIL_TEMPLATE_MANAGER_SERVICE;
    }

    public static ReportTemplateManagerService getReportTemplateManagerService() throws IOException {
        synchronized (REPORT_TEMPLATE_MONITOR) {
            if (REPORT_TEMPLATE_MANAGER_SERVICE == null) {
                UserProperties userProperties = getUserProperties();
                REPORT_TEMPLATE_MANAGER_SERVICE = new ReportTemplateManagerService(
                        userProperties.getUrl(), userProperties.getUserName(),
                        userProperties.getPassword());
            }
        }
        return REPORT_TEMPLATE_MANAGER_SERVICE;
    }

    private static UserProperties getUserProperties() throws FileNotFoundException, IOException {
        File file = new File("UserData.txt");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String url = bufferedReader.readLine();
        String userName = bufferedReader.readLine();
        String password = bufferedReader.readLine();

        UserProperties userProperties = new UserProperties(url, userName, password);
        return userProperties;
    }

}
