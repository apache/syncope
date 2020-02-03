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
package org.apache.syncope.fit.core.reference;

import java.util.Optional;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRule;
import org.apache.syncope.core.persistence.api.dao.PullCorrelationRuleConfClass;
import org.apache.syncope.core.persistence.api.dao.PullMatch;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@PullCorrelationRuleConfClass(LinkedAccountSamplePullCorrelationRuleConf.class)
public class LinkedAccountSamplePullCorrelationRule implements PullCorrelationRule {

    public static final String VIVALDI_KEY = "b3cbc78d-32e6-4bd4-92e0-bbe07566a2ee";

    @Autowired
    private UserDAO userDAO;

    @Override
    public SearchCond getSearchCond(final SyncDelta syncDelta, final Provision provision) {
        AttrCond cond = new AttrCond();

        Attribute email = syncDelta.getObject().getAttributeByName("email");
        if (email != null && !CollectionUtils.isEmpty(email.getValue())) {
            cond.setSchema("email");
            cond.setType(AttrCond.Type.EQ);
            cond.setExpression(email.getValue().get(0).toString());
        } else {
            cond.setSchema("");
        }

        return SearchCond.getLeaf(cond);
    }

    @Transactional(readOnly = true)
    @Override
    public PullMatch matching(final Any<?> any, final SyncDelta syncDelta, final Provision provision) {
        // if match with internal user vivaldi was found but firstName is different, update linked account
        // instead of updating user
        Attribute firstName = syncDelta.getObject().getAttributeByName("firstName");
        if (VIVALDI_KEY.equals(any.getKey())
                && firstName != null && !CollectionUtils.isEmpty(firstName.getValue())
                && !"Antonio".equals(firstName.getValue().get(0).toString())) {

            return new PullMatch(MatchType.LINKED_ACCOUNT, any);
        }

        return PullCorrelationRule.super.matching(any, syncDelta, provision);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<PullMatch> unmatching(final SyncDelta syncDelta, final Provision provision) {
        // if no match with internal user was found, link account to vivaldi instead of creating new user
        return Optional.of(new PullMatch(MatchType.LINKED_ACCOUNT, userDAO.find(VIVALDI_KEY)));
    }
}
