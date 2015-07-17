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
package org.apache.syncope.core.persistence.jpa.slice;

import java.util.List;
import org.apache.openjpa.slice.FinderTargetPolicy;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.misc.security.AuthContextUtils;
import org.apache.syncope.core.persistence.jpa.entity.JPADomain;

public class DomainFinderTargetPolicy implements FinderTargetPolicy {

    @Override
    public String[] getTargets(final Class<?> cls, final Object oid, final List<String> slices, final Object context) {
        return new String[] {
            JPADomain.class.equals(cls)
            ? SyncopeConstants.MASTER_DOMAIN
            : AuthContextUtils.getDomain()
        };
    }

}
