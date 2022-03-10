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
package org.apache.syncope.core.workflow.java;

import java.time.OffsetDateTime;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.spring.security.AuthContextUtils;

public abstract class AbstractWorkflowAdapter {

    protected void metadata(final Any<?> any, final String username, final String context) {
        String who = username
                + AuthContextUtils.getDelegatedBy().map(d -> " [delegated by " + d + "]").orElse(StringUtils.EMPTY);
        OffsetDateTime now = OffsetDateTime.now();

        if (any.getCreationDate() == null) {
            any.setCreationDate(now);
            any.setCreator(who);
            any.setCreationContext(context);
        }

        any.setLastModifier(who);
        any.setLastChangeDate(now);
        any.setLastChangeContext(context);
    }
}
