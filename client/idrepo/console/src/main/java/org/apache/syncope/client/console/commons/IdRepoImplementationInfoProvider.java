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
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.common.lib.policy.AccountRuleConf;
import org.apache.syncope.common.lib.policy.PasswordRuleConf;
import org.apache.syncope.common.lib.report.ReportConf;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

public class IdRepoImplementationInfoProvider implements ImplementationInfoProvider {

    private static final long serialVersionUID = -6620368595630782392L;

    protected final ClassPathScanImplementationLookup lookup;

    protected final ImplementationRestClient implementationRestClient;

    public IdRepoImplementationInfoProvider(
            final ClassPathScanImplementationLookup lookup,
            final ImplementationRestClient implementationRestClient) {

        this.lookup = lookup;
        this.implementationRestClient = implementationRestClient;
    }

    @Override
    public ViewMode getViewMode(final ImplementationTO implementation) {
        return implementation.getEngine() == ImplementationEngine.GROOVY
                ? ViewMode.GROOVY_BODY
                : IdRepoImplementationType.REPORT_DELEGATE.equals(implementation.getType())
                || IdRepoImplementationType.ACCOUNT_RULE.equals(implementation.getType())
                || IdRepoImplementationType.PASSWORD_RULE.equals(implementation.getType())
                ? ViewMode.JSON_BODY
                : ViewMode.JAVA_CLASS;
    }

    @Override
    public List<String> getClasses(final ImplementationTO implementation, final ViewMode viewMode) {
        List<String> classes = List.of();
        if (viewMode == ViewMode.JAVA_CLASS) {
            classes = SyncopeConsoleSession.get().getPlatformInfo().getJavaImplInfo(implementation.getType()).
                    map(javaImplInfo -> new ArrayList<>(javaImplInfo.getClasses())).orElseGet(ArrayList::new);
        } else if (viewMode == ViewMode.JSON_BODY) {
            switch (implementation.getType()) {
                case IdRepoImplementationType.REPORT_DELEGATE:
                    classes = lookup.getClasses(ReportConf.class).stream().
                            map(Class::getName).collect(Collectors.toList());
                    break;

                case IdRepoImplementationType.ACCOUNT_RULE:
                    classes = lookup.getClasses(AccountRuleConf.class).stream().
                            map(Class::getName).collect(Collectors.toList());
                    break;

                case IdRepoImplementationType.PASSWORD_RULE:
                    classes = lookup.getClasses(PasswordRuleConf.class).stream().
                            map(Class::getName).collect(Collectors.toList());
                    break;

                default:
            }
        }
        if (!classes.isEmpty()) {
            Collections.sort(classes);
        }

        return classes;
    }

    @Override
    public String getGroovyTemplateClassName(final String implementationType) {
        String templateClassName = null;

        switch (implementationType) {
            case IdRepoImplementationType.ACCOUNT_RULE:
                templateClassName = "MyAccountRule";
                break;

            case IdRepoImplementationType.PASSWORD_RULE:
                templateClassName = "MyPasswordRule";
                break;

            case IdRepoImplementationType.TASKJOB_DELEGATE:
                templateClassName = "MySchedTaskJobDelegate";
                break;

            case IdRepoImplementationType.REPORT_DELEGATE:
                templateClassName = "MyReportJobDelegate";
                break;

            case IdRepoImplementationType.LOGIC_ACTIONS:
                templateClassName = "MyLogicActions";
                break;

            case IdRepoImplementationType.MACRO_ACTIONS:
                templateClassName = "MyMacroActions";
                break;

            case IdRepoImplementationType.ATTR_VALUE_VALIDATOR:
                templateClassName = "MyAttrValueValidator";
                break;

            case IdRepoImplementationType.RECIPIENTS_PROVIDER:
                templateClassName = "MyRecipientsProvider";
                break;

            case IdRepoImplementationType.ITEM_TRANSFORMER:
                templateClassName = "MyItemTransformer";
                break;

            case IdRepoImplementationType.COMMAND:
                templateClassName = "MyCommand";
                break;

            default:
        }

        return templateClassName;
    }

    @Override
    public Class<?> getClass(final String implementationType, final String name) {
        Class<?> clazz = null;
        switch (implementationType) {
            case IdRepoImplementationType.REPORT_DELEGATE:
                clazz = lookup.getClasses(ReportConf.class).stream().
                        filter(c -> c.getName().equals(name)).findFirst().orElse(null);
                break;

            case IdRepoImplementationType.ACCOUNT_RULE:
                clazz = lookup.getClasses(AccountRuleConf.class).stream().
                        filter(c -> c.getName().equals(name)).findFirst().orElse(null);
                break;

            case IdRepoImplementationType.PASSWORD_RULE:
                clazz = lookup.getClasses(PasswordRuleConf.class).stream().
                        filter(c -> c.getName().equals(name)).findFirst().orElse(null);
                break;

            default:
        }

        return clazz;
    }

    @Override
    public IModel<List<String>> getTaskJobDelegates() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdRepoImplementationType.TASKJOB_DELEGATE).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };
    }

    @Override
    public IModel<List<String>> getReportJobDelegates() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdRepoImplementationType.REPORT_DELEGATE).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };
    }

    @Override
    public IModel<List<String>> getReconFilterBuilders() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return List.of();
            }
        };
    }

    @Override
    public IModel<List<String>> getLiveSyncDeltaMappers() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return List.of();
            }
        };
    }

    @Override
    public IModel<List<String>> getMacroActions() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdRepoImplementationType.MACRO_ACTIONS).stream().
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
                return List.of();
            }
        };
    }

    @Override
    public IModel<List<String>> getPushActions() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                return List.of();
            }
        };
    }
}
