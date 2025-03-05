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
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.commons.AnyDataProvider;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.TokenColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.any.StatusPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.ReflectionUtils;

public abstract class AnyDirectoryPanel<A extends AnyTO, E extends AbstractAnyRestClient<A>>
        extends DirectoryPanel<A, AnyWrapper<A>, AnyDataProvider<A>, E> {

    private static final long serialVersionUID = -1100228004207271270L;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected AuditRestClient auditRestClient;

    protected final List<PlainSchemaTO> plainSchemas;

    protected final List<DerSchemaTO> derSchemas;

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

    protected final BaseModal<Serializable> utilityModal = new BaseModal<>(Constants.OUTER);

    protected AnyDirectoryPanel(final String id, final Builder<A, E> builder) {
        this(id, builder, true);
    }

    protected AnyDirectoryPanel(final String id, final Builder<A, E> builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);
        if (SyncopeConsoleSession.get().owns(String.format("%s_CREATE", builder.type), builder.realm)
                && builder.realm.startsWith(SyncopeConstants.ROOT_REALM)) {
            MetaDataRoleAuthorizationStrategy.authorizeAll(addAjaxLink, RENDER);
        } else {
            MetaDataRoleAuthorizationStrategy.unauthorizeAll(addAjaxLink, RENDER);
        }
        if (builder.dynRealm == null) {
            setReadOnly(!SyncopeConsoleSession.get().owns(String.format("%s_UPDATE", builder.type), builder.realm));
        } else {
            setReadOnly(!SyncopeConsoleSession.get().owns(String.format("%s_UPDATE", builder.type), builder.dynRealm));
        }

        realm = builder.realm;
        type = builder.type;
        fiql = builder.fiql;

        utilityModal.size(Modal.Size.Large);
        addOuterObject(utilityModal);
        setWindowClosedReloadCallback(utilityModal);

        modal.size(Modal.Size.Large);

        altDefaultModal.size(Modal.Size.Large);

        plainSchemas = AnyDirectoryPanelBuilder.class.cast(builder).getAnyTypeClassTOs().stream().
                flatMap(anyTypeClassTO -> anyTypeClassTO.getPlainSchemas().stream()).
                map(schema -> {
                    try {
                        return schemaRestClient.<PlainSchemaTO>read(SchemaType.PLAIN, schema);
                    } catch (SyncopeClientException e) {
                        LOG.warn("Could not read plain schema {}, ignoring", schema, e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());

        derSchemas = AnyDirectoryPanelBuilder.class.cast(builder).getAnyTypeClassTOs().stream().
                flatMap(anyTypeClassTO -> anyTypeClassTO.getDerSchemas().stream()).
                map(schema -> {
                    try {
                        return schemaRestClient.<DerSchemaTO>read(SchemaType.DERIVED, schema);
                    } catch (SyncopeClientException e) {
                        LOG.warn("Could not read derived schema {}, ignoring", schema, e);
                        return null;
                    }
                }).
                filter(Objects::nonNull).
                collect(Collectors.toList());

        initResultTable();

        SyncopeWebApplication.get().getAnyDirectoryPanelAdditionalActionsProvider().add(
                this,
                modal,
                wizardInModal,
                container,
                type,
                realm,
                fiql,
                rows,
                plainSchemas.stream().map(PlainSchemaTO::getKey).collect(Collectors.toList()),
                derSchemas.stream().map(DerSchemaTO::getKey).collect(Collectors.toList()),
                pageRef);
    }

    @Override
    protected List<IColumn<A, String>> getColumns() {
        List<IColumn<A, String>> columns = new ArrayList<>();
        columns.add(new KeyPropertyColumn<>(
                new ResourceModel(Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME));

        List<IColumn<A, String>> prefcolumns = new ArrayList<>();
        PreferenceManager.getList(DisplayAttributesModalPanel.getPrefDetailView(type)).stream().
                filter(name -> !Constants.KEY_FIELD_NAME.equalsIgnoreCase(name)).
                forEach(name -> addPropertyColumn(
                name,
                ReflectionUtils.findField(DisplayAttributesModalPanel.getTOClass(type), name),
                prefcolumns));

        PreferenceManager.getList(DisplayAttributesModalPanel.getPrefPlainAttributeView(type)).stream().
                map(a -> plainSchemas.stream().filter(p -> p.getKey().equals(a)).findFirst()).
                flatMap(Optional::stream).
                forEach(s -> prefcolumns.add(new AttrColumn<>(
                s.getKey(), s.getLabel(SyncopeConsoleSession.get().getLocale()), SchemaType.PLAIN)));

        PreferenceManager.getList(DisplayAttributesModalPanel.getPrefDerivedAttributeView(type)).stream().
                map(a -> derSchemas.stream().filter(p -> p.getKey().equals(a)).findFirst()).
                flatMap(Optional::stream).
                forEach(s -> prefcolumns.add(new AttrColumn<>(
                s.getKey(), s.getLabel(SyncopeConsoleSession.get().getLocale()), SchemaType.DERIVED)));

        // Add defaults in case of no selection
        if (prefcolumns.isEmpty()) {
            for (String name : getDefaultAttributeSelection()) {
                addPropertyColumn(
                        name,
                        ReflectionUtils.findField(DisplayAttributesModalPanel.getTOClass(type), name),
                        prefcolumns);
            }

            PreferenceManager.setList(
                    DisplayAttributesModalPanel.getPrefDetailView(type),
                    List.of(getDefaultAttributeSelection()));
        }

        columns.addAll(prefcolumns);
        return columns;
    }

    protected abstract String[] getDefaultAttributeSelection();

    protected void addPropertyColumn(
            final String name,
            final Field field,
            final List<IColumn<A, String>> columns) {

        if (Constants.KEY_FIELD_NAME.equalsIgnoreCase(name)) {
            columns.add(new KeyPropertyColumn<>(new ResourceModel(name, name), name, name));
        } else if (Constants.DEFAULT_TOKEN_FIELD_NAME.equalsIgnoreCase(name)) {
            columns.add(new TokenColumn<>(new ResourceModel(name, name), name));
        } else if (field != null && !field.isSynthetic()
                && (field.getType().equals(Boolean.class) || field.getType().equals(boolean.class))) {

            columns.add(new BooleanPropertyColumn<>(new ResourceModel(name, name), name, name));
        } else if (field != null && !field.isSynthetic()
                && (field.getType().equals(Date.class) || field.getType().equals(OffsetDateTime.class))) {

            columns.add(new DatePropertyColumn<>(new ResourceModel(name, name), name, name));
        } else {
            columns.add(new PropertyColumn<>(new ResourceModel(name, name), name, name));
        }
    }

    @Override
    protected AnyDataProvider<A> dataProvider() {
        return new AnyDataProvider<>(restClient, rows, filtered, realm, type, pageRef).setFIQL(this.fiql);
    }

    public AnyDataProvider<A> getDataProvider() {
        return dataProvider;
    }

    public void search(final String fiql, final AjaxRequestTarget target) {
        this.fiql = fiql;
        dataProvider.setFIQL(fiql);
        super.search(target);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        List<ActionLink.ActionType> batches = new ArrayList<>();
        batches.add(ActionLink.ActionType.DELETE);
        return batches;
    }

    @FunctionalInterface
    public interface AnyDirectoryPanelBuilder extends Serializable {

        List<AnyTypeClassTO> getAnyTypeClassTOs();
    }

    public abstract static class Builder<A extends AnyTO, E extends AbstractAnyRestClient<A>>
            extends DirectoryPanel.Builder<A, AnyWrapper<A>, E>
            implements AnyDirectoryPanelBuilder {

        private static final long serialVersionUID = -6828423611982275640L;

        /**
         * Realm related to current panel.
         */
        protected String realm = SyncopeConstants.ROOT_REALM;

        protected String dynRealm = null;

        /**
         * Any type related to current panel.
         */
        protected final String type;

        private final List<AnyTypeClassTO> anyTypeClassTOs;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final E restClient,
                final String type,
                final PageReference pageRef) {

            super(restClient, pageRef);
            this.anyTypeClassTOs = anyTypeClassTOs;
            this.type = type;
        }

        public Builder<A, E> setRealm(final String realm) {
            this.realm = realm;
            return this;
        }

        public Builder<A, E> setDynRealm(final String dynRealm) {
            this.dynRealm = dynRealm;
            return this;
        }

        @Override
        public List<AnyTypeClassTO> getAnyTypeClassTOs() {
            return this.anyTypeClassTOs;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Panel customResultBody(final String panelId, final AnyWrapper<A> item, final Serializable result) {
        if (!(result instanceof ProvisioningResult)) {
            throw new IllegalStateException("Unsupported result type");
        }

        return new StatusPanel(
                panelId,
                ((ProvisioningResult<A>) result).getEntity(),
                new ListModel<>(new ArrayList<>()),
                ((ProvisioningResult<A>) result).getPropagationStatuses().stream().map(status -> {
                    ConnObject before = status.getBeforeObj();
                    ConnObjectWrapper afterObjWrapper = new ConnObjectWrapper(
                            ((ProvisioningResult<A>) result).getEntity(),
                            status.getResource(),
                            status.getAfterObj());
                    return Triple.of(before, afterObjWrapper, status.getFailureReason());
                }).collect(Collectors.toList()),
                pageRef);
    }
}
