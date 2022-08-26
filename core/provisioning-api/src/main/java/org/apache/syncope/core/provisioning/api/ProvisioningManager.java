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
package org.apache.syncope.core.provisioning.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.PropagationStatus;

public interface ProvisioningManager<C extends AnyCR, U extends AnyUR> {

    Pair<String, List<PropagationStatus>> create(
            C anyCR, boolean nullPriorityAsync, String creator, String context);

    Pair<U, List<PropagationStatus>> update(
            U anyUR, Set<String> excludedResources, boolean nullPriorityAsync, String updater, String context);

    String unlink(U anyUR, String updater, String context);

    String link(U anyUR, String updater, String context);

    List<PropagationStatus> deprovision(
            String anyKey, Collection<String> resources, boolean nullPriorityAsync, String executor);

    List<PropagationStatus> delete(String anyKey, boolean nullPriorityAsync, String eraser, String context);

    List<PropagationStatus> delete(
            String anyKey, Set<String> excludedResources, boolean nullPriorityAsync, String eraser, String context);
}
