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
package org.apache.syncope.core.workflow.api;

/**
 * Wrapper for all workflow related exceptions. Original exceptions will depend on UserWorkflowAdapter implementation.
 *
 * @see UserWorkflowAdapter
 */
public class WorkflowException extends RuntimeException {

    private static final long serialVersionUID = -6261173250078013869L;

    public WorkflowException(final String message) {
        super(message);
    }

    /**
     * Return a new instance wrapping the original workflow exception.
     *
     * @param cause original workflow exception
     */
    public WorkflowException(final Throwable cause) {
        super(cause);
    }

    /**
     * Return a new instance wrapping the original workflow exception, additionally providing a local message.
     *
     * @param message local message
     * @param cause original workflow exception
     */
    public WorkflowException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
