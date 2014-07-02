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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractSyncTaskTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.SyncTaskTO;
import org.apache.syncope.common.types.MatchingRule;
import org.apache.syncope.common.types.UnmatchingRule;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Abstract Modal window for Sync and Push Task form.
 */
public abstract class AbstractSyncTaskModalPage extends AbstractSchedTaskModalPage {

    private static final long serialVersionUID = 2148403203517274669L;

    protected AjaxDropDownChoicePanel<MatchingRule> matchingRule;

    protected AjaxDropDownChoicePanel<UnmatchingRule> unmatchingRule;

    public AbstractSyncTaskModalPage(final ModalWindow window, final AbstractSyncTaskTO taskTO,
            final PageReference pageRef) {

        super(window, taskTO, pageRef);

        final IModel<List<String>> allResources = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                final List<String> resourceNames = new ArrayList<String>();

                for (ResourceTO resourceTO : resourceRestClient.getAll()) {

                    resourceNames.add(resourceTO.getName());
                }
                return resourceNames;
            }
        };

        final AjaxDropDownChoicePanel<String> resource = new AjaxDropDownChoicePanel<String>("resource",
                getString("resourceName"), new PropertyModel<String>(taskTO, "resource"));
        resource.setChoices(allResources.getObject());
        resource.setChoiceRenderer(new SelectChoiceRenderer<String>());
        resource.addRequiredLabel();
        resource.setEnabled(taskTO.getId() == 0);
        resource.setStyleSheet("ui-widget-content ui-corner-all long_dynamicsize");

        profile.add(resource);

        final WebMarkupContainer syncActionsClassNames = new WebMarkupContainer("syncActionsClassNames");
        syncActionsClassNames.setOutputMarkupId(true);
        profile.add(syncActionsClassNames);

        final AjaxLink<Void> first = new IndicatingAjaxLink<Void>("first") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                taskTO.getActionsClassNames().add(StringUtils.EMPTY);
                setVisible(false);
                target.add(syncActionsClassNames);
            }
        };
        first.setOutputMarkupPlaceholderTag(true);
        first.setVisible(taskTO.getActionsClassNames().isEmpty());
        syncActionsClassNames.add(first);

        final ListView<String> actionsClasses = new ListView<String>("actionsClasses",
                new PropertyModel<List<String>>(taskTO, "actionsClassNames")) {

                    private static final long serialVersionUID = 9101744072914090143L;

                    @Override
                    protected void populateItem(final ListItem<String> item) {
                        final String className = item.getModelObject();

                        final DropDownChoice<String> actionsClass = new DropDownChoice<String>(
                                "actionsClass", new Model<String>(className), taskRestClient.getSyncActionsClasses());
                        actionsClass.setNullValid(true);
                        actionsClass.setRequired(true);
                        actionsClass.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_BLUR) {

                            private static final long serialVersionUID = -1107858522700306810L;

                            @Override
                            protected void onUpdate(final AjaxRequestTarget target) {
                                taskTO.getActionsClassNames().
                                set(item.getIndex(), actionsClass.getModelObject());
                            }
                        });
                        actionsClass.setRequired(true);
                        actionsClass.setOutputMarkupId(true);
                        actionsClass.setRequired(true);
                        item.add(actionsClass);

                        AjaxLink<Void> minus = new IndicatingAjaxLink<Void>("drop") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                taskTO.getActionsClassNames().remove(className);
                                first.setVisible(taskTO.getActionsClassNames().isEmpty());
                                target.add(syncActionsClassNames);
                            }
                        };
                        item.add(minus);

                        final AjaxLink<Void> plus = new IndicatingAjaxLink<Void>("add") {

                            private static final long serialVersionUID = -7978723352517770644L;

                            @Override
                            public void onClick(final AjaxRequestTarget target) {
                                taskTO.getActionsClassNames().add(StringUtils.EMPTY);
                                target.add(syncActionsClassNames);
                            }
                        };
                        plus.setOutputMarkupPlaceholderTag(true);
                        plus.setVisible(item.getIndex() == taskTO.getActionsClassNames().size() - 1);
                        item.add(plus);
                    }
                };
        syncActionsClassNames.add(actionsClasses);
        // SYNCOPE-473: provisional, when push action classes will be implemented list will be enabled and container
        // moved to correct class (SyncTaskModalPage or PushTaskModalPage)
        syncActionsClassNames.setEnabled(taskTO instanceof SyncTaskTO);

        final AjaxCheckBoxPanel creates = new AjaxCheckBoxPanel("performCreate", getString("creates"),
                new PropertyModel<Boolean>(taskTO, "performCreate"));
        profile.add(creates);

        final AjaxCheckBoxPanel updates = new AjaxCheckBoxPanel("performUpdate", getString("updates"),
                new PropertyModel<Boolean>(taskTO, "performUpdate"));
        profile.add(updates);

        final AjaxCheckBoxPanel deletes = new AjaxCheckBoxPanel("performDelete", getString("updates"),
                new PropertyModel<Boolean>(taskTO, "performDelete"));
        profile.add(deletes);

        final AjaxCheckBoxPanel syncStatus = new AjaxCheckBoxPanel("syncStatus", getString("syncStatus"),
                new PropertyModel<Boolean>(taskTO, "syncStatus"));
        profile.add(syncStatus);

        matchingRule = new AjaxDropDownChoicePanel<MatchingRule>(
                "matchingRule", "matchingRule", new PropertyModel<MatchingRule>(taskTO, "matchingRule"));
        matchingRule.setChoices(Arrays.asList(MatchingRule.values()));
        ((DropDownChoice) matchingRule.getField()).setNullValid(false);

        unmatchingRule = new AjaxDropDownChoicePanel<UnmatchingRule>(
                "unmatchingRule", "unmatchingRule", new PropertyModel<UnmatchingRule>(taskTO, "unmatchingRule"));
        unmatchingRule.setChoices(Arrays.asList(UnmatchingRule.values()));
        ((DropDownChoice) unmatchingRule.getField()).setNullValid(false);
    }
}
