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
package org.apache.syncope.core.report;

import org.apache.syncope.report.ReportletConf;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Interface for all elements that can be embedded in a report.
 *
 * @see org.apache.syncope.core.persistence.beans.Report
 */
public interface Reportlet<T extends ReportletConf> {

    /**
     * Set this reportlet configuration.
     *
     * @param conf configuration
     */
    void setConf(T conf);

    /**
     * Actual data extraction for reporting.
     *
     * @param handler SAX content handler for streaming result
     * @throws SAXException if there is any problem in SAX handling
     * @throws ReportException if anything goes wrong
     */
    void extract(ContentHandler handler) throws SAXException, ReportException;
}
