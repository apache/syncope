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
package org.apache.syncope.client.console.pages;

import com.fasterxml.jackson.databind.json.JsonMapper;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.List;
import org.apache.syncope.client.console.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.console.panels.AnysPanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel;
import org.apache.syncope.client.console.panels.RealmChoicePanel.ChosenRealm;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.ResultPanel;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Anys extends BasePage {

    private static final long serialVersionUID = -1100228004207271270L;

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    public static final String SELECTED_INDEX = "selectedIndex";

    public static final String INITIAL_REALM = "initialRealm";

    @SpringBean
    protected RealmRestClient realmRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final RealmChoicePanel realmChoicePanel;

    protected final WebMarkupContainer content;

    protected final BaseModal<RealmTO> modal;

    public Anys(final PageParameters parameters) {
        super(parameters);

        body.add(BookmarkablePageLinkBuilder.build("dashboard", "dashboardBr", Dashboard.class));

        content = new WebMarkupContainer("content");
        body.add(content.setOutputMarkupId(true));

        realmChoicePanel = buildRealmChoicePanel(parameters.get(INITIAL_REALM).toOptionalString(), getPageReference());
        content.add(realmChoicePanel);

        content.add(new Label("body", "Root realm"));

        modal = new BaseModal<>("modal");
        modal.size(Modal.Size.Large);
        content.add(modal);

        modal.setWindowClosedCallback(target -> {
            target.add(realmChoicePanel.reloadRealmTree(target));
            target.add(content);
            modal.show(false);
        });

        updateRealmContent(realmChoicePanel.getCurrentRealm(), parameters.get(SELECTED_INDEX).toInt(0));
    }

    protected RealmChoicePanel buildRealmChoicePanel(final String initialRealm, final PageReference pageRef) {
        RealmChoicePanel panel = new RealmChoicePanel("realmChoicePanel", initialRealm, realmRestClient, pageRef);
        panel.setOutputMarkupId(true);
        return panel;
    }

    public RealmChoicePanel getRealmChoicePanel() {
        return realmChoicePanel;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ChosenRealm choosenRealm) {
            updateRealmContent(choosenRealm.obj(), 0);
            choosenRealm.target().add(content);
        }
    }

    protected WebMarkupContainer updateRealmContent(final RealmTO realmTO, final int selectedIndex) {
        if (realmTO != null) {
            content.addOrReplace(new Content(realmTO, anyTypeRestClient.listAnyTypes(), selectedIndex));
        }
        return content;
    }

    protected class Content extends AnysPanel {

        private static final long serialVersionUID = 8221398624379357183L;

        protected Content(final RealmTO realmTO, final List<AnyTypeTO> anyTypes, final int selectedIndex) {
            super("body", realmTO, anyTypes, selectedIndex, Anys.this.getPageReference());
        }

        @Override
        protected void setWindowClosedReloadCallback(final BaseModal<?> modal) {
            modal.setWindowClosedCallback(target -> {
                if (modal.getContent() instanceof ResultPanel<?, ?> rp) {
                    RealmTO newRealmTO = RealmTO.class.cast(ProvisioningResult.class.cast(rp.getResult()).getEntity());
                    // reload realmChoicePanel label too - SYNCOPE-1151
                    target.add(realmChoicePanel.reloadRealmTree(target, Model.of(newRealmTO)));
                    realmChoicePanel.setCurrentRealm(newRealmTO);
                    send(Anys.this, Broadcast.DEPTH, new ChosenRealm(newRealmTO, target));
                } else {
                    target.add(realmChoicePanel.reloadRealmTree(target));
                }
                target.add(content);
                modal.show(false);
            });
        }
    }
}
