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
package org.apache.syncope.client.console.policies;

import java.io.Serializable;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wizards.AttrWizardBuilder;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;

public class AccessPolicyAttrsWizardBuilder extends AttrWizardBuilder {

    private static final long serialVersionUID = 33625775269155L;

    private final AccessPolicyTO accessPolicy;

    private final AccessPolicyAttrsDirectoryPanel.AttrsAccessor attrsAccessor;

    public AccessPolicyAttrsWizardBuilder(
            final AccessPolicyTO accessPolicy,
            final AccessPolicyAttrsDirectoryPanel.AttrsAccessor attrsAccessor,
            final Attr attr,
            final PageReference pageRef) {

        super(attr, pageRef);
        this.accessPolicy = accessPolicy;
        this.attrsAccessor = attrsAccessor;
    }

    @Override
    protected Serializable onApplyInternal(final Attr modelObject) {
        attrsAccessor.apply(accessPolicy.getConf()).removeIf(p -> modelObject.getSchema().equals(p.getSchema()));
        attrsAccessor.apply(accessPolicy.getConf()).add(modelObject);

        PolicyRestClient.update(PolicyType.ACCESS, accessPolicy);

        return null;
    }
}
