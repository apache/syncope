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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class WAPushModalPanel extends AbstractModalPanel<Serializable> {

    private static final long serialVersionUID = -8589310598889871801L;

    @SpringBean
    protected WAConfigRestClient waConfigRestClient;

    protected final Model<WAConfigService.PushSubject> subjectModel = new Model<>(WAConfigService.PushSubject.conf);

    protected final ListModel<String> servicesModel = new ListModel<>();

    public WAPushModalPanel(
            final BaseModal<Serializable> modal,
            final List<NetworkService> instances,
            final PageReference pageRef) {

        super(modal, pageRef);

        List<String> addresses = instances.stream().
                map(NetworkService::getAddress).distinct().sorted().collect(Collectors.toList());
        servicesModel.setObject(addresses);

        add(new AjaxPalettePanel.Builder<String>().setName("services").setAllowMoveAll(true).build(
                "services",
                servicesModel,
                new ListModel<>(addresses)).addRequiredLabel());

        add(new AjaxDropDownChoicePanel<>(
                "subject", getString("subject"), subjectModel).
                setChoices(List.of(WAConfigService.PushSubject.values())).
                setChoiceRenderer(s -> getString(s.name(), Model.of(), s.name())).setNullValid(false));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            waConfigRestClient.push(subjectModel.getObject(), servicesModel.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While pushing to WA", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
