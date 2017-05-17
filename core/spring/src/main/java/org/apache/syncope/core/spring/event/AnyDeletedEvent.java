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
package org.apache.syncope.core.spring.event;

import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.context.ApplicationEvent;

public class AnyDeletedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 6389886937942135639L;

    private final AnyTypeKind anyTypeKind;

    private final String anyKey;

    private final String domain;

    public AnyDeletedEvent(final Object source, final AnyTypeKind anyTypeKind, final String anyKey) {
        super(source);
        this.anyTypeKind = anyTypeKind;
        this.anyKey = anyKey;
        this.domain = AuthContextUtils.getDomain();
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public String getAnyKey() {
        return anyKey;
    }

    public String getDomain() {
        return domain;
    }
}
