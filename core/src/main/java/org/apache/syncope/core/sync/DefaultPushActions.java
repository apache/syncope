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
package org.apache.syncope.core.sync;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.sync.impl.AbstractSyncopeResultHandler;
import org.identityconnectors.framework.common.objects.Attribute;
import org.quartz.JobExecutionException;

/**
 * Default (empty) implementation of PushActions.
 */
public abstract class DefaultPushActions implements PushActions {

    @Override
    public void beforeAll(final AbstractSyncopeResultHandler<?, ?> handler) throws JobExecutionException {
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeAssign(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUpdate(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {

        return delta;
    }

    @Override
    public <T extends AbstractAttributable> void after(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject,
            SyncResult result) throws JobExecutionException {
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeProvision(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeLink(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUnlink(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeUnassign(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeDeprovision(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public <T extends AbstractAttributable> Map.Entry<String, Set<Attribute>> beforeDelete(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final Map.Entry<String, Set<Attribute>> delta,
            final T subject) throws JobExecutionException {
        return delta;
    }

    @Override
    public void afterAll(
            final AbstractSyncopeResultHandler<?, ?> handler,
            final List<SyncResult> results)
            throws JobExecutionException {
    }
}
