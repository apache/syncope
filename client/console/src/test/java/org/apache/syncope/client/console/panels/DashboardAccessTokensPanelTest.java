/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *i
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.syncope.client.console.AbstractTest;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.junit.jupiter.api.Test;

public class DashboardAccessTokensPanelTest extends AbstractTest {

    @Test
    public void accessTokensTest() {

        TESTER.clickLink("body:content:tabbedPanel:tabs-container:tabs:1:link");

        TESTER.assertComponent("body:content:tabbedPanel:panel", DashboardAccessTokensPanel.class);
        TESTER.assertComponent("body:content:tabbedPanel:panel:accessTokens", WebMarkupContainer.class);

        String pathAccessToken =
                "body:content:tabbedPanel:panel:accessTokens:container:content:searchContainer:resultTable:tablePanel:groupForm";

        Component componentAccessToken = TESTER.getComponentFromLastRenderedPage(pathAccessToken);
        assertNotNull(componentAccessToken);

    }
}
