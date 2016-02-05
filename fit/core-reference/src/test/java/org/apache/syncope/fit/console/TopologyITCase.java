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
package org.apache.syncope.fit.console;

import org.apache.syncope.client.console.commons.Constants;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Before;
import org.junit.Test;

public class TopologyITCase extends AbstractConsoleITCase {

    @Before
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
    }

    @Test
    public void showTopology() {
        wicketTester.clickLink("topologyLI:topology");
        wicketTester.assertComponent("syncope", WebMarkupContainer.class);
        wicketTester.assertComponent("resources:1", WebMarkupContainer.class);
        wicketTester.assertComponent("resources:2:resources:0", WebMarkupContainer.class);
    }

    @Test
    public void showTopologyToggleMenu() {
        wicketTester.clickLink("topologyLI:topology");
        wicketTester.executeAjaxEvent("resources:2:resources:0:res", Constants.ON_CLICK);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:delete", AjaxLink.class);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:edit", AjaxLink.class);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:propagation", AjaxLink.class);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:synchronization", AjaxLink.class);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:push", AjaxLink.class);
        wicketTester.executeAjaxEvent("syncope", Constants.ON_CLICK);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:tasks", AjaxLink.class);
        wicketTester.executeAjaxEvent("conns:0:conns:4:conn", Constants.ON_CLICK);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:create", AjaxLink.class);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:delete", AjaxLink.class);
        wicketTester.assertComponent("toggle:togglePanelContainer:container:actions:edit", AjaxLink.class);
    }

    @Test
    public void executeSyncTask() {
        wicketTester.clickLink("topologyLI:topology");
        wicketTester.executeAjaxEvent("resources:2:resources:0:res", Constants.ON_CLICK);
        wicketTester.clickLink("toggle:togglePanelContainer:container:actions:synchronization");
        wicketTester.clickLink("toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:10:cell:panelExecute:executeLink");
        wicketTester.clickLink("toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:first:"
                + "container:content:startAt:togglePanelContainer:startAtForm:startAt");
        wicketTester.assertInfoMessages("Operation executed successfully");
    }

    @Test
    public void editPushTask() {
        wicketTester.clickLink("topologyLI:topology");
        wicketTester.executeAjaxEvent("resources:5:resources:0:res", Constants.ON_CLICK);
        wicketTester.clickLink("toggle:togglePanelContainer:container:actions:push");
        wicketTester.clickLink("toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:"
                + "first:container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:"
                + "body:rows:1:cells:9:cell:panelEdit:editLink");

        final FormTester formTester = wicketTester.newFormTester("toggle:outherObjectsRepeater:1:outher:form:content:"
                + "tasks:firstLevelContainer:first:container:content:wizard:form");

        formTester.setValue("view:description:textField", "test");
        formTester.submit("buttons:finish");

        wicketTester.assertLabel("toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:first:"
                + "container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable:body:rows:"
                + "2:cells:4:cell", "test");
    }

    @Test
    public void createSchedTask() {
        wicketTester.clickLink("topologyLI:topology");
        wicketTester.executeAjaxEvent("syncope", Constants.ON_CLICK);
        wicketTester.clickLink("toggle:togglePanelContainer:container:actions:tasks");
        wicketTester.clickLink("toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:first:"
                + "container:content:add");

        final FormTester formTester = wicketTester.newFormTester(
                "toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:first:"
                + "container:content:wizard:form");
        formTester.setValue("view:name:textField", "test");
        formTester.select("view:jobDelegateClassName:dropDownChoiceField", 0);
        
        formTester.submit("buttons:next");

        wicketTester.assertComponent("toggle:outherObjectsRepeater:1:outher:form:content:tasks:firstLevelContainer:"
                + "first:container:content:wizard:form:view:schedule:seconds:textField", TextField.class);

        formTester.submit("buttons:finish");
    }
}
