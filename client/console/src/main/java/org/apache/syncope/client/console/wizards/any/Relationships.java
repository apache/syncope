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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AnyDirectoryPanel;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.AnySelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RelationshipTypeRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.console.wicket.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;

public class Relationships extends WizardStep implements WizardModel.ICondition {

    private static final long serialVersionUID = 855618618337931784L;

    private final PageReference pageRef;

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final AnyTO anyTO;

    private final RelationshipTypeRestClient relationshipTypeRestClient = new RelationshipTypeRestClient();

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

    private Fragment getViewFragment() {
        final Map<String, List<RelationshipTO>> relationships = new HashMap<>();
        addRelationship(relationships, getCurrentRelationships().toArray(new RelationshipTO[] {}));

        final Fragment viewFragment = new Fragment("relationships", "viewFragment", this);
        viewFragment.setOutputMarkupId(true);

        viewFragment.add(new Accordion("relationships", relationships.keySet().stream().map(relationship -> {
            return new AbstractTab(new ResourceModel("relationship", relationship)) {

                private static final long serialVersionUID = 1037272333056449378L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new ListViewPanel.Builder<>(RelationshipTO.class, pageRef).
                            setItems(relationships.get(relationship)).
                            includes("rightType", "rightKey").
                            addAction(new ActionLink<RelationshipTO>() {

                                private static final long serialVersionUID = -6847033126124401556L;

                                @Override
                                public void onClick(
                                        final AjaxRequestTarget target, final RelationshipTO modelObject) {
                                    removeRelationships(relationships, modelObject);
                                    send(Relationships.this, Broadcast.DEPTH, new ListViewReload<>(target));
                                }
                            }, ActionType.DELETE, AnyEntitlement.UPDATE.getFor(anyTO.getType()), true).
                            build(panelId);
                }
            };
        }).collect(Collectors.toList())) {

            private static final long serialVersionUID = 1037272333056449379L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                if (relationships.isEmpty()) {
                    response.render(OnDomReadyHeaderItem.forScript(String.format(
                            "$('#emptyPlaceholder').append(\"%s\")", getString("relationships.empty.list"))));
                }
            }
        });

        final ActionsPanel<RelationshipTO> panel = new ActionsPanel<>("actions", null);
        viewFragment.add(panel);

        panel.add(new ActionLink<RelationshipTO>() {

            private static final long serialVersionUID = 3257738274365467945L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RelationshipTO ignore) {
                Fragment addFragment = new Fragment("relationships", "addFragment", Relationships.this);
                addOrReplace(addFragment);
                addFragment.add(new Specification().setRenderBodyOnly(true));
                target.add(Relationships.this);
            }
        }, ActionType.CREATE, AnyEntitlement.UPDATE.getFor(anyTO.getType())).hideLabel();

        return viewFragment;
    }

    private List<RelationshipTO> getCurrentRelationships() {
        return anyTO instanceof GroupableRelatableTO
                ? GroupableRelatableTO.class.cast(anyTO).getRelationships()
                : Collections.<RelationshipTO>emptyList();
    }

    private void addRelationship(final Map<String, List<RelationshipTO>> relationships, final RelationshipTO... rels) {

        for (RelationshipTO relationship : rels) {
            final List<RelationshipTO> listrels;
            if (relationships.containsKey(relationship.getType())) {
                listrels = relationships.get(relationship.getType());
            } else {
                listrels = new ArrayList<>();
                relationships.put(relationship.getType(), listrels);
            }
            listrels.add(relationship);
        }
    }

    private void addNewRelationships(final RelationshipTO... rels) {
        getCurrentRelationships().addAll(Arrays.asList(rels));
    }

    private void removeRelationships(
            final Map<String, List<RelationshipTO>> relationships, final RelationshipTO... rels) {
        final List<RelationshipTO> currentRels = getCurrentRelationships();
        for (RelationshipTO relationship : rels) {
            currentRels.remove(relationship);
            if (relationships.containsKey(relationship.getType())) {
                final List<RelationshipTO> rellist = relationships.get(relationship.getType());
                rellist.remove(relationship);
                if (rellist.isEmpty()) {
                    relationships.remove(relationship.getType());
                }
            }
        }
    }

    @Override
    public boolean evaluate() {
        // [SYNCOPE-1171] - skip current step when the are no relationships types in Syncope
        return !relationshipTypeRestClient.list().isEmpty();
    }

    public class Specification extends Panel {

        private static final long serialVersionUID = 6199050589175839467L;

        private final RelationshipTO rel;

        private AnyObjectSearchPanel anyObjectSearchPanel;

        private WizardMgtPanel<AnyWrapper<AnyObjectTO>> anyObjectDirectoryPanel;

        public Specification() {
            super("specification");
            rel = new RelationshipTO();

            final List<String> availableRels = relationshipTypeRestClient.list().stream().
                    map(EntityTO::getKey).collect(Collectors.toList());

            final AjaxDropDownChoicePanel<String> type = new AjaxDropDownChoicePanel<>(
                    "type", "type", new PropertyModel<>(rel, "type"));
            type.setChoices(availableRels);
            add(type.setRenderBodyOnly(true));

            final List<AnyTypeTO> availableTypes = anyTypeRestClient.listAnyTypes().stream().
                    filter(anyType -> anyType.getKind() != AnyTypeKind.GROUP
                    && anyType.getKind() != AnyTypeKind.USER).collect(Collectors.toList());

            final AjaxDropDownChoicePanel<AnyTypeTO> rightType = new AjaxDropDownChoicePanel<>(
                    "rightType", "rightType", new PropertyModel<AnyTypeTO>(rel, "rightType") {

                private static final long serialVersionUID = -5861057041758169508L;

                @Override
                public AnyTypeTO getObject() {
                    for (AnyTypeTO obj : availableTypes) {
                        if (obj.getKey().equals(rel.getRightType())) {
                            return obj;
                        }
                    }
                    return null;
                }

                @Override
                public void setObject(final AnyTypeTO object) {
                    rel.setRightType(object == null ? null : object.getKey());
                }
            }, false);
            rightType.setChoices(availableTypes);
            rightType.setChoiceRenderer(new IChoiceRenderer<AnyTypeTO>() {

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
            // enable "rightType" dropdown only if "type" option is selected - SYNCOPE-1140
            rightType.setEnabled(false);
            add(rightType);

            final WebMarkupContainer container = new WebMarkupContainer("searchPanelContainer");
            container.setOutputMarkupId(true);
            add(container);

            Fragment emptyFragment = new Fragment("searchPanel", "emptyFragment", this);
            container.add(emptyFragment.setRenderBodyOnly(true));

            type.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    Fragment emptyFragment = new Fragment("searchPanel", "emptyFragment", Specification.this);
                    container.addOrReplace(emptyFragment.setRenderBodyOnly(true));
                    rightType.setModelObject(null);
                    // enable "rightType" dropdown only if "type" option is selected - SYNCOPE-1140
                    rightType.setEnabled(type.getModelObject() != null && !type.getModelObject().isEmpty());
                    target.add(rightType);
                    target.add(container);
                }
            });

            rightType.getField().add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    final AnyTypeTO anyType = rightType.getModelObject();
                    if (anyType == null) {
                        Fragment emptyFragment = new Fragment("searchPanel", "emptyFragment", Specification.this);
                        container.addOrReplace(emptyFragment.setRenderBodyOnly(true));
                    } else {
                        final Fragment fragment = new Fragment("searchPanel", "searchFragment", Specification.this);
                        container.addOrReplace(fragment.setRenderBodyOnly(true));

                        anyObjectSearchPanel = new AnyObjectSearchPanel.Builder(
                                anyType.getKey(),
                                new ListModel<>(new ArrayList<>())).
                                enableSearch(Specification.this).
                                build("searchPanel");
                        fragment.add(anyObjectSearchPanel.setRenderBodyOnly(true));

                        anyObjectDirectoryPanel = new AnyObjectSelectionDirectoryPanel.Builder(
                                anyTypeClassRestClient.list(anyType.getClasses()),
                                anyType.getKey(),
                                pageRef).
                                setFiql(SyncopeClient.getAnyObjectSearchConditionBuilder(anyType.getKey()).
                                        is("key").notNullValue().query()).
                                setWizardInModal(true).build("searchResultPanel");
                        fragment.add(anyObjectDirectoryPanel.setRenderBodyOnly(true));
                    }
                    target.add(container);
                }
            });
        }

        @Override
        public void onEvent(final IEvent<?> event) {
            if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
                final AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).
                        getTarget();
                final String fiql = SearchUtils.buildFIQL(anyObjectSearchPanel.getModel().getObject(),
                        SyncopeClient.getAnyObjectSearchConditionBuilder(anyObjectSearchPanel.getBackObjectType()));
                AnyDirectoryPanel.class.cast(Specification.this.anyObjectDirectoryPanel).search(fiql, target);
            } else if (event.getPayload() instanceof AnySelectionDirectoryPanel.ItemSelection) {
                final AjaxRequestTarget target = AnySelectionDirectoryPanel.ItemSelection.class.cast(event.
                        getPayload()).getTarget();

                AnyTO right = AnySelectionDirectoryPanel.ItemSelection.class.cast(event.getPayload()).getSelection();
                rel.setRightKey(right.getKey());

                Relationships.this.addNewRelationships(rel);

                Relationships.this.addOrReplace(getViewFragment().setRenderBodyOnly(true));
                target.add(Relationships.this);
            } else {
                super.onEvent(event);
            }
        }
    }
}
