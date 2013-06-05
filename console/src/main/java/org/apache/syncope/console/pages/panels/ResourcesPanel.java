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
package org.apache.syncope.console.pages.panels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.MembershipTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.RoleTreeBuilder;
import org.apache.syncope.console.commons.RoleUtils;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.commons.StatusUtils;
import org.apache.syncope.console.rest.ResourceRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.console.wicket.markup.html.form.NonI18nPalette;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.component.Recorder;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ResourcesPanel extends Panel {

    private static final long serialVersionUID = -8728071019777410008L;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    @SpringBean
    private RoleTreeBuilder roleTreeBuilder;

    private final AbstractAttributableTO attributableTO;

    private final Set<String> previousResources;

    private final List<String> allResources;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 8644108944633025494L;

        private String id;

        private Object to;

        private StatusPanel statusPanel;

        public Builder(final String id) {
            this.id = id;
        }

        public Builder attributableTO(final Object to) {
            this.to = to;
            return this;
        }

        public Builder statusPanel(final StatusPanel statusPanel) {
            this.statusPanel = statusPanel;
            return this;
        }

        public ResourcesPanel build() {
            return new ResourcesPanel(this);
        }
    }

    private ResourcesPanel(final Builder builder) {
        super(builder.id);
        attributableTO = (AbstractAttributableTO) builder.to;
        previousResources = new HashSet<String>(attributableTO.getResources());
        allResources = new ArrayList<String>();
        for (ResourceTO resourceTO : resourceRestClient.getAllResources()) {
            allResources.add(resourceTO.getName());
        }
        Collections.sort(allResources);

        AjaxPalettePanel<String> resourcesPalette = null;

        if (attributableTO instanceof UserTO) {
            resourcesPalette = new AjaxRecordingPalettePanel<String>("resourcesPalette",
                    new PropertyModel<List<String>>(attributableTO, "resources"),
                    new ListModel<String>(allResources), builder.statusPanel);
        } else if (attributableTO instanceof RoleTO) {
            List<String> selectedResources = new ArrayList<String>(((RoleTO) attributableTO).getResources());
            Collections.sort(selectedResources);

            resourcesPalette = new AjaxPalettePanel<String>("resourcesPalette",
                    new PropertyModel<List<String>>(attributableTO, "resources"), new ListModel<String>(allResources));
        }
        add(resourcesPalette);

    }

    private class AjaxRecordingPalettePanel<T> extends AjaxPalettePanel<T> {

        private static final long serialVersionUID = -4215625881756021988L;

        private final StatusPanel statusPanel;

        public AjaxRecordingPalettePanel(final String id, final IModel<List<T>> model, final ListModel<T> choices,
                final StatusPanel statusPanel) {

            super(id, model, choices, new SelectChoiceRenderer<T>(), false);
            this.statusPanel = statusPanel;
        }

        @Override
        protected Palette<T> createPalette(final IModel<List<T>> model, final ListModel<T> choices,
                final IChoiceRenderer<T> renderer, final boolean allowOrder) {

            return new NonI18nPalette<T>("paletteField", model, choices, renderer, 8, allowOrder) {

                private static final long serialVersionUID = -3415146226879212841L;

                @Override
                protected Recorder<T> newRecorderComponent() {
                    Recorder<T> recorder = super.newRecorderComponent();
                    recorder.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = 5538299138211283825L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            if (attributableTO instanceof UserTO) {
                                UserTO userTO = (UserTO) attributableTO;

                                Set<String> resourcesToRemove = new HashSet<String>(previousResources);
                                resourcesToRemove.removeAll(userTO.getResources());
                                if (!resourcesToRemove.isEmpty()) {
                                    Set<String> resourcesAssignedViaMembership = new HashSet<String>();
                                    for (MembershipTO membTO : userTO.getMemberships()) {
                                        RoleTO roleTO = RoleUtils.findRole(roleTreeBuilder, membTO.getRoleId());
                                        if (roleTO != null) {
                                            resourcesAssignedViaMembership.addAll(roleTO.getResources());
                                        }
                                    }
                                    resourcesToRemove.removeAll(resourcesAssignedViaMembership);
                                }

                                previousResources.clear();
                                previousResources.addAll(userTO.getResources());

                                StatusUtils.update(statusPanel, target, userTO.getResources(), resourcesToRemove);
                            }
                        }
                    });
                    return recorder;
                }
            };
        }
    }
}
