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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wizards.any.ResultPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.NotificationPanel;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractWizardMgtPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.ModalPanelBuilder;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public abstract class WizardMgtPanel<T extends Serializable> extends AbstractWizardMgtPanel<T> {

    private static final long serialVersionUID = -4152438633429194882L;

    private boolean readOnly = false;

    protected final String actualId;

    private final WebMarkupContainer container;

    protected final Fragment initialFragment;

    protected final boolean wizardInModal;

    private boolean containerAutoRefresh = true;

    protected PageReference pageRef;

    protected final AjaxLink<?> addAjaxLink;

    protected Label utilityIcon;

    protected AjaxLink<?> utilityAjaxLink;

    protected ModalPanelBuilder<T> newItemPanelBuilder;

    protected NotificationPanel notificationPanel;

    protected boolean footerVisibility = false;

    protected boolean showResultPanel = false;

    private final List<Component> outerObjects = new ArrayList<>();

    protected final BaseModal<T> modal = new BaseModal<>(Constants.OUTER) {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(footerVisibility);
        }
    };

    protected WizardMgtPanel(final String id) {
        this(id, false);
    }

    protected WizardMgtPanel(final String id, final boolean wizardInModal) {
        super(id);
        setOutputMarkupId(true);

        this.actualId = wizardInModal ? BaseModal.CONTENT_ID : WIZARD_ID;
        this.wizardInModal = wizardInModal;

        outerObjects.add(modal);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);
        add(container);

        initialFragment = new Fragment("content", "default", this);
        container.addOrReplace(initialFragment);

        addAjaxLink = new AjaxLink<T>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(WizardMgtPanel.this, Broadcast.BREADTH,
                        new ActionLinksTogglePanel.ActionLinkToggleCloseEventPayload(target));
                send(WizardMgtPanel.this, Broadcast.EXACT,
                        new AjaxWizard.NewItemActionEvent<>(null, target));
            }
        };

        addAjaxLink.setEnabled(false);
        addAjaxLink.setVisible(false);
        initialFragment.addOrReplace(addAjaxLink);

        utilityAjaxLink = new AjaxLink<T>("utility") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(WizardMgtPanel.this, Broadcast.EXACT, new ExitEvent(target));
            }
        };

        utilityAjaxLink.setEnabled(false);
        utilityAjaxLink.setVisible(false);
        initialFragment.addOrReplace(utilityAjaxLink);

        utilityIcon = new Label("utilityIcon");
        utilityAjaxLink.add(utilityIcon);

        add(new ListView<>("outerObjectsRepeater", outerObjects) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<Component> item) {
                item.add(item.getModelObject());
            }
        });
    }

    public String getActualId() {
        return actualId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            // default behaviour: change it catching the event if needed
            modal.close(target);
        } else if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            AjaxWizard.NewItemEvent<T> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
            Optional<AjaxRequestTarget> target = newItemEvent.getTarget();
            T item = newItemEvent.getItem();

            boolean modalPanelAvailable = newItemEvent.getModalPanel() != null || newItemPanelBuilder != null;

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent && modalPanelAvailable) {
                WizardModalPanel<?> modalPanel;
                if (newItemEvent.getModalPanel() == null && newItemPanelBuilder != null) {
                    newItemPanelBuilder.setItem(item);

                    modalPanel = newItemPanelBuilder.build(
                            actualId,
                            ((AjaxWizard.NewItemActionEvent<T>) newItemEvent).getIndex(),
                            item != null
                                    ? isReadOnly()
                                            ? AjaxWizard.Mode.READONLY
                                            : AjaxWizard.Mode.EDIT
                                    : AjaxWizard.Mode.CREATE);
                } else {
                    modalPanel = newItemEvent.getModalPanel();
                }

                if (wizardInModal) {
                    modal.setFormModel(item);

                    target.ifPresent(t -> t.add(modal.setContent(modalPanel)));

                    modal.header(Optional.ofNullable(newItemEvent.getTitleModel()).
                        orElseGet(() -> new StringResourceModel(
                            String.format("any.%s", newItemEvent.getEventDescription()),
                            this,
                            Model.of(modalPanel.getItem()))));
                    modal.show(true);
                } else {
                    Fragment fragment = new Fragment("content", "wizard", WizardMgtPanel.this);

                    fragment.add(new Label(
                            "title",
                            Optional.ofNullable(newItemEvent.getTitleModel()).
                                orElseGet(() -> Model.of(StringUtils.EMPTY))));

                    fragment.add(Component.class.cast(modalPanel));
                    container.addOrReplace(fragment);
                }

                target.ifPresent(this::customActionCallback);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                if (wizardInModal) {
                    target.ifPresent(modal::close);
                } else {
                    container.addOrReplace(initialFragment);
                }

                target.ifPresent(this::customActionOnCancelCallback);
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                target.ifPresent(t -> ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(t));

                if (wizardInModal && showResultPanel) {
                    modal.setContent(new ResultPanel<>(
                            item,
                            AjaxWizard.NewItemFinishEvent.class.cast(newItemEvent).getResult()) {

                        private static final long serialVersionUID = -2630573849050255233L;

                        @Override
                        protected void closeAction(final AjaxRequestTarget target) {
                            modal.close(target);
                        }

                        @Override
                        protected Panel customResultBody(
                                final String panelId,
                                final T item,
                                final Serializable result) {

                            return WizardMgtPanel.this.customResultBody(panelId, item, result);
                        }
                    });
                    target.ifPresent(t -> t.add(modal.getForm()));
                } else if (wizardInModal) {
                    target.ifPresent(modal::close);
                } else {
                    container.addOrReplace(initialFragment);
                }

                target.ifPresent(this::customActionOnFinishCallback);
            }

            if (containerAutoRefresh) {
                target.ifPresent(t -> t.add(container));
            }
        }
        super.onEvent(event);
    }

    protected final WizardMgtPanel<T> disableContainerAutoRefresh() {
        containerAutoRefresh = false;
        return this;
    }

    /*
     * Override this method to specify your custom result body panel.
     */
    protected Panel customResultBody(final String panelId, final T item, final Serializable result) {
        return new Panel(panelId) {

            private static final long serialVersionUID = 5538299138211283825L;

        };
    }

    /**
     * Show utility button sending ExitEvent payload by default.
     *
     * @return the current instance.
     */
    protected final WizardMgtPanel<T> enableUtilityButton() {
        utilityAjaxLink.setEnabled(true);
        utilityAjaxLink.setVisible(true);
        return this;
    }

    /**
     * Add object inside the main container.
     *
     * @param childs components to be added.
     * @return the current panel instance.
     */
    public MarkupContainer addInnerObject(final Component... childs) {
        return initialFragment.add(childs);
    }

    /**
     * Add or replace object inside the main container.
     *
     * @param childs components to be added.
     * @return the current panel instance.
     */
    public MarkupContainer addOrReplaceInnerObject(final Component... childs) {
        return initialFragment.addOrReplace(childs);
    }

    /**
     * Add object outside the main container.
     * Use this method just to be not influenced by specific inner object css'.
     * Be sure to provide {@code outer} as id.
     *
     * @param childs components to be added.
     * @return the current panel instance.
     */
    public final WizardMgtPanel<T> addOuterObject(final Component... childs) {
        outerObjects.addAll(List.of(childs));
        return this;
    }

    public <B extends ModalPanelBuilder<T>> WizardMgtPanel<T> setPageRef(final PageReference pageRef) {
        this.pageRef = pageRef;
        return this;
    }

    public <B extends ModalPanelBuilder<T>> WizardMgtPanel<T> setShowResultPanel(final boolean showResultPanel) {
        this.showResultPanel = showResultPanel;
        return this;
    }

    protected <B extends ModalPanelBuilder<T>> WizardMgtPanel<T> addNewItemPanelBuilder(
            final B panelBuilder, final boolean newItemDefaultButtonEnabled) {

        this.newItemPanelBuilder = panelBuilder;

        if (this.newItemPanelBuilder != null) {
            addAjaxLink.setEnabled(newItemDefaultButtonEnabled);
            addAjaxLink.setVisible(newItemDefaultButtonEnabled);
            this.newItemPanelBuilder.setEventSink(WizardMgtPanel.this);
        }

        return this;
    }

    protected WizardMgtPanel<T> addNotificationPanel(final NotificationPanel notificationPanel) {
        this.notificationPanel = notificationPanel;
        return this;
    }

    public WizardMgtPanel<T> setFooterVisibility(final boolean footerVisibility) {
        this.footerVisibility = footerVisibility;
        return this;
    }

    /**
     * Set window close callback for the given modal.
     *
     * @param modal target modal.
     */
    protected void setWindowClosedReloadCallback(final BaseModal<?> modal) {
        modal.setWindowClosedCallback(target -> modal.show(false));
    }

    /**
     * Custom action to perform on create/edit action callback.
     *
     * @param target Ajax request target.
     */
    protected void customActionCallback(final AjaxRequestTarget target) {
    }

    /**
     * Custom action to perform on close callback on finish event.
     *
     * @param target Ajax request target.
     */
    protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
    }

    /**
     * Custom action to perform on close callback on cancel event.
     *
     * @param target Ajax request target.
     */
    protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
    }

    /**
     * PanelInWizard abstract builder.
     *
     * @param <T> list item reference type.
     */
    public abstract static class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 1908836274665387084L;

        protected final PageReference pageRef;

        private ModalPanelBuilder<T> newItemPanelBuilder;

        private boolean newItemDefaultButtonEnabled = true;

        private NotificationPanel notificationPanel;

        private boolean showResultPage = false;

        private boolean wizardInModal = false;

        protected Builder(final PageReference pageRef) {
            this.pageRef = pageRef;
        }

        protected abstract WizardMgtPanel<T> newInstance(String id, boolean wizardInModal);

        /**
         * Builds a list view.
         *
         * @param id component id.
         * @return List view.
         */
        public WizardMgtPanel<T> build(final String id) {
            return newInstance(id, wizardInModal).
                    setPageRef(this.pageRef).
                    setShowResultPanel(this.showResultPage).
                    addNewItemPanelBuilder(this.newItemPanelBuilder, this.newItemDefaultButtonEnabled).
                    addNotificationPanel(this.notificationPanel);
        }

        public void setShowResultPage(final boolean showResultPage) {
            this.showResultPage = showResultPage;
        }

        public Builder<T> addNewItemPanelBuilder(final ModalPanelBuilder<T> panelBuilder) {
            this.newItemPanelBuilder = panelBuilder;
            return this;
        }

        /**
         * Adds new item panel builder.
         *
         * @param panelBuilder new item panel builder.
         * @param newItemDefaultButtonEnabled enable default button to adda new item.
         * @return the current builder.
         */
        public Builder<T> addNewItemPanelBuilder(
                final ModalPanelBuilder<T> panelBuilder, final boolean newItemDefaultButtonEnabled) {

            this.newItemDefaultButtonEnabled = newItemDefaultButtonEnabled;
            return addNewItemPanelBuilder(panelBuilder);
        }

        /**
         * Adds new item panel builder and enables default button to adda new item.
         *
         * @param notificationPanel new item panel builder.
         * @return the current builder.
         */
        public Builder<T> addNotificationPanel(final NotificationPanel notificationPanel) {
            this.notificationPanel = notificationPanel;
            return this;
        }

        /**
         * Specifies to open an edit item wizard into a new modal page.
         *
         * @param wizardInModal TRUE to request to open wizard in a new modal.
         * @return the current builder.
         */
        public Builder<T> setWizardInModal(final boolean wizardInModal) {
            this.wizardInModal = wizardInModal;
            return this;
        }
    }

    public static class ExitEvent {

        private final AjaxRequestTarget target;

        public ExitEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final boolean readOnly) {
        this.readOnly = readOnly;
    }
}
