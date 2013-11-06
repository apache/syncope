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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.PropagationStatusTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.PropagationTaskExecStatus;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.status.Status;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
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
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Show user or role status after performing a successful operation.
 */
public class ResultStatusModalPage extends BaseModalPage {

    private static final long serialVersionUID = 2646115294319713723L;

    private static final String IMG_STATUSES = "statuses/";

    @SpringBean(name = "anonymousUser")
    private String anonymousUser;

    private final AbstractAttributableTO attributable;

    private final UserModalPage.Mode mode;

    /**
     * Status management utilities.
     */
    private final StatusUtils statusUtils;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 220361441802274899L;

        private ModalWindow window;

        private UserModalPage.Mode mode;

        private AbstractAttributableTO attributable;

        public Builder(final ModalWindow window, final AbstractAttributableTO attributable) {
            this.window = window;
            this.attributable = attributable;
        }

        public ResultStatusModalPage.Builder mode(final UserModalPage.Mode mode) {
            this.mode = mode;
            return this;
        }

        public ResultStatusModalPage build() {
            return new ResultStatusModalPage(this);
        }
    }

    private ResultStatusModalPage(final Builder builder) {
        super();
        this.attributable = builder.attributable;
        statusUtils = new StatusUtils(this.userRestClient);
        if (builder.mode == null) {
            this.mode = UserModalPage.Mode.ADMIN;
        } else {
            this.mode = builder.mode;
        }

        final BaseModalPage page = this;

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final Fragment fragment = new Fragment("resultFrag", mode == UserModalPage.Mode.SELF
                ? "userSelfResultFrag"
                : "propagationResultFrag", this);
        fragment.setOutputMarkupId(true);
        container.add(fragment);

        if (mode == UserModalPage.Mode.ADMIN) {
            // add Syncope propagation status
            PropagationStatusTO syncope = new PropagationStatusTO();
            syncope.setResource("Syncope");
            syncope.setStatus(PropagationTaskExecStatus.SUCCESS);

            List<PropagationStatusTO> propagations = new ArrayList<PropagationStatusTO>();
            propagations.add(syncope);
            propagations.addAll(attributable.getPropagationStatusTOs());

            fragment.add(new Label("info",
                    ((attributable instanceof UserTO) && ((UserTO) attributable).getUsername() != null)
                    ? ((UserTO) attributable).getUsername()
                    : ((attributable instanceof RoleTO) && ((RoleTO) attributable).getName() != null)
                    ? ((RoleTO) attributable).getName()
                    : String.valueOf(attributable.getId())));

            final ListView<PropagationStatusTO> propRes = new ListView<PropagationStatusTO>("resources",
                    propagations) {

                        private static final long serialVersionUID = -1020475259727720708L;

                        @Override
                        protected void populateItem(final ListItem<PropagationStatusTO> item) {
                            final PropagationStatusTO propTO = (PropagationStatusTO) item.getDefaultModelObject();

                            final ListView attributes = getConnObjectView(propTO);

                            final Fragment attrhead;
                            if (attributes.getModelObject() == null || attributes.getModelObject().isEmpty()) {
                                attrhead = new Fragment("attrhead", "emptyAttrHeadFrag", page);
                            } else {
                                attrhead = new Fragment("attrhead", "attrHeadFrag", page);
                            }

                            item.add(attrhead);
                            item.add(attributes);

                            attrhead.add(new Label("resource", propTO.getResource()));

                            attrhead.add(new Label("propagation", propTO.getStatus() == null
                                            ? "UNDEFINED" : propTO.getStatus().toString()));

                            final Image image;
                            final String alt, title;
                            final ModalWindow failureWindow = new ModalWindow("failureWindow");
                            final AjaxLink<?> failureWindowLink = new AjaxLink<Void>("showFailureWindow") {

                                private static final long serialVersionUID = -7978723352517770644L;

                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                    failureWindow.show(target);
                                }
                            };

                            switch (propTO.getStatus()) {

                                case SUCCESS:
                                case SUBMITTED:
                                case CREATED:
                                    image = new Image("icon", IMG_STATUSES + Status.ACTIVE.toString()
                                            + Constants.PNG_EXT);
                                    alt = "success icon";
                                    title = "success";
                                    failureWindow.setVisible(false);
                                    failureWindowLink.setEnabled(false);
                                    break;

                                default:
                                    image = new Image("icon", IMG_STATUSES + Status.SUSPENDED.toString()
                                            + Constants.PNG_EXT);
                                    alt = "failure icon";
                                    title = "failure";
                            }

                            image.add(new Behavior() {

                                private static final long serialVersionUID = 1469628524240283489L;

                                @Override
                                public void onComponentTag(final Component component, final ComponentTag tag) {
                                    tag.put("alt", alt);
                                    tag.put("title", title);
                                }
                            });
                            final FailureMessageModalPage executionFailureMessagePage;
                            if (propTO.getFailureReason() == null) {
                                executionFailureMessagePage =
                                new FailureMessageModalPage(failureWindow.getContentId(), StringUtils.EMPTY);
                            } else {
                                executionFailureMessagePage =
                                new FailureMessageModalPage(failureWindow.getContentId(), propTO.getFailureReason());
                            }

                            failureWindow.setPageCreator(new ModalWindow.PageCreator() {

                                private static final long serialVersionUID = -7834632442532690940L;

                                @Override
                                public Page createPage() {
                                    return executionFailureMessagePage;
                                }
                            });
                            failureWindow.setCookieName("failureWindow");
                            failureWindow.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
                            failureWindowLink.add(image);
                            attrhead.add(failureWindowLink);
                            attrhead.add(failureWindow);
                        }
                    };
            fragment.add(propRes);
        }

        final AjaxLink<Void> close = new IndicatingAjaxLink<Void>("close") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                if (mode == UserModalPage.Mode.SELF && anonymousUser.equals(SyncopeSession.get().getUsername())) {
                    SyncopeSession.get().invalidate();
                }
                builder.window.close(target);
            }
        };
        container.add(close);

        setOutputMarkupId(true);
    }

    /**
     * Get remote attributes list view.
     *
     * @param propTO propagation TO.
     * @return list view.
     */
    private ListView getConnObjectView(final PropagationStatusTO propTO) {
        final ConnObjectTO before = propTO.getBeforeObj();
        final ConnObjectTO after = propTO.getAfterObj();

        // sorted in reversed presentation order
        final List<String> head = new ArrayList<String>();
        if (attributable instanceof UserTO) {
            head.add(ConnIdSpecialAttributeName.PASSWORD);
            head.add(ConnIdSpecialAttributeName.ENABLE);
        }
        head.add(ConnIdSpecialAttributeName.UID);
        head.add(ConnIdSpecialAttributeName.NAME);

        final Map<String, AttributeTO> beforeAttrMap;
        if (before == null) {
            beforeAttrMap = Collections.<String, AttributeTO>emptyMap();
        } else {
            beforeAttrMap = before.getAttrMap();
        }

        final Map<String, AttributeTO> afterAttrMap;
        if (after == null) {
            afterAttrMap = Collections.<String, AttributeTO>emptyMap();
        } else {
            afterAttrMap = after.getAttrMap();
        }

        final Set<String> attributes = new HashSet<String>();
        attributes.addAll(beforeAttrMap.keySet());
        attributes.addAll(afterAttrMap.keySet());

        if (!(attributable instanceof UserTO)) {
            attributes.remove(ConnIdSpecialAttributeName.PASSWORD);
            attributes.remove(ConnIdSpecialAttributeName.ENABLE);
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

        return new ListView("attrs", profile) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem item) {
                String name = item.getModelObject().toString();

                final Fragment beforeValue;
                final Fragment afterValue;
                if (ConnIdSpecialAttributeName.ENABLE.equals(name)) {
                    beforeValue = getStatusIcon("beforeValue", propTO.getResource(), before);
                    afterValue = getStatusIcon("afterValue", propTO.getResource(), after);
                } else {
                    beforeValue = getLabelValue("beforeValue", name, beforeAttrMap);
                    afterValue = getLabelValue("afterValue", name, afterAttrMap);
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
    private Fragment getLabelValue(final String id, final String attrName, final Map<String, AttributeTO> attrMap) {
        final String value;

        final AttributeTO attr = attrMap.get(attrName);

        if (attr == null || attr.getValues() == null || attr.getValues().isEmpty()) {
            value = "";
        } else {
            if (ConnIdSpecialAttributeName.PASSWORD.equals(attrName)) {
                value = "********";
            } else {
                value = attr.getValues().size() > 1
                        ? attr.getValues().toString()
                        : attr.getValues().get(0);
            }
        }

        Component label = new Label("value", value).add(new Behavior() {

            private static final long serialVersionUID = 1469628524240283489L;

            @Override
            public void onComponentTag(final Component component, final ComponentTag tag) {
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
     * @param id component id to be replaced with the fragment content
     * @param resourceName resource name
     * @param objectTO connector object TO
     * @return fragment.
     */
    private Fragment getStatusIcon(final String id, final String resourceName, final ConnObjectTO objectTO) {
        final Image image;
        final String alt, title;
        switch (statusUtils.getStatusBean(
                attributable, resourceName, objectTO, this.attributable instanceof RoleTO).getStatus()) {

            case ACTIVE:
                image = new Image("status", IMG_STATUSES + Status.ACTIVE.toString()
                        + Constants.PNG_EXT);
                alt = "active icon";
                title = "Enabled";
                break;

            case SUSPENDED:
                image = new Image("status", IMG_STATUSES + Status.SUSPENDED.toString()
                        + Constants.PNG_EXT);
                alt = "inactive icon";
                title = "Disabled";
                break;

            default:
                image = null;
                alt = null;
                title = null;
        }

        final Fragment frag;
        if (image == null) {
            frag = new Fragment(id, "emptyFrag", this);
        } else {
            image.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("alt", alt);
                    tag.put("title", title);
                    tag.put("width", "12px");
                    tag.put("height", "12px");
                }
            });

            frag = new Fragment(id, "remoteStatusFrag", this);
            frag.add(image);
        }

        return frag;
    }
}
