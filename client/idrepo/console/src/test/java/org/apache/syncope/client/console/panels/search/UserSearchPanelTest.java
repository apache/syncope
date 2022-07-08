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
package org.apache.syncope.client.console.panels.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.client.console.AbstractAdminTest;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.tester.FormTester;
import org.junit.jupiter.api.Test;

public class UserSearchPanelTest extends AbstractAdminTest {

    @Test
    public void test() {
        BasePage page = new BasePage();
        PageReference pageRef = mock(PageReference.class);
        when(pageRef.getPage()).thenReturn(page);

        SearchClause clause = new SearchClause();
        clause.setComparator(SearchClause.Comparator.EQUALS);
        clause.setType(SearchClause.Type.ATTRIBUTE);
        clause.setProperty("username");

        TESTER.startComponentInPage(new UserSearchPanel.Builder(
                new ListModel<>(List.of(clause)), pageRef).
                required(true).enableSearch().build("content"));

        FormTester formTester = TESTER.newFormTester(
                "content:searchFormContainer:search:multiValueContainer:innerForm");

        assertEquals("username", formTester.getForm().
                get("content:view:0:panel:container:property:textField").getDefaultModelObjectAsString());
        assertNull(formTester.getForm().
                get("content:view:0:panel:container:value:textField").getDefaultModelObject());

        formTester.setValue("content:view:0:panel:container:property:textField", "firstname");
        TESTER.executeAjaxEvent(formTester.getForm().
                get("content:view:0:panel:container:property:textField"), Constants.ON_KEYDOWN);
        formTester.setValue("content:view:0:panel:container:value:textField", "vincenzo");
        TESTER.executeAjaxEvent(formTester.getForm().
                get("content:view:0:panel:container:value:textField"), Constants.ON_KEYDOWN);

        Component searchButton = formTester.getForm().
                get("content:view:0:panel:container:operatorContainer:operator:search");
        TESTER.clickLink(searchButton);
        TESTER.executeAjaxEvent(searchButton.getPageRelativePath(), Constants.ON_CLICK);

        assertEquals("firstname", formTester.getForm().
                get("content:view:0:panel:container:property:textField").getDefaultModelObjectAsString());
        assertEquals("vincenzo", formTester.getForm().
                get("content:view:0:panel:container:value:textField").getDefaultModelObjectAsString());
    }
}
