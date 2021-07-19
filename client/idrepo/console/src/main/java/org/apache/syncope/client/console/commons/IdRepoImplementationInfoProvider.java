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
import org.apache.syncope.common.lib.report.ReportletConf;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.beans.factory.annotation.Autowired;

public class IdRepoImplementationInfoProvider implements ImplementationInfoProvider {

    private static final long serialVersionUID = -6620368595630782392L;

    @Autowired
    protected ClassPathScanImplementationLookup lookup;

    @Override
    public ViewMode getViewMode(final ImplementationTO implementation) {
        return implementation.getEngine() == ImplementationEngine.GROOVY
                ? ViewMode.GROOVY_BODY
                : IdRepoImplementationType.REPORTLET.equals(implementation.getType())
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
                case IdRepoImplementationType.REPORTLET:
                    classes = lookup.getClasses(ReportletConf.class).stream().
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
            case IdRepoImplementationType.REPORTLET:
                templateClassName = "MyReportlet";
                break;

            case IdRepoImplementationType.ACCOUNT_RULE:
                templateClassName = "MyAccountRule";
                break;

            case IdRepoImplementationType.PASSWORD_RULE:
                templateClassName = "MyPasswordRule";
                break;

            case IdRepoImplementationType.TASKJOB_DELEGATE:
                templateClassName = "MySchedTaskJobDelegate";
                break;

            case IdRepoImplementationType.LOGIC_ACTIONS:
                templateClassName = "MyLogicActions";
                break;

            case IdRepoImplementationType.VALIDATOR:
                templateClassName = "MyValidator";
                break;

            case IdRepoImplementationType.RECIPIENTS_PROVIDER:
                templateClassName = "MyRecipientsProvider";
                break;

            case IdRepoImplementationType.ITEM_TRANSFORMER:
                templateClassName = "MyItemTransformer";
                break;

            default:
        }

        return templateClassName;
    }

    @Override
    public Class<?> getClass(final String implementationType, final String name) {
        Class<?> clazz = null;
        switch (implementationType) {
            case IdRepoImplementationType.REPORTLET:
                clazz = lookup.getClasses(ReportletConf.class).stream().
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
                return ImplementationRestClient.list(IdRepoImplementationType.TASKJOB_DELEGATE).stream().
                    map(EntityTO::getKey).sorted().collect(Collectors.toList());
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
    public IModel<List<String>> getPullActions() {
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
