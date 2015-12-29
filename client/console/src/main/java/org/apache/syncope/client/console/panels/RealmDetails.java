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

import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmDetails extends Panel {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final Logger LOG = LoggerFactory.getLogger(RealmDetails.class);

    private final WebMarkupContainer container;

    public RealmDetails(final String id, final RealmTO realmTO) {
        this(id, realmTO, null, true);
    }

    public RealmDetails(
            final String id, final RealmTO realmTO, final ActionLinksPanel<?> actions, final boolean unwraped) {
        super(id);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.setRenderBodyOnly(unwraped);
        add(container);

        final FieldPanel<String> name = new AjaxTextFieldPanel(
                "name", "name", new PropertyModel<String>(realmTO, "name"), false);
        name.addRequiredLabel();
        container.add(name);

        final FieldPanel<String> fullPath = new AjaxTextFieldPanel(
                "fullPath", "fullPath", new PropertyModel<String>(realmTO, "fullPath"), false);
        fullPath.setEnabled(false);
        container.add(fullPath);

        final FieldPanel<String> accountPolicy = new AjaxTextFieldPanel(
                "accountPolicy", "accountPolicy", new PropertyModel<String>(realmTO, "accountPolicy"), false);
        container.add(accountPolicy);

        final FieldPanel<String> passwordPolicy = new AjaxTextFieldPanel(
                "passwordPolicy", "passwordPolicy", new PropertyModel<String>(realmTO, "passwordPolicy"), false);
        container.add(passwordPolicy);

        if (actions == null) {
            add(new Fragment("actions", "emptyFragment", this).setRenderBodyOnly(true));
        } else {
            Fragment fragment = new Fragment("actions", "actionsFragment", this);
            fragment.add(actions);
            add(fragment.setRenderBodyOnly(true));
        }
    }

    public RealmDetails setContentEnabled(final boolean enable) {
        container.setEnabled(enable);
        return this;
    }
}
