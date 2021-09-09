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

import javax.sql.DataSource;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.SyncopeCoreLoader;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;

public class DomainIndexLoader implements SyncopeCoreLoader {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainIndexLoader.class);

    protected final ElasticsearchIndexManager indexManager;

    public DomainIndexLoader(final ElasticsearchIndexManager indexManager) {
        this.indexManager = indexManager;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void load(final String domain, final DataSource datasource) {
        try {
            if (!indexManager.existsIndex(domain, AnyTypeKind.USER)) {
                indexManager.createIndex(domain, AnyTypeKind.USER,
                        indexManager.defaultSettings(), indexManager.defaultMapping());
            }
            if (!indexManager.existsIndex(domain, AnyTypeKind.GROUP)) {
                indexManager.createIndex(domain, AnyTypeKind.GROUP,
                        indexManager.defaultSettings(), indexManager.defaultMapping());
            }
            if (!indexManager.existsIndex(domain, AnyTypeKind.ANY_OBJECT)) {
                indexManager.createIndex(domain, AnyTypeKind.ANY_OBJECT,
                        indexManager.defaultSettings(), indexManager.defaultMapping());
            }
        } catch (Exception e) {
            LOG.error("While creating index for domain {}", domain, e);
        }
    }
}
