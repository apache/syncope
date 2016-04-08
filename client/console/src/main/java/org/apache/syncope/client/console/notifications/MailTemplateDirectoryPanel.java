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

import static org.apache.wicket.Component.ENABLE;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.notifications.MailTemplateDirectoryPanel.MailTemplateProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class MailTemplateDirectoryPanel
        extends DirectoryPanel<MailTemplateTO, MailTemplateTO, MailTemplateProvider, NotificationRestClient> {

    private static final long serialVersionUID = -3789392431954221446L;

    public MailTemplateDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();

        modal.size(Modal.Size.Small);
        modal.addSumbitButton();
        setFooterVisibility(true);

        utilityModal.size(Modal.Size.Large);
        utilityModal.addSumbitButton();

        addNewItemPanelBuilder(new AbstractModalPanelBuilder<MailTemplateTO>(new MailTemplateTO(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public ModalPanel<MailTemplateTO> build(final String id, final int index, final AjaxWizard.Mode mode) {
                return new MailTemplateModal(modal, new MailTemplateTO(), pageReference);
            }
        }, true);
        restClient = new NotificationRestClient();

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.MAIL_TEMPLATE_CREATE);
    }

    @Override
    protected List<IColumn<MailTemplateTO, String>> getColumns() {

        final List<IColumn<MailTemplateTO, String>> columns = new ArrayList<IColumn<MailTemplateTO, String>>();
        columns.add(new PropertyColumn<MailTemplateTO, String>(
                new StringResourceModel("key", this, null), "key", "key"));

        columns.add(new ActionColumn<MailTemplateTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<MailTemplateTO> getActions(
                    final String componentId, final IModel<MailTemplateTO> model) {

                final ActionLinksPanel.Builder<MailTemplateTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<MailTemplateTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final MailTemplateTO ignore) {
                        MailTemplateContentModal.MailTemplateContentTO content
                                = new MailTemplateContentModal.MailTemplateContentTO(
                                        model.getObject().getKey(), MailTemplateFormat.HTML);
                        content.setContent(
                                restClient.readTemplateFormat(model.getObject().getKey(), MailTemplateFormat.HTML));

                        utilityModal.header(new ResourceModel("mail.template.html", "HTML Content"));
                        utilityModal.setContent(new MailTemplateContentModal(utilityModal, content, pageRef));
                        utilityModal.show(true);
                        target.add(utilityModal);
                    }
                }, ActionLink.ActionType.HTML_EDIT, StandardEntitlement.MAIL_TEMPLATE_UPDATE);

                panel.add(new ActionLink<MailTemplateTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final MailTemplateTO ignore) {
                        MailTemplateContentModal.MailTemplateContentTO content
                                = new MailTemplateContentModal.MailTemplateContentTO(
                                        model.getObject().getKey(), MailTemplateFormat.TEXT);
                        content.setContent(
                                restClient.readTemplateFormat(model.getObject().getKey(), MailTemplateFormat.TEXT));
                        
                        utilityModal.setFormModel(content);
                        utilityModal.header(new ResourceModel("mail.template.text", "TEXT Content"));
                        utilityModal.setContent(new MailTemplateContentModal(utilityModal, content, pageRef));
                        utilityModal.show(true);
                        target.add(utilityModal);
                    }
                }, ActionLink.ActionType.TEXT_EDIT, StandardEntitlement.MAIL_TEMPLATE_UPDATE);

                panel.add(new ActionLink<MailTemplateTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final MailTemplateTO ignore) {
                        try {
                            restClient.deleteTemplate(model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                            error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                        }
                        SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.MAIL_TEMPLATE_DELETE);

                return panel.build(componentId);
            }
        });
        return columns;
    }

    @Override
    protected MailTemplateProvider dataProvider() {
        return new MailTemplateProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_MAIL_TEMPLATE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    public class MailTemplateProvider extends DirectoryDataProvider<MailTemplateTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private final SortableDataProviderComparator<MailTemplateTO> comparator;

        public MailTemplateProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<MailTemplateTO>(this);
        }

        @Override
        public Iterator<MailTemplateTO> iterator(final long first, final long count) {
            final List<MailTemplateTO> list = restClient.getAllAvailableTemplates();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getAllAvailableTemplates().size();
        }

        @Override
        public IModel<MailTemplateTO> model(final MailTemplateTO mailTemplateTO) {
            return new AbstractReadOnlyModel<MailTemplateTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public MailTemplateTO getObject() {
                    return mailTemplateTO;
                }
            };
        }
    }
}
