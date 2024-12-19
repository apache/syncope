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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.common.lib.policy.InboundCorrelationRuleConf;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdMImplementationType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class IdMImplementationInfoProvider extends IdRepoImplementationInfoProvider {

    private static final long serialVersionUID = -5385695412826366167L;

    public IdMImplementationInfoProvider(
            final ClassPathScanImplementationLookup lookup,
            final ImplementationRestClient implementationRestClient) {

        super(lookup, implementationRestClient);
    }

    @Override
    public ViewMode getViewMode(final ImplementationTO implementation) {
        return IdMImplementationType.INBOUND_CORRELATION_RULE.equals(implementation.getType())
                || IdMImplementationType.PUSH_CORRELATION_RULE.equals(implementation.getType())
                ? ViewMode.JSON_BODY
                : super.getViewMode(implementation);
    }

    @Override
    public List<String> getClasses(final ImplementationTO implementation, final ViewMode viewMode) {
        List<String> classes = new ArrayList<>();
        if (viewMode == ViewMode.JSON_BODY && IdMImplementationType.values().containsKey(implementation.getType())) {
            switch (implementation.getType()) {
                case IdMImplementationType.INBOUND_CORRELATION_RULE:
                    classes = lookup.getClasses(InboundCorrelationRuleConf.class).stream().
                            map(Class::getName).collect(Collectors.toList());
                    break;

                case IdMImplementationType.PUSH_CORRELATION_RULE:
                    classes = lookup.getClasses(PushCorrelationRuleConf.class).stream().
                            map(Class::getName).collect(Collectors.toList());
                    break;

                default:
            }
            Collections.sort(classes);
        } else {
            classes = super.getClasses(implementation, viewMode);
        }

        return classes;
    }

    @Override
    public String getGroovyTemplateClassName(final String implementationType) {
        String templateClassName;

        switch (implementationType) {
            case IdMImplementationType.RECON_FILTER_BUILDER:
                templateClassName = "MyReconFilterBuilder";
                break;

            case IdMImplementationType.PROPAGATION_ACTIONS:
                templateClassName = "MyPropagationActions";
                break;

            case IdMImplementationType.INBOUND_ACTIONS:
                templateClassName = "MyInboundActions";
                break;

            case IdMImplementationType.PUSH_ACTIONS:
                templateClassName = "MyPushActions";
                break;

            case IdMImplementationType.INBOUND_CORRELATION_RULE:
                templateClassName = "MyInboundCorrelationRule";
                break;

            case IdMImplementationType.PUSH_CORRELATION_RULE:
                templateClassName = "MyPushCorrelationRule";
                break;

            case IdMImplementationType.PROVISION_SORTER:
                templateClassName = "MyProvisionSorter";
                break;

            default:
                templateClassName = super.getGroovyTemplateClassName(implementationType);
        }

        return templateClassName;
    }

    @Override
    public Class<?> getClass(final String implementationType, final String name) {
        Class<?> clazz;
        switch (implementationType) {
            case IdMImplementationType.INBOUND_CORRELATION_RULE:
                clazz = lookup.getClasses(InboundCorrelationRuleConf.class).stream().
                        filter(c -> c.getName().equals(name)).findFirst().orElse(null);
                break;

            case IdMImplementationType.PUSH_CORRELATION_RULE:
                clazz = lookup.getClasses(PushCorrelationRuleConf.class).stream().
                        filter(c -> c.getName().equals(name)).findFirst().orElse(null);
                break;

            default:
                clazz = super.getClass(implementationType, name);
        }

        return clazz;
    }

    @Override
    public IModel<List<String>> getReconFilterBuilders() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.RECON_FILTER_BUILDER).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };
    }

    @Override
    public IModel<List<String>> getLiveSyncDeltaMappers() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.LIVE_SYNC_DELTA_MAPPER).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };
    }

    @Override
    public IModel<List<String>> getInboundActions() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.INBOUND_ACTIONS).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };
    }

    @Override
    public IModel<List<String>> getPushActions() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdMImplementationType.PUSH_ACTIONS).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };
    }
}
