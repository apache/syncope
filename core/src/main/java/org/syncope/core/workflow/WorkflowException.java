/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.workflow;

/**
 * Wrapper for all workflow related exceptions. Original exceptions will depend
 * on UserWorkflowAdapter implementation.
 *
 * @see UserWorkflowAdapter
 */
public class WorkflowException extends Exception {

    /**
     * Generated serialVersionUID.
     */
    private static final long serialVersionUID = -6261173250078013869L;

    /**
     * Return a new instance wrapping the original workflow exception.
     *
     * @param cause original workflow exception
     */
    public WorkflowException(final Throwable cause) {
        super(cause);
    }
}
