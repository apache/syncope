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
package org.apache.syncope.client.console.init;

import java.util.Optional;
import org.apache.syncope.common.lib.attr.AttrRepoConf;
import org.apache.syncope.common.lib.auth.AuthModuleConf;
import org.apache.syncope.common.lib.clientapps.UsernameAttributeProviderConf;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.AuthPolicyConf;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

public class AMClassPathScanImplementationContributor implements ClassPathScanImplementationContributor {

    private static final long serialVersionUID = 2493303413513242525L;

    @Override
    public void extend(final ClassPathScanningCandidateComponentProvider scanner) {
        scanner.addIncludeFilter(new AssignableTypeFilter(AuthModuleConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AttrRepoConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AccessPolicyConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AttrReleasePolicyConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(AuthPolicyConf.class));
        scanner.addIncludeFilter(new AssignableTypeFilter(UsernameAttributeProviderConf.class));
    }

    @Override
    public Optional<String> getLabel(final Class<?> clazz) {
        if (AuthModuleConf.class.isAssignableFrom(clazz)) {
            return Optional.of(AuthModuleConf.class.getName());
        }
        if (AttrRepoConf.class.isAssignableFrom(clazz)) {
            return Optional.of(AttrRepoConf.class.getName());
        }
        if (AccessPolicyConf.class.isAssignableFrom(clazz)) {
            return Optional.of(AccessPolicyConf.class.getName());
        }
        if (AttrReleasePolicyConf.class.isAssignableFrom(clazz)) {
            return Optional.of(AttrReleasePolicyConf.class.getName());
        }
        if (AuthPolicyConf.class.isAssignableFrom(clazz)) {
            return Optional.of(AuthPolicyConf.class.getName());
        }
        if (UsernameAttributeProviderConf.class.isAssignableFrom(clazz)) {
            return Optional.of(UsernameAttributeProviderConf.class.getName());
        }
        return Optional.empty();
    }
}
