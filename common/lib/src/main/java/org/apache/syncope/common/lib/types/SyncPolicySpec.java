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
package org.apache.syncope.common.lib.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;

@XmlType
public class SyncPolicySpec implements PolicySpec {

    private static final long serialVersionUID = -3144027171719498127L;

    private final List<SyncPolicySpecItem> items = new ArrayList<>();

    /**
     * Conflict resolution action.
     */
    private ConflictResolutionAction conflictResolutionAction;

    public ConflictResolutionAction getConflictResolutionAction() {
        return conflictResolutionAction == null
                ? ConflictResolutionAction.IGNORE
                : conflictResolutionAction;
    }

    public void setConflictResolutionAction(final ConflictResolutionAction conflictResolutionAction) {
        this.conflictResolutionAction = conflictResolutionAction;
    }

    public SyncPolicySpecItem getItem(final String anyTypeKey) {
        return CollectionUtils.find(items, new Predicate<SyncPolicySpecItem>() {

            @Override
            public boolean evaluate(final SyncPolicySpecItem item) {
                return anyTypeKey != null && anyTypeKey.equals(item.getAnyTypeKey());
            }
        });
    }

    @XmlElementWrapper(name = "items")
    @XmlElement(name = "item")
    @JsonProperty("items")
    public List<SyncPolicySpecItem> getItems() {
        return items;
    }
}
