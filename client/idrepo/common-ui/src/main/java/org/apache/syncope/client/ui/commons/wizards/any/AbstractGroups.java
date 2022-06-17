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
package org.apache.syncope.client.ui.commons.wizards.any;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

public abstract class AbstractGroups extends WizardStep implements ICondition {

    private static final long serialVersionUID = 552437609667518888L;

    protected static final int MAX_GROUP_LIST_CARDINALITY = 30;

    protected final AnyTO anyTO;

    protected WebMarkupContainer dyngroupsContainer;

    protected WebMarkupContainer dynrealmsContainer;

    protected WebMarkupContainer groupsContainer;

    public <T extends AnyTO> AbstractGroups(final AnyWrapper<T> modelObject) {
        super();
        this.anyTO = modelObject.getInnerObject();

        setOutputMarkupId(true);

        groupsContainer = new WebMarkupContainer("groupsContainer");
        groupsContainer.setOutputMarkupId(true);
        groupsContainer.setOutputMarkupPlaceholderTag(true);
        add(groupsContainer);

        // ------------------
        // insert changed label if needed
        // ------------------
        if (modelObject instanceof UserWrapper
                && UserWrapper.class.cast(modelObject).getPreviousUserTO() != null
                && !ListUtils.isEqualList(
                        UserWrapper.class.cast(modelObject).getInnerObject().getMemberships(),
                        UserWrapper.class.cast(modelObject).getPreviousUserTO().getMemberships())) {
            groupsContainer.add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            groupsContainer.add(new Label("changed", StringUtils.EMPTY));
        }
        // ------------------
    }

    protected abstract void addGroupsPanel();

    protected abstract void addDynamicRealmsContainer();

    protected abstract void addDynamicGroupsContainer();

    @Override
    public boolean evaluate() {
        return true;
    }
}
