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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.syncope.client.console.AbstractFlowableConsoleTest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.junit.jupiter.api.Test;

class UserRequestFormPanelTest extends AbstractFlowableConsoleTest {

    private UserRequestFormPanel panel(final UserRequestForm form, final AtomicBoolean viewed) {
        return new UserRequestFormPanel("panel", form) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void viewDetails(final AjaxRequestTarget target) {
                viewed.set(true);
            }
        };
    }

    @Test
    void userDetailsVisibleAndClickable() {
        UserRequestForm form = new UserRequestForm();
        form.setUserTO(new UserTO());

        AtomicBoolean viewed = new AtomicBoolean();
        TESTER.startComponentInPage(panel(form, viewed));

        TESTER.assertVisible("panel:userDetails");
        TESTER.assertEnabled("panel:userDetails");

        TESTER.clickLink("panel:userDetails");
        assertTrue(viewed.get());
    }

    @Test
    void userDetailsHiddenWithoutUser() {
        UserRequestForm form = new UserRequestForm();
        form.setUserTO(null);

        AtomicBoolean viewed = new AtomicBoolean();
        TESTER.startComponentInPage(panel(form, viewed));

        TESTER.assertInvisible("panel:userDetails");
        assertFalse(viewed.get());
    }
}
