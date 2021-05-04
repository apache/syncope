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

import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAAccessPolicy.TABLE)
public class JPAAccessPolicy extends AbstractPolicy implements AccessPolicy {

    private static final long serialVersionUID = -4190607009908888884L;

    public static final String TABLE = "AccessPolicy";

    @Basic
    private Boolean enabled = true;

    @Basic
    private Boolean ssoEnabled = true;

    @Lob
    private String jsonConf;

    @Override
    public boolean isEnabled() {
        return BooleanUtils.isNotFalse(enabled);
    }

    @Override
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isSsoEnabled() {
        return BooleanUtils.isNotFalse(ssoEnabled);
    }

    @Override
    public void setSsoEnabled(final boolean ssoEnabled) {
        this.ssoEnabled = ssoEnabled;
    }

    @Override
    public AccessPolicyConf getConf() {
        return jsonConf == null
                ? null
                : POJOHelper.deserialize(jsonConf, AccessPolicyConf.class);
    }

    @Override
    public void setConf(final AccessPolicyConf conf) {
        jsonConf = Optional.ofNullable(conf).map(POJOHelper::serialize).orElse(null);
    }
}
