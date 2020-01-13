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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.MergeLinkedAccountsWizardModel;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MergeLinkedAccountsResourceSelectionPanel extends WizardStep implements ICondition {
    private static final long serialVersionUID = 1221037007528732347L;

    private final MergeLinkedAccountsWizardModel wizardModel;

    public MergeLinkedAccountsResourceSelectionPanel(final MergeLinkedAccountsWizardModel wizardModel,
                                                     final PageReference pageReference) {
        super();
        setOutputMarkupId(true);
        this.wizardModel = wizardModel;
        add(new ResourceSelectionDirectoryPanel("resources", pageReference));
    }

    @Override
    public boolean evaluate() {
        return SyncopeConsoleApplication.get().getSecuritySettings().getAuthorizationStrategy().
            isActionAuthorized(this, RENDER);
    }

    @Override
    public String getTitle() {
        setSummaryModel(new StringResourceModel("mergeLinkedAccounts.searchResource.summary",
            Model.of(wizardModel.getMergingUser())));
        setTitleModel(new StringResourceModel("mergeLinkedAccounts.searchResource.title",
            Model.of(wizardModel.getMergingUser())));
        return super.getTitle();
    }

    private class ResourceSelectionDirectoryPanel extends
        DirectoryPanel<ResourceTO, ResourceTO,
            ResourceSelectionDirectoryPanel.ResourcesDataProvider, ResourceRestClient> {

        private static final String PAGINATOR_ROWS = "linked.account.merge.paginator.rows";

        private static final long serialVersionUID = 6005711052393825472L;

        ResourceSelectionDirectoryPanel(final String id, final PageReference pageReference) {
            super(id, pageReference, true);

            this.restClient = new ResourceRestClient();
            modal.size(Modal.Size.Large);
            setOutputMarkupId(true);
            disableCheckBoxes();
            initResultTable();
        }

        @Override
        protected ResourcesDataProvider dataProvider() {
            return new ResourcesDataProvider(this.rows);
        }

        @Override
        protected String paginatorRowsKey() {
            return PAGINATOR_ROWS;
        }

        @Override
        protected List<IColumn<ResourceTO, String>> getColumns() {
            List<IColumn<ResourceTO, String>> columns = new ArrayList<>();
            columns.add(new PropertyColumn<>(new ResourceModel("connector"), "key", "key"));
            return columns;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return Collections.emptyList();
        }

        @Override
        protected ActionsPanel<ResourceTO> getActions(final IModel<ResourceTO> model) {
            final ActionsPanel<ResourceTO> panel = super.getActions(model);
            panel.add(new ActionLink<ResourceTO>() {
                private static final long serialVersionUID = -7978723352517770644L;
                @Override
                public void onClick(final AjaxRequestTarget target, final ResourceTO resource) {
                    MergeLinkedAccountsResourceSelectionPanel.this
                        .wizardModel.setResource(resource);
                }
            }, ActionLink.ActionType.SELECT, StandardEntitlement.RESOURCE_READ);
            return panel;
        }

        protected final class ResourcesDataProvider extends DirectoryDataProvider<ResourceTO> {

            private static final long serialVersionUID = -185944053385660794L;

            private final SortableDataProviderComparator<ResourceTO> comparator;

            private ResourcesDataProvider(final int paginatorRows) {
                super(paginatorRows);
                setSort("key", SortOrder.ASCENDING);
                comparator = new SortableDataProviderComparator<>(this);
            }

            @Override
            public Iterator<ResourceTO> iterator(final long first, final long count) {
                List<ResourceTO> list = new ResourceRestClient().list();
                Collections.sort(list, comparator);
                return list.subList((int) first, (int) first + (int) count).iterator();
            }

            @Override
            public long size() {
                return restClient.list().size();
            }

            @Override
            public IModel<ResourceTO> model(final ResourceTO object) {
                return new CompoundPropertyModel<>(object);
            }
        }
    }
}
