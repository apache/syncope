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
package org.syncope.core.persistence.beans;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import org.syncope.client.report.ReportletConf;
import org.syncope.client.util.XMLSerializer;

@Entity
public class ReportletConfInstance extends AbstractBaseBean {

    private static final long serialVersionUID = -2436055132955674610L;

    @Id
    private Long id;

    @Lob
    private String serializedInstance;

    @ManyToOne
    private Report report;

    public Long getId() {
        return id;
    }

    public Report getReport() {
        return report;
    }

    public void setReport(final Report report) {
        this.report = report;
    }

    public ReportletConf getInstance() {
        return serializedInstance == null
                ? null
                : XMLSerializer.<ReportletConf>deserialize(serializedInstance);
    }

    public void setInstance(final ReportletConf instance) {
        serializedInstance = instance == null
                ? null
                : XMLSerializer.serialize(instance);
    }
}
