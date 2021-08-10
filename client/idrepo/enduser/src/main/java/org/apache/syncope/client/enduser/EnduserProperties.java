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
package org.apache.syncope.client.enduser;

import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.enduser.pages.BasePage;
import org.apache.syncope.client.enduser.panels.Sidebar;
import org.apache.syncope.client.ui.commons.CommonUIProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("enduser")
public class EnduserProperties extends CommonUIProperties {

    private String adminUser;

    private Class<? extends Sidebar> sidebar = Sidebar.class;

    private String customFormLayout = "classpath:/customFormLayout.json";

    private boolean captcha;

    private final Map<String, Class<? extends BasePage>> page = new HashMap<>();

    public String getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(final String adminUser) {
        this.adminUser = adminUser;
    }

    public Class<? extends Sidebar> getSidebar() {
        return sidebar;
    }

    public void setSidebar(final Class<? extends Sidebar> sidebar) {
        this.sidebar = sidebar;
    }

    public String getCustomFormLayout() {
        return customFormLayout;
    }

    public void setCustomFormLayout(final String customFormLayout) {
        this.customFormLayout = customFormLayout;
    }

    public boolean isCaptcha() {
        return captcha;
    }

    public void setCaptcha(final boolean captcha) {
        this.captcha = captcha;
    }

    public Map<String, Class<? extends BasePage>> getPage() {
        return page;
    }
}
