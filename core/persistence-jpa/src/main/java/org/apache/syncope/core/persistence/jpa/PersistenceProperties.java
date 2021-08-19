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
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyObjectDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnySearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAuditConfDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAGroupDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrValueDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainSchemaDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPAEntityFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("persistence")
public class PersistenceProperties {

    private String remoteCommitProvider = "sjvm";

    private String metaDataFactory;

    private Class<? extends EntityFactory> entityFactory = JPAEntityFactory.class;

    private Class<? extends PlainSchemaDAO> plainSchemaDAO = JPAPlainSchemaDAO.class;

    private Class<? extends PlainAttrDAO> plainAttrDAO = JPAPlainAttrDAO.class;

    private Class<? extends PlainAttrValueDAO> plainAttrValueDAO = JPAPlainAttrValueDAO.class;

    private Class<? extends AnySearchDAO> anySearchDAO = JPAAnySearchDAO.class;

    private Class<? extends UserDAO> userDAO = JPAUserDAO.class;

    private Class<? extends GroupDAO> groupDAO = JPAGroupDAO.class;

    private Class<? extends AnyObjectDAO> anyObjectDAO = JPAAnyObjectDAO.class;

    private Class<? extends AuditConfDAO> auditConfDAO = JPAAuditConfDAO.class;

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

    public Class<? extends EntityFactory> getEntityFactory() {
        return entityFactory;
    }

    public void setEntityFactory(final Class<? extends EntityFactory> entityFactory) {
        this.entityFactory = entityFactory;
    }

    public Class<? extends PlainSchemaDAO> getPlainSchemaDAO() {
        return plainSchemaDAO;
    }

    public void setPlainSchemaDAO(final Class<? extends PlainSchemaDAO> plainSchemaDAO) {
        this.plainSchemaDAO = plainSchemaDAO;
    }

    public Class<? extends PlainAttrDAO> getPlainAttrDAO() {
        return plainAttrDAO;
    }

    public void setPlainAttrDAO(final Class<? extends PlainAttrDAO> plainAttrDAO) {
        this.plainAttrDAO = plainAttrDAO;
    }

    public Class<? extends PlainAttrValueDAO> getPlainAttrValueDAO() {
        return plainAttrValueDAO;
    }

    public void setPlainAttrValueDAO(final Class<? extends PlainAttrValueDAO> plainAttrValueDAO) {
        this.plainAttrValueDAO = plainAttrValueDAO;
    }

    public Class<? extends AnySearchDAO> getAnySearchDAO() {
        return anySearchDAO;
    }

    public void setAnySearchDAO(final Class<? extends AnySearchDAO> anySearchDAO) {
        this.anySearchDAO = anySearchDAO;
    }

    public Class<? extends UserDAO> getUserDAO() {
        return userDAO;
    }

    public void setUserDAO(final Class<? extends UserDAO> userDAO) {
        this.userDAO = userDAO;
    }

    public Class<? extends GroupDAO> getGroupDAO() {
        return groupDAO;
    }

    public void setGroupDAO(final Class<? extends GroupDAO> groupDAO) {
        this.groupDAO = groupDAO;
    }

    public Class<? extends AnyObjectDAO> getAnyObjectDAO() {
        return anyObjectDAO;
    }

    public void setAnyObjectDAO(final Class<? extends AnyObjectDAO> anyObjectDAO) {
        this.anyObjectDAO = anyObjectDAO;
    }

    public Class<? extends AuditConfDAO> getAuditConfDAO() {
        return auditConfDAO;
    }

    public void setAuditConfDAO(final Class<? extends AuditConfDAO> auditConfDAO) {
        this.auditConfDAO = auditConfDAO;
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
