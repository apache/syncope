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

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DomainPoolModalPanel extends AbstractModalPanel<Domain> {

    private static final long serialVersionUID = -2676426626979352532L;

    @SpringBean
    private DomainOps domainOps;

    private final Domain domain;

    public DomainPoolModalPanel(final Domain domain, final BaseModal<Domain> modal, final PageReference pageRef) {
        super(modal, pageRef);
        this.domain = domain;

        IModel<Integer> poolMaxActiveModel;
        IModel<Integer> poolMinIdleModel;
        if (domain instanceof JPADomain) {
            poolMaxActiveModel = new PropertyModel<>(domain, "poolMaxActive");
            poolMinIdleModel = new PropertyModel<>(domain, "poolMinIdle");
        } else {
            poolMaxActiveModel = new PropertyModel<>(domain, "maxConnectionPoolSize");
            poolMinIdleModel = new Model<>();
        }

        add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).build(
                "poolMaxActive",
                "poolMaxActive",
                Integer.class,
                poolMaxActiveModel).setRequired(true));
        add(new AjaxNumberFieldPanel.Builder<Integer>().min(0).build(
                "poolMinIdle",
                "poolMinIdle",
                Integer.class,
                poolMinIdleModel).
                setRequired(domain instanceof JPADomain).
                setOutputMarkupPlaceholderTag(true).
                setVisible(domain instanceof JPADomain));
    }

    @Override
    public Domain getItem() {
        return domain;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        int max = 10;
        int min = 0;
        if (domain instanceof JPADomain jpaDomain) {
            max = jpaDomain.getPoolMaxActive();
            min = jpaDomain.getPoolMinIdle();
        } else if (domain instanceof Neo4jDomain neo4jDomain) {
            max = neo4jDomain.getMaxConnectionPoolSize();
        }

        try {
            domainOps.adjustPoolSize(domain.getKey(), max, min);

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            this.modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating domain", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
