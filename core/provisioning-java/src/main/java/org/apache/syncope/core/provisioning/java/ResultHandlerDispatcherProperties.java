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
package org.apache.syncope.core.provisioning.java;

import org.apache.syncope.core.provisioning.java.pushpull.SyncopeResultHandlerDispatcher;

public class ResultHandlerDispatcherProperties {

    /**
     * Maximum time to wait for a free slot before rejecting a new handler task.
     */
    private int submitTimeoutSeconds = SyncopeResultHandlerDispatcher.DEFAULT_SUBMIT_TIMEOUT_SECONDS;

    /**
     * Maximum overall time to wait for the submitted handler tasks to complete.
     */
    private int completionTimeoutSeconds = SyncopeResultHandlerDispatcher.DEFAULT_COMPLETION_TIMEOUT_SECONDS;

    public int getSubmitTimeoutSeconds() {
        return submitTimeoutSeconds;
    }

    public void setSubmitTimeoutSeconds(final int submitTimeoutSeconds) {
        this.submitTimeoutSeconds = submitTimeoutSeconds;
    }

    public int getCompletionTimeoutSeconds() {
        return completionTimeoutSeconds;
    }

    public void setCompletionTimeoutSeconds(final int completionTimeoutSeconds) {
        this.completionTimeoutSeconds = completionTimeoutSeconds;
    }
}
