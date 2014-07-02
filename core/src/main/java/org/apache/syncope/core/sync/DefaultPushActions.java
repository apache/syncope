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

import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.quartz.JobExecutionException;

/**
 * Default (empty) implementation of PushActions.
 */
public abstract class DefaultPushActions implements PushActions {

    @Override
    public void beforeAll(final SyncProfile<?, ?> profile) throws JobExecutionException {
    }

    @Override
    public <T extends AbstractAttributable> T beforeAssign(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeUpdate(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeProvision(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeLink(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeUnlink(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeUnassign(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeDeprovision(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> T beforeDelete(
            final SyncProfile<?, ?> profile,
            final T subject) throws JobExecutionException {
        return subject;
    }

    @Override
    public <T extends AbstractAttributable> void after(
            final SyncProfile<?, ?> profile,
            final T subject,
            SyncResult result) throws JobExecutionException {
    }

    @Override
    public void afterAll(
            final SyncProfile<?, ?> profile,
            final List<SyncResult> results)
            throws JobExecutionException {
    }
}
