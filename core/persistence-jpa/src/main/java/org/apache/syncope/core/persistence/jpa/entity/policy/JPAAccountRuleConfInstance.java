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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAAccountRuleConfInstance.TABLE)
public class JPAAccountRuleConfInstance extends AbstractGeneratedKeyEntity {

    private static final long serialVersionUID = -2436055132955674610L;

    public static final String TABLE = "AccountRuleConfInstance";

    @Lob
    private String serializedInstance;

    @ManyToOne
    private JPAAccountPolicy accountPolicy;

    public AccountPolicy getAccountPolicy() {
        return accountPolicy;
    }

    public void setAccountPolicy(final AccountPolicy report) {
        checkType(report, JPAAccountPolicy.class);
        this.accountPolicy = (JPAAccountPolicy) report;
    }

    public AccountRuleConf getInstance() {
        return serializedInstance == null
                ? null
                : POJOHelper.deserialize(serializedInstance, AccountRuleConf.class);
    }

    public void setInstance(final AccountRuleConf instance) {
        this.serializedInstance = instance == null
                ? null
                : POJOHelper.serialize(instance);
    }
}
