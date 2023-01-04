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
package org.apache.syncope.core.rest.cxf;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.impl.api.local.ThreadClassLoaderManager;
import org.slf4j.MDC;

/**
 * Removes any known thread-local variable.
 */
public class ThreadLocalCleanupOutInterceptor extends AbstractPhaseInterceptor<Message> {

    public ThreadLocalCleanupOutInterceptor() {
        super(Phase.POST_INVOKE);
    }

    @Override
    public void handleMessage(final Message message) throws Fault {
        FormatUtils.clear();

        ThreadClassLoaderManager.clearInstance();
        CurrentLocale.clear();

        MDC.clear();
    }
}
