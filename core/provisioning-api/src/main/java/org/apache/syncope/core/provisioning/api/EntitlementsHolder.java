/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * License); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * AS IS BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.syncope.common.lib.types.AnyEntitlement;

public final class EntitlementsHolder {

    private static final Object MONITOR = new Object();

    private static EntitlementsHolder INSTANCE;

    public static EntitlementsHolder getInstance() {
        synchronized (MONITOR) {
            if (INSTANCE == null) {
                INSTANCE = new EntitlementsHolder();
            }
        }
        return INSTANCE;
    }

    private final Set<String> values = Collections.synchronizedSet(new HashSet<String>());

    private EntitlementsHolder() {
        // private constructor for singleton
    }

    public void init(final Collection<String> values) {
        this.values.addAll(values);
    }

    public void addFor(final String anyType) {
        for (AnyEntitlement operation : AnyEntitlement.values()) {
            this.values.add(operation.getFor(anyType));
        }
    }

    public void removeFor(final String anyType) {
        for (AnyEntitlement operation : AnyEntitlement.values()) {
            this.values.remove(operation.getFor(anyType));
        }
    }

    public Set<String> getValues() {
        return Collections.unmodifiableSet(values);
    }
}
