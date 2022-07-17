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
import java.util.Collection;
import java.util.List;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.AnyDirectoryPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.model.IModel;

public abstract class AnySelectionDirectoryPanel<A extends AnyTO, E extends AbstractAnyRestClient<A>>
        extends AnyDirectoryPanel<A, E> {

    private static final long serialVersionUID = -1100228004207271272L;

    protected AnySelectionDirectoryPanel(
            final String id,
            final AnyDirectoryPanel.Builder<A, E> builder,
            final boolean wizardInModal) {

        super(id, builder, wizardInModal);

        SyncopeWebApplication.get().getAnyDirectoryPanelAdditionalActionsProvider().hide();
    }

    @Override
    public ActionsPanel<A> getActions(final IModel<A> model) {
        final ActionsPanel<A> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final A ignore) {
                send(AnySelectionDirectoryPanel.this,
                    Broadcast.BUBBLE, new ItemSelection<>(target, model.getObject()));
            }
        }, ActionType.SELECT, AnyEntitlement.READ.getFor(type));

        return panel;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        return List.of();
    }

    public abstract static class Builder<A extends AnyTO, E extends AbstractAnyRestClient<A>>
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
