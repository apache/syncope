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

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public final class RelationshipViewPanel extends WizardMgtPanel<RelationshipTO> {

    private static final long serialVersionUID = -7510529471158257903L;

    private ActionLinksTogglePanel<RelationshipTO> togglePanel;

    private final ListView<RelationshipTO> relationshipsList;

    private RelationshipViewPanel(
            final String id,
            final List<RelationshipTO> relationships,
            final AnyTO anyTO,
            final boolean reuseItem,
            final boolean wizardInModal) {
        super(id, wizardInModal);
        addInnerObject(getHeader());
        relationshipsList = new ListView<>("relationships", relationships) {

            private static final long serialVersionUID = 4983556433071042668L;

            @Override
            protected void populateItem(final ListItem<RelationshipTO> relationshipItem) {
                RelationshipTO relationshipTO = relationshipItem.getModelObject();
                buildRowLabels(relationshipItem, relationshipTO, anyTO);

                ActionsPanel<RelationshipTO> action = new ActionsPanel<>("action", new Model<>(relationshipTO));
                action.add(new ActionLink<>() {

                    private static final long serialVersionUID = 5207800927605869051L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final RelationshipTO modelObject) {
                        relationships.remove(modelObject);
                        relationshipsList.remove(relationshipItem);
                        target.add(RelationshipViewPanel.this);
                    }
                }, ActionLink.ActionType.DELETE, AnyEntitlement.UPDATE.getFor(anyTO.getType()), true).hideLabel();

                if (togglePanel != null) {
                    relationshipItem.add(new AttributeModifier("style", "cursor: pointer;"));
                    relationshipItem.add(new AjaxEventBehavior(Constants.ON_CLICK) {

                        private static final long serialVersionUID = -9027652037484739586L;

                        @Override
                        protected String findIndicatorId() {
                            return StringUtils.EMPTY;
                        }

                        @Override
                        protected void onEvent(final AjaxRequestTarget target) {
                            togglePanel.toggleWithContent(target, action, relationshipTO);
                        }
                    });
                }

                if (togglePanel == null) {
                    relationshipItem.add(action);
                } else {
                    relationshipItem.add(new ActionsPanel<>("action", new Model<>(relationshipTO))
                            .setVisible(false)
                            .setEnabled(false));
                }
            }
        };
        relationshipsList.setOutputMarkupId(true);
        relationshipsList.setReuseItems(reuseItem);
        relationshipsList.setRenderBodyOnly(true);

        addInnerObject(relationshipsList);

    }

    private WebMarkupContainer getHeader() {
        WebMarkupContainer headerContainer = new WebMarkupContainer("header");
        headerContainer.add(new Label("header_left_end", getString("left.end")));
        headerContainer.add(new Label("header_relationship", new ResourceModel("relationship")));
        headerContainer.add(new Label("header_right_end", new ResourceModel("right.end")));
        return headerContainer;
    }

    private void buildRowLabels(
            final ListItem<RelationshipTO> row,
            final RelationshipTO relationshipTO,
            final AnyTO anyTO) {
        boolean isLeftRelation = relationshipTO.getEnd() == RelationshipTO.End.LEFT;
        String anyName = anyTO instanceof UserTO
                ? UserTO.class.cast(anyTO).getUsername()
                : AnyObjectTO.class.cast(anyTO).getName();

        row.add(new Label("relationship", relationshipTO.getType()));
        Label leftEnd = new Label("left_end", isLeftRelation
                ? String.format("%s %s", anyTO.getType(), anyName)
                : String.format("%s %s", relationshipTO.getOtherEndType(), relationshipTO.getOtherEndName()));

        Label rightEnd = new Label("right_end", isLeftRelation
                ? String.format("%s %s", relationshipTO.getOtherEndType(), relationshipTO.getOtherEndName())
                : String.format("%s %s", anyTO.getType(), anyName));

        if (anyTO.getKey() != null && anyTO.getKey().equals(relationshipTO.getOtherEndKey())) {
            setBold(leftEnd, rightEnd);
        } else {
            setBold(isLeftRelation ? leftEnd : rightEnd);
        }
        row.add(leftEnd, rightEnd);
    }

    private void setBold(final Label... labels) {
        for (Label label : labels) {
            label.add(new AttributeModifier("style", "font-weight: bold;"));
        }
    }

    public static class Builder extends WizardMgtPanel.Builder<RelationshipTO> {

        private static final long serialVersionUID = -3643771352897992172L;

        private List<RelationshipTO> relationships;

        private AnyTO anyTO;

        private boolean reuseItem = true;

        public Builder(final PageReference pageRef) {
            super(pageRef);
            this.relationships = null;
            this.anyTO = null;
        }

        public RelationshipViewPanel.Builder setAnyTO(final AnyTO anyTO) {
            this.anyTO = anyTO;
            return this;
        }


        public RelationshipViewPanel.Builder setRelationships(final List<RelationshipTO> relationships) {
            this.relationships = relationships;
            return this;
        }

        public RelationshipViewPanel.Builder addItem(final RelationshipTO item) {
            if (item == null) {
                return this;
            }

            if (this.relationships == null) {
                this.relationships = new ArrayList<>();
            }

            this.relationships.add(item);
            return this;
        }

        public RelationshipViewPanel.Builder setReuseItem(final boolean reuseItem) {
            this.reuseItem = reuseItem;
            return this;
        }

        @Override
        protected WizardMgtPanel<RelationshipTO> newInstance(final String id, final boolean wizardInModal) {
            return new RelationshipViewPanel(id, relationships, anyTO, reuseItem, wizardInModal);
        }

    }

}

