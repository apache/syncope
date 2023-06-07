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
package org.apache.syncope.client.console.commons;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.PolicyRenderer;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.markup.html.form.AbstractSingleSelectChoice;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class IdRepoRealmPolicyProvider implements RealmPolicyProvider {

    private static final long serialVersionUID = 2933826125264961282L;

    protected final PolicyRestClient policyRestClient;

    public IdRepoRealmPolicyProvider(final PolicyRestClient policyRestClient) {
        this.policyRestClient = policyRestClient;
    }

    @Override
    public void add(final RealmTO realmTO, final RepeatingView view) {
        Map<String, String> accountPolicies = policyRestClient.list(PolicyType.ACCOUNT).stream().
                collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        AjaxDropDownChoicePanel<String> accountPolicy = new AjaxDropDownChoicePanel<>(
                view.newChildId(),
                new ResourceModel("accountPolicy", "accountPolicy").getObject(),
                new PropertyModel<>(realmTO, "accountPolicy"),
                false);
        accountPolicy.setChoiceRenderer(new PolicyRenderer(accountPolicies));
        accountPolicy.setChoices(new ArrayList<>(accountPolicies.keySet()));
        ((AbstractSingleSelectChoice<?>) accountPolicy.getField()).setNullValid(true);
        view.add(accountPolicy);

        Map<String, String> passwordPolicies = policyRestClient.list(PolicyType.PASSWORD).stream().
                collect(Collectors.toMap(PolicyTO::getKey, PolicyTO::getName, (v1, v2) -> v1, LinkedHashMap::new));
        AjaxDropDownChoicePanel<String> passwordPolicy = new AjaxDropDownChoicePanel<>(
                view.newChildId(),
                new ResourceModel("passwordPolicy", "passwordPolicy").getObject(),
                new PropertyModel<>(realmTO, "passwordPolicy"),
                false);
        passwordPolicy.setChoiceRenderer(new PolicyRenderer(passwordPolicies));
        passwordPolicy.setChoices(new ArrayList<>(passwordPolicies.keySet()));
        ((AbstractSingleSelectChoice<?>) passwordPolicy.getField()).setNullValid(true);
        view.add(passwordPolicy);
    }
}
