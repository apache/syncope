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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.console.panels.AnyObjectDisplayAttributesModalPanel;
import org.apache.syncope.client.console.panels.AnyDirectoryPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.TokenColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public abstract class AnySelectionDirectoryPanel<A extends AnyTO, E extends AbstractAnyRestClient<A, ?>>
        extends AnyDirectoryPanel<A, E> {

    private static final long serialVersionUID = -1100228004207271272L;

    private final Class<A> reference;

    protected AnySelectionDirectoryPanel(
            final String id,
            final AnyDirectoryPanel.Builder<A, E> builder,
            final Class<A> reference,
            final boolean wizardInModal) {

        super(id, builder, wizardInModal);
        this.reference = reference;
    }

    @Override
    protected List<IColumn<A, String>> getColumns() {
        final List<IColumn<A, String>> columns = new ArrayList<>();

        for (String name : prefMan.getList(getRequest(), getPrefDetailsView())) {
            final Field field = ReflectionUtils.findField(AnyObjectTO.class, name);

            if ("key".equalsIgnoreCase(name)) {
                columns.add(new KeyPropertyColumn<A>(new ResourceModel(name, name), name, name));
            } else if (reference == UserTO.class && "token".equalsIgnoreCase(name)) {
                columns.add(new TokenColumn<A>(new ResourceModel(name, name), name));
            } else if (field != null
                    && (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class))) {

                columns.add(new BooleanPropertyColumn<A>(new ResourceModel(name, name), name, name));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new DatePropertyColumn<A>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(new PropertyColumn<A, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), getPrefPlainAttributesView())) {
            if (pSchemaNames.contains(name)) {
                columns.add(new AttrColumn<A>(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), getPrefDerivedAttributesView())) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn<A>(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : getDisplayAttributes()) {
                columns.add(new PropertyColumn<A, String>(new ResourceModel(name, name), name, name));
            }

            prefMan.setList(getRequest(), getResponse(), getPrefDetailsView(), Arrays.asList(getDisplayAttributes()));
        }

        columns.add(new ActionColumn<A, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<A> getActions(final String componentId, final IModel<A> model) {
                final ActionLinksPanel.Builder<A> panel = ActionLinksPanel.builder();

                panel.add(new ActionLink<A>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final A ignore) {
                        send(AnySelectionDirectoryPanel.this,
                                Broadcast.BUBBLE, new ItemSelection<>(target, model.getObject()));
                    }
                }, ActionType.SELECT, AnyEntitlement.READ.getFor(type));

                return panel.build(componentId, model.getObject());
            }

            @Override
            public ActionLinksPanel<A> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<A> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<A>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final A ignore) {
                        // still missing content
                        target.add(altDefaultModal.setContent(new AnyObjectDisplayAttributesModalPanel<>(
                                altDefaultModal, page.getPageReference(), pSchemaNames, dSchemaNames, type)));

                        altDefaultModal.addSubmitButton();
                        altDefaultModal.header(new ResourceModel("any.attr.display"));
                        altDefaultModal.show(true);
                    }
                }, ActionType.CHANGE_VIEW, AnyEntitlement.READ.getFor(type)).add(new ActionLink<A>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final A ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionType.RELOAD, AnyEntitlement.SEARCH.getFor(type)).
                        build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionType> getBulkActions() {
        return Collections.<ActionType>emptyList();
    }

    protected abstract String[] getDisplayAttributes();

    protected abstract String getPrefDetailsView();

    protected abstract String getPrefPlainAttributesView();

    protected abstract String getPrefDerivedAttributesView();

    public abstract static class Builder<A extends AnyTO, E extends AbstractAnyRestClient<A, ?>>
            extends AnyDirectoryPanel.Builder<A, E> {

        private static final long serialVersionUID = 5460024856989891156L;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final E restClient,
                final String type,
                final PageReference pageRef) {

            super(anyTypeClassTOs, restClient, type, pageRef);
            this.filtered = true;
            this.checkBoxEnabled = false;
        }
    }

    public static class ItemSelection<T extends AnyTO> implements Serializable {

        private static final long serialVersionUID = 1242677935378149180L;

        private final AjaxRequestTarget target;

        private final T usr;

        public ItemSelection(final AjaxRequestTarget target, final T usr) {
            this.target = target;
            this.usr = usr;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public T getSelection() {
            return usr;
        }
    }
}
