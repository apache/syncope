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
package org.apache.syncope.client.console;

import java.util.List;
import org.apache.syncope.client.console.commons.AccessPolicyConfProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.AnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.AnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.ExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdRepoAccessPolicyConfProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyDirectoryPanelAdditionalActionLinksProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyDirectoryPanelAdditionalActionsProvider;
import org.apache.syncope.client.console.commons.IdRepoAnyWizardBuilderAdditionalSteps;
import org.apache.syncope.client.console.commons.IdRepoExternalResourceProvider;
import org.apache.syncope.client.console.commons.IdRepoImplementationInfoProvider;
import org.apache.syncope.client.console.commons.IdRepoPolicyTabProvider;
import org.apache.syncope.client.console.commons.IdRepoRealmPolicyProvider;
import org.apache.syncope.client.console.commons.IdRepoStatusProvider;
import org.apache.syncope.client.console.commons.IdRepoVirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.commons.ImplementationInfoProvider;
import org.apache.syncope.client.console.commons.PolicyTabProvider;
import org.apache.syncope.client.console.commons.PreviewUtils;
import org.apache.syncope.client.console.commons.RealmPolicyProvider;
import org.apache.syncope.client.console.commons.StatusProvider;
import org.apache.syncope.client.console.commons.VirSchemaDetailsPanelProvider;
import org.apache.syncope.client.console.init.ClassPathScanImplementationContributor;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.rest.AccessTokenRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.AuditRestClient;
import org.apache.syncope.client.console.rest.CommandRestClient;
import org.apache.syncope.client.console.rest.DelegationRestClient;
import org.apache.syncope.client.console.rest.DynRealmRestClient;
import org.apache.syncope.client.console.rest.FIQLQueryRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.LoggerConf;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.RelationshipTypeRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.rest.SecurityQuestionRestClient;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.rest.UserSelfRestClient;
import org.apache.syncope.client.ui.commons.MIMETypesLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdRepoConsoleContext {

    @ConditionalOnMissingBean
    @Bean
    public ClassPathScanImplementationLookup classPathScanImplementationLookup(
            final ConsoleProperties props,
            final List<ClassPathScanImplementationContributor> classPathScanImplementationContributors) {

        ClassPathScanImplementationLookup lookup =
                new ClassPathScanImplementationLookup(classPathScanImplementationContributors, props);
        lookup.load();
        return lookup;
    }

    @ConditionalOnMissingBean
    @Bean
    public MIMETypesLoader mimeTypesLoader() {
        MIMETypesLoader mimeTypesLoader = new MIMETypesLoader();
        mimeTypesLoader.load();
        return mimeTypesLoader;
    }

    @ConditionalOnMissingBean
    @Bean
    public PreviewUtils previewUtils(final ClassPathScanImplementationLookup lookup) {
        return new PreviewUtils(lookup);
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceProvider resourceProvider() {
        return new IdRepoExternalResourceProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyDirectoryPanelAdditionalActionsProvider anyDirectoryPanelAdditionalActionsProvider() {
        return new IdRepoAnyDirectoryPanelAdditionalActionsProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyDirectoryPanelAdditionalActionLinksProvider anyDirectoryPanelAdditionalActionLinksProvider() {
        return new IdRepoAnyDirectoryPanelAdditionalActionLinksProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyWizardBuilderAdditionalSteps anyWizardBuilderAdditionalSteps() {
        return new IdRepoAnyWizardBuilderAdditionalSteps();
    }

    @ConditionalOnMissingBean
    @Bean
    public StatusProvider statusProvider() {
        return new IdRepoStatusProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaDetailsPanelProvider virSchemaDetailsPanelProvider() {
        return new IdRepoVirSchemaDetailsPanelProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationInfoProvider implementationInfoProvider(
            final ClassPathScanImplementationLookup lookup,
            final ImplementationRestClient implementationRestClient) {

        return new IdRepoImplementationInfoProvider(lookup, implementationRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmPolicyProvider realmPolicyProvider(final PolicyRestClient policyRestClient) {
        return new IdRepoRealmPolicyProvider(policyRestClient);
    }

    @Bean
    public PolicyTabProvider idRepoPolicyTabProvider(final PolicyRestClient policyRestClient) {
        return new IdRepoPolicyTabProvider(policyRestClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessPolicyConfProvider accessPolicyConfProvider() {
        return new IdRepoAccessPolicyConfProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenRestClient accessTokenRestClient() {
        return new AccessTokenRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectRestClient anyObjectRestClient() {
        return new AnyObjectRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassRestClient anyTypeClassRestClient() {
        return new AnyTypeClassRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeRestClient anyTypeRestClient() {
        return new AnyTypeRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditRestClient auditRestClient() {
        return new AuditRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public CommandRestClient commandRestClient() {
        return new CommandRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationRestClient delegationRestClient() {
        return new DelegationRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmRestClient dynRealmRestClient() {
        return new DynRealmRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryRestClient fiqlQueryRestClient() {
        return new FIQLQueryRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupRestClient groupRestClient() {
        return new GroupRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationRestClient implementationRestClient() {
        return new ImplementationRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public LoggerConf loggerConf() {
        return new LoggerConf();
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationRestClient notificationRestClient() {
        return new NotificationRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyRestClient policyRestClient() {
        return new PolicyRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmRestClient realmRestClient() {
        return new RealmRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeRestClient relationshipTypeRestClient() {
        return new RelationshipTypeRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportRestClient reportRestClient() {
        return new ReportRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleRestClient roleRestClient() {
        return new RoleRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SchemaRestClient schemaRestClient() {
        return new SchemaRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionRestClient securityQuestionRestClient() {
        return new SecurityQuestionRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeRestClient syncopeRestClient() {
        return new SyncopeRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskRestClient taskRestClient() {
        return new TaskRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRestClient userRestClient() {
        return new UserRestClient();
    }

    @ConditionalOnMissingBean
    @Bean
    public UserSelfRestClient userSelfRestClient() {
        return new UserSelfRestClient();
    }
}
