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
package org.apache.syncope.client.console.wizards.any;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.AnyDirectoryPanel;
import org.apache.syncope.client.console.panels.RelationshipViewPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.AnySelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RelationshipTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Relationships extends WizardStep implements ICondition {

    private static final long serialVersionUID = 855618618337931784L;

    @SpringBean
    protected RelationshipTypeRestClient relationshipTypeRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    protected AnyObjectRestClient anyObjectRestClient;

    protected final AnyTO anyTO;

    protected final PageReference pageRef;

    public Relationships(final AnyWrapper<?> modelObject, final PageReference pageRef) {
        super();
        add(new Label("title", new ResourceModel("any.relationships")));

        if (modelObject instanceof UserWrapper
                && UserWrapper.class.cast(modelObject).getPreviousUserTO() != null
                && !ListUtils.isEqualList(
                        UserWrapper.class.cast(modelObject).getInnerObject().getRelationships(),
                        UserWrapper.class.cast(modelObject).getPreviousUserTO().getRelationships())) {
            add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            add(new Label("changed", StringUtils.EMPTY));
        }

        this.anyTO = modelObject.getInnerObject();
        this.pageRef = pageRef;

        // ------------------------
        // Existing relationships
        // ------------------------
        add(getViewFragment().setRenderBodyOnly(true));
        // ------------------------ 
    }

    @Override
    public Component getHeader(final String id, final Component parent, final IWizard wizard) {
        return super.getHeader(id, parent, wizard).setVisible(false);
    }

    protected Specification newSpecification() {
        return new Specification();
    }

    protected Fragment getViewFragment() {
        Fragment viewFragment = new Fragment("relationships", "viewFragment", this);
        viewFragment.setOutputMarkupId(true);

        List<RelationshipTO> relationships = getCurrentRelationships();
        viewFragment.add(relationships.isEmpty()
                ? new Label("relationships", new Model<>(getString("relationships.empty.list")))
                : new RelationshipViewPanel.Builder(pageRef).
                        setAnyTO(anyTO).
                        setRelationships(relationships).
                        build("relationships"));

        ActionsPanel<RelationshipTO> panel = new ActionsPanel<>("actions", null);
        viewFragment.add(panel);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 3257738274365467945L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RelationshipTO ignore) {
                Fragment addFragment = new Fragment("relationships", "addFragment", Relationships.this);
                addOrReplace(addFragment);
                addFragment.add(newSpecification().setRenderBodyOnly(true));
                target.add(Relationships.this);
            }
        }, ActionType.CREATE, AnyEntitlement.UPDATE.getFor(anyTO.getType())).hideLabel();

        return viewFragment;
    }

    protected List<RelationshipTO> getCurrentRelationships() {
        return anyTO instanceof GroupableRelatableTO
                ? GroupableRelatableTO.class.cast(anyTO).getRelationships()
                : List.of();
    }

    protected void addNewRelationships(final RelationshipTO... rels) {
        getCurrentRelationships().addAll(List.of(rels));
    }

    @Override
    public boolean evaluate() {
        // [SYNCOPE-1171] - skip current step when the are no relationships types in Syncope
        return !relationshipTypeRestClient.list().isEmpty();
    }

    public class Specification extends Panel {

        private static final long serialVersionUID = 6199050589175839467L;

        protected final RelationshipTO rel;

        protected final AjaxDropDownChoicePanel<String> type;

        protected final AjaxDropDownChoicePanel<AnyTypeTO> otherType;

        protected final WebMarkupContainer container;

        protected final Fragment emptyFragment;

        protected final Fragment fragment;

        protected AnyObjectSearchPanel anyObjectSearchPanel;

        protected WizardMgtPanel<AnyWrapper<AnyObjectTO>> anyObjectDirectoryPanel;

        public Specification() {
            super("specification");
            rel = new RelationshipTO();
            rel.setEnd(RelationshipTO.End.LEFT);

            List<String> availableRels = relationshipTypeRestClient.list().stream().
                    map(RelationshipTypeTO::getKey).collect(Collectors.toList());

            type = new AjaxDropDownChoicePanel<>("type", "type", new PropertyModel<>(rel, "type"));
            type.setChoices(availableRels);
            add(type.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true).setRenderBodyOnly(true));

            List<AnyTypeTO> availableTypes = anyTypeRestClient.listAnyTypes().stream().
                    filter(anyType -> anyType.getKind() != AnyTypeKind.GROUP
                    && anyType.getKind() != AnyTypeKind.USER).collect(Collectors.toList());

            otherType = new AjaxDropDownChoicePanel<>("otherType", "otherType", new PropertyModel<>(rel, "otherType") {

                private static final long serialVersionUID = -5861057041758169508L;

                @Override
                public AnyTypeTO getObject() {
                    for (AnyTypeTO obj : availableTypes) {
                        if (obj.getKey().equals(rel.getOtherEndType())) {
                            return obj;
                        }
                    }
                    return null;
                }

                @Override
                public void setObject(final AnyTypeTO object) {
                    rel.setOtherEndType(Optional.ofNullable(object).map(AnyTypeTO::getKey).orElse(null));
                }
            }, false);
            otherType.setChoices(availableTypes);
            otherType.setChoiceRenderer(new IChoiceRenderer<>() {

                private static final long serialVersionUID = -734743540442190178L;

                @Override
                public Object getDisplayValue(final AnyTypeTO object) {
                    return object.getKey();
                }

                @Override
                public String getIdValue(final AnyTypeTO object, final int index) {
                    return object.getKey();
                }

                @Override
                public AnyTypeTO getObject(final String id, final IModel<? extends List<? extends AnyTypeTO>> choices) {
                    return choices.getObject().stream().
                            filter(anyTypeTO -> id.equals(anyTypeTO.getKey())).findAny().orElse(null);
                }
            });
            // enable "otherType" dropdown only if "type" option is selected - SYNCOPE-1140
            otherType.setEnabled(false);
            add(otherType.setOutputMarkupId(true).setOutputMarkupPlaceholderTag(true));

            container = new WebMarkupContainer("searchPanelContainer");
            add(container.setOutputMarkupId(true));

            emptyFragment = new Fragment("searchPanel", "emptyFragment", this);
            container.add(emptyFragment.setRenderBodyOnly(true));

            fragment = new Fragment("searchPanel", "searchFragment", Specification.this);

            type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    container.addOrReplace(emptyFragment.setRenderBodyOnly(true));
                    otherType.setModelObject(null);
                    // enable "otherType" dropdown only if "type" option is selected - SYNCOPE-1140
                    otherType.setEnabled(type.getModelObject() != null && !type.getModelObject().isEmpty());
                    target.add(otherType);
                    target.add(container);
                }
            });

            otherType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    AnyTypeTO anyType = otherType.getModelObject();
                    if (anyType == null) {
                        container.addOrReplace(emptyFragment.setRenderBodyOnly(true));
                    } else {
                        setupFragment(anyType);
                        container.addOrReplace(fragment.setRenderBodyOnly(true));
                    }
                    target.add(container);
                }
            });
        }

        protected void setupFragment(final AnyTypeTO anyType) {
            anyObjectSearchPanel = new AnyObjectSearchPanel.Builder(
                    anyType.getKey(),
                    new ListModel<>(new ArrayList<>()),
                    pageRef).
                    enableSearch(Specification.this).
                    build("searchPanel");
            fragment.addOrReplace(anyObjectSearchPanel.setRenderBodyOnly(true));

            anyObjectDirectoryPanel = new AnyObjectSelectionDirectoryPanel.Builder(
                    anyTypeClassRestClient.list(anyType.getClasses()),
                    anyObjectRestClient,
                    anyType.getKey(),
                    pageRef).
                    setFiql(SyncopeClient.getAnyObjectSearchConditionBuilder(anyType.getKey()).
                            is(Constants.KEY_FIELD_NAME).notNullValue().query()).
                    setWizardInModal(true).build("searchResultPanel");
            fragment.addOrReplace(anyObjectDirectoryPanel.setRenderBodyOnly(true));
        }

        @Override
        public void onEvent(final IEvent<?> event) {
            if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
                AjaxRequestTarget target =
                        SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();
                String fiql = SearchUtils.buildFIQL(anyObjectSearchPanel.getModel().getObject(),
                        SyncopeClient.getAnyObjectSearchConditionBuilder(anyObjectSearchPanel.getBackObjectType()));
                AnyDirectoryPanel.class.cast(Specification.this.anyObjectDirectoryPanel).search(fiql, target);
            } else if (event.getPayload() instanceof AnySelectionDirectoryPanel.ItemSelection) {
                AjaxRequestTarget target =
                        AnySelectionDirectoryPanel.ItemSelection.class.cast(event.getPayload()).getTarget();

                AnyTO right = AnySelectionDirectoryPanel.ItemSelection.class.cast(event.getPayload()).getSelection();
                rel.setOtherEndKey(right.getKey());
                rel.setOtherEndName(AnyObjectTO.class.cast(right).getName());

                Relationships.this.addNewRelationships(rel);

                Relationships.this.addOrReplace(getViewFragment().setRenderBodyOnly(true));
                target.add(Relationships.this);
            } else {
                super.onEvent(event);
            }
        }
    }
}
