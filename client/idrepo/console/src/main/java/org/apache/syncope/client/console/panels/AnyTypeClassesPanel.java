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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
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

public class AnyTypeClassesPanel extends TypesDirectoryPanel<
        AnyTypeClassTO, AnyTypeClassesPanel.AnyTypeClassProvider, AnyTypeClassRestClient> {

    private static final long serialVersionUID = -2356760296223908382L;

    public AnyTypeClassesPanel(final String id, final AnyTypeClassRestClient restClient, final PageReference pageRef) {
        super(id, restClient, false, pageRef);
        disableCheckBoxes();

        this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<AnyTypeClassTO>(new AnyTypeClassTO(), pageRef) {

            private static final long serialVersionUID = -6388405037134399367L;

            @Override
            public WizardModalPanel<AnyTypeClassTO> build(
                    final String id, final int index, final AjaxWizard.Mode mode) {
                final AnyTypeClassTO modelObject = newModelObject();
                return new AnyTypeClassModalPanel(modal, modelObject, pageRef) {

                    private static final long serialVersionUID = -6227956682141146095L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target) {
                        try {
                            if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
                                restClient.create(modelObject);
                            } else {
                                restClient.update(modelObject);
                            }

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            AnyTypeClassesPanel.this.updateResultTable(target);
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While creating or updating AnyTypeClassTO", e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                };
            }

        }, true);

        initResultTable();
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.ANYTYPE_CREATE);
    }

    @Override
    protected AnyTypeClassesPanel.AnyTypeClassProvider dataProvider() {
        return new AnyTypeClassProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_ANYTYPE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<AnyTypeClassTO, String>> getColumns() {
        final List<IColumn<AnyTypeClassTO, String>> columns = new ArrayList<>();

        for (Field field : AnyTypeClassTO.class.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                final String fieldName = field.getName();
                if (field.getType().isArray()
                        || Collection.class.isAssignableFrom(field.getType())
                        || Map.class.isAssignableFrom(field.getType())) {

                    columns.add(new PropertyColumn<>(
                            new ResourceModel(field.getName()), field.getName()));
                } else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
                    columns.add(new BooleanPropertyColumn<>(
                            new ResourceModel(field.getName()), field.getName(), field.getName()));
                } else {
                    columns.add(new PropertyColumn<>(
                            new ResourceModel(field.getName()), field.getName(), field.getName()) {

                        private static final long serialVersionUID = -6902459669035442212L;

                        @Override
                        public String getCssClass() {
                            String css = super.getCssClass();
                            if (Constants.KEY_FIELD_NAME.equals(fieldName)) {
                                css = StringUtils.isBlank(css)
                                        ? "col-xs-1"
                                        : css + " col-xs-1";
                            }
                            return css;
                        }
                    });
                }
            }
        }

        return columns;
    }

    @Override
    public ActionsPanel<AnyTypeClassTO> getActions(final IModel<AnyTypeClassTO> model) {
        final ActionsPanel<AnyTypeClassTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AnyTypeClassTO ignore) {
                send(AnyTypeClassesPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.ANYTYPECLASS_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AnyTypeClassTO ignore) {
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
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.ANYTYPECLASS_DELETE, true);

        return panel;
    }

    protected final class AnyTypeClassProvider extends DirectoryDataProvider<AnyTypeClassTO> {

        private static final long serialVersionUID = -185944053385660794L;

        protected final SortableDataProviderComparator<AnyTypeClassTO> comparator;

        protected AnyTypeClassProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AnyTypeClassTO> iterator(final long first, final long count) {
            List<AnyTypeClassTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<AnyTypeClassTO> model(final AnyTypeClassTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
