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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SecurityQuestionsPanel.SecurityQuestionsProvider;
import org.apache.syncope.client.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.SecurityQuestionTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class SecurityQuestionsPanel extends DirectoryPanel<
        SecurityQuestionTO, SecurityQuestionTO, SecurityQuestionsProvider, SecurityQuestionRestClient> {

    private static final long serialVersionUID = 3323019773236588850L;

    public SecurityQuestionsPanel(
            final String id,
            final SecurityQuestionRestClient restClient,
            final PageReference pageRef) {

        super(id, new Builder<SecurityQuestionTO, SecurityQuestionTO, SecurityQuestionRestClient>(restClient, pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<SecurityQuestionTO> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        }.disableCheckBoxes());

        modal.addSubmitButton();
        modal.size(Modal.Size.Large);
        modal.setWindowClosedCallback(target -> {
            modal.show(false);
            target.add(container);
        });
        setFooterVisibility(true);

        this.addNewItemPanelBuilder(
                new AbstractModalPanelBuilder<SecurityQuestionTO>(new SecurityQuestionTO(), pageRef) {

            private static final long serialVersionUID = -6388405037134399367L;

            @Override
            public WizardModalPanel<SecurityQuestionTO> build(
                    final String id, final int index, final AjaxWizard.Mode mode) {

                return new SecurityQuestionsModalPanel(modal, newModelObject(), pageRef);
            }
        }, true);

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.SECURITY_QUESTION_CREATE);
    }

    @Override
    protected SecurityQuestionsProvider dataProvider() {
        return new SecurityQuestionsProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_SECURITY_QUESTIONS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<SecurityQuestionTO, String>> getColumns() {
        List<IColumn<SecurityQuestionTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("content", this), "content", "content"));

        return columns;
    }

    @Override
    public ActionsPanel<SecurityQuestionTO> getActions(final IModel<SecurityQuestionTO> model) {
        ActionsPanel<SecurityQuestionTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SecurityQuestionTO ignore) {
                send(SecurityQuestionsPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.SECURITY_QUESTION_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SecurityQuestionTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.SECURITY_QUESTION_DELETE, true);

        return panel;
    }

    protected final class SecurityQuestionsProvider extends DirectoryDataProvider<SecurityQuestionTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<SecurityQuestionTO> comparator;

        private SecurityQuestionsProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<SecurityQuestionTO> iterator(final long first, final long count) {
            List<SecurityQuestionTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<SecurityQuestionTO> model(final SecurityQuestionTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
