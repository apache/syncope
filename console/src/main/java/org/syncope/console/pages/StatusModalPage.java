/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console.pages;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Check;
import org.apache.wicket.markup.html.form.CheckGroup;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.UserTO;
import org.syncope.console.rest.ResourceRestClient;
import org.syncope.console.rest.UserRestClient;
import org.syncope.types.IntMappingType;

public class StatusModalPage extends BaseModalPage {

    private static final long serialVersionUID = 4114026480146090961L;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private ResourceRestClient resourceRestClient;

    private enum STATUS {

        ACTIVE,
        SUSPENDED,
        UNDEFINED,
        USER_NOT_FOUND
    }

    final List<String> selectedResources = new ArrayList<String>();

    public StatusModalPage(
            final PageReference callerPageRef,
            final ModalWindow window,
            final UserTO userTO) {

        super();

        final Map<String, STATUS> statuses = getRemoteStatuses(userTO);

        final Form form = new Form("form");
        add(form);

        final List<String> externalResources = new ArrayList<String>();
        externalResources.add("Syncope");
        externalResources.addAll(userTO.getResources());

        final CheckGroup selections = new CheckGroup(
                "selections", new PropertyModel(this, "selectedResources"));
        form.add(selections);

        final ListView<String> resources = new ListView<String>(
                "resources", externalResources) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Check("check", item.getModel()));
                item.add(new Label("name", new ResourceModel(
                        item.getModelObject(), item.getModelObject())));

                final Image image;
                final String alt, title;

                switch ("Syncope".equals(item.getModelObject())
                        ? STATUS.valueOf(userTO.getStatus().toUpperCase())
                        : statuses.get(item.getModelObject())) {
                    case ACTIVE:
                        image = new Image("icon", "statuses/active.png");
                        alt = "active icon";
                        title = "Enabled";
                        break;
                    case UNDEFINED:
                        image = new Image("icon", "statuses/undefined.png");
                        item.setEnabled(false);
                        alt = "undefined icon";
                        title = "Undefined status";
                        break;
                    case USER_NOT_FOUND:
                        image = new Image("icon", "statuses/usernotfound.png");
                        item.setEnabled(false);
                        alt = "notfound icon";
                        title = "User not found";
                        break;
                    default:
                        image = new Image("icon", "statuses/inactive.png");
                        alt = "inactive icon";
                        title = "Disabled";
                }

                image.add(new Behavior() {

                    private static final long serialVersionUID = 1469628524240283489L;

                    @Override
                    public void onComponentTag(
                            Component component, ComponentTag tag) {
                        tag.put("alt", alt);
                        tag.put("title", title);
                    }
                });

                item.add(image);
            }
        };

        selections.add(resources);

        final AjaxButton disable = new IndicatingAjaxButton(
                "disable", new ResourceModel("disable", "Disable")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target,
                    final Form form) {

                userRestClient.suspend(userTO.getId(), selectedResources);

                if (callerPageRef.getPage() instanceof BasePage) {
                    ((BasePage) callerPageRef.getPage()).setModalResult(true);
                }

                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        final AjaxButton enable = new IndicatingAjaxButton(
                "enable", new ResourceModel("enable", "Enable")) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(
                    final AjaxRequestTarget target,
                    final Form form) {

                userRestClient.reactivate(userTO.getId(), selectedResources);

                ((BasePage) callerPageRef.getPage()).setModalResult(true);

                window.close(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        form.add(disable);
        form.add(enable);
    }

    private Map<String, STATUS> getRemoteStatuses(final UserTO userTO) {
        Map<String, STATUS> statuses = new HashMap<String, STATUS>();

        for (String res : userTO.getResources()) {
            ResourceTO resourceTO = resourceRestClient.read(res);
            Map.Entry<IntMappingType, String> accountId = getAccountId(
                    resourceTO);

            String objectId = null;

            switch (accountId != null ? accountId.getKey() : IntMappingType.SyncopeUserId) {
                case SyncopeUserId:
                    objectId = String.valueOf(userTO.getId());
                    break;
                case Username:
                    objectId = userTO.getUsername();
                    break;
                case UserSchema:
                    AttributeTO attributeTO = null;
                    attributeTO =
                            userTO.getAttributeMap().get(accountId.getValue());
                    objectId =
                            attributeTO != null
                            && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0) : null;
                    break;
                case UserDerivedSchema:
                    attributeTO = userTO.getDerivedAttributeMap().
                            get(accountId.getValue());
                    objectId =
                            attributeTO != null
                            && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0) : null;
                    break;
                case UserVirtualSchema:
                    attributeTO = userTO.getVirtualAttributeMap().
                            get(accountId.getValue());
                    objectId =
                            attributeTO != null
                            && attributeTO.getValues() != null
                            && !attributeTO.getValues().isEmpty()
                            ? attributeTO.getValues().get(0) : null;
                    break;
                default:
            }

            STATUS status;

            try {
                final ConnObjectTO objectTO =
                        userRestClient.getRemoteObject(res, objectId);

                final Boolean enabled = isEnabled(objectTO);

                status = enabled == null
                        ? STATUS.UNDEFINED
                        : enabled ? STATUS.ACTIVE : STATUS.SUSPENDED;

            } catch (Exception e) {
                LOG.warn("User '{}' not found on resource '{}'",
                        objectId, res);

                status = STATUS.USER_NOT_FOUND;
            }

            statuses.put(res, status);
        }

        return statuses;
    }

    private Boolean isEnabled(final ConnObjectTO objectTO) {
        final String STATUSATTR = "__ENABLE__";

        final Map<String, AttributeTO> attributeTOs = objectTO.getAttributeMap();
        final AttributeTO status = attributeTOs.get(STATUSATTR);

        return status != null && status.getValues() != null
                && !status.getValues().isEmpty()
                ? Boolean.parseBoolean(status.getValues().get(0)) : null;
    }

    private Map.Entry<IntMappingType, String> getAccountId(
            final ResourceTO resourceTO) {
        Map.Entry<IntMappingType, String> accountId = null;

        for (SchemaMappingTO mapping : resourceTO.getMappings()) {
            if (mapping.isAccountid()) {
                accountId = new AbstractMap.SimpleEntry<IntMappingType, String>(
                        mapping.getIntMappingType(),
                        mapping.getIntAttrName());
            }
        }

        return accountId;
    }
}
