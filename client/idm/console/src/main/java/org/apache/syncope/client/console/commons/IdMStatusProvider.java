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
package org.apache.syncope.client.console.commons;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.status.ReconStatusUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;

public class IdMStatusProvider implements StatusProvider {

    private static final long serialVersionUID = 1875374599950896631L;

    @Override
    public List<Triple<ConnObjectTO, ConnObjectWrapper, String>> get(
            final AnyTO any, final Collection<String> resources) {

        return ReconStatusUtils.getReconStatuses(
                AnyTypeKind.fromTOClass(any.getClass()), any.getKey(), any.getResources()).stream().
                map(status -> Triple.<ConnObjectTO, ConnObjectWrapper, String>of(
                status.getRight().getOnSyncope(),
                new ConnObjectWrapper(any, status.getLeft(), status.getRight().getOnResource()),
                null)).
                collect(Collectors.toList());
    }
}
