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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.AnyDataProvider;
import org.apache.syncope.client.console.commons.SerializableTransformer;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.any.AnyHandler;
import org.apache.syncope.client.console.wizards.any.StatusPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.util.ListModel;

public abstract class AnyDirectoryPanel<T extends AnyTO>
        extends DirectoryPanel<T, AnyHandler<T>, AnyDataProvider<T>, AbstractAnyRestClient<T>> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected final SchemaRestClient schemaRestClient = new SchemaRestClient();

    protected final List<String> pSchemaNames;

    protected final List<String> dSchemaNames;

    /**
     * Filter used in case of filtered search.
     */
    protected String fiql;

    /**
     * Realm related to current panel.
     */
    protected final String realm;

    /**
     * Any type related to current panel.
     */
    protected final String type;

    protected final BaseModal<Serializable> utilityModal = new BaseModal<>("outer");

    protected AnyDirectoryPanel(final String id, final Builder<T> builder) {
        super(id, builder);
        this.realm = builder.realm;
        this.type = builder.type;
        this.fiql = builder.fiql;

        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);

        modal.size(Modal.Size.Large);
        altDefaultModal.size(Modal.Size.Large);

        this.pSchemaNames = new ArrayList<>();
        for (AnyTypeClassTO anyTypeClassTO : AnyDirectoryPanelBuilder.class.cast(builder).getAnyTypeClassTOs()) {
            this.pSchemaNames.addAll(anyTypeClassTO.getPlainSchemas());
        }
        this.dSchemaNames = new ArrayList<>();
        for (AnyTypeClassTO anyTypeClassTO : AnyDirectoryPanelBuilder.class.cast(builder).getAnyTypeClassTOs()) {
            this.dSchemaNames.addAll(anyTypeClassTO.getDerSchemas());
        }

        initResultTable();
    }

    @Override
    protected AnyDataProvider<T> dataProvider() {
        final AnyDataProvider<T> dp = new AnyDataProvider<>(restClient, rows, filtered, realm, type);
        return dp.setFIQL(this.fiql);
    }

    public void search(final String fiql, final AjaxRequestTarget target) {
        this.fiql = fiql;
        dataProvider.setFIQL(fiql);
        super.search(target);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionLink.ActionType> bulkActions = new ArrayList<>();

        bulkActions.add(ActionLink.ActionType.DELETE);
        bulkActions.add(ActionLink.ActionType.SUSPEND);
        bulkActions.add(ActionLink.ActionType.REACTIVATE);

        return bulkActions;
    }

    public interface AnyDirectoryPanelBuilder extends Serializable {

        List<AnyTypeClassTO> getAnyTypeClassTOs();
    }

    public abstract static class Builder<T extends AnyTO>
            extends DirectoryPanel.Builder<T, AnyHandler<T>, AbstractAnyRestClient<T>>
            implements AnyDirectoryPanelBuilder {

        private static final long serialVersionUID = -6828423611982275640L;

        /**
         * Realm related to current panel.
         */
        protected String realm = "/";

        /**
         * Any type related to current panel.
         */
        protected final String type;

        private final List<AnyTypeClassTO> anyTypeClassTOs;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final AbstractAnyRestClient<T> restClient,
                final String type,
                final PageReference pageRef) {

            super(restClient, pageRef);
            this.anyTypeClassTOs = anyTypeClassTOs;
            this.type = type;
        }

        public Builder<T> setRealm(final String realm) {
            this.realm = realm;
            return this;
        }

        @Override
        public List<AnyTypeClassTO> getAnyTypeClassTOs() {
            return this.anyTypeClassTOs;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Panel customResultBody(final String panelId, final AnyHandler<T> item, final Serializable result) {
        if (!(result instanceof ProvisioningResult)) {
            throw new IllegalStateException("Unsupported result type");
        }

        return new StatusPanel(
                panelId,
                ((ProvisioningResult<T>) result).getAny(),
                new ListModel<>(new ArrayList<StatusBean>()),
                CollectionUtils.collect(
                        ((ProvisioningResult<T>) result).getPropagationStatuses(),
                        new SerializableTransformer<PropagationStatus, Pair<ConnObjectTO, ConnObjectWrapper>>() {

                    private static final long serialVersionUID = -4931455531906427515L;

                    @Override
                    public Pair<ConnObjectTO, ConnObjectWrapper> transform(final PropagationStatus input) {
                        ConnObjectTO before = input.getBeforeObj();
                        ConnObjectWrapper afterObjWrapper = new ConnObjectWrapper(
                                ((ProvisioningResult<T>) result).getAny(),
                                input.getResource(),
                                input.getAfterObj());
                        return Pair.of(before, afterObjWrapper);
                    }

                }, new ArrayList<Pair<ConnObjectTO, ConnObjectWrapper>>()),
                pageRef);
    }
}
