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

import java.util.HashSet;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.provisioning.api.propagation.PropagationActions;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
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
public class GoogleAppsPropagationActions implements PropagationActions {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleAppsPropagationActions.class);

    protected static String getEmailAttrName() {
        return "emails";
    }

    @Transactional
    @Override
    public void before(final PropagationTask task, final ConnectorObject beforeObj) {
        if (task.getOperation() == ResourceOperation.DELETE || task.getOperation() == ResourceOperation.NONE) {
            return;
        }
        if (AnyTypeKind.USER != task.getAnyTypeKind()) {
            return;
        }

        Set<Attribute> attrs = new HashSet<>(task.getAttributes());

        if (AttributeUtil.find(getEmailAttrName(), attrs) == null) {
            LOG.warn("Can't find {} to set as {} attribute value, skipping...", getEmailAttrName(), Name.NAME);
            return;
        }

        Name name = AttributeUtil.getNameFromAttributes(attrs);
        if (name != null) {
            attrs.remove(name);
        }
        attrs.add(new Name(AttributeUtil.find(getEmailAttrName(), attrs).getValue().get(0).toString()));

        task.setAttributes(attrs);
    }
}
