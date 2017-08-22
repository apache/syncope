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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ProvisionTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class ConnObjects extends Panel implements ModalPanel {

    private static final long serialVersionUID = -1143512993584984838L;

    private final AjaxDropDownChoicePanel<String> anyTypes;

    private final MultilevelPanel connObjects;

    public ConnObjects(final ResourceTO resource, final PageReference pageRef) {
        super(BaseModal.CONTENT_ID);

        List<String> availableAnyTypes = resource.getProvisions().stream().
                map(ProvisionTO::getAnyType).collect(Collectors.toList());
        Collections.sort(availableAnyTypes, new AnyTypeRestClient.AnyTypeKeyComparator());
        if (resource.getOrgUnit() != null) {
            availableAnyTypes.add(0, SyncopeConstants.REALM_ANYTYPE);
        }

        anyTypes = new AjaxDropDownChoicePanel<>("anyTypes", "anyTypes", new Model<>());
        anyTypes.setChoices(availableAnyTypes);
        anyTypes.hideLabel();
        anyTypes.setNullValid(false);
        if (availableAnyTypes.contains(AnyTypeKind.USER.name())) {
            anyTypes.setDefaultModelObject(AnyTypeKind.USER.name());
        } else if (availableAnyTypes.contains(AnyTypeKind.GROUP.name())) {
            anyTypes.setDefaultModelObject(AnyTypeKind.GROUP.name());
        } else if (!availableAnyTypes.isEmpty()) {
            anyTypes.setDefaultModelObject(availableAnyTypes.get(0));
        }
        add(anyTypes);

        connObjects = new MultilevelPanel("connObjects") {

            private static final long serialVersionUID = 1473786800290434002L;

            @Override
            protected void prev(final AjaxRequestTarget target) {
                anyTypes.setEnabled(true);
                target.add(anyTypes);

                super.prev(target);
            }

        };
        connObjects.setFirstLevel(new NextableConnObjectDirectoryPanel(
                connObjects, resource.getKey(), anyTypes.getField().getModelObject(), pageRef));
        connObjects.setOutputMarkupId(true);
        add(connObjects);

        anyTypes.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                connObjects.setFirstLevel(new NextableConnObjectDirectoryPanel(
                        connObjects, resource.getKey(), anyTypes.getField().getModelObject(), pageRef));
                target.add(connObjects);
            }
        });
    }

    private class NextableConnObjectDirectoryPanel extends ConnObjectListViewPanel {

        private static final long serialVersionUID = 956427874406567048L;

        NextableConnObjectDirectoryPanel(
                final MultilevelPanel multiLevelPanelRef,
                final String resource,
                final String anyType,
                final PageReference pageRef) {

            super(MultilevelPanel.FIRST_LEVEL_ID, resource, anyType, pageRef);
        }

        @Override
        protected void viewConnObject(final ConnObjectTO connObjectTO, final AjaxRequestTarget target) {
            anyTypes.setEnabled(false);
            target.add(anyTypes);

            connObjects.next(
                    new StringResourceModel("connObject.view", this, new Model<>(connObjectTO)).getObject(),
                    new ConnObjectDetails(connObjectTO),
                    target);
        }

    }
}
