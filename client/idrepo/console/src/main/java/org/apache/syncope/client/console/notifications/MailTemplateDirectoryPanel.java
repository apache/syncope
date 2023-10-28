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
package org.apache.syncope.client.console.notifications;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.commons.TemplateContent;
import org.apache.syncope.client.console.notifications.MailTemplateDirectoryPanel.MailTemplateProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class MailTemplateDirectoryPanel
        extends DirectoryPanel<MailTemplateTO, MailTemplateTO, MailTemplateProvider, NotificationRestClient> {

    private static final long serialVersionUID = -3789392431954221446L;

    protected final BaseModal<String> utilityModal = new BaseModal<>(Constants.OUTER);

    public MailTemplateDirectoryPanel(
            final String id,
            final NotificationRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        disableCheckBoxes();

        modal.size(Modal.Size.Small);
        modal.addSubmitButton();
        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
        setFooterVisibility(true);

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);
        utilityModal.size(Modal.Size.Large);
        utilityModal.addSubmitButton();

        addNewItemPanelBuilder(new AbstractModalPanelBuilder<MailTemplateTO>(new MailTemplateTO(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public WizardModalPanel<MailTemplateTO> build(
                    final String id, final int index, final AjaxWizard.Mode mode) {

                return new TemplateModal(modal, restClient, new MailTemplateTO(), pageRef);
            }
        }, true);

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.MAIL_TEMPLATE_CREATE);
    }

    @Override
    protected List<IColumn<MailTemplateTO, String>> getColumns() {
        List<IColumn<MailTemplateTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this),
                Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        return columns;
    }

    @Override
    public ActionsPanel<MailTemplateTO> getActions(final IModel<MailTemplateTO> model) {
        final ActionsPanel<MailTemplateTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MailTemplateTO ignore) {
                TemplateContent<MailTemplateFormat> content = new TemplateContent<>(model.getObject().getKey(),
                        MailTemplateFormat.HTML);
                content.setContent(
                        restClient.readTemplateFormat(model.getObject().getKey(), MailTemplateFormat.HTML));

                utilityModal.header(new ResourceModel("mail.template.html", "HTML Content"));
                utilityModal.setContent(new TemplateContentEditorPanel(content, pageRef));
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.HTML, IdRepoEntitlement.MAIL_TEMPLATE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MailTemplateTO ignore) {
                TemplateContent<MailTemplateFormat> content = new TemplateContent<>(model.getObject().getKey(),
                        MailTemplateFormat.TEXT);
                content.setContent(
                        restClient.readTemplateFormat(model.getObject().getKey(), MailTemplateFormat.TEXT));

                utilityModal.header(new ResourceModel("mail.template.text", "TEXT Content"));
                utilityModal.setContent(new TemplateContentEditorPanel(content, pageRef));
                utilityModal.show(true);
                target.add(utilityModal);
            }
        }, ActionLink.ActionType.TEXT, IdRepoEntitlement.MAIL_TEMPLATE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MailTemplateTO ignore) {
                try {
                    restClient.deleteTemplate(model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.MAIL_TEMPLATE_DELETE, true);

        return panel;
    }

    @Override
    protected MailTemplateProvider dataProvider() {
        return new MailTemplateProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_MAIL_TEMPLATE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected final class MailTemplateProvider extends DirectoryDataProvider<MailTemplateTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private final SortableDataProviderComparator<MailTemplateTO> comparator;

        public MailTemplateProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<MailTemplateTO> iterator(final long first, final long count) {
            List<MailTemplateTO> templates = restClient.listTemplates();
            templates.sort(comparator);
            return templates.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.listTemplates().size();
        }

        @Override
        public IModel<MailTemplateTO> model(final MailTemplateTO mailTemplateTO) {
            return new IModel<>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public MailTemplateTO getObject() {
                    return mailTemplateTO;
                }
            };
        }
    }

    private class TemplateContentEditorPanel extends XMLEditorPanel {

        private static final long serialVersionUID = -3528875878627216097L;

        private final TemplateContent<MailTemplateFormat> content;

        TemplateContentEditorPanel(
                final TemplateContent<MailTemplateFormat> content,
                final PageReference pageRef) {

            super(utilityModal, new PropertyModel<>(content, "content"), false, pageRef);
            this.content = content;
        }

        @Override
        public void onSubmit(final AjaxRequestTarget target) {
            if (StringUtils.isBlank(content.getContent())) {
                SyncopeConsoleSession.get().error("No content to save");
            } else {
                try {
                    restClient.updateTemplateFormat(
                            content.getKey(), content.getContent(), content.getFormat());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    modal.show(false);
                    modal.close(target);
                } catch (Exception e) {
                    LOG.error("While updating template for {}", content.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
            }
            ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
        }
    }
}
