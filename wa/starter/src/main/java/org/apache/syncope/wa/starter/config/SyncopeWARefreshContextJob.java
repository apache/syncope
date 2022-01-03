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
package org.apache.syncope.wa.starter.config;

import org.apache.syncope.wa.bootstrap.WARestClient;
import org.apache.commons.lang.StringUtils;
import org.apereo.cas.support.saml.idp.metadata.generator.SamlIdPMetadataGenerator;
import org.apereo.cas.support.saml.services.idp.metadata.SamlIdPMetadataDocument;
import org.apereo.cas.util.AsciiArtUtils;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import java.util.Optional;
import org.quartz.JobExecutionException;

public class SyncopeWARefreshContextJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(SyncopeWARefreshContextJob.class);

    @Autowired
    private ContextRefresher contextRefresher;

    @Autowired
    private SamlIdPMetadataGenerator metadataGenerator;

    public SyncopeWARefreshContextJob() {
    }

    @Override
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            LOG.debug("Attempting to refresh WA application context");
            if (!WARestClient.isReady()) {
                LOG.debug("Syncope client is not yet ready");
                throw new IllegalStateException("Syncope core is not yet ready to access requests");
            }
            contextRefresher.refresh();
            LOG.info("Refreshed application context to bootstrap property sources, etc...");

            LOG.info("Generating SAML2 IdP metadata metadata");
            SamlIdPMetadataDocument document = metadataGenerator.generate(Optional.empty());
            LOG.info("Generated SAML2 IdP metadata for {}", document.getAppliesTo());

            advertiseReady();
        } catch (final Exception e) {
            throw new JobExecutionException("While generating SAML2 IdP metadata", e);
        }
    }

    private static void advertiseReady() {
        AsciiArtUtils.printAsciiArtReady(LOG, StringUtils.EMPTY);
        LOG.info("Ready to process requests");
    }
}
