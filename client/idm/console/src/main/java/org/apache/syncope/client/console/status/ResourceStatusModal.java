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
package org.apache.syncope.client.console.status;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.Model;

public class ResourceStatusModal extends StatusModal<ResourceTO> {

    private static final long serialVersionUID = 1066124171682570080L;

    private Model<String> typeModel = new Model<>();

    public ResourceStatusModal(
            final PageReference pageRef,
            final ResourceTO resource) {

        super(pageRef, resource, null, false);

        List<String> availableAnyTypes = resource.getProvisions().stream().
                map(Provision::getAnyType).
                sorted(AnyTypeRestClient.KEY_COMPARATOR).
                collect(Collectors.toList());

        AjaxDropDownChoicePanel<String> anyTypes =
                new AjaxDropDownChoicePanel<>("anyTypes", "anyTypes", typeModel, false);
        anyTypes.setChoices(availableAnyTypes);
        anyTypes.hideLabel();
        add(anyTypes);

        anyTypes.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                ResourceStatusDirectoryPanel.class.cast(directoryPanel).
                        updateResultTable(typeModel.getObject(), target);
            }
        });
    }

    @Override
    protected DirectoryPanel<
        StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?>> getStatusDirectoryPanel(
            final MultilevelPanel mlp,
            final PageReference pageReference,
            final ResourceTO entity,
            final String itemKeyFieldName,
            final boolean statusOnly) {

        return new ResourceStatusDirectoryPanel(mlp, pageReference, null, entity);
    }
}
