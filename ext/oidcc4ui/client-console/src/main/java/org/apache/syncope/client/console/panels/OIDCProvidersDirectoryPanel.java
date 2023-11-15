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
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.OIDCProvidersDirectoryPanel.OIDCProvidersProvider;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.OIDCProviderRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.OIDCProviderWizardBuilder;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.UserTemplateWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.OIDCC4UIEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class OIDCProvidersDirectoryPanel extends DirectoryPanel<
        OIDCC4UIProviderTO, OIDCC4UIProviderTO, OIDCProvidersProvider, OIDCProviderRestClient> {

    private static final long serialVersionUID = -1356497878858616714L;

    protected static final String PREF_OIDC_PROVIDERS_PAGINATOR_ROWS = "oidc.providers.paginator.rows";

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    private final BaseModal<Serializable> templateModal;

    public OIDCProvidersDirectoryPanel(
            final String id,
            final OIDCProviderRestClient restClient,
            final PageReference pageRef) {

        super(id, new Builder<OIDCC4UIProviderTO, OIDCC4UIProviderTO, OIDCProviderRestClient>(restClient, pageRef) {

            private static final long serialVersionUID = -5542535388772406165L;

            @Override
            protected WizardMgtPanel<OIDCC4UIProviderTO> newInstance(final String id,
                    final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        }.disableCheckBoxes());

        this.addNewItemPanelBuilder(new OIDCProviderWizardBuilder(
                this, new OIDCC4UIProviderTO(), implementationRestClient, restClient, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, OIDCC4UIEntitlement.OP_CREATE);

        modal.size(Modal.Size.Large);

        templateModal = new BaseModal<>("outer") {

            private static final long serialVersionUID = 5787433530654262016L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        templateModal.setWindowClosedCallback(target -> templateModal.show(false));
        templateModal.size(Modal.Size.Large);
        addOuterObject(templateModal);

        initResultTable();
    }

    @Override
    protected OIDCProvidersProvider dataProvider() {
        return new OIDCProvidersProvider(rows);
    }

    @Override
    protected ActionLinksTogglePanel<OIDCC4UIProviderTO> actionTogglePanel() {
        return new ActionLinksTogglePanel<>(Constants.OUTER, pageRef) {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            public void updateHeader(final AjaxRequestTarget target, final Serializable object) {
                if (object instanceof OIDCC4UIProviderTO provider) {
                    setHeader(target, StringUtils.abbreviate(provider.getName(), HEADER_FIRST_ABBREVIATION));
                } else {
                    super.updateHeader(target, object);
                }
            }
        };
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_OIDC_PROVIDERS_PAGINATOR_ROWS;

    }

    @Override
    protected List<IColumn<OIDCC4UIProviderTO, String>> getColumns() {
        List<IColumn<OIDCC4UIProviderTO, String>> columns = new ArrayList<>();
        columns.add(new KeyPropertyColumn<>(new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<>(new ResourceModel("name"), "name", "name"));
        columns.add(new PropertyColumn<>(new ResourceModel("issuer"), "issuer", "issuer"));
        columns.add(new PropertyColumn<>(new ResourceModel("clientID"), "clientID", "clientID"));
        return columns;

    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    public ActionsPanel<OIDCC4UIProviderTO> getActions(final IModel<OIDCC4UIProviderTO> model) {
        final ActionsPanel<OIDCC4UIProviderTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final OIDCC4UIProviderTO ignore) {
                OIDCC4UIProviderTO object = restClient.read(model.getObject().getKey());
                send(OIDCProvidersDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(object, target));
                modal.header(Model.of(StringUtils.capitalize(("Edit " + object.getName()))));
            }
        }, ActionLink.ActionType.EDIT, OIDCC4UIEntitlement.OP_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 8557679125857348178L;

            @Override
            public void onClick(final AjaxRequestTarget target, final OIDCC4UIProviderTO ignore) {
                OIDCC4UIProviderTO object = restClient.read(model.getObject().getKey());

                UserTemplateWizardBuilder builder = new UserTemplateWizardBuilder(
                        object.getUserTemplate(),
                        anyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses(),
                        new UserFormLayoutInfo(),
                        userRestClient,
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
        }, ActionLink.ActionType.TEMPLATE, OIDCC4UIEntitlement.OP_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5467832321897812767L;

            @Override
            public void onClick(final AjaxRequestTarget target, final OIDCC4UIProviderTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, OIDCC4UIEntitlement.OP_DELETE, true);
        return panel;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            AjaxWizard.NewItemEvent<?> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
            WizardModalPanel<?> modalPanel = newItemEvent.getModalPanel();

            if (newItemEvent instanceof AjaxWizard.NewItemActionEvent && modalPanel != null) {
                IModel<Serializable> model = new CompoundPropertyModel<>(modalPanel.getItem());
                templateModal.setFormModel(model);
                templateModal.header(newItemEvent.getTitleModel());
                newItemEvent.getTarget().ifPresent(target -> target.add(templateModal.setContent(modalPanel)));
                templateModal.show(true);
            } else if (newItemEvent instanceof AjaxWizard.NewItemCancelEvent) {
                newItemEvent.getTarget().ifPresent(templateModal::close);
            } else if (newItemEvent instanceof AjaxWizard.NewItemFinishEvent) {
                newItemEvent.getTarget().ifPresent(templateModal::close);
            }
        }
    }

    protected final class OIDCProvidersProvider extends DirectoryDataProvider<OIDCC4UIProviderTO> {

        private static final long serialVersionUID = -2865055116864423761L;

        private final SortableDataProviderComparator<OIDCC4UIProviderTO> comparator;

        public OIDCProvidersProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<OIDCC4UIProviderTO> iterator(final long first, final long count) {
            List<OIDCC4UIProviderTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<OIDCC4UIProviderTO> model(final OIDCC4UIProviderTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
