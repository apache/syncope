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
package org.apache.syncope.core.persistence.jpa.entity;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;

@Entity
@Table(name = JPAReportTemplate.TABLE)
@Cacheable
public class JPAReportTemplate extends AbstractProvidedKeyEntity implements ReportTemplate {

    private static final long serialVersionUID = 7755855927366231089L;

    public static final String TABLE = "ReportTemplate";

    @Lob
    private String foTemplate;

    @Lob
    private String csvTemplate;

    @Lob
    private String htmlTemplate;

    @Override
    public String getFOTemplate() {
        return foTemplate;
    }

    @Override
    public void setFOTemplate(final String foTemplate) {
        this.foTemplate = foTemplate;
    }

    @Override
    public String getCSVTemplate() {
        return csvTemplate;
    }

    @Override
    public void setCSVTemplate(final String csvTemplate) {
        this.csvTemplate = csvTemplate;
    }

    @Override
    public String getHTMLTemplate() {
        return htmlTemplate;
    }

    @Override
    public void setHTMLTemplate(final String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

}
