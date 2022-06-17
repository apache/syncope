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
package org.apache.syncope.core.provisioning.api.data;

import java.util.Map;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.provisioning.api.PropagationByResource;

public interface GroupDataBinder {

    GroupTO getGroupTO(String key);

    TypeExtensionTO getTypeExtensionTO(TypeExtension typeExt);

    GroupTO getGroupTO(Group group, boolean details);

    void create(Group group, GroupCR groupCR);

    PropagationByResource<String> update(Group group, GroupUR groupUR);

    /**
     * Finds any objects having resources assigned exclusively because of memberships of the given group.
     *
     * @param groupKey group key
     * @return map containing pairs with any object key and operations to be performed on those resources (DELETE,
     * typically).
     */
    Map<String, PropagationByResource<String>> findAnyObjectsWithTransitiveResources(String groupKey);

    /**
     * Finds users having resources assigned exclusively because of memberships of the given group.
     *
     * @param groupKey group key
     * @return map containing pairs with user key and operations to be performed on those resources (DELETE,
     * typically).
     */
    Map<String, PropagationByResource<String>> findUsersWithTransitiveResources(String groupKey);
}
