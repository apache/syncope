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

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.CheckGroupSelector;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ListViewPanel<T extends Serializable> extends WizardMgtPanel<T> {

    private static final long serialVersionUID = -7982691107029848579L;

    private static final Logger LOG = LoggerFactory.getLogger(ListViewPanel.class);

    public enum CheckAvailability {

        /**
         * No checks.
         */
        NONE,
        /**
         * Enabled checks including check group selector.
         */
        AVAILABLE,
        /**
         * Disabled checks.
         */
        DISABLED

    }

    private final CheckGroupSelector groupSelector;

    private final Model<CheckAvailability> check;

    private final ListView<T> beans;

    private final List<T> listOfItems;

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
            final CheckAvailability check,
            final boolean reuseItem,
            final boolean wizardInModal,
            final IModel<? extends Collection<T>> model) {
        super(id, wizardInModal);
        setOutputMarkupId(true);

        this.check = Model.of(check);

        addInnerObject(new Label("caption", new ResourceModel("listview.caption", StringUtils.EMPTY)));

        final CheckGroup<T> checkGroup = new CheckGroup<>("group", model);
        checkGroup.setOutputMarkupId(true);
        checkGroup.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = -151291731388673682L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // ignore
            }
        });
        addInnerObject(checkGroup);

        groupSelector = new CheckGroupSelector("groupselector", checkGroup);
        addInnerObject(groupSelector.setOutputMarkupId(true)
                .setOutputMarkupPlaceholderTag(true)
                .setVisible(this.check.getObject() == CheckAvailability.AVAILABLE)
                .setEnabled(this.check.getObject() == CheckAvailability.AVAILABLE));

        final List<String> toBeIncluded;
        if (includes == null || includes.isEmpty()) {
            toBeIncluded = new ArrayList<>();
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

        addInnerObject(header(toBeIncluded));

        beans = new ListView<T>("beans", listOfItems) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<T> beanItem) {
                beanItem.add(new Check<>("check", beanItem.getModel(), checkGroup).setOutputMarkupId(true)
                        .setOutputMarkupPlaceholderTag(true)
                        .setVisible(ListViewPanel.this.check.getObject() == CheckAvailability.AVAILABLE
                                || ListViewPanel.this.check.getObject() == CheckAvailability.DISABLED)
                        .setEnabled(ListViewPanel.this.check.getObject() == CheckAvailability.AVAILABLE));

                final T bean = beanItem.getModelObject();

                final ListView<String> fields = new ListView<String>("fields", toBeIncluded) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void populateItem(final ListItem<String> fieldItem) {
                        fieldItem.add(getValueComponent(fieldItem.getModelObject(), bean));
                    }
                };
                beanItem.add(fields);
                beanItem.add(actions.build("actions", bean));
            }
        };
        beans.setOutputMarkupId(true);
        beans.setReuseItems(reuseItem);
        beans.setRenderBodyOnly(true);
        checkGroup.add(beans);
    }

    private ListView<String> header(final List<String> labels) {
        return new ListView<String>("names", labels) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Label("name", new ResourceModel(item.getModelObject(), item.getModelObject())));
            }
        };
    }

    public void setCheckAvailability(final CheckAvailability check) {
        // used to perform selectable enabling check condition
        this.check.setObject(check);

        final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);

        // reload group selector
        target.add(groupSelector.setVisible(check == CheckAvailability.AVAILABLE));
        // reload the list view panel
        target.add(ListViewPanel.this);
    }

    protected abstract Component getValueComponent(final String key, final T bean);

    /**
     * ListViewPanel builder.
     *
     * @param <T> list item reference type.
     */
    public static class Builder<T extends Serializable> extends WizardMgtPanel.Builder<T> {

        private static final long serialVersionUID = -3643771352897992172L;

        private IModel<? extends Collection<T>> model = Model.of(Collections.<T>emptyList());

        private final List<String> includes = new ArrayList<>();

        private final ActionLinksPanel.Builder<T> actions;

        private List<T> items;

        private CheckAvailability check = CheckAvailability.NONE;

        private boolean reuseItem = true;

        private final Class<T> reference;

        public Builder(final Class<T> reference, final PageReference pageRef) {
            super(pageRef);
            this.reference = reference;
            this.items = null;
            this.actions = ActionLinksPanel.<T>builder();
        }

        public Builder<T> setModel(final IModel<? extends Collection<T>> model) {
            this.model = model;
            return this;
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

        public Builder<T> withChecks(final CheckAvailability check) {
            this.check = check;
            return this;
        }

        public void setReuseItem(final boolean reuseItem) {
            this.reuseItem = reuseItem;
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

        /**
         * Overridable method to generate field value rendering component.
         *
         * @param key field key.
         * @param bean source bean.
         * @return field rendering component.
         */
        protected Component getValueComponent(final String key, final T bean) {
            LOG.debug("Processing field {}", key);

            Object value;
            try {
                value = PropertyResolver.getPropertyGetter(key, bean).invoke(bean);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LOG.error("Error retrieving value for field {}", key, e);
                value = StringUtils.EMPTY;
            }

            LOG.debug("Field value {}", value);

            return value == null
                    ? new Label("field", StringUtils.EMPTY)
                    : new Label("field", new ResourceModel(value.toString(), value.toString()));
        }

        protected T getActualItem(final T item, final List<T> list) {
            return item == null
                    ? null
                    : IteratorUtils.find(list.iterator(), new Predicate<T>() {

                        @Override
                        public boolean evaluate(final T object) {
                            return item.equals(object);
                        }
                    });
        }

        @Override
        protected WizardMgtPanel<T> newInstance(final String id, final boolean wizardInModal) {
            return new ListViewPanel<T>(
                    id, items, reference, includes, actions, check, reuseItem, wizardInModal, model) {

                private static final long serialVersionUID = 1L;

                @Override
                protected Component getValueComponent(final String key, final T bean) {
                    return Builder.this.getValueComponent(key, bean);
                }

                @Override
                protected T getActualItem(final T item, final List<T> list) {
                    return Builder.this.getActualItem(item, list);
                }

                @Override
                protected void customActionCallback(final AjaxRequestTarget target) {
                    Builder.this.customActionCallback(target);
                }

                @Override
                protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
                    Builder.this.customActionOnFinishCallback(target);
                }

                @Override
                protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
                    Builder.this.customActionOnCancelCallback(target);
                }
            };
        }

        protected void customActionCallback(final AjaxRequestTarget target) {
        }

        protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
        }

        protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof AjaxWizard.NewItemEvent) {
            final T item = ((AjaxWizard.NewItemEvent<T>) event.getPayload()).getItem();
            final AjaxRequestTarget target = ((AjaxWizard.NewItemEvent<T>) event.getPayload()).getTarget();

            if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                final T old = getActualItem(item, ListViewPanel.this.listOfItems);
                int indexOf = ListViewPanel.this.listOfItems.size();
                if (old != null) {
                    indexOf = ListViewPanel.this.listOfItems.indexOf(old);
                    ListViewPanel.this.listOfItems.remove(old);
                }
                ListViewPanel.this.listOfItems.add(indexOf, item);
            }

            target.add(ListViewPanel.this);
            super.onEvent(event);
        } else if (event.getPayload() instanceof ListViewPanel.ListViewReload) {
            final ListViewPanel.ListViewReload<?> payload = (ListViewPanel.ListViewReload<?>) event.getPayload();
            if (payload.getItems() != null) {
                ListViewPanel.this.listOfItems.clear();
                try {
                    ListViewPanel.this.listOfItems.addAll((List<T>) payload.getItems());
                } catch (RuntimeException re) {
                    LOG.warn("Error reloading items", re);
                }
            }
            payload.getTarget().add(ListViewPanel.this);
        } else {
            super.onEvent(event);
        }
    }

    protected abstract T getActualItem(final T item, final List<T> list);

    public static class ListViewReload<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = 1509151005816590312L;

        private final AjaxRequestTarget target;

        private final List<T> items;

        public ListViewReload(final AjaxRequestTarget target) {
            this.target = target;
            this.items = null;
        }

        public ListViewReload(final List<T> items, final AjaxRequestTarget target) {
            this.target = target;
            this.items = items;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public List<T> getItems() {
            return items;
        }
    }
}
