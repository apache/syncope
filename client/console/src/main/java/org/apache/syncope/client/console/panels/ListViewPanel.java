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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ListViewPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ListViewPanel.class);

    private final ClearIndicatingAjaxButton addButton;

    private AjaxWizardBuilder<T> newItemPanelBuilder;

    private final WebMarkupContainer container;

    private final Fragment initialFragment;

    private final List<T> listOfItems;

    private NotificationPanel notificationPanel;

    /**
     * Table view of a list of beans.
     *
     * @param id id.
     * @param list list of item.
     * @param reference list item reference class.
     * @param includes Used to sort and restrict the set of bean's fields to be shown.
     * @param actions item actions.
     */
    private ListViewPanel(
            final String id,
            final List<T> list,
            final Class<T> reference,
            final List<String> includes,
            final ActionLinksPanel.Builder<T> actions,
            final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);

        container = new WebMarkupContainer("container");
        add(container.setOutputMarkupId(true));

        initialFragment = new Fragment("content", "table", this);
        container.addOrReplace(initialFragment);

        initialFragment.add(new Label("caption", new ResourceModel("listview.caption", StringUtils.EMPTY)));

        final List<String> toBeIncluded;
        if (includes == null || includes.isEmpty()) {
            toBeIncluded = new ArrayList<String>();
            for (Field field : Arrays.asList(reference.getDeclaredFields())) {
                toBeIncluded.add(field.getName());
            }
        } else {
            toBeIncluded = includes;
        }

        if (toBeIncluded.isEmpty()) {
            LOG.warn("No field has been retrieved from {}", reference.getName());
            listOfItems = new ArrayList<>();
        } else if (list == null || list.isEmpty()) {
            LOG.info("No item to be shown");
            listOfItems = new ArrayList<>();
        } else {
            listOfItems = list;
            if (LOG.isDebugEnabled()) {
                for (String field : toBeIncluded) {
                    LOG.debug("Show field {}", field);
                }
            }
        }

        final ListView<String> names = new ListView<String>("names", toBeIncluded) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Label("name", new ResourceModel(item.getModelObject(), item.getModelObject())));
            }
        };
        initialFragment.add(names);

        final ListView<T> beans = new ListView<T>("beans", listOfItems) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<T> beanItem) {
                final T bean = beanItem.getModelObject();

                final ListView<String> fields = new ListView<String>("fields", toBeIncluded) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(final ListItem<String> fieldItem) {
                        try {
                            LOG.debug("Processing field {}", fieldItem.getModelObject());

                            final Object value = new PropertyDescriptor(fieldItem.getModelObject(), bean.getClass()).
                                    getReadMethod().invoke(bean);

                            LOG.debug("Field value {}", value);

                            fieldItem.add(value == null
                                    ? new Label("field", StringUtils.EMPTY)
                                    : new Label("field", new ResourceModel(value.toString(), value.toString())));

                        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException 
                                | InvocationTargetException e) {
                            LOG.error("Error retrieving value for field {}", fieldItem.getModelObject(), e);
                            fieldItem.add(new Label("field", StringUtils.EMPTY));
                        }
                    }
                };
                beanItem.add(fields);
                beanItem.add(actions.build("actions", bean));
            }
        };
        beans.setReuseItems(true);
        initialFragment.add(beans);

        addButton = new ClearIndicatingAjaxButton("add", pageRef) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                send(ListViewPanel.this, Broadcast.DEPTH, new AjaxWizard.NewItemActionEvent<T>(null, target));
            }
        };

        addButton.setEnabled(false);
        addButton.setVisible(false);

        initialFragment.add(addButton);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            final AjaxRequestTarget target = AjaxWizard.NewItemEvent.class.cast(event.getPayload()).getTarget();

            final T item = ((AjaxWizard.NewItemEvent<T>) event.getPayload()).getItem();

            if (event.getPayload() instanceof AjaxWizard.NewItemActionEvent) {
                final Fragment fragment = new Fragment("content", "wizard", ListViewPanel.this);
                newItemPanelBuilder.setItem(item);

                fragment.add(newItemPanelBuilder.build(
                        ((AjaxWizard.NewItemActionEvent<T>) event.getPayload()).getIndex()));

                container.addOrReplace(fragment);
            } else {
                if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                    if (item != null && !this.listOfItems.contains(item)) {
                        this.listOfItems.add(item);
                    }

                    if (notificationPanel != null) {
                        getSession().info(getString(Constants.OPERATION_SUCCEEDED));
                        notificationPanel.refresh(target);
                    }
                }
                container.addOrReplace(initialFragment);
            }

            target.add(container);
        }
        super.onEvent(event);
    }

    private ListViewPanel<T> addNewItemPanelBuilder(final AjaxWizardBuilder<T> panelBuilder) {
        this.newItemPanelBuilder = panelBuilder;

        if (this.newItemPanelBuilder != null) {
            addButton.setEnabled(true);
            addButton.setVisible(true);
        }

        return this;
    }

    private ListViewPanel<T> addNotificationPanel(final NotificationPanel notificationPanel) {
        this.notificationPanel = notificationPanel;
        return this;
    }

    public static <T extends Serializable> Builder<T> builder(final Class<T> reference, final PageReference pageRef) {
        return new Builder<T>(reference, pageRef);
    }

    /**
     * ListViewPanel builder.
     *
     * @param <T> list item reference type.
     */
    public static final class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 1L;

        private final PageReference pageRef;

        private final Class<T> reference;

        private final List<String> includes = new ArrayList<>();

        private final ActionLinksPanel.Builder<T> actions;

        private List<T> items;

        private AjaxWizardBuilder<T> newItemPanelBuilder;

        private NotificationPanel notificationPanel;

        private Builder(final Class<T> reference, final PageReference pageRef) {
            this.pageRef = pageRef;
            this.reference = reference;
            this.items = null;
            this.actions = ActionLinksPanel.<T>builder(pageRef);
        }

        /**
         * Builds a list view.
         *
         * @param id component id.
         * @return List view.
         */
        public ListViewPanel<T> build(final String id) {
            return new ListViewPanel<T>(id, items, reference, includes, actions, pageRef).
                    addNewItemPanelBuilder(newItemPanelBuilder).addNotificationPanel(notificationPanel);
        }

        /**
         * Sets list of items.
         *
         * @param items list of items.
         * @return current builder object.
         */
        public Builder<T> setItems(final List<T> items) {
            this.items = items;
            return this;
        }

        /**
         * Adds item.
         *
         * @param item item.
         * @return current builder object.
         */
        public Builder<T> addItem(final T item) {
            if (item == null) {
                return this;
            }

            if (this.items == null) {
                this.items = new ArrayList<>();
            }

            this.items.add(item);
            return this;
        }

        /**
         * Gives fields to be shown. It could be used to give an order as well.
         *
         * @param includes field names to be shown.
         * @return current builder object.
         */
        public Builder<T> includes(final String... includes) {
            for (String include : includes) {
                if (include != null && !this.includes.contains(include)) {
                    this.includes.add(include);
                }
            }
            return this;
        }

        /**
         * Add item action (the given order is ignored.
         *
         * @param link action link.
         * @param type action type.
         * @param entitlements entitlements.
         * @return current builder object.
         */
        public Builder<T> addAction(
                final ActionLink<T> link, final ActionLink.ActionType type, final String entitlements) {
            actions.add(link, type, entitlements);
            return this;
        }

        public Builder<T> addNewItemPanelBuilder(final AjaxWizardBuilder<T> panelBuilder) {
            this.newItemPanelBuilder = panelBuilder;
            return this;
        }

        public Builder<T> addNotificationPanel(final NotificationPanel notificationPanel) {
            this.notificationPanel = notificationPanel;
            return this;
        }
    }
}
