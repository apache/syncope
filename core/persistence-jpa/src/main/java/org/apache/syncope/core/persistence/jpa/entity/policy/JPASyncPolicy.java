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
import org.apache.syncope.common.lib.policy.SyncPolicySpec;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.misc.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.policy.SyncPolicy;

@Entity
@Table(name = JPASyncPolicy.TABLE)
public class JPASyncPolicy extends AbstractPolicy implements SyncPolicy {

    private static final long serialVersionUID = -6090413855809521279L;

    public static final String TABLE = "SyncPolicy";

    @Lob
    private String specification;

    public JPASyncPolicy() {
        super();
        this.type = PolicyType.SYNC;
    }

    @Override
    public SyncPolicySpec getSpecification() {
        return POJOHelper.deserialize(specification, SyncPolicySpec.class);
    }

    @Override
    public void setSpecification(final SyncPolicySpec policy) {
        this.specification = POJOHelper.serialize(policy);
    }
}
