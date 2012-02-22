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
package org.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.to.PropagationTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.commons.StatusUtils;
import org.syncope.console.pages.UserModalPage;
import org.syncope.types.PropagationTaskExecStatus;

/**
 * User management result panel.
 */
public class UserManagementResultPanel extends Panel {

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 2646115294319713723L;

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(UserManagementResultPanel.class);

    /**
     * Status management utilities.
     */
    @SpringBean
    private StatusUtils statusUtils;

    /**
     * Panel constructor.
     *
     * @param id panel id.
     * @param window guest modal window.
     * @param mode operation mode.
     * @param userTO User TO.
     */
    public UserManagementResultPanel(
            final String id,
            final ModalWindow window,
            final UserModalPage.Mode mode,
            final UserTO userTO) {

        super(id);

        // shortcut to retrieve fragments inside inner classes
        final Panel panel = this;

        final WebMarkupContainer container =
                new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final Fragment fragment = new Fragment("userModalResultFrag",
                mode == UserModalPage.Mode.SELF
                ? "userModalSelfResultFrag" : "userModalPropagationResultFrag",
                this);

        fragment.setOutputMarkupId(true);
        container.add(fragment);

        if (mode == UserModalPage.Mode.ADMIN) {

            // add Syncope propagation status
            PropagationTO syncope = new PropagationTO();
            syncope.setResourceName("Syncope");
            syncope.setStatus(PropagationTaskExecStatus.SUCCESS);

            List<PropagationTO> propagations = new ArrayList<PropagationTO>();
            propagations.add(syncope);
            propagations.addAll(userTO.getPropagationTOs());

            fragment.add(new Label("userInfo", userTO.getUsername() != null
                    ? userTO.getUsername() : String.valueOf(userTO.getId())));

            final ListView<PropagationTO> propRes = new ListView<PropagationTO>(
                    "resources", propagations) {

                private static final long serialVersionUID =
                        -1020475259727720708L;

                @Override
                protected void populateItem(final ListItem item) {
                    final PropagationTO propTO =
                            (PropagationTO) item.getDefaultModelObject();

                    final ListView attributes = getConnObjectView(propTO);

                    final Fragment attrhead;

                    if (attributes.getModelObject() != null
                            && !attributes.getModelObject().isEmpty()) {
                        attrhead = new Fragment(
                                "attrhead", "attrHeadFrag", panel);
                    } else {
                        attrhead = new Fragment(
                                "attrhead", "emptyAttrHeadFrag", panel);
                    }

                    item.add(attrhead);
                    item.add(attributes);

                    attrhead.add(new Label("resource", propTO.getResourceName()));

                    attrhead.add(new Label("propagation",
                            propTO.getStatus() != null
                            ? propTO.getStatus().toString() : "UNDEFINED"));

                    final Image image;
                    final String alt, title;

                    switch (propTO.getStatus()) {
                        case SUCCESS:
                        case SUBMITTED:
                        case CREATED:
                            image = new Image("icon", "statuses/active.png");
                            alt = "success icon";
                            title = "success";
                            break;
                        default:
                            image = new Image("icon", "statuses/inactive.png");
                            alt = "failure icon";
                            title = "failure";
                    }

                    image.add(new Behavior() {

                        private static final long serialVersionUID =
                                1469628524240283489L;

                        @Override
                        public void onComponentTag(
                                final Component component,
                                final ComponentTag tag) {
                            tag.put("alt", alt);
                            tag.put("title", title);
                        }
                    });

                    attrhead.add(image);
                }
            };
            fragment.add(propRes);
        }

        final AjaxLink close = new IndicatingAjaxLink("close") {

            private static final long serialVersionUID =
                    -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                window.close(target);
            }
        };
        container.add(close);
    }

    /**
     * Get remote attributes list view.
     *
     * @param propTO propagation TO.
     * @return list view.
     */
    private ListView getConnObjectView(final PropagationTO propTO) {
        final ConnObjectTO before = propTO.getBefore();
        final ConnObjectTO after = propTO.getAfter();

        // sorted in reversed presentation order
        final List<String> head = Arrays.asList(new String[]{
                    "__PASSWORD__", "__ENABLE__", "__UID__", "__NAME__"});

        final Map<String, AttributeTO> beforeAttrMap;

        final Map<String, AttributeTO> afterAttrMap;

        final Set<String> attributes = new HashSet<String>();

        if (before != null) {
            beforeAttrMap = before.getAttributeMap();
            attributes.addAll(beforeAttrMap.keySet());
        } else {
            beforeAttrMap = Collections.EMPTY_MAP;
        }

        if (after != null) {
            afterAttrMap = after.getAttributeMap();
            attributes.addAll(afterAttrMap.keySet());
        } else {
            afterAttrMap = Collections.EMPTY_MAP;
        }

        final List<String> profile = new ArrayList<String>();
        profile.addAll(attributes);
        profile.removeAll(head);
        Collections.sort(profile);

        for (String attr : head) {
            if (attributes.contains(attr)) {
                profile.add(0, attr);
            }
        }

        return new ListView("attributes", profile) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem item) {
                String name = item.getModelObject().toString();

                final Fragment beforeValue;
                final Fragment afterValue;

                if ("__ENABLE__".equals(name)) {
                    beforeValue = getStatusIcon("beforeValue", before);
                    afterValue = getStatusIcon("afterValue", after);
                } else {
                    beforeValue = getLabelValue(
                            "beforeValue", name, beforeAttrMap);

                    afterValue = getLabelValue(
                            "afterValue", name, afterAttrMap);
                }

                item.add(new Label("attrName", new ResourceModel(name, name)));

                item.add(beforeValue);
                item.add(afterValue);
            }
        };
    }

    /**
     * Get fragment for attribute value (not remote status).
     *
     * @param id component id to be replaced with the fragment content.
     * @param attrName remote attribute name
     * @param attrMap remote attributes map.
     * @return fragment.
     */
    private Fragment getLabelValue(
            final String id,
            final String attrName,
            final Map<String, AttributeTO> attrMap) {
        final String value;

        final AttributeTO attr = attrMap.get(attrName);

        if (attr != null
                && attr.getValues() != null
                && !attr.getValues().isEmpty()) {

            if ("__PASSWORD__".equals(attrName)) {
                value = "********";
            } else {
                value = attr.getValues().size() > 1
                        ? attr.getValues().toString() : attr.getValues().get(0);
            }

        } else {
            value = "";
        }

        Component label = new Label("value", value).add(
                new Behavior() {

                    private static final long serialVersionUID =
                            1469628524240283489L;

                    @Override
                    public void onComponentTag(
                            final Component component,
                            final ComponentTag tag) {
                        tag.put("title", value);
                    }
                });

        final Fragment frag = new Fragment(id, "attrValueFrag", this);
        frag.add(label);

        return frag;
    }

    /**
     * Get fragment for user status icon.
     *
     * @param id component id to be replaced with the fragment content.
     * @param objectTO connector object TO.
     * @return fragment.
     */
    private Fragment getStatusIcon(
            final String id,
            final ConnObjectTO objectTO) {
        final Image image;
        final String alt, title;

        switch (statusUtils.getRemoteStatus(objectTO).getStatus()) {
            case ACTIVE:
                image = new Image("status", "statuses/active.png");
                alt = "active icon";
                title = "Enabled";
                break;
            case SUSPENDED:
                image = new Image("status", "statuses/inactive.png");
                alt = "inactive icon";
                title = "Disabled";
                break;
            default:
                image = null;
                alt = null;
                title = null;
        }

        final Fragment frag;

        if (image != null) {
            image.add(new Behavior() {

                private static final long serialVersionUID =
                        1469628524240283489L;

                @Override
                public void onComponentTag(
                        final Component component,
                        final ComponentTag tag) {
                    tag.put("alt", alt);
                    tag.put("title", title);
                    tag.put("width", "12px");
                    tag.put("height", "12px");
                }
            });

            frag = new Fragment(id, "remoteStatusFrag", this);
            frag.add(image);
        } else {
            frag = new Fragment(id, "emptyFrag", this);
        }

        return frag;
    }
}
