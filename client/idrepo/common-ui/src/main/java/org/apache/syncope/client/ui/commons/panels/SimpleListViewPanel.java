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
package org.apache.syncope.client.ui.commons.panels;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleListViewPanel<T extends Serializable> extends Panel {

    private static final long serialVersionUID = -7982691107029848579L;

    private static final Logger LOG = LoggerFactory.getLogger(SimpleListViewPanel.class);

    private final ListView<T> beans;

    private final List<T> listOfItems;

    /**
     * Table view of a list of beans.
     *
     * @param id        id.
     * @param list      list of item.
     * @param reference list item reference class.
     * @param includes  Used to sort and restrict the set of bean's fields to be shown.
     */
    private SimpleListViewPanel(
            final String id,
            final List<T> list,
            final Class<T> reference,
            final List<String> includes) {

        super(id);
        setOutputMarkupId(true);

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

        add(header(toBeIncluded));

        beans = new ListView<>("beans", listOfItems) {

            private static final long serialVersionUID = -9112553137618363167L;

            @Override
            protected void populateItem(final ListItem<T> beanItem) {
                final T bean = beanItem.getModelObject();

                final ListView<String> fields = new ListView<>("fields", toBeIncluded) {

                    private static final long serialVersionUID = -9112553137618363167L;

                    @Override
                    protected void populateItem(final ListItem<String> fieldItem) {
                        fieldItem.add(getValueComponent(fieldItem.getModelObject(), bean));
                    }
                };

                beanItem.add(fields);
            }
        };
        add(beans.setOutputMarkupId(true).setRenderBodyOnly(true));
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
     * SimpleListViewPanel builder.
     *
     * @param <T> list item reference type.
     */
    public static class Builder<T extends Serializable> implements Serializable {

        private static final long serialVersionUID = -3643771352897992172L;

        private final List<String> includes = new ArrayList<>();

        private List<T> items;

        private final Class<T> reference;

        private final PageReference pageReference;

        public Builder(final Class<T> reference, final PageReference pageRef) {
            this.pageReference = pageRef;
            this.reference = reference;
            this.items = null;
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
         * Overridable method to generate field value rendering component.
         *
         * @param key  field key.
         * @param bean source bean.
         * @return field rendering component.
         */
        protected Component getValueComponent(final String key, final T bean) {
            LOG.debug("Processing field {}", key);

            Object value;
            try {
                value = includes.contains(key)
                        ? PropertyResolver.getPropertyGetter(key, bean).invoke(bean)
                        : StringUtils.EMPTY;
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

        public SimpleListViewPanel<T> build(final String id) {
            return new SimpleListViewPanel<T>(id, items, reference, includes) {

                @Override
                protected Component getValueComponent(final String key, final T bean) {
                    return Builder.this.getValueComponent(key, bean);
                }

                @Override
                protected T getActualItem(final T item, final List<T> list) {
                    return Builder.this.getActualItem(item, list);
                }

            };
        }

    }

    protected abstract T getActualItem(T item, List<T> list);

    protected abstract Component getValueComponent(String key, T bean);

}
