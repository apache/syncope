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
package org.apache.syncope.client.console.tasks;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Modal window with Task form (to stop and start execution).
 */
public class PropagationTaskDetails extends TaskDetails<PropagationTaskTO> {

    private static final long serialVersionUID = -4110576026663173545L;

    public PropagationTaskDetails(final PropagationTaskTO taskTO, final PageReference pageRef) {
        super(taskTO, pageRef);
    }

    @Override
    protected List<ITab> buildTabList(final PropagationTaskTO taskTO, final PageReference pageRef) {
        final List<ITab> res = new ArrayList<>();
        res.add(new AbstractTab(new Model<>("profile")) {

            private static final long serialVersionUID = -5861786415855103559L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new Profile(panelId, taskTO);
            }
        });
        return res;
    }

    public class Profile extends Panel {

        private static final long serialVersionUID = -1518744792346267268L;

        public Profile(final String id, final PropagationTaskTO taskTO) {
            super(id);
            add(new AjaxTextFieldPanel(
                    "key", getString("key"), new PropertyModel<String>(taskTO, "key")).
                    setEnabled(false));

            add(new AjaxTextFieldPanel(
                    "anyKey", getString("anyKey"), new PropertyModel<String>(taskTO, "anyKey")).
                    setEnabled(false));

            add(new AjaxTextFieldPanel(
                    "resource", getString("resource"), new PropertyModel<String>(taskTO, "resource")).
                    setEnabled(false));
        }
    }
}
