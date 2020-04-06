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

import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.EncryptedFieldPanel;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DomainAdminCredentialsPanel extends AbstractModalPanel<Domain> {

    private static final long serialVersionUID = 2893133457496751451L;

    @SpringBean
    private DomainOps domainOps;

    private final Domain domain;

    public DomainAdminCredentialsPanel(
            final Domain domain, final BaseModal<Domain> modal, final PageReference pageRef) {

        super(modal, pageRef);
        this.domain = domain;

        AjaxDropDownChoicePanel<CipherAlgorithm> adminCipherAlgorithm = new AjaxDropDownChoicePanel<>(
                "adminCipherAlgorithm", "adminCipherAlgorithm",
                new PropertyModel<>(domain, "adminCipherAlgorithm"), false);
        adminCipherAlgorithm.setChoices(List.of(CipherAlgorithm.values()));
        adminCipherAlgorithm.addRequiredLabel();
        adminCipherAlgorithm.setNullValid(false);
        add(adminCipherAlgorithm);

        EncryptedFieldPanel adminPassword = new EncryptedFieldPanel(
                "adminPassword", "adminPassword", new PropertyModel<>(domain, "adminPassword"), false);
        adminPassword.setRequired(true);
        add(adminPassword);
    }

    @Override
    public Domain getItem() {
        return domain;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            domainOps.changeAdminPassword(domain.getKey(), domain.getAdminPassword(), domain.getAdminCipherAlgorithm());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating domain", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
