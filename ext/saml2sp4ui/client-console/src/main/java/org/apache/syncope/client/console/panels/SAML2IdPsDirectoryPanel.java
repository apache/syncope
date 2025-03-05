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
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SAML2IdPsDirectoryPanel.SAML2IdPsProvider;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wizards.SAML2IdPWizardBuilder;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.UserTemplateWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SAML2SP4UIEntitlement;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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

public class SAML2IdPsDirectoryPanel extends DirectoryPanel<
        SAML2SP4UIIdPTO, SAML2SP4UIIdPTO, SAML2IdPsProvider, SAML2IdPsRestClient> {

    private static final long serialVersionUID = 4792356089584116041L;

    protected static final String PREF_SAML2_IDPS_PAGINATOR_ROWS = "saml2.idps.paginator.rows";

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    protected final BaseModal<String> metadataModal = new BaseModal<>("outer");

    protected final BaseModal<Serializable> templateModal;

    public SAML2IdPsDirectoryPanel(final String id, final SAML2IdPsRestClient restClient, final PageReference pageRef) {
        super(id, new Builder<SAML2SP4UIIdPTO, SAML2SP4UIIdPTO, SAML2IdPsRestClient>(restClient, pageRef) {

            private static final long serialVersionUID = 8517982765290075155L;

            @Override
            protected WizardMgtPanel<SAML2SP4UIIdPTO> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        }.disableCheckBoxes());

        addNewItemPanelBuilder(new SAML2IdPWizardBuilder(
                this, new SAML2SP4UIIdPTO(), implementationRestClient, restClient, pageRef), false);

        modal.addSubmitButton();
        modal.size(Modal.Size.Large);
        modal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });

        addOuterObject(metadataModal);
        setWindowClosedReloadCallback(metadataModal);
        metadataModal.size(Modal.Size.Large);

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

        final ImportMetadata importMetadata = new ImportMetadata("importMetadata", container, pageRef);
        addInnerObject(importMetadata);
        AjaxLink<Void> importMetadataLink = new AjaxLink<>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                importMetadata.toggle(target, true);
            }
        };
        ((MarkupContainer) get("container:content")).addOrReplace(importMetadataLink);
    }

    @Override
    protected SAML2IdPsProvider dataProvider() {
        return new SAML2IdPsProvider(rows);
    }

    @Override
    protected ActionLinksTogglePanel<SAML2SP4UIIdPTO> actionTogglePanel() {
        return new ActionLinksTogglePanel<>(Constants.OUTER, pageRef) {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            public void updateHeader(final AjaxRequestTarget target, final Serializable modelObject) {
                if (modelObject instanceof final SAML2SP4UIIdPTO saml2SP4UIIdPTO) {
                    setHeader(target, StringUtils.abbreviate(
                            saml2SP4UIIdPTO.getName(), HEADER_FIRST_ABBREVIATION));
                } else {
                    super.updateHeader(target, modelObject);
                }
            }
        };
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_SAML2_IDPS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<SAML2SP4UIIdPTO, String>> getColumns() {
        List<IColumn<SAML2SP4UIIdPTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<>(new ResourceModel("name"), "name", "name"));
        columns.add(new PropertyColumn<>(new ResourceModel("entityID"), "entityID", "entityID"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("bindingType"), "bindingType", "bindingType"));
        columns.add(new BooleanPropertyColumn<>(
                new ResourceModel("logoutSupported"), "logoutSupported", "logoutSupported"));

        return columns;
    }

    @Override
    public ActionsPanel<SAML2SP4UIIdPTO> getActions(final IModel<SAML2SP4UIIdPTO> model) {
        final ActionsPanel<SAML2SP4UIIdPTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2SP4UIIdPTO ignore) {
                SAML2SP4UIIdPTO object = restClient.read(model.getObject().getKey());
                metadataModal.header(Model.of(object.getName() + " - Metadata"));
                metadataModal.setContent(new XMLEditorPanel(
                        metadataModal,
                        Model.of(new String(Base64.getMimeDecoder().decode(object.getMetadata()))),
                        true,
                        pageRef));
                metadataModal.show(true);
                target.add(metadataModal);
            }
        }, ActionLink.ActionType.HTML, SAML2SP4UIEntitlement.IDP_READ);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2SP4UIIdPTO ignore) {
                SAML2SP4UIIdPTO object = restClient.read(model.getObject().getKey());
                send(SAML2IdPsDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(object, target));
            }
        }, ActionLink.ActionType.EDIT, SAML2SP4UIEntitlement.IDP_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2SP4UIIdPTO ignore) {
                final SAML2SP4UIIdPTO object = restClient.read(model.getObject().getKey());

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
                        new StringResourceModel("template.title", SAML2IdPsDirectoryPanel.this).getString())));
                templateModal.setContent(builder.build(BaseModal.CONTENT_ID));
                templateModal.show(true);
                target.add(templateModal);

            }
        }, ActionLink.ActionType.TEMPLATE, SAML2SP4UIEntitlement.IDP_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -5467832321897812767L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2SP4UIIdPTO ignore) {
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
        }, ActionLink.ActionType.DELETE, SAML2SP4UIEntitlement.IDP_DELETE, true);

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

    protected final class SAML2IdPsProvider extends DirectoryDataProvider<SAML2SP4UIIdPTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<SAML2SP4UIIdPTO> comparator;

        private SAML2IdPsProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<SAML2SP4UIIdPTO> iterator(final long first, final long count) {
            List<SAML2SP4UIIdPTO> list = restClient.list();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<SAML2SP4UIIdPTO> model(final SAML2SP4UIIdPTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
