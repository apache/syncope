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
package org.apache.syncope.client.console.wizards.any;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdMConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.any.MergeLinkedAccountsResourcesPanel.ResourceSelectionDirectoryPanel.ResourcesDataProvider;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
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
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MergeLinkedAccountsResourcesPanel extends WizardStep implements ICondition {

    private static final long serialVersionUID = 1221037007528732347L;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    protected final MergeLinkedAccountsWizardModel wizardModel;

    public MergeLinkedAccountsResourcesPanel(
            final MergeLinkedAccountsWizardModel wizardModel,
            final PageReference pageRef) {

        super();
        setOutputMarkupId(true);
        this.wizardModel = wizardModel;
        add(new ResourceSelectionDirectoryPanel("resources", resourceRestClient, pageRef));
    }

    @Override
    public boolean evaluate() {
        return SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
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

    protected class ResourceSelectionDirectoryPanel extends
            DirectoryPanel<ResourceTO, ResourceTO, ResourcesDataProvider, ResourceRestClient> {

        private static final long serialVersionUID = 6005711052393825472L;

        ResourceSelectionDirectoryPanel(
                final String id,
                final ResourceRestClient restClient,
                final PageReference pageRef) {

            super(id, restClient, pageRef, true);

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
            return IdMConstants.PREF_RESOURCES_PAGINATOR_ROWS;
        }

        @Override
        protected List<IColumn<ResourceTO, String>> getColumns() {
            List<IColumn<ResourceTO, String>> columns = new ArrayList<>();
            columns.add(new PropertyColumn<>(new ResourceModel("resource"), "key", "key"));
            return columns;
        }

        @Override
        protected ActionsPanel<ResourceTO> getActions(final IModel<ResourceTO> model) {
            final ActionsPanel<ResourceTO> panel = super.getActions(model);
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ResourceTO resource) {
                    MergeLinkedAccountsWizardModel model = MergeLinkedAccountsResourcesPanel.this.wizardModel;
                    String connObjectKeyValue = restClient.getConnObjectKeyValue(
                            resource.getKey(),
                            model.getMergingUser().getType(),
                            model.getMergingUser().getKey());
                    if (connObjectKeyValue != null) {
                        model.setResource(resource);
                        String tableId = MergeLinkedAccountsResourcesPanel.this.
                                get("resources:container:content:searchContainer:resultTable"
                                        + ":tablePanel:groupForm:checkgroup:dataTable").
                                getMarkupId();
                        String js = "$('#" + tableId + "').removeClass('active');";
                        js += "$('#" + tableId + " tbody tr td div').filter(function() "
                                + "{return $(this).text() === \"" + resource.getKey() + "\";})"
                                + ".parent().parent().addClass('active');";
                        target.prependJavaScript(js);

                    } else {
                        error("Unable to determine connector object key");
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.SELECT, IdMEntitlement.RESOURCE_READ);
            return panel;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return List.of();
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
                List<ResourceTO> list = restClient.list();
                list.sort(comparator);
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
