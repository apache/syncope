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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.TypeExtensionDirectoryPanel.TypeExtensionDataProvider;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.any.TypeExtensionWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.SubmitableModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TypeExtensionDirectoryPanel
        extends DirectoryPanel<TypeExtensionTO, TypeExtensionTO, TypeExtensionDataProvider, BaseRestClient>
        implements SubmitableModalPanel {

    private static final long serialVersionUID = -4117015319209624858L;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    protected final BaseModal<Serializable> baseModal;

    protected final GroupTO groupTO;

    protected TypeExtensionDirectoryPanel(
            final BaseModal<Serializable> baseModal,
            final GroupTO groupTO,
            final PageReference pageRef) {

        super(BaseModal.CONTENT_ID, null, pageRef, false);

        this.baseModal = baseModal;
        this.groupTO = groupTO;

        TypeExtensionWizardBuilder builder = new TypeExtensionWizardBuilder(
                groupTO,
                new TypeExtensionTO(),
                new StringResourceModel("anyType", this).getObject(),
                new StringResourceModel("auxClasses", this).getObject(),
                anyTypeRestClient,
                anyTypeClassRestClient,
                pageRef);
        this.addNewItemPanelBuilder(builder, true);

        setShowResultPanel(false);
        initResultTable();
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        GroupUR req = new GroupUR();
        req.setKey(groupTO.getKey());
        req.getTypeExtensions().addAll(groupTO.getTypeExtensions());

        try {
            groupRestClient.update(groupTO.getETagValue(), req);

            this.baseModal.show(false);
            this.baseModal.close(target);

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("Group update failure", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    public void onError(final AjaxRequestTarget target) {
        SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    protected TypeExtensionDataProvider dataProvider() {
        return new TypeExtensionDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_TYPE_EXTENSIONS_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<TypeExtensionTO, String>> getColumns() {
        List<IColumn<TypeExtensionTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                Model.of("Any Type"), "anyType", "anyType"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("auxClasses", this), "auxClasses", "auxClasses"));

        return columns;
    }

    @Override
    public ActionsPanel<TypeExtensionTO> getActions(final IModel<TypeExtensionTO> model) {
        final ActionsPanel<TypeExtensionTO> panel = super.getActions(model);
        final TypeExtensionTO typeExtension = model.getObject();

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final TypeExtensionTO ignore) {
                send(TypeExtensionDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(typeExtension, target));
            }
        }, ActionLink.ActionType.EDIT, StringUtils.EMPTY);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final TypeExtensionTO ignore) {
                groupTO.getTypeExtension(typeExtension.getAnyType()).ifPresent(typeExt -> {
                    groupTO.getTypeExtensions().remove(typeExt);
                    target.add(container);
                });
            }
        }, ActionLink.ActionType.DELETE, StringUtils.EMPTY, true);
        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    protected class TypeExtensionDataProvider extends DirectoryDataProvider<TypeExtensionTO> {

        private static final long serialVersionUID = 4533123471004692755L;

        public TypeExtensionDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("anyType", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<? extends TypeExtensionTO> iterator(final long first, final long count) {
            return groupTO.getTypeExtensions().subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            return groupTO.getTypeExtensions().size();
        }

        @Override
        public IModel<TypeExtensionTO> model(final TypeExtensionTO object) {
            return new CompoundPropertyModel<>(object);
        }

    }

    @Override
    protected void customActionCallback(final AjaxRequestTarget target) {
        // change modal footer visibility
        send(TypeExtensionDirectoryPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
    }

    @Override
    protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
        // change modal footer visibility
        send(TypeExtensionDirectoryPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
    }

    @Override
    protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
        // change modal footer visibility
        send(TypeExtensionDirectoryPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
    }
}
