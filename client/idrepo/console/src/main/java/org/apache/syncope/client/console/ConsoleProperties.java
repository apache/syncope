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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AnyPanel;
import org.apache.syncope.client.ui.commons.CommonUIProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("console")
public class ConsoleProperties extends CommonUIProperties {

    private static final long serialVersionUID = -6444470724127309370L;

    public static class Topology implements Serializable {

        private static final long serialVersionUID = -4530238696048859905L;

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

    private String adminUser = "admin";

    private final Map<String, Class<? extends BasePage>> page = new HashMap<>();

    private String defaultAnyPanelClass = AnyPanel.class.getName();

    private int realmsFullTreeThreshold = 20;

    private final Topology topology = new Topology();

    @Override
    public String getAdminUser() {
        return adminUser;
    }

    @Override
    public void setAdminUser(final String adminUser) {
        this.adminUser = adminUser;
    }

    public Map<String, Class<? extends BasePage>> getPage() {
        return page;
    }

    public String getDefaultAnyPanelClass() {
        return defaultAnyPanelClass;
    }

    public void setDefaultAnyPanelClass(final String defaultAnyPanelClass) {
        this.defaultAnyPanelClass = defaultAnyPanelClass;
    }

    public int getRealmsFullTreeThreshold() {
        return realmsFullTreeThreshold;
    }

    public void setRealmsFullTreeThreshold(final int realmsFullTreeThreshold) {
        this.realmsFullTreeThreshold = realmsFullTreeThreshold;
    }

    public Topology getTopology() {
        return topology;
    }
}
