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
package org.apache.syncope.core.provisioning.camel.producer;

import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultProducer;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;

public abstract class AbstractProducer extends DefaultProducer {

    private final AnyTypeKind anyTypeKind;

    private PropagationManager propagationManager;

    private PropagationTaskExecutor taskExecutor;

    private boolean pull;

    private String executor;

    public AbstractProducer(final Endpoint endpoint, final AnyTypeKind anyTypeKind) {
        super(endpoint);
        this.anyTypeKind = anyTypeKind;
    }

    public void setPropagationManager(final PropagationManager propagationManager) {
        this.propagationManager = propagationManager;
    }

    public PropagationManager getPropagationManager() {
        return propagationManager;
    }

    public void setPropagationTaskExecutor(final PropagationTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    public PropagationTaskExecutor getPropagationTaskExecutor() {
        return taskExecutor;
    }

    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public boolean isPull() {
        return pull;
    }

    public void setPull(final boolean pull) {
        this.pull = pull;
    }

    public String getExecutor() {
        return executor;
    }

    public void setExecutor(final String executor) {
        this.executor = executor;
    }
}
