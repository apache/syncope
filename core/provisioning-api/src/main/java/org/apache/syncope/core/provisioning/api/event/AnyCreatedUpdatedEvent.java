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
package org.apache.syncope.core.provisioning.api.event;

import org.apache.syncope.core.persistence.api.entity.Any;
import org.springframework.context.ApplicationEvent;

public class AnyCreatedUpdatedEvent<A extends Any<?>> extends ApplicationEvent {

    private static final long serialVersionUID = -781747175059834365L;

    private final A any;

    private final String domain;

    public AnyCreatedUpdatedEvent(final Object source, final A any, final String domain) {
        super(source);
        this.any = any;
        this.domain = domain;
    }

    public A getAny() {
        return any;
    }

    public String getDomain() {
        return domain;
    }

}
