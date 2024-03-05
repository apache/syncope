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
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.syncope.core.provisioning.java.job.Job;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.slf4j.MDC;

/**
 * Populate MDC with sensible information, for the current thread.
 *
 * MDC is then cleared up by {@link org.apache.syncope.core.rest.cxf.ThreadLocalCleanupOutInterceptor}
 */
public class MDCInInterceptor extends AbstractPhaseInterceptor<Message> {

    // just same value as org.apache.cxf.ext.logging.event.LogEvent.KEY_EXCHANGE_ID
    protected static final String KEY_EXCHANGE_ID = "exchangeId";

    public MDCInInterceptor() {
        super(Phase.PRE_INVOKE);
    }

    @Override
    public void handleMessage(final Message message) throws Fault {
        Exchange exchange = message.getExchange();

        // this ensures we are not duplicating nor conflicting with
        // org.apache.cxf.ext.logging.AbstractLoggingInterceptor.createExchangeId(Message)
        String exchangeId = (String) exchange.get(KEY_EXCHANGE_ID);
        if (exchangeId == null) {
            exchangeId = SecureRandomUtils.generateRandomUUID().toString();
            exchange.put(KEY_EXCHANGE_ID, exchangeId);
        }

        MDC.put(Job.OPERATION_ID, exchangeId);
    }
}
