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
package org.apache.syncope.client.console.authprofiles;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.AuthProfileRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

public abstract class AuthProfileItemDirectoryPanel<I extends BaseBean>
        extends DirectoryPanel<I, I, AuthProfileItemDirectoryPanel<I>.AuthProfileItemProvider, AuthProfileRestClient> {

    private static final long serialVersionUID = 7640851594812655896L;

    protected final BaseModal<AuthProfileTO> authProfileModal;

    protected final AuthProfileTO authProfile;

    public AuthProfileItemDirectoryPanel(
            final String id,
            final AuthProfileRestClient restClient,
            final BaseModal<AuthProfileTO> authProfileModal,
            final AuthProfileTO authProfile,
            final PageReference pageRef) {

        super(id, restClient, pageRef, false);

        this.authProfileModal = authProfileModal;
        this.authProfile = authProfile;

        setOutputMarkupId(true);

        enableUtilityButton();
        setFooterVisibility(false);

        addNewItemPanelBuilder(new AuthProfileItemWizardBuilder(pageRef), true);

        disableCheckBoxes();
        initResultTable();
    }

    protected abstract List<I> getItems();

    protected abstract I defaultItem();

    protected abstract String sortProperty();

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            authProfileModal.close(target);
        } else if (event.getPayload() instanceof final AjaxWizard.EditItemActionEvent<?> payload) {
            payload.getTarget().ifPresent(actionTogglePanel::close);
        }
        super.onEvent(event);
    }

    @Override
    protected AuthProfileItemProvider dataProvider() {
        return new AuthProfileItemProvider(sortProperty(), rows);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    public ActionsPanel<I> getActions(final IModel<I> model) {
        ActionsPanel<I> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final I ignore) {
                send(AuthProfileItemDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, AMEntitlement.AUTH_PROFILE_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final I ignore) {
                try {
                    getItems().remove(model.getObject());
                    restClient.update(authProfile);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {} from {}", model.getObject(), authProfile.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, AMEntitlement.AUTH_PROFILE_UPDATE, true);

        return panel;
    }

    protected class AuthProfileItemProvider extends DirectoryDataProvider<I> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final SortableDataProviderComparator<I> comparator;

        AuthProfileItemProvider(final String sort, final int paginatorRows) {
            super(paginatorRows);

            setSort(sort, SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<I> iterator(final long first, final long count) {
            List<I> list = getItems();
            list.sort(comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return getItems().size();
        }

        @Override
        public IModel<I> model(final I object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    protected class AuthProfileItemWizardBuilder extends AuthProfileWizardBuilder<I> {

        private static final long serialVersionUID = -7174537333960225216L;

        protected AuthProfileItemWizardBuilder(final PageReference pageRef) {
            super(defaultItem(), new StepModel<>(), pageRef);
        }

        @Override
        protected Serializable onApplyInternal(final I modelObject) {
            if (modelObject instanceof GoogleMfaAuthAccount) {
                BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(modelObject);
                @SuppressWarnings("unchecked")
                List<Serializable> values = (List<Serializable>) wrapper.getPropertyValue("scratchCodes");
                if (values != null) {
                    List<Integer> converted = values.stream().map(value -> {
                        if (value instanceof Integer) {
                            return Integer.class.cast(value);
                        }
                        if (value instanceof String) {
                            try {
                                return Integer.valueOf((String) value);
                            } catch (NumberFormatException e) {
                                LOG.error("Could not convert to Integer: {}", value, e);
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                    wrapper.setPropertyValue("scratchCodes", converted);
                }
            }

            getItems().remove(model.getInitialModelObject());
            getItems().add(modelObject);
            restClient.update(authProfile);

            return modelObject;
        }
    }
}
