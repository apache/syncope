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
package org.apache.syncope.core.provisioning.java.propagation;

import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.spring.implementation.InstanceScope;
import org.apache.syncope.core.spring.implementation.SyncopeImplementation;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is required during setup of an External Resource based on the ConnId
 * <a href="https://github.com/Tirasa/ConnIdGoogleAppsBundle">GoogleApps connector</a>.
 *
 * It ensures to send the configured e-mail address as {@code __NAME__}.
 */
@SyncopeImplementation(scope = InstanceScope.PER_CONTEXT)
public class GoogleAppsPropagationActions implements PropagationActions {

    protected static final Logger LOG = LoggerFactory.getLogger(GoogleAppsPropagationActions.class);

    protected String getEmailAttrName() {
        return "emails";
    }

    @Transactional
    @Override
    public void before(final PropagationTaskInfo taskInfo) {
        if (taskInfo.getOperation() == ResourceOperation.DELETE || taskInfo.getOperation() == ResourceOperation.NONE) {
            return;
        }
        if (AnyTypeKind.USER != taskInfo.getAnyTypeKind()) {
            return;
        }

        PropagationData data = taskInfo.getPropagationData();
        if (data.getAttributes() != null) {
            Set<Attribute> attrs = data.getAttributes();

            if (AttributeUtil.find(getEmailAttrName(), attrs) == null) {
                LOG.warn("Can't find {} to set as {} attribute value, skipping...", getEmailAttrName(), Name.NAME);
                return;
            }

            Optional.ofNullable(AttributeUtil.getNameFromAttributes(attrs)).ifPresent(attrs::remove);
            attrs.add(new Name(AttributeUtil.find(getEmailAttrName(), attrs).getValue().getFirst().toString()));
        }
    }
}
