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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.TypeExtensionDirectoryPanel.TypeExtensionDataProvider;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.TypeExtensionWizardBuilder;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class TypeExtensionDirectoryPanel
        extends DirectoryPanel<TypeExtensionTO, TypeExtensionTO, TypeExtensionDataProvider, BaseRestClient>
        implements SubmitableModalPanel {

    private static final long serialVersionUID = -4117015319209624858L;

    private final BaseModal<Serializable> baseModal;

    private final GroupTO groupTO;

    protected TypeExtensionDirectoryPanel(
            final BaseModal<Serializable> baseModal,
            final GroupTO groupTO,
            final PageReference pageRef) {

        super(BaseModal.CONTENT_ID, pageRef, false);

        this.baseModal = baseModal;
        this.groupTO = groupTO;

        TypeExtensionWizardBuilder builder = new TypeExtensionWizardBuilder(
                groupTO,
                new TypeExtensionTO(),
                new StringResourceModel("anyType", this).getObject(),
                new StringResourceModel("auxClasses", this).getObject(),
                pageRef);
        this.addNewItemPanelBuilder(builder, true);

        setShowResultPage(false);
        initResultTable();
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        GroupPatch patch = new GroupPatch();
        patch.setKey(groupTO.getKey());
        patch.getTypeExtensions().addAll(groupTO.getTypeExtensions());

        try {
            new GroupRestClient().update(groupTO.getETagValue(), patch);

            this.baseModal.show(false);
            this.baseModal.close(target);

            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
        } catch (Exception e) {
            LOG.error("Group update failure", e);
            SyncopeConsoleSession.get().error(getString(Constants.ERROR) + ": " + e.getMessage());

        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    protected TypeExtensionDataProvider dataProvider() {
        return new TypeExtensionDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_TYPE_EXTENSIONS_PAGINATOR_ROWS;
    }

    @Override
    protected List<IColumn<TypeExtensionTO, String>> getColumns() {
        List<IColumn<TypeExtensionTO, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<TypeExtensionTO, String>(
                Model.of("Any Type"), "anyType", "anyType"));
        columns.add(new PropertyColumn<TypeExtensionTO, String>(
                new StringResourceModel("auxClasses", this), "auxClasses", "auxClasses"));

        columns.add(new ActionColumn<TypeExtensionTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public ActionLinksPanel<TypeExtensionTO> getActions(
                    final String componentId, final IModel<TypeExtensionTO> model) {

                final TypeExtensionTO typeExtension = model.getObject();

                return ActionLinksPanel.<TypeExtensionTO>builder().add(new ActionLink<TypeExtensionTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final TypeExtensionTO ignore) {
                        send(TypeExtensionDirectoryPanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(typeExtension, target));
                    }
                }, ActionLink.ActionType.EDIT).add(new ActionLink<TypeExtensionTO>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final TypeExtensionTO ignore) {
                        groupTO.getTypeExtensions().remove(
                                groupTO.getTypeExtension(typeExtension.getAnyType()));
                        target.add(container);
                    }
                }, ActionLink.ActionType.DELETE).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.emptyList();
    }

    protected class TypeExtensionDataProvider extends DirectoryDataProvider<TypeExtensionTO> {

        private static final long serialVersionUID = 4533123471004692755L;

        public TypeExtensionDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("anyKey", SortOrder.ASCENDING);
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
        // change modal foter visibility
        send(TypeExtensionDirectoryPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
    }

    @Override
    protected void customActionOnCancelCallback(final AjaxRequestTarget target) {
        // change modal foter visibility
        send(TypeExtensionDirectoryPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
    }

    @Override
    protected void customActionOnFinishCallback(final AjaxRequestTarget target) {
        // change modal foter visibility
        send(TypeExtensionDirectoryPanel.this, Broadcast.BUBBLE, new BaseModal.ChangeFooterVisibilityEvent(target));
    }
}
