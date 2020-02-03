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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizardBuilder;
import org.apache.wicket.PageReference;

public abstract class BaseAjaxWizardBuilder<T extends Serializable> extends AjaxWizardBuilder<T> {

    private static final long serialVersionUID = -5729026353086171480L;

    public BaseAjaxWizardBuilder(final T defaultItem, final PageReference pageRef) {
        super(defaultItem, pageRef);
    }

    @Override
    protected long getMaxWaitTimeInSeconds() {
        return SyncopeWebApplication.get().getMaxWaitTimeInSeconds();
    }

    @Override
    protected void sendError(final Exception exception) {
        SyncopeConsoleSession.get().onException(exception);
    }

    @Override
    protected void sendWarning(final String message) {
        SyncopeConsoleSession.get().warn(message);
    }

    @Override
    protected Future<Pair<Serializable, Serializable>> execute(
            final Callable<Pair<Serializable, Serializable>> future) {

        return SyncopeConsoleSession.get().execute(future);
    }
}
