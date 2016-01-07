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

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.Collapsible;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SerializableTransformer;
import org.apache.syncope.client.console.panels.AnyObjectSearchResultPanel;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSelectionSearchResultPanel;
import org.apache.syncope.client.console.panels.search.AnySelectionSearchResultPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;

public class Relationships extends WizardStep {

    private static final long serialVersionUID = 855618618337931784L;

    private final PageReference pageRef;

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    private final AnyTO anyTO;

    public Relationships(final AnyTO anyTO, final PageReference pageRef) {
        super();
        this.anyTO = anyTO;
        this.pageRef = pageRef;

        // ------------------------
        // Existing relationships
        // ------------------------
        add(getViewFragment().setRenderBodyOnly(true));
        // ------------------------ 
    }

    private Fragment getViewFragment() {
        final Map<String, List<RelationshipTO>> relationships = new HashMap<>();
        addRelationship(relationships, getCurrentRelationships().toArray(new RelationshipTO[] {}));

        final Fragment viewFragment = new Fragment("relationships", "viewFragment", this);
        viewFragment.setOutputMarkupId(true);

        viewFragment.add(new Collapsible("relationships",
                CollectionUtils.collect(relationships.keySet(), new SerializableTransformer<String, ITab>() {

                    private static final long serialVersionUID = 3514912643300593122L;

                    @Override
                    public ITab transform(final String input) {
                        return new AbstractTab(new ResourceModel("relationship", input)) {

                            private static final long serialVersionUID = 1037272333056449378L;

                            @Override
                            public Panel getPanel(final String panelId) {
                                return new ListViewPanel.Builder<>(RelationshipTO.class, pageRef).
                                        setItems(relationships.get(input)).
                                        includes("rightType", "rightKey").
                                        addAction(new ActionLink<RelationshipTO>() {

                                            private static final long serialVersionUID = -6847033126124401556L;

                                            @Override
                                            public void onClick(
                                                    final AjaxRequestTarget target, final RelationshipTO modelObject) {
                                                removeRelationships(relationships, modelObject);
                                                send(Relationships.this, Broadcast.DEPTH, new ListViewReload(target));
                                            }
                                        }, ActionLink.ActionType.DELETE,
                                                String.format("%s_%s", anyTO.getType(), AnyEntitlement.UPDATE)).
                                        build(panelId);
                            }
                        };
                    }
                }, new ArrayList<ITab>())) {

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

        viewFragment.add(ActionLinksPanel.<RelationshipTO>builder(pageRef).add(new ActionLink<RelationshipTO>() {

            private static final long serialVersionUID = 3257738274365467945L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RelationshipTO ignore) {
                Fragment addFragment = new Fragment("relationships", "addFragment", Relationships.this);
                addOrReplace(addFragment);
                addFragment.add(new Specification().setRenderBodyOnly(true));
                target.add(Relationships.this);
            }
        }, ActionLink.ActionType.CREATE, String.format("%s_%s", anyTO.getType(), AnyEntitlement.UPDATE)).
                build("actions"));

        return viewFragment;
    }

    private List<RelationshipTO> getCurrentRelationships() {
        return anyTO instanceof UserTO ? UserTO.class.cast(anyTO).getRelationships() : anyTO instanceof AnyObjectTO
                ? AnyObjectTO.class.cast(anyTO).getRelationships()
                : Collections.<RelationshipTO>emptyList();
    }

    private void addRelationship(
            final Map<String, List<RelationshipTO>> relationships, final RelationshipTO... rels) {
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
        for (RelationshipTO relationship : rels) {
            getCurrentRelationships().add(relationship);
        }
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

    public class Specification extends Panel {

        private static final long serialVersionUID = 6199050589175839467L;

        private final RelationshipTO rel;

        private AnyObjectSearchPanel anyObjectSearchPanel;

        private WizardMgtPanel<AnyHandler<AnyObjectTO>> anyObjectSearchResultPanel;

        public Specification() {
            super("specification");
            rel = new RelationshipTO();

            final ArrayList<String> availableRels = CollectionUtils.collect(
                    SyncopeConsoleSession.get().getService(RelationshipTypeService.class).list(),
                    new SerializableTransformer<RelationshipTypeTO, String>() {

                private static final long serialVersionUID = 5498141517922697858L;

                @Override
                public String transform(final RelationshipTypeTO input) {
                    return input.getKey();
                }
            }, new ArrayList<String>());

            final AjaxDropDownChoicePanel<String> type = new AjaxDropDownChoicePanel<String>(
                    "type", "type", new PropertyModel<String>(rel, "type"));
            type.setChoices(availableRels);
            add(type.setRenderBodyOnly(true));

            final List<AnyTypeTO> availableTypes
                    = ListUtils.select(anyTypeRestClient.getAll(), new Predicate<AnyTypeTO>() {

                        @Override
                        public boolean evaluate(final AnyTypeTO object) {
                            return object.getKind() != AnyTypeKind.GROUP && object.getKind() != AnyTypeKind.USER;
                        }
                    });

            final AjaxDropDownChoicePanel<AnyTypeTO> rightType = new AjaxDropDownChoicePanel<AnyTypeTO>(
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
                    return IterableUtils.find(choices.getObject(), new Predicate<Object>() {

                        @Override
                        public boolean evaluate(final Object object) {
                            return id.equals(AnyTypeTO.class.cast(object).getKey());
                        }
                    });
                }
            });
            add(rightType);

            final WebMarkupContainer container = new WebMarkupContainer("searchPanelContainer");
            container.setOutputMarkupId(true);
            add(container);

            Fragment emptyFragment = new Fragment("searchPanel", "emptyFragment", this);
            container.add(emptyFragment.setRenderBodyOnly(true));

            type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    Fragment emptyFragment = new Fragment("searchPanel", "emptyFragment", Specification.this);
                    container.addOrReplace(emptyFragment.setRenderBodyOnly(true));
                    rightType.setModelObject(null);
                    target.add(rightType);
                    target.add(container);
                }
            });

            rightType.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

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
                                new ListModel<SearchClause>(new ArrayList<SearchClause>())).
                                enableSearch().
                                build("searchPanel");
                        fragment.add(anyObjectSearchPanel.setRenderBodyOnly(true));

                        anyObjectSearchResultPanel = new AnyObjectSelectionSearchResultPanel.Builder(
                                anyTypeRestClient.getAnyTypeClass(anyType.getClasses().toArray(new String[] {})),
                                new AnyObjectRestClient(),
                                anyType.getKey(),
                                pageRef).setFiltered(true).
                                setFiql(SyncopeClient.getAnyObjectSearchConditionBuilder(anyType.getKey()).
                                        is("key").notNullValue().query()).
                                build("searchResultPanel");
                        fragment.add(anyObjectSearchResultPanel.setRenderBodyOnly(true));
                    }
                    target.add(container);
                }
            });
        }

        @Override
        public void onEvent(final IEvent<?> event) {
            if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
                final AjaxRequestTarget target
                        = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();
                final String fiql = SearchUtils.buildFIQL(anyObjectSearchPanel.getModel().getObject(),
                        SyncopeClient.getAnyObjectSearchConditionBuilder(anyObjectSearchPanel.getBackObjectType()));
                AnyObjectSearchResultPanel.class.cast(anyObjectSearchResultPanel).search(fiql, target);
            } else if (event.getPayload() instanceof AnySelectionSearchResultPanel.ItemSelection) {
                final AjaxRequestTarget target
                        = AnySelectionSearchResultPanel.ItemSelection.class.cast(event.getPayload()).getTarget();

                AnyTO right = AnySelectionSearchResultPanel.ItemSelection.class.cast(event.getPayload()).getSelection();
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
