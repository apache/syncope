/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.client.console.wizards.any;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.util.ListModel;
import org.springframework.beans.BeanUtils;

public class GroupWizardBuilder extends AnyWizardBuilder<GroupTO> {

    private static final long serialVersionUID = 5945391813567245081L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    /**
     * Construct.
     *
     * @param id The component id
     * @param groupTO any
     * @param anyTypeClasses any type classes
     * @param pageRef Caller page reference.
     */
    public GroupWizardBuilder(
            final String id, final GroupTO groupTO, final List<String> anyTypeClasses, final PageReference pageRef) {
        super(id, groupTO, anyTypeClasses, pageRef);
    }

    /**
     * This method has been overridden to manage asynchronous translation of FIQL string to search clases list and
     * viceversa.
     *
     * @param item wizard backend item.
     * @return the current builder.
     */
    @Override
    public AjaxWizardBuilder<GroupTO> setItem(final GroupTO item) {
        final GroupTO actual = new GroupHandler();
        BeanUtils.copyProperties(item == null ? getDefaultItem() : item, actual);
        return super.setItem(actual);
    }

    @Override
    protected void onApplyInternal(final GroupTO modelObject) {
        final ProvisioningResult<GroupTO> actual;

        GroupTO toBeProcessed = modelObject instanceof GroupHandler
                ? GroupHandler.class.cast(modelObject).toGroupTO()
                : modelObject;

        if (modelObject.getKey() == 0) {
            actual = groupRestClient.create(toBeProcessed);
        } else {
            final GroupPatch patch = AnyOperations.diff(toBeProcessed, getOriginalItem(), true);

            // update user just if it is changed
            if (!patch.isEmpty()) {
                actual = groupRestClient.update(getOriginalItem().getETagValue(), patch);
            }
        }
    }

    @Override
    protected GroupWizardBuilder addOptionalDetailsPanel(final GroupTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new GroupDetails(modelObject,
                new ListModel<>(Collections.<StatusBean>emptyList()), false, pageRef, modelObject.getKey() > 0));
        return this;
    }

    /**
     * Class tprovided to handle asychronous FIQL string translations.
     */
    public static class GroupHandler extends GroupTO {

        private static final long serialVersionUID = 8058288034211558375L;

        private List<SearchClause> uDynClauses;

        private Map<String, List<SearchClause>> aDynClauses;

        public List<SearchClause> getUDynClauses() {
            if (this.uDynClauses == null) {
                this.uDynClauses = SearchUtils.getSearchClauses(super.getUDynMembershipCond());
            }
            return this.uDynClauses;
        }

        public void setUDynClauses(final List<SearchClause> uDynClauses) {
            this.uDynClauses = uDynClauses;
        }

        public Map<String, List<SearchClause>> getADynClauses() {
            if (this.aDynClauses == null) {
                this.aDynClauses = SearchUtils.getSearchClauses(super.getADynMembershipConds());
            }
            return this.aDynClauses;
        }

        public void setADynClauses(final Map<String, List<SearchClause>> aDynClauses) {
            this.aDynClauses = aDynClauses;
        }

        @Override
        public String getUDynMembershipCond() {
            if (CollectionUtils.isEmpty(this.uDynClauses)) {
                return super.getUDynMembershipCond();
            } else {
                return getFIQLString(this.uDynClauses, SyncopeClient.getUserSearchConditionBuilder());
            }
        }

        @Override
        public Map<String, String> getADynMembershipConds() {
            if (this.aDynClauses == null || this.aDynClauses.isEmpty()) {
                return super.getADynMembershipConds();
            } else {
                final Map<String, String> res = new HashMap<>();

                for (Map.Entry<String, List<SearchClause>> entry : this.aDynClauses.entrySet()) {
                    res.put(entry.getKey(), getFIQLString(entry.getValue(),
                            SyncopeClient.getAnyObjectSearchConditionBuilder(entry.getKey())));
                }

                return res;
            }
        }

        private String getFIQLString(final List<SearchClause> clauses, final AbstractFiqlSearchConditionBuilder bld) {
            return SearchUtils.buildFIQL(clauses, bld);
        }

        public GroupTO toGroupTO() {
            final GroupTO res = new GroupTO();
            BeanUtils.copyProperties(this, res, "uDynClauses", "aDynClauses");
            res.setUDynMembershipCond(this.getUDynMembershipCond());
            res.getADynMembershipConds().putAll(this.getADynMembershipConds());
            return res;
        }
    }
}
