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
package org.apache.syncope.core.propagation.impl;

import java.util.List;
import org.apache.syncope.client.to.PropagationTO;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.propagation.PropagationHandler;
import org.apache.syncope.types.PropagationTaskExecStatus;
import org.identityconnectors.framework.common.objects.ConnectorObject;

public class DefaultPropagationHandler implements PropagationHandler {

    private final ConnObjectUtil connObjectUtil;

    private final List<PropagationTO> propagations;

    public DefaultPropagationHandler(final ConnObjectUtil connObjectUtil, final List<PropagationTO> propagations) {
        this.connObjectUtil = connObjectUtil;
        this.propagations = propagations;
    }

    @Override
    public void handle(final String resourceName, final PropagationTaskExecStatus executionStatus,
            final ConnectorObject beforeObj, final ConnectorObject afterObj) {

        final PropagationTO propagation = new PropagationTO();
        propagation.setResourceName(resourceName);
        propagation.setStatus(executionStatus);

        if (beforeObj != null) {
            propagation.setBeforeObj(connObjectUtil.getConnObjectTO(beforeObj));
        }

        if (afterObj != null) {
            propagation.setAfterObj(connObjectUtil.getConnObjectTO(afterObj));
        }

        propagations.add(propagation);
    }
}
