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

import java.io.OutputStream;
import java.util.List;
import org.apache.syncope.common.lib.to.WorkflowDefinitionTO;

public interface WorkflowDefinitionAdapter {

    /**
     * @return all available workflow definitions.
     */
    List<WorkflowDefinitionTO> getDefinitions();

    /**
     * Export the workflow definition for the given key, in the requested format.
     *
     * @param key definition key
     * @param format export format
     * @param os export stream
     */
    void exportDefinition(String key, WorkflowDefinitionFormat format, OutputStream os);

    /**
     * Export the workflow graphical representation for the given key (if available).
     *
     * @param key definition key
     * @param os export stream
     */
    void exportDiagram(String key, OutputStream os);

    /**
     * Import the workflow definition for the given key.
     *
     * @param key definition key
     * @param format import format
     * @param definition definition
     */
    void importDefinition(String key, WorkflowDefinitionFormat format, String definition);

    /**
     * Remove the workflow definition for the given key.
     *
     * @param key definition key
     */
    void deleteDefinition(String key);
}
