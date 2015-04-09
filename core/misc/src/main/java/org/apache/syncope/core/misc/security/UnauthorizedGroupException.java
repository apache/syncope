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
package org.apache.syncope.core.misc.security;

import java.util.Collection;
import java.util.Collections;

public class UnauthorizedGroupException extends RuntimeException {

    private static final long serialVersionUID = 7540587364235915081L;

    private final Collection<Long> groupIds;

    public UnauthorizedGroupException(final Collection<Long> groupIds) {
        super("Missing entitlement for group(s) " + groupIds);
        this.groupIds = groupIds;
    }

    public UnauthorizedGroupException(final Long groupId) {
        this(Collections.singleton(groupId));
    }

    public Collection<Long> getGroupIds() {
        return groupIds;
    }
}
