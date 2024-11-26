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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
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

    private ActionLinksTogglePanel<T> togglePanel;

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
            final ActionsPanel<T> actions,
            final CheckAvailability check,
            final boolean reuseItem,
            final boolean wizardInModal,
            final boolean captionVisible,
            final IModel<? extends Collection<T>> model) {

        super(id, wizardInModal);
        setOutputMarkupId(true);

        togglePanel = getTogglePanel();

        this.check = Model.of(check);

        WebMarkupContainer captionContainer = new WebMarkupContainer("captionContainer");
        captionContainer.setOutputMarkupPlaceholderTag(true);
        captionContainer.setVisible(captionVisible);
        addInnerObject(captionContainer);

        Label caption = new Label("caption", new ResourceModel("listview.caption", StringUtils.EMPTY));
        captionContainer.add(caption);

        CheckGroup<T> checkGroup = new CheckGroup<>("group", model);
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
        addInnerObject(groupSelector.setOutputMarkupId(true).
                setOutputMarkupPlaceholderTag(true).
                setVisible(this.check.getObject() == CheckAvailability.AVAILABLE).
                setEnabled(this.check.getObject() == CheckAvailability.AVAILABLE));

        final List<String> toBeIncluded;
        if (includes == null || includes.isEmpty()) {
            toBeIncluded = new ArrayList<>();
            for (Field field : reference.getDeclaredFields()) {
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
            LOG.debug("Show fields {}", toBeIncluded);
        }

        addInnerObject(header(toBeIncluded));

        beans = new ListView<>("beans", listOfItems) {

            private static final long serialVersionUID = -9112553137618363167L;

            @Override
            protected void populateItem(final ListItem<T> beanItem) {
                beanItem.add(new Check<>("check", beanItem.getModel(), checkGroup).setOutputMarkupId(true).
                        setOutputMarkupPlaceholderTag(true).
                        setVisible(ListViewPanel.this.check.getObject() == CheckAvailability.AVAILABLE
                                || ListViewPanel.this.check.getObject() == CheckAvailability.DISABLED).
                        setEnabled(ListViewPanel.this.check.getObject() == CheckAvailability.AVAILABLE));

                final T bean = beanItem.getModelObject();

                final ListView<String> fields = new ListView<>("fields", toBeIncluded) {

                    private static final long serialVersionUID = -9112553137618363167L;

                    @Override
                    protected void populateItem(final ListItem<String> fieldItem) {
                        fieldItem.add(getValueComponent(fieldItem.getModelObject(), bean));
                        if (togglePanel != null) {
                            fieldItem.add(new AttributeModifier("style", "cursor: pointer;"));
                            fieldItem.add(new AjaxEventBehavior(Constants.ON_CLICK) {

                                private static final long serialVersionUID = -9027652037484739586L;

                                @Override
                                protected String findIndicatorId() {
                                    return StringUtils.EMPTY;
                                }

                                @Override
                                protected void onEvent(final AjaxRequestTarget target) {
                                    togglePanel.toggleWithContent(
                                            target,
                                            actions.cloneWithLabels("actions", new Model<>(bean)),
                                            bean);
                                }
                            });
                        }
                    }
                };

                beanItem.add(fields);

                if (togglePanel == null) {
                    beanItem.add(actions.clone("actions", new Model<>(bean)));
                } else {
                    beanItem.add(new ActionsPanel<>("actions", new Model<>(bean)).setVisible(false).setEnabled(false));
                }
            }
        };
        beans.setOutputMarkupId(true);
        beans.setReuseItems(reuseItem);
        beans.setRenderBodyOnly(true);
        checkGroup.add(beans);
    }

    protected ListView<String> header(final List<String> labels) {
        return new ListView<>("names", labels) {

            private static final long serialVersionUID = -9112553137618363167L;

            @Override
            protected void populateItem(final ListItem<String> item) {
                item.add(new Label(Constants.NAME_FIELD_NAME,
                        new ResourceModel(item.getModelObject(), item.getModelObject())));
            }
        };
    }

    /**
     * Use this to refresh the ListView with updated items (e.g. from callback methods)
     *
     * @param elements new items
     */
    public void refreshList(final List<T> elements) {
        beans.setList(elements);
    }

    public void setCheckAvailability(final CheckAvailability check) {
        // used to perform selectable enabling check condition
        this.check.setObject(check);

        RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(t -> {
            // reload group selector
            t.add(groupSelector.setVisible(check == CheckAvailability.AVAILABLE), groupSelector.getMarkupId());
            // reload the list view panel
            t.add(ListViewPanel.this, getMarkupId());
        });
    }

    protected abstract Component getValueComponent(String key, T bean);

    /**
     * ListViewPanel builder.
     *
     * @param <T> list item reference type.
     */
    public static class Builder<T extends Serializable> extends WizardMgtPanel.Builder<T> {

        private static final long serialVersionUID = -3643771352897992172L;

        private IModel<? extends Collection<T>> model = Model.of(List.of());

        private final List<String> includes = new ArrayList<>();

        private final ActionsPanel<T> actions;

        private List<T> items;

        private CheckAvailability check = CheckAvailability.NONE;

        private boolean reuseItem = true;

        private boolean captionVisible = true;

        private final Class<T> reference;

        public Builder(final Class<T> reference, final PageReference pageRef) {
            super(pageRef);
            this.reference = reference;
            this.items = null;
            this.actions = new ActionsPanel<>("actions", null);
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

        public Builder<T> setReuseItem(final boolean reuseItem) {
            this.reuseItem = reuseItem;
            return this;
        }

        public Builder<T> setCaptionVisible(final boolean captionVisible) {
            this.captionVisible = captionVisible;
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
            return addAction(link, type, entitlements, false);
        }

        /**
         * Add item action (the given order is ignored.
         *
         * @param link action link.
         * @param type action type.
         * @param entitlements entitlements.
         * @param onConfirm specify TRUE to ask for confirmation.
         * @return current builder object.
         */
        public Builder<T> addAction(
                final ActionLink<T> link,
                final ActionLink.ActionType type,
                final String entitlements,
                final boolean onConfirm) {
            actions.add(link, type, entitlements, onConfirm).hideLabel();
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

            return Optional.ofNullable(value)
                    .map(o -> new Label("field", new ResourceModel(o.toString(), o.toString())))
                    .orElseGet(() -> new Label("field", StringUtils.EMPTY));
        }

        protected T getActualItem(final T item, final List<T> list) {
            return item == null
                    ? null
                    : list.stream().filter(item::equals).findAny().orElse(null);
        }

        @Override
        protected WizardMgtPanel<T> newInstance(final String id, final boolean wizardInModal) {
            return new ListViewPanel<>(
                    id, items, reference, includes, actions, check, reuseItem, wizardInModal, captionVisible, model) {

                private static final long serialVersionUID = -1715389337530657988L;

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

                @Override
                protected ActionLinksTogglePanel<T> getTogglePanel() {
                    return Builder.this.getTogglePanel();
                }
            };
        }

        protected ActionLinksTogglePanel<T> getTogglePanel() {
            return null;
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
            final Optional<AjaxRequestTarget> target = ((AjaxWizard.NewItemEvent<T>) event.getPayload()).getTarget();

            if (event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {
                final T old = getActualItem(item, ListViewPanel.this.listOfItems);
                int indexOf = ListViewPanel.this.listOfItems.size();
                if (old != null) {
                    indexOf = ListViewPanel.this.listOfItems.indexOf(old);
                    ListViewPanel.this.listOfItems.remove(old);
                }
                ListViewPanel.this.listOfItems.add(indexOf, item);
            }

            target.ifPresent(t -> t.add(ListViewPanel.this));
            super.onEvent(event);
        } else if (event.getPayload() instanceof final ListViewPanel.ListViewReload<?> payload) {
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

    protected abstract T getActualItem(T item, List<T> list);

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

    protected ActionLinksTogglePanel<T> getTogglePanel() {
        return null;
    }
}
