/*
 * Copyright 2013 The Apache Software Foundation.
 *
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
package org.apache.syncope.common.to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.AbstractBaseBean;
import org.codehaus.jackson.annotate.JsonIgnore;

@XmlRootElement(name = "BulkActionRes")
@XmlType
public class BulkActionRes extends AbstractBaseBean {

    @XmlEnum
    @XmlType(name = "bulkActionStatus")
    public enum Status {

        SUCCESS,
        FAILURE

    }

    private List<Result> results;

    public void setResults(final List<Result> results) {
        this.results = results;
    }

    public List<Result> getResults() {
        return results;
    }

    @JsonIgnore
    public void add(final Object id, final Status status) {
        if (results == null) {
            results = new ArrayList<Result>();
        }

        if (id != null) {
            results.add(new Result(id.toString(), status));
        }
    }

    @JsonIgnore
    public Map<String, Status> getResultMap() {
        final Map<String, Status> res = new HashMap<String, Status>();
        if (results != null) {
            for (Result result : results) {
                res.put(result.getKey(), result.getValue());
            }
        }
        return res;
    }

    @JsonIgnore
    public List getResultByStatus(final Status status) {
        final List<String> res = new ArrayList<String>();
        if (results != null) {
            for (Result result : results) {
                if (result.getValue() == status) {
                    res.add(result.getKey());
                }
            }
        }
        return res;
    }

    public static class Result {

        private String key;

        private Status value;

        public Result() {
        }

        public Result(String key, Status value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Status getValue() {
            return value;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setValue(Status value) {
            this.value = value;
        }
    }
}
