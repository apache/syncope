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
package org.apache.syncope.client.console.reports;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
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
import org.apache.syncope.client.console.notifications.TemplateContentModal;
import org.apache.syncope.client.console.notifications.TemplateModal;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ReportTemplateTO;
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
import org.apache.syncope.client.console.panels.WizardModalPanel;
import org.apache.syncope.client.console.reports.ReportTemplateDirectoryPanel.ReportTemplateProvider;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;

public class ReportTemplateDirectoryPanel
        extends DirectoryPanel<ReportTemplateTO, ReportTemplateTO, ReportTemplateProvider, ReportRestClient> {

    private static final long serialVersionUID = -3789392431954221446L;

    protected final BaseModal<Serializable> utilityModal = new BaseModal<>("outer");

    public ReportTemplateDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);
        utilityModal.size(Modal.Size.Large);
        utilityModal.addSubmitButton();

        restClient = new ReportRestClient();
        addNewItemPanelBuilder(new AbstractModalPanelBuilder<ReportTemplateTO>(new ReportTemplateTO(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public WizardModalPanel<ReportTemplateTO> build(
                    final String id, final int index, final AjaxWizard.Mode mode) {
                return new TemplateModal<ReportTemplateTO, ReportTemplateFormat>(
                        modal, restClient, new ReportTemplateTO(), pageReference);
            }
        }, true);

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.MAIL_TEMPLATE_CREATE);
    }

    @Override
    protected List<IColumn<ReportTemplateTO, String>> getColumns() {
        List<IColumn<ReportTemplateTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<ReportTemplateTO, String>(
                new StringResourceModel("key", this, null), "key", "key"));

        columns.add(new ActionColumn<ReportTemplateTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<ReportTemplateTO> getActions(
                    final String componentId, final IModel<ReportTemplateTO> model) {

                final ActionLinksPanel.Builder<ReportTemplateTO> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<ReportTemplateTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ReportTemplateTO ignore) {
                        TemplateContentModal.TemplateContent<ReportTemplateFormat> content
                                = new TemplateContentModal.TemplateContent<ReportTemplateFormat>(
                                        model.getObject().getKey(), ReportTemplateFormat.FO);
                        content.setContent(
                                restClient.readTemplateFormat(model.getObject().getKey(), ReportTemplateFormat.FO));

                        utilityModal.header(new ResourceModel("report.template.fo", "FO Content"));
                        utilityModal.setContent(new TemplateContentModal<ReportTemplateTO, ReportTemplateFormat>(
                                utilityModal, restClient, content, pageRef));
                        utilityModal.show(true);
                        target.add(utilityModal);
                    }
                }, ActionLink.ActionType.FO_EDIT, StandardEntitlement.MAIL_TEMPLATE_UPDATE);

                panel.add(new ActionLink<ReportTemplateTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ReportTemplateTO ignore) {
                        TemplateContentModal.TemplateContent<ReportTemplateFormat> content
                                = new TemplateContentModal.TemplateContent<ReportTemplateFormat>(
                                        model.getObject().getKey(), ReportTemplateFormat.HTML);
                        content.setContent(
                                restClient.readTemplateFormat(model.getObject().getKey(), ReportTemplateFormat.HTML));

                        utilityModal.header(new ResourceModel("report.template.html", "HTML Content"));
                        utilityModal.setContent(new TemplateContentModal<ReportTemplateTO, ReportTemplateFormat>(
                                utilityModal, restClient, content, pageRef));
                        utilityModal.show(true);
                        target.add(utilityModal);
                    }
                }, ActionLink.ActionType.HTML_EDIT, StandardEntitlement.MAIL_TEMPLATE_UPDATE);

                panel.add(new ActionLink<ReportTemplateTO>() {

                    private static final long serialVersionUID = -7978723352517770645L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ReportTemplateTO ignore) {
                        TemplateContentModal.TemplateContent<ReportTemplateFormat> content
                                = new TemplateContentModal.TemplateContent<ReportTemplateFormat>(
                                        model.getObject().getKey(), ReportTemplateFormat.CSV);
                        content.setContent(
                                restClient.readTemplateFormat(model.getObject().getKey(), ReportTemplateFormat.CSV));

                        utilityModal.setFormModel(content);
                        utilityModal.header(new ResourceModel("report.template.text", "TEXT Content"));
                        utilityModal.setContent(new TemplateContentModal<ReportTemplateTO, ReportTemplateFormat>(
                                utilityModal, restClient, content, pageRef));
                        utilityModal.show(true);
                        target.add(utilityModal);
                    }
                }, ActionLink.ActionType.TEXT_EDIT, StandardEntitlement.MAIL_TEMPLATE_UPDATE);

                panel.add(new ActionLink<ReportTemplateTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ReportTemplateTO ignore) {
                        try {
                            restClient.deleteTemplate(model.getObject().getKey());
                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (SyncopeClientException e) {
                            LOG.error("While deleting object {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                                    getName() : e.getMessage());
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
    protected ReportTemplateProvider dataProvider() {
        return new ReportTemplateProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_MAIL_TEMPLATE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    public class ReportTemplateProvider extends DirectoryDataProvider<ReportTemplateTO> {

        private static final long serialVersionUID = -276043813563988590L;

        private final SortableDataProviderComparator<ReportTemplateTO> comparator;

        public ReportTemplateProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ReportTemplateTO> iterator(final long first, final long count) {
            final List<ReportTemplateTO> list = restClient.listTemplates();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.listTemplates().size();
        }

        @Override
        public IModel<ReportTemplateTO> model(final ReportTemplateTO reportTemplateTO) {
            return new AbstractReadOnlyModel<ReportTemplateTO>() {

                private static final long serialVersionUID = 774694801558497248L;

                @Override
                public ReportTemplateTO getObject() {
                    return reportTemplateTO;
                }
            };
        }
    }
}
