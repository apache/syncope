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
package org.apache.syncope.core.logic;

import java.util.Collection;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;

public abstract class AbstractResourceAssociator<A extends AnyTO> extends AbstractLogic<A> {

    public abstract A unlink(String key, Collection<String> resources);

    public abstract A link(String key, Collection<String> resources);

    public abstract ProvisioningResult<A> unassign(
            String key, Collection<String> resources, boolean nullPriorityAsync);

    public abstract ProvisioningResult<A> assign(
            String key, Collection<String> resources, boolean changepwd, String password, boolean nullPriorityAsync);

    public abstract ProvisioningResult<A> deprovision(
            String key, Collection<String> resources, boolean nullPriorityAsync);

    public abstract ProvisioningResult<A> provision(
            String key, Collection<String> resources, boolean changepwd, String password, boolean nullPriorityAsync);
}
