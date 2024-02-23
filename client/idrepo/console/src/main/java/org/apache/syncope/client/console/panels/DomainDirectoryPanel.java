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

import com.fasterxml.jackson.databind.JsonNode;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DomainDirectoryPanel.DomainProvider;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class DomainDirectoryPanel extends DirectoryPanel<Domain, Domain, DomainProvider, SyncopeRestClient> {

    private static final long serialVersionUID = -1039907608594680220L;

    @SpringBean
    private DomainOps domainOps;

    private final BaseModal<Domain> utilityModal = new BaseModal<>(Constants.OUTER);

    private Class<? extends Domain> domainClass = JPADomain.class;

    public DomainDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, null, pageRef);
        disableCheckBoxes();

        modal.size(Modal.Size.Large);

        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        addOuterObject(utilityModal);
        utilityModal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        utilityModal.size(Modal.Size.Small);
        utilityModal.addSubmitButton();

        Domain newDomain;
        try {
            JsonNode info = SyncopeConsoleSession.get().getAnonymousClient().info();
            if (info.has("persistence") && info.get("persistence").has("vendor")
                    && "OpenJPA".equals(info.get("persistence").get("vendor").asText())) {

                domainClass = JPADomain.class;
            } else {
                domainClass = Neo4jDomain.class;
            }

            newDomain = domainClass.getConstructor().newInstance();
        } catch (Exception e) {
            LOG.error("Could not instantiate {}", domainClass.getName(), e);
            newDomain = new JPADomain();
        }

        addNewItemPanelBuilder(new DomainWizardBuilder(domainOps, newDomain, pageRef), true);

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.KEYMASTER);
    }

    @Override
    protected List<IColumn<Domain, String>> getColumns() {
        List<IColumn<Domain, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new StringResourceModel("key", this), "key", "key"));
        if (JPADomain.class.equals(domainClass)) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("jdbcURL", this), "jdbcURL", "jdbcURL"));
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("poolMaxActive", this), "poolMaxActive", "poolMaxActive"));
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("poolMinIdle", this), "poolMinIdle", "poolMinIdle"));
        } else {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("uri", this), "uri", "uri"));
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("poolMaxActive", this),
                    "maxConnectionPoolSize", "maxConnectionPoolSize"));
        }
        return columns;
    }

    @Override
    protected ActionsPanel<Domain> getActions(final IModel<Domain> model) {
        ActionsPanel<Domain> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 7610801302168867641L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Domain ignore) {
                utilityModal.header(new ResourceModel("adjust.pool.size"));
                utilityModal.setContent(new DomainPoolModalPanel(model.getObject(), utilityModal, pageRef));
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.KEYMASTER);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 7610801302168867641L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Domain ignore) {
                utilityModal.header(new ResourceModel("set.admin.credentials"));
                utilityModal.setContent(new DomainAdminCredentialsPanel(model.getObject(), utilityModal, pageRef));
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.PASSWORD_MANAGEMENT, IdRepoEntitlement.KEYMASTER);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Domain ignore) {
                final Domain domain = model.getObject();
                try {
                    domainOps.delete(domain.getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (KeymasterException e) {
                    LOG.error("While deleting {}", domain.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.KEYMASTER, true);

        return panel;
    }

    @Override
    protected DomainProvider dataProvider() {
        return new DomainProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_DOMAIN_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();

    }

    protected final class DomainProvider extends DirectoryDataProvider<Domain> {

        private static final long serialVersionUID = 8668261951640646188L;

        private final SortableDataProviderComparator<Domain> comparator;

        public DomainProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<? extends Domain> iterator(final long first, final long count) {
            List<Domain> list = domainOps.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return domainOps.list().size();
        }

        @Override
        public IModel<Domain> model(final Domain object) {
            return new IModel<>() {

                private static final long serialVersionUID = 8093553921710742624L;

                @Override
                public Domain getObject() {
                    return object;
                }
            };
        }
    }
}
