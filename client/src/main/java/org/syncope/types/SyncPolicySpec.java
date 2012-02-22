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
package org.syncope.types;

import java.util.ArrayList;
import java.util.List;
import org.syncope.client.SchemaList;

public class SyncPolicySpec extends AbstractPolicySpec {

    private static final long serialVersionUID = -3144027171719498127L;

    /**
     * SyncopeUsers attributes and user schemas used to disambiguate.
     */
    @SchemaList(extended = true)
    private List<String> alternativeSearchAttrs;

    /**
     * Conflict resolution action.
     */
    private ConflictResolutionAction conflictResolutionAction;

    public ConflictResolutionAction getConflictResolutionAction() {
        if (conflictResolutionAction == null) {
            return conflictResolutionAction.IGNORE;
        } else {
            return conflictResolutionAction;
        }
    }

    public void setConflictResolutionAction(
            final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    public List<String> getAlternativeSearchAttrs() {
        if (alternativeSearchAttrs == null) {
            alternativeSearchAttrs = new ArrayList<String>();
        }
        return alternativeSearchAttrs;
    }

    public void setAlternativeSearchAttrs(
            final List<String> alternativeSearchAttrs) {
        this.alternativeSearchAttrs = alternativeSearchAttrs;
    }
}
