/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.client.report;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Interface for all elements that can be embedded in a report.
 *
 * @see org.syncope.core.persistence.beans.Report
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
include = JsonTypeInfo.As.PROPERTY, property = "@class")
public interface Reportlet {

    /**
     * Give name of this reportlet instance.
     *
     * @return name of this reportlet instance
     */
    String getName();

    /**
     * Actual data extraction for reporting.
     *
     * @param handler SAX content handler for streaming result
     * @throws SAXException if anything goes wrong
     */
    void extract(ContentHandler handler)
            throws SAXException;
}
