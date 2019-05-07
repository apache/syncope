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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.OIDCProvidersDirectoryPanel.OIDCProvidersProvider;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.OIDCProviderRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.OIDCProviderWizardBuilder;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.UserTemplateWizardBuilder;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.OIDCClientEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class OIDCProvidersDirectoryPanel extends DirectoryPanel<
        OIDCProviderTO, OIDCProviderTO, OIDCProvidersProvider, OIDCProviderRestClient> {

    private static final long serialVersionUID = -1356497878858616714L;

    private static final String PREF_OIDC_PROVIDERS_PAGINATOR_ROWS = "oidc.providers.paginator.rows";

    private final BaseModal<Serializable> templateModal;

    public OIDCProvidersDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, new Builder<OIDCProviderTO, OIDCProviderTO, OIDCProviderRestClient>(new OIDCProviderRestClient(),
                pageRef) {

            private static final long serialVersionUID = -5542535388772406165L;

            @Override
            protected WizardMgtPanel<OIDCProviderTO> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        }.disableCheckBoxes());

        this.addNewItemPanelBuilder(new OIDCProviderWizardBuilder(this, new OIDCProviderTO(), pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, OIDCClientEntitlement.OP_CREATE);

        modal.size(Modal.Size.Large);

        actionTogglePanel = new ActionLinksTogglePanel<OIDCProviderTO>("outer", pageRef) {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            public void toggleWithContent(
                    final AjaxRequestTarget target,
                    final ActionsPanel<OIDCProviderTO> actionsPanel,
                    final OIDCProviderTO modelObject) {

                super.toggleWithContent(target, actionsPanel, modelObject);
                setHeader(target, StringUtils.abbreviate(modelObject.getName(), 25));
                this.toggle(target, true);
            }

        };
        addOuterObject(actionTogglePanel);

        templateModal = new BaseModal<Serializable>("outer") {

            private static final long serialVersionUID = 5787433530654262016L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        templateModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
//                target.add(content);
                templateModal.show(false);
            }
        });
        templateModal.size(Modal.Size.Large);
        addOuterObject(templateModal);

        initResultTable();

    }

    @Override
    protected OIDCProvidersProvider dataProvider() {
        return new OIDCProvidersProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_OIDC_PROVIDERS_PAGINATOR_ROWS;

    }

    @Override
    protected List<IColumn<OIDCProviderTO, String>> getColumns() {
        List<IColumn<OIDCProviderTO, String>> columns = new ArrayList<>();
        columns.add(new KeyPropertyColumn<OIDCProviderTO>(new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<OIDCProviderTO, String>(new ResourceModel("name"), "name", "name"));
        columns.add(new PropertyColumn<OIDCProviderTO, String>(new ResourceModel("issuer"), "issuer", "issuer"));
        columns.add(new PropertyColumn<OIDCProviderTO, String>(new ResourceModel("clientID"), "clientID", "clientID"));
        return columns;

    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.<ActionLink.ActionType>emptyList();

    }

    @Override
    public ActionsPanel<OIDCProviderTO> getActions(final IModel<OIDCProviderTO> model) {
        final ActionsPanel<OIDCProviderTO> panel = super.getActions(model);

        panel.add(new ActionLink<OIDCProviderTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final OIDCProviderTO ignore) {
                OIDCProviderTO object = restClient.read(model.getObject().getKey());
                send(OIDCProvidersDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(object, target));
                modal.header(Model.of(StringUtils.capitalize(("Edit " + object.getName()))));
            }
        }, ActionLink.ActionType.EDIT, OIDCClientEntitlement.OP_UPDATE);

        panel.add(new ActionLink<OIDCProviderTO>() {

            private static final long serialVersionUID = 8557679125857348178L;

            @Override
            public void onClick(final AjaxRequestTarget target, final OIDCProviderTO ignore) {
                final OIDCProviderTO object = restClient.read(model.getObject().getKey());

                UserTemplateWizardBuilder builder = new UserTemplateWizardBuilder(
                        object.getUserTemplate(),
                        new AnyTypeRestClient().read(AnyTypeKind.USER.name()).getClasses(),
                        new UserFormLayoutInfo(),
                        pageRef) {

                    private static final long serialVersionUID = -7978723352517770634L;

                    @Override
                    protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
                        object.setUserTemplate(modelObject.getInnerObject());
                        restClient.update(object);

                        return modelObject;
                    }
                };
                templateModal.header(Model.of(StringUtils.capitalize(
                        new StringResourceModel("template.title", OIDCProvidersDirectoryPanel.this).getString())));
                templateModal.setContent(builder.build(BaseModal.CONTENT_ID));
                templateModal.show(true);
                target.add(templateModal);

            }
        }, ActionLink.ActionType.TEMPLATE, OIDCClientEntitlement.OP_UPDATE);

        panel.add(new ActionLink<OIDCProviderTO>() {

            private static final long serialVersionUID = -5467832321897812767L;

            @Override
            public void onClick(final AjaxRequestTarget target, final OIDCProviderTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, OIDCClientEntitlement.OP_DELETE, true);
        return panel;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            AjaxWizard.NewItemEvent<?> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
            WizardModalPanel<?> modalPanel = newItemEvent.getModalPanel();

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent && modalPanel != null) {
                final IModel<Serializable> model = new CompoundPropertyModel<>(modalPanel.getItem());
                templateModal.setFormModel(model);
                templateModal.header(newItemEvent.getResourceModel());
                newItemEvent.getTarget().add(templateModal.setContent(modalPanel));
                templateModal.show(true);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                templateModal.close(newItemEvent.getTarget());
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                templateModal.close(newItemEvent.getTarget());
            }
        }
    }

    protected final class OIDCProvidersProvider extends DirectoryDataProvider<OIDCProviderTO> {

        private static final long serialVersionUID = -2865055116864423761L;

        private final SortableDataProviderComparator<OIDCProviderTO> comparator;

        public OIDCProvidersProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<OIDCProviderTO> iterator(final long first, final long count) {
            List<OIDCProviderTO> list = restClient.list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<OIDCProviderTO> model(final OIDCProviderTO object) {
            return new CompoundPropertyModel<>(object);
        }

    }
}
