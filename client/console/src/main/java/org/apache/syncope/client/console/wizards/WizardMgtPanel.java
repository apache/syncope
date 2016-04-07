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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.NotificationPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wizards.any.ResultPage;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSource;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public abstract class WizardMgtPanel<T extends Serializable> extends Panel implements IEventSource {

    private static final long serialVersionUID = -4152438633429194882L;

    private final WebMarkupContainer container;

    private final Fragment initialFragment;

    private final boolean wizardInModal;

    protected PageReference pageRef;

    protected final AjaxLink<?> addAjaxLink;

    protected AbstractModalPanelBuilder<T> newItemPanelBuilder;

    protected NotificationPanel notificationPanel;

    protected boolean footerVisibility = false;

    protected boolean showResultPage = false;

    protected final BaseModal<T> modal = new BaseModal<T>("modal") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(footerVisibility);
        }

    };

    protected final BaseModal<T> altDefaultModal = new BaseModal<>("alternativeDefaultModal");

    protected final BaseModal<T> displayAttributeModal = new BaseModal<>("displayAttributeModal");

    protected final BaseModal<Serializable> utilityModal = new BaseModal<>("utilityModal");

    protected WizardMgtPanel(final String id) {
        this(id, false);
    }

    protected WizardMgtPanel(final String id, final boolean wizardInModal) {
        super(id);
        setOutputMarkupId(true);

        this.wizardInModal = wizardInModal;

        super.add(modal);
        super.add(altDefaultModal);
        super.add(displayAttributeModal);
        super.add(utilityModal);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupPlaceholderTag(true).setOutputMarkupId(true);
        super.add(container);

        initialFragment = new Fragment("content", "default", this);
        container.addOrReplace(initialFragment);

        addAjaxLink = new AjaxLink<T>("add") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(WizardMgtPanel.this, Broadcast.EXACT, new AjaxWizard.NewItemActionEvent<T>(null, target));
            }
        };

        addAjaxLink.setEnabled(false);
        addAjaxLink.setVisible(false);
        initialFragment.addOrReplace(addAjaxLink);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            final AjaxWizard.NewItemEvent<T> newItemEvent = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
            final AjaxRequestTarget target = newItemEvent.getTarget();
            final T item = newItemEvent.getItem();

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent && newItemPanelBuilder != null) {
                newItemPanelBuilder.setItem(item);

                final ModalPanel<T> modalPanel = newItemPanelBuilder.build(
                        ((AjaxWizard.NewItemActionEvent<T>) newItemEvent).getIndex(),
                        item != null ? AjaxWizard.Mode.EDIT : AjaxWizard.Mode.CREATE);

                if (wizardInModal) {
                    final IModel<T> model = new CompoundPropertyModel<>(item);
                    modal.setFormModel(model);

                    target.add(modal.setContent(modalPanel));

                    modal.header(new StringResourceModel(
                            String.format("any.%s", newItemEvent.getEventDescription()),
                            this,
                            new Model<>(modalPanel.getItem())));
                    modal.show(true);
                } else {
                    final Fragment fragment = new Fragment("content", "wizard", WizardMgtPanel.this);
                    fragment.add(Component.class.cast(modalPanel));
                    container.addOrReplace(fragment);
                }
            } else if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent) {
                if (wizardInModal) {
                    modal.show(false);
                    modal.close(target);
                } else {
                    container.addOrReplace(initialFragment);
                }
            } else if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                info(getString(Constants.OPERATION_SUCCEEDED));
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);

                if (wizardInModal && showResultPage) {
                    modal.setContent(new ResultPage<T>(
                            item,
                            AjaxWizard.NewItemFinishEvent.class.cast(newItemEvent).getResult()) {

                        private static final long serialVersionUID = -2630573849050255233L;

                        @Override
                        protected void closeAction(final AjaxRequestTarget target) {
                            modal.show(false);
                            modal.close(target);
                        }

                        @Override
                        protected Panel customResultBody(
                                final String id, final T item, final Serializable result) {
                            return WizardMgtPanel.this.customResultBody(id, item, result);
                        }
                    });
                    target.add(modal.getForm());
                } else if (wizardInModal) {
                    modal.show(false);
                    modal.close(target);
                } else {
                    container.addOrReplace(initialFragment);
                }
            }

            target.add(container);
        }
        super.onEvent(event);
    }

    /*
     * Override this method to specify your custom result body panel.
     */
    protected Panel customResultBody(final String panelId, final T item, final Serializable result) {
        return new Panel(panelId) {

            private static final long serialVersionUID = 5538299138211283825L;

        };
    }

    @Override
    public Component add(final Behavior... behaviors) {
        return super.add(behaviors);
    }

    public final MarkupContainer addInnerObject(final Component childs) {
        return initialFragment.add(childs);
    }

    public <B extends AbstractModalPanelBuilder<T>> WizardMgtPanel<T> setPageRef(final PageReference pageRef) {
        this.pageRef = pageRef;
        return this;
    }

    public <B extends AbstractModalPanelBuilder<T>> WizardMgtPanel<T> setShowResultPage(final boolean showResultPage) {
        this.showResultPage = showResultPage;
        return this;
    }

    protected <B extends AbstractModalPanelBuilder<T>> WizardMgtPanel<T> addNewItemPanelBuilder(
            final B panelBuilder, final boolean newItemDefaultButtonEnabled) {
        this.newItemPanelBuilder = panelBuilder;

        if (this.newItemPanelBuilder != null) {
            addAjaxLink.setEnabled(newItemDefaultButtonEnabled);
            addAjaxLink.setVisible(newItemDefaultButtonEnabled);
        }

        return this;
    }

    protected WizardMgtPanel<T> addNotificationPanel(final NotificationPanel notificationPanel) {
        this.notificationPanel = SyncopeConsoleSession.get().getNotificationPanel();
        return this;
    }

    public WizardMgtPanel<T> setFooterVisibility(final boolean footerVisibility) {
        this.footerVisibility = footerVisibility;
        return this;
    }

    /**
     * PanelInWizard abstract builder.
     *
     * @param <T> list item reference type.
     */
    public abstract static class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 1L;

        protected final PageReference pageRef;

        private AbstractModalPanelBuilder<T> newItemPanelBuilder;

        private boolean newItemDefaultButtonEnabled = true;

        private NotificationPanel notificationPanel;

        private boolean showResultPage = false;

        protected Builder(final PageReference pageRef) {
            this.pageRef = pageRef;
        }

        protected abstract WizardMgtPanel<T> newInstance(final String id);

        /**
         * Builds a list view.
         *
         * @param id component id.
         * @return List view.
         */
        public WizardMgtPanel<T> build(final String id) {
            return newInstance(id).
                    setPageRef(this.pageRef).
                    setShowResultPage(this.showResultPage).
                    addNewItemPanelBuilder(this.newItemPanelBuilder, this.newItemDefaultButtonEnabled).
                    addNotificationPanel(this.notificationPanel);
        }

        public void setShowResultPage(final boolean showResultPage) {
            this.showResultPage = showResultPage;
        }

        public Builder<T> addNewItemPanelBuilder(final AbstractModalPanelBuilder<T> panelBuilder) {
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
                final AbstractModalPanelBuilder<T> panelBuilder, final boolean newItemDefaultButtonEnabled) {
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
    }
}
