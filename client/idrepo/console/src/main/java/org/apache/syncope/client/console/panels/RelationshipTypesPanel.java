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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.RelationshipTypesPanel.RelationshipTypeProvider;
import org.apache.syncope.client.console.rest.RelationshipTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class RelationshipTypesPanel extends TypesDirectoryPanel<
        RelationshipTypeTO, RelationshipTypeProvider, RelationshipTypeRestClient> {

    private static final long serialVersionUID = -3731778000138547357L;

    protected final BaseModal<Serializable> typeExtensionsModal = new BaseModal<>(Constants.OUTER);

    public RelationshipTypesPanel(
            final String id,
            final RelationshipTypeRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, false, pageRef);
        disableCheckBoxes();

        typeExtensionsModal.size(Modal.Size.Large);
        typeExtensionsModal.addSubmitButton();
        setWindowClosedReloadCallback(typeExtensionsModal);
        addOuterObject(typeExtensionsModal);

        this.addNewItemPanelBuilder(
                new AbstractModalPanelBuilder<RelationshipTypeTO>(new RelationshipTypeTO(), pageRef) {

            private static final long serialVersionUID = -6388405037134399367L;

            @Override
            public WizardModalPanel<RelationshipTypeTO> build(
                    final String id, final int index, final AjaxWizard.Mode mode) {

                final RelationshipTypeTO modelObject = newModelObject();
                return new RelationshipTypeModalPanel(modal, modelObject, pageRef) {

                    private static final long serialVersionUID = -6227956682141146094L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target) {
                        try {
                            if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
                                restClient.create(modelObject);
                            } else {
                                restClient.update(modelObject);
                            }
                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            RelationshipTypesPanel.this.updateResultTable(target);
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While creating or updating {}", modelObject, e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                };
            }
        }, true);

        initResultTable();
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.RELATIONSHIPTYPE_CREATE);
    }

    @Override
    protected RelationshipTypeProvider dataProvider() {
        return new RelationshipTypeProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_RELATIONSHIPTYPE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<RelationshipTypeTO, String>> getColumns() {
        List<IColumn<RelationshipTypeTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new ResourceModel(Constants.DESCRIPTION_FIELD_NAME),
                Constants.DESCRIPTION_FIELD_NAME,
                Constants.DESCRIPTION_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new ResourceModel("leftEndAnyType"), "leftEndAnyType", "leftEndAnyType"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("rightEndAnyType"), "rightEndAnyType", "rightEndAnyType"));

        return columns;
    }

    @Override
    public ActionsPanel<RelationshipTypeTO> getActions(final IModel<RelationshipTypeTO> model) {
        final ActionsPanel<RelationshipTypeTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RelationshipTypeTO ignore) {
                send(RelationshipTypesPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.RELATIONSHIPTYPE_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 6242834621660352855L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RelationshipTypeTO ignore) {
                target.add(typeExtensionsModal.setContent(new TypeExtensionDirectoryPanel(
                        typeExtensionsModal, model.getObject(), pageRef) {

                    private static final long serialVersionUID = -2603789363348077538L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target) {
                        try {
                            RelationshipTypesPanel.this.restClient.update(model.getObject());

                            baseModal.show(false);
                            baseModal.close(target);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        } catch (Exception e) {
                            LOG.error("RelationshipType update failure", e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));
                typeExtensionsModal.header(new StringResourceModel("typeExtensions", model));
                typeExtensionsModal.show(true);
            }
        }, ActionType.TYPE_EXTENSIONS, IdRepoEntitlement.RELATIONSHIPTYPE_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RelationshipTypeTO ignore) {
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
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.RELATIONSHIPTYPE_DELETE, true);

        return panel;
    }

    protected final class RelationshipTypeProvider extends DirectoryDataProvider<RelationshipTypeTO> {

        private static final long serialVersionUID = -185944053385660794L;

        protected final SortableDataProviderComparator<RelationshipTypeTO> comparator;

        protected RelationshipTypeProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<RelationshipTypeTO> iterator(final long first, final long count) {
            final List<RelationshipTypeTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<RelationshipTypeTO> model(final RelationshipTypeTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
