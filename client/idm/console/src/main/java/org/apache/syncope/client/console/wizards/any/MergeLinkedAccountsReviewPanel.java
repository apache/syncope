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
import java.util.stream.Collectors;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.any.MergeLinkedAccountsReviewPanel.LinkedAccountsReviewDirectoryPanel.LinkedAccountsDataProvider;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MergeLinkedAccountsReviewPanel extends WizardStep {

    private static final long serialVersionUID = 1221037007528732347L;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    public MergeLinkedAccountsReviewPanel(
            final MergeLinkedAccountsWizardModel wizardModel,
            final PageReference pageRef) {

        super();
        setOutputMarkupId(true);
        setTitleModel(new StringResourceModel("mergeLinkedAccounts.reviewAccounts.title"));
        add(new LinkedAccountsReviewDirectoryPanel("linkedAccounts", resourceRestClient, pageRef, wizardModel));
    }

    protected static class LinkedAccountsReviewDirectoryPanel extends
            DirectoryPanel<LinkedAccountTO, LinkedAccountTO, LinkedAccountsDataProvider, ResourceRestClient> {

        private static final String PAGINATOR_ROWS = "linked.account.review.paginator.rows";

        private static final long serialVersionUID = 6005711052393825472L;

        private final MergeLinkedAccountsWizardModel wizardModel;

        LinkedAccountsReviewDirectoryPanel(
                final String id,
                final ResourceRestClient restClient,
                final PageReference pageRef,
                final MergeLinkedAccountsWizardModel wizardModel) {

            super(id, restClient, pageRef, true);
            this.wizardModel = wizardModel;
            modal.size(Modal.Size.Large);
            setOutputMarkupId(true);
            disableCheckBoxes();
            initResultTable();
        }

        @Override
        protected LinkedAccountsDataProvider dataProvider() {
            return new LinkedAccountsDataProvider(this.rows);
        }

        @Override
        protected String paginatorRowsKey() {
            return PAGINATOR_ROWS;
        }

        @Override
        protected List<IColumn<LinkedAccountTO, String>> getColumns() {
            List<IColumn<LinkedAccountTO, String>> columns = new ArrayList<>();
            columns.add(new PropertyColumn<>(new ResourceModel("resource"), "resource", "resource"));
            columns.add(new PropertyColumn<>(
                    new ResourceModel("connObjectKeyValue"), "connObjectKeyValue", "connObjectKeyValue"));
            columns.add(new PropertyColumn<>(
                    new ResourceModel(Constants.USERNAME_FIELD_NAME),
                    Constants.USERNAME_FIELD_NAME, Constants.USERNAME_FIELD_NAME));
            columns.add(new BooleanPropertyColumn<>(
                    new ResourceModel("suspended"), "suspended", "suspended"));
            return columns;
        }

        @Override
        protected Collection<ActionLink.ActionType> getBatches() {
            return List.of();
        }

        protected List<LinkedAccountTO> previewAccounts() {
            UserTO mergingUser = wizardModel.getMergingUser();

            // Move linked accounts into the target/base user as linked accounts
            List<LinkedAccountTO> accounts = mergingUser.getLinkedAccounts().stream().map(acct -> {
                LinkedAccountTO linkedAccount =
                        new LinkedAccountTO.Builder(acct.getResource(), acct.getConnObjectKeyValue())
                                .password(acct.getPassword())
                                .suspended(acct.isSuspended())
                                .username(acct.getUsername())
                                .build();
                linkedAccount.getPlainAttrs().addAll(acct.getPlainAttrs());
                return linkedAccount;
            }).collect(Collectors.toList());

            // Move merging user's resources into the target/base user as a linked account
            accounts.addAll(mergingUser.getResources().stream().map(resource -> {
                String connObjectKeyValue = restClient.getConnObjectKeyValue(resource,
                        mergingUser.getType(), mergingUser.getKey());
                return new LinkedAccountTO.Builder(resource, connObjectKeyValue).build();
            }).toList());

            // Move merging user into target/base user as a linked account
            String connObjectKeyValue = restClient.getConnObjectKeyValue(
                    wizardModel.getResource().getKey(),
                    mergingUser.getType(), mergingUser.getKey());
            LinkedAccountTO linkedAccount =
                    new LinkedAccountTO.Builder(wizardModel.getResource().getKey(), connObjectKeyValue)
                            .password(mergingUser.getPassword())
                            .suspended(mergingUser.isSuspended())
                            .username(mergingUser.getUsername())
                            .build();
            linkedAccount.getPlainAttrs().addAll(mergingUser.getPlainAttrs());
            accounts.add(linkedAccount);

            return accounts;
        }

        protected final class LinkedAccountsDataProvider extends DirectoryDataProvider<LinkedAccountTO> {

            private static final long serialVersionUID = -185944053385660794L;

            private final SortableDataProviderComparator<LinkedAccountTO> comparator;

            private LinkedAccountsDataProvider(final int paginatorRows) {
                super(paginatorRows);
                setSort("resource", SortOrder.ASCENDING);
                comparator = new SortableDataProviderComparator<>(this);
            }

            @Override
            public Iterator<LinkedAccountTO> iterator(final long first, final long count) {
                List<LinkedAccountTO> list = previewAccounts();
                list.sort(comparator);
                return list.subList((int) first, (int) first + (int) count).iterator();
            }

            @Override
            public long size() {
                return previewAccounts().size();
            }

            @Override
            public IModel<LinkedAccountTO> model(final LinkedAccountTO object) {
                return new CompoundPropertyModel<>(object);
            }
        }
    }
}
