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
package org.apache.syncope.core.persistence.jpa.entity.policy;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.apache.syncope.common.lib.policy.PullPolicySpec;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;

@Entity
@Table(name = JPAPullPolicy.TABLE)
public class JPAPullPolicy extends AbstractPolicy implements PullPolicy {

    private static final long serialVersionUID = -6090413855809521279L;

    public static final String TABLE = "PullPolicy";

    @Lob
    private String specification;

    @Override
    public PullPolicySpec getSpecification() {
        return POJOHelper.deserialize(specification, PullPolicySpec.class);
    }

    @Override
    public void setSpecification(final PullPolicySpec policy) {
        this.specification = POJOHelper.serialize(policy);
    }
}
