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
import org.apache.syncope.client.console.wizards.SAML2IdPWizardBuilder;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SAML2IdPsDirectoryPanel.SAML2IdPsProvider;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.SAML2IdPsRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.XMLEditorPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.UserTemplateWizardBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.SAML2SPEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.crypt.Base64;

public class SAML2IdPsDirectoryPanel extends DirectoryPanel<
        SAML2IdPTO, SAML2IdPTO, SAML2IdPsProvider, SAML2IdPsRestClient> {

    private static final long serialVersionUID = 4792356089584116041L;

    private static final String PREF_SAML2_IDPS_PAGINATOR_ROWS = "saml2.idps.paginator.rows";

    private final BaseModal<String> metadataModal = new BaseModal<>("outer");

    private final BaseModal<Serializable> templateModal;

    public SAML2IdPsDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, new Builder<SAML2IdPTO, SAML2IdPTO, SAML2IdPsRestClient>(new SAML2IdPsRestClient(), pageRef) {

            private static final long serialVersionUID = 8517982765290075155L;

            @Override
            protected WizardMgtPanel<SAML2IdPTO> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        }.disableCheckBoxes());
        this.addNewItemPanelBuilder(new SAML2IdPWizardBuilder(this, new SAML2IdPTO(), pageRef), false);

        modal.addSubmitButton();
        modal.size(Modal.Size.Large);
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);
                modal.show(false);
            }
        });

        // need to override to change the menu header
        actionTogglePanel = new ActionLinksTogglePanel<SAML2IdPTO>("outer", pageRef) {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            public void toggleWithContent(
                    final AjaxRequestTarget target,
                    final ActionsPanel<SAML2IdPTO> actionsPanel,
                    final SAML2IdPTO modelObject) {

                super.toggleWithContent(target, actionsPanel, modelObject);
                setHeader(target, StringUtils.abbreviate(modelObject.getName(), 25));
                this.toggle(target, true);
            }

        };
        addOuterObject(actionTogglePanel);

        addOuterObject(metadataModal);
        setWindowClosedReloadCallback(metadataModal);
        metadataModal.size(Modal.Size.Large);

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

        final ImportMetadata importMetadata = new ImportMetadata("importMetadata", container, pageRef);
        addInnerObject(importMetadata);
        AjaxLink<Void> importMetadataLink = new AjaxLink<Void>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                importMetadata.toggle(target, true);
            }
        };

        ((WebMarkupContainer) get("container:content")).addOrReplace(importMetadataLink);
    }

    @Override
    protected SAML2IdPsProvider dataProvider() {
        return new SAML2IdPsProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_SAML2_IDPS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    @Override
    protected List<IColumn<SAML2IdPTO, String>> getColumns() {
        List<IColumn<SAML2IdPTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<>(new ResourceModel("name"), "name", "name"));
        columns.add(new PropertyColumn<>(new ResourceModel("entityID"), "entityID", "entityID"));
        columns.add(new BooleanPropertyColumn<>(
                new ResourceModel("useDeflateEncoding"), "useDeflateEncoding", "useDeflateEncoding"));
        columns.add(new BooleanPropertyColumn<>(
            new ResourceModel("supportUnsolicited"), "supportUnsolicited", "supportUnsolicited"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("bindingType"), "bindingType", "bindingType"));
        columns.add(new BooleanPropertyColumn<>(
                new ResourceModel("logoutSupported"), "logoutSupported", "logoutSupported"));

        return columns;
    }

    @Override
    public ActionsPanel<SAML2IdPTO> getActions(final IModel<SAML2IdPTO> model) {
        final ActionsPanel<SAML2IdPTO> panel = super.getActions(model);

        panel.add(new ActionLink<SAML2IdPTO>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2IdPTO ignore) {
                SAML2IdPTO object = restClient.read(model.getObject().getKey());
                metadataModal.header(Model.of(object.getName() + " - Metadata"));
                metadataModal.setContent(new XMLEditorPanel(
                        metadataModal,
                        Model.of(new String(Base64.decodeBase64(object.getMetadata()))),
                        true,
                        pageRef));
                metadataModal.show(true);
                target.add(metadataModal);
            }
        }, ActionLink.ActionType.HTML, SAML2SPEntitlement.IDP_READ);
        panel.add(new ActionLink<SAML2IdPTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2IdPTO ignore) {
                SAML2IdPTO object = restClient.read(model.getObject().getKey());
                send(SAML2IdPsDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(object, target));
            }
        }, ActionLink.ActionType.EDIT, SAML2SPEntitlement.IDP_UPDATE);
        panel.add(new ActionLink<SAML2IdPTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2IdPTO ignore) {
                final SAML2IdPTO object = restClient.read(model.getObject().getKey());

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
                        new StringResourceModel("template.title", SAML2IdPsDirectoryPanel.this).getString())));
                templateModal.setContent(builder.build(BaseModal.CONTENT_ID));
                templateModal.show(true);
                target.add(templateModal);

            }
        }, ActionLink.ActionType.TEMPLATE, SAML2SPEntitlement.IDP_UPDATE);
        panel.add(new ActionLink<SAML2IdPTO>() {

            private static final long serialVersionUID = -5467832321897812767L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SAML2IdPTO ignore) {
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
        }, ActionLink.ActionType.DELETE, SAML2SPEntitlement.IDP_DELETE, true);

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

    protected final class SAML2IdPsProvider extends DirectoryDataProvider<SAML2IdPTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<SAML2IdPTO> comparator;

        private SAML2IdPsProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("name", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<SAML2IdPTO> iterator(final long first, final long count) {
            List<SAML2IdPTO> list = restClient.list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<SAML2IdPTO> model(final SAML2IdPTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
