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
package org.apache.syncope.core.persistence.jpa;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("persistence")
public class PersistenceProperties {

    private String remoteCommitProvider = "sjvm";

    private String metaDataFactory;

    private String viewsXML = "classpath:views.xml";

    private String indexesXML = "classpath:indexes.xml";

    @NestedConfigurationProperty
    private final List<DomainProperties> domain = new ArrayList<>();

    public String getRemoteCommitProvider() {
        return remoteCommitProvider;
    }

    public void setRemoteCommitProvider(final String remoteCommitProvider) {
        this.remoteCommitProvider = remoteCommitProvider;
    }

    public String getMetaDataFactory() {
        return metaDataFactory;
    }

    public void setMetaDataFactory(final String metaDataFactory) {
        this.metaDataFactory = metaDataFactory;
    }

    public String getViewsXML() {
        return viewsXML;
    }

    public void setViewsXML(final String viewsXML) {
        this.viewsXML = viewsXML;
    }

    public String getIndexesXML() {
        return indexesXML;
    }

    public void setIndexesXML(final String indexesXML) {
        this.indexesXML = indexesXML;
    }

    public List<DomainProperties> getDomain() {
        return domain;
    }
}
