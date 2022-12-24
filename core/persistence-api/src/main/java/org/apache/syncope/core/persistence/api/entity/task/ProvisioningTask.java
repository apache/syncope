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
package org.apache.syncope.core.persistence.api.entity.task;

import java.util.List;
import org.apache.syncope.common.lib.types.MatchingRule;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.common.lib.types.UnmatchingRule;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Implementation;

public interface ProvisioningTask<T extends SchedTask> extends SchedTask {

    ExternalResource getResource();

    void setResource(ExternalResource resource);

    boolean add(Implementation action);

    List<? extends Implementation> getActions();

    boolean isPerformCreate();

    void setPerformCreate(boolean performCreate);

    boolean isPerformDelete();

    void setPerformDelete(boolean performDelete);

    boolean isPerformUpdate();

    void setPerformUpdate(boolean performUpdate);

    boolean isSyncStatus();

    void setSyncStatus(boolean syncStatus);

    MatchingRule getMatchingRule();

    void setMatchingRule(MatchingRule matchigRule);

    UnmatchingRule getUnmatchingRule();

    void setUnmatchingRule(UnmatchingRule unmatchigRule);

    ThreadPoolSettings getConcurrentSettings();

    void setConcurrentSettings(ThreadPoolSettings settings);
}
