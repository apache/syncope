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

import org.apache.syncope.client.console.pages.Engagements;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EngagementsITCase extends AbstractConsoleITCase {

    private static final String SCHED_TASK_FORM =
            "body:content:tabbedPanel:panel:firstLevelContainer:first:outerObjectsRepeater:0:outer:form:content:form";

    @BeforeEach
    public void login() {
        doLogin(ADMIN_UNAME, ADMIN_PWD);
        TESTER.clickLink("body:engagementsLI:engagements", false);
        TESTER.assertRenderedPage(Engagements.class);
    }

    @Test
    public void createSchedTask() {
        TESTER.clickLink("body:content:tabbedPanel:panel:firstLevelContainer:first:container:content:add");

        FormTester formTester = TESTER.newFormTester(SCHED_TASK_FORM);
        formTester.setValue("view:name:textField", "test");
        formTester.select("view:jobDelegate:dropDownChoiceField", 0);

        formTester.submit("buttons:next");
        TESTER.cleanupFeedbackMessages();

        formTester = TESTER.newFormTester(SCHED_TASK_FORM);

        TESTER.assertComponent(SCHED_TASK_FORM + ":view:schedule:seconds:textField", TextField.class);

        formTester.submit("buttons:finish");
        TESTER.cleanupFeedbackMessages();
    }
}
