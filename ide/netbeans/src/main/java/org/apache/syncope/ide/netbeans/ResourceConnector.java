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

import java.io.IOException;
import java.util.prefs.Preferences;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.ide.netbeans.service.MailTemplateManagerService;
import org.apache.syncope.ide.netbeans.service.ReportTemplateManagerService;
import org.apache.syncope.ide.netbeans.service.ImplementationManagerService;
import org.apache.syncope.ide.netbeans.service.SyncopeManagerService;
import org.apache.syncope.ide.netbeans.view.ResourceExplorerTopComponent;
import org.openide.util.NbPreferences;

public final class ResourceConnector {

    private static SyncopeManagerService SYNCOPE_MANAGER_SERVICE;

    private static MailTemplateManagerService MAIL_TEMPLATE_MANAGER_SERVICE;

    private static ReportTemplateManagerService REPORT_TEMPLATE_MANAGER_SERVICE;

    private static ImplementationManagerService IMPLEMENTATION_MANAGER_SERVICE;

    private static final Object SYNCOPE_MONITOR = new Object();

    private static final Object MAIL_TEMPLATE_MONITOR = new Object();

    private static final Object REPORT_TEMPLATE_MONITOR = new Object();

    private static final Object IMPLEMENTATION_MONITOR = new Object();

    private ResourceConnector() {
    }

    public static SyncopeManagerService getSyncopeManagerService() {
        synchronized (SYNCOPE_MONITOR) {
            ConnectionParams connParams = getConnectionParams();
            SYNCOPE_MANAGER_SERVICE = new SyncopeManagerService(
                    connParams.getUrl(),
                    connParams.getUsername(),
                    connParams.getPassword());
        }
        return SYNCOPE_MANAGER_SERVICE;
    }

    public static MailTemplateManagerService getMailTemplateManagerService() throws IOException {
        synchronized (MAIL_TEMPLATE_MONITOR) {
            ConnectionParams connParams = getConnectionParams();
            MAIL_TEMPLATE_MANAGER_SERVICE = new MailTemplateManagerService(
                    connParams.getUrl(),
                    connParams.getUsername(),
                    connParams.getPassword());
        }
        return MAIL_TEMPLATE_MANAGER_SERVICE;
    }

    public static ReportTemplateManagerService getReportTemplateManagerService() throws IOException {
        synchronized (REPORT_TEMPLATE_MONITOR) {
            ConnectionParams connParams = getConnectionParams();
            REPORT_TEMPLATE_MANAGER_SERVICE = new ReportTemplateManagerService(
                    connParams.getUrl(),
                    connParams.getUsername(),
                    connParams.getPassword());
        }
        return REPORT_TEMPLATE_MANAGER_SERVICE;
    }

    public static ImplementationManagerService getImplementationManagerService() throws IOException {
        synchronized (IMPLEMENTATION_MONITOR) {
            ConnectionParams connParams = getConnectionParams();
            IMPLEMENTATION_MANAGER_SERVICE = new ImplementationManagerService(
                    connParams.getUrl(),
                    connParams.getUsername(),
                    connParams.getPassword());
        }
        return IMPLEMENTATION_MANAGER_SERVICE;
    }

    public static ConnectionParams getConnectionParams() {
        Preferences prefs = NbPreferences.forModule(ResourceExplorerTopComponent.class);
        return ConnectionParams.builder()
                .scheme(prefs.get("scheme", "http"))
                .host(prefs.get("host", "localhost"))
                .port(prefs.get("port", "8080"))
                .username(prefs.get("username", StringUtils.EMPTY))
                .password(prefs.get("password", StringUtils.EMPTY))
                .build();
    }

}
