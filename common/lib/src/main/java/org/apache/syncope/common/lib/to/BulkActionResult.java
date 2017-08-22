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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.jaxb.XmlGenericMapAdapter;

@XmlRootElement(name = "bulkActionResult")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(BulkActionResult.Status.class)
public class BulkActionResult extends AbstractBaseBean {

    private static final long serialVersionUID = 2868894178821778133L;

    @XmlEnum
    @XmlType(name = "bulkActionStatus")
    public enum Status {

        // general bulk action result statuses
        SUCCESS,
        FAILURE,
        // specific propagation task execution statuses
        CREATED,
        NOT_ATTEMPTED;

    }

    @XmlJavaTypeAdapter(XmlGenericMapAdapter.class)
    @JsonIgnore
    private final Map<String, Status> results = new HashMap<>();

    @JsonProperty
    public Map<String, Status> getResults() {
        return results;
    }

    @JsonIgnore
    public List<String> getResultByStatus(final Status status) {
        final List<String> result = new ArrayList<>();

        results.entrySet().stream().
                filter((entry) -> (entry.getValue() == status)).
                forEachOrdered(entry -> result.add(entry.getKey()));

        return Collections.unmodifiableList(result);
    }
}
