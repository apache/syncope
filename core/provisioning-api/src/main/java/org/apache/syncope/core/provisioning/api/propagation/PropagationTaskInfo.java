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
package org.apache.syncope.core.provisioning.api.propagation;

import java.util.Optional;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.identityconnectors.framework.common.objects.ConnectorObject;

public class PropagationTaskInfo extends PropagationTaskTO {

    private static final long serialVersionUID = -2879861567335503099L;

    /**
     * Object on External Resource before propagation takes place.
     *
     * null: beforeObj was not attempted to read
     * not null, but not present: beforeObj was attempted to read, but not found
     * not null and present: beforeObj value is available
     */
    private Optional<ConnectorObject> beforeObj;

    public Optional<ConnectorObject> getBeforeObj() {
        return beforeObj;
    }

    public void setBeforeObj(final Optional<ConnectorObject> beforeObj) {
        this.beforeObj = beforeObj;
    }
}
