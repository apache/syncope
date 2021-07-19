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
package org.apache.syncope.client.console;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AnyPanel;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("console")
public class ConsoleProperties {

    public static class Topology {

        private int corePoolSize = 10;

        private int maxPoolSize = 20;

        private int queueCapacity = 50;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(final int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(final int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(final int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    private String anonymousUser;

    private String anonymousKey;

    private boolean useGZIPCompression = true;

    private int maxUploadFileSizeMB = 5;

    private long maxWaitTimeOnApplyChanges = 30L;

    private String reconciliationReportKey;

    private final Map<String, Class<? extends BasePage>> page = new HashMap<>();

    private String defaultAnyPanelClass = AnyPanel.class.getName();

    private final Topology topology = new Topology();

    private boolean xForward = true;

    private String xForwardProtocolHeader = "X-Forwarded-Proto";

    private int xForwardHttpPort = 80;

    private int xForwardHttpsPort = 443;

    private boolean csrf = true;

    private final Map<String, String> securityHeaders = new HashMap<>();

    public String getAnonymousUser() {
        return anonymousUser;
    }

    public void setAnonymousUser(final String anonymousUser) {
        this.anonymousUser = anonymousUser;
    }

    public String getAnonymousKey() {
        return anonymousKey;
    }

    public void setAnonymousKey(final String anonymousKey) {
        this.anonymousKey = anonymousKey;
    }

    public boolean isUseGZIPCompression() {
        return useGZIPCompression;
    }

    public void setUseGZIPCompression(final boolean useGZIPCompression) {
        this.useGZIPCompression = useGZIPCompression;
    }

    public int getMaxUploadFileSizeMB() {
        return maxUploadFileSizeMB;
    }

    public void setMaxUploadFileSizeMB(final int maxUploadFileSizeMB) {
        this.maxUploadFileSizeMB = maxUploadFileSizeMB;
    }

    public long getMaxWaitTimeOnApplyChanges() {
        return maxWaitTimeOnApplyChanges;
    }

    public void setMaxWaitTimeOnApplyChanges(final long maxWaitTimeOnApplyChanges) {
        this.maxWaitTimeOnApplyChanges = maxWaitTimeOnApplyChanges;
    }

    public String getReconciliationReportKey() {
        return reconciliationReportKey;
    }

    public void setReconciliationReportKey(final String reconciliationReportKey) {
        this.reconciliationReportKey = reconciliationReportKey;
    }

    public String getDefaultAnyPanelClass() {
        return defaultAnyPanelClass;
    }

    public void setDefaultAnyPanelClass(final String defaultAnyPanelClass) {
        this.defaultAnyPanelClass = defaultAnyPanelClass;
    }

    public boolean isxForward() {
        return xForward;
    }

    public void setxForward(final boolean xForward) {
        this.xForward = xForward;
    }

    public String getxForwardProtocolHeader() {
        return xForwardProtocolHeader;
    }

    public void setxForwardProtocolHeader(final String xForwardProtocolHeader) {
        this.xForwardProtocolHeader = xForwardProtocolHeader;
    }

    public int getxForwardHttpPort() {
        return xForwardHttpPort;
    }

    public void setxForwardHttpPort(final int xForwardHttpPort) {
        this.xForwardHttpPort = xForwardHttpPort;
    }

    public int getxForwardHttpsPort() {
        return xForwardHttpsPort;
    }

    public void setxForwardHttpsPort(final int xForwardHttpsPort) {
        this.xForwardHttpsPort = xForwardHttpsPort;
    }

    public boolean isCsrf() {
        return csrf;
    }

    public void setCsrf(final boolean csrf) {
        this.csrf = csrf;
    }

    public Map<String, Class<? extends BasePage>> getPage() {
        return page;
    }

    public Topology getTopology() {
        return topology;
    }

    public Map<String, String> getSecurityHeaders() {
        return securityHeaders;
    }
}
