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

import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.apache.syncope.core.provisioning.api.pushpull.PullCorrelationRule;

/**
 * Test pull rule relying on <tt>email</tt> attribute value.
 */
public class TestPullRule implements PullCorrelationRule {

    @Override
    public SearchCond getSearchCond(final ConnectorObject connObj) {
        AttributeCond cond = new AttributeCond();
        cond.setSchema("email");
        cond.setType(AttributeCond.Type.EQ);
        cond.setExpression(connObj.getName().getNameValue());

        return SearchCond.getLeafCond(cond);
    }
}
