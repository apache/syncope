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
package org.apache.syncope.client.console.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.syncope.client.console.AbstractTest;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.wicket.Component;
import org.junit.jupiter.api.Test;

public class DashboardOverviewPanelTest extends AbstractTest {

    @Test
    public void overviewTest() {

        SystemInfo systemInfo = new SystemInfo();
        systemInfo.setHostname("testHostname");
        systemInfo.setJvm("testJvm");
        systemInfo.setOs("testOs");
        systemInfo.setStartTime(10);
        systemInfo.setAvailableProcessors(20);

        String searchPathUsers = "body:content:tabbedPanel:panel:container:totalUsers:box:number";
        String searchPathGroups = "body:content:tabbedPanel:panel:container:totalGroups:box:number";
        String searchPathAny = "body:content:tabbedPanel:panel:container:totalAny1OrRoles:box:number";
        String searchPathResources = "body:content:tabbedPanel:panel:container:totalAny2OrResources:box:number";

        Component componentUsers = TESTER.getComponentFromLastRenderedPage(searchPathUsers);
        assertNotNull(componentUsers);
        Component componentGroups = TESTER.getComponentFromLastRenderedPage(searchPathGroups);
        assertNotNull(componentUsers);
        Component componentAny = TESTER.getComponentFromLastRenderedPage(searchPathAny);
        assertNotNull(componentUsers);
        Component componentResources = TESTER.getComponentFromLastRenderedPage(searchPathResources);
        assertNotNull(componentUsers);

        assertEquals("4", componentUsers.getDefaultModelObjectAsString());
        assertEquals("16", componentGroups.getDefaultModelObjectAsString());
        assertEquals("6", componentAny.getDefaultModelObjectAsString());
        assertEquals("21", componentResources.getDefaultModelObjectAsString());

    }

}
