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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "camelMetrics")
@XmlType
public class CamelMetrics extends AbstractBaseBean {

    private static final long serialVersionUID = -391404198406614231L;

    @XmlRootElement(name = "meanRate")
    @XmlType
    public static class MeanRate extends AbstractBaseBean {

        private static final long serialVersionUID = -233921226510124154L;

        private String routeId;

        private double value;

        public String getRouteId() {
            return routeId;
        }

        public void setRouteId(final String routeId) {
            this.routeId = routeId;
        }

        public double getValue() {
            return value;
        }

        public void setValue(final double value) {
            this.value = value;
        }

    }

    private final List<MeanRate> responseMeanRates = new ArrayList<>();

    @XmlElementWrapper(name = "responseMeanRates")
    @XmlElement(name = "meanRate")
    @JsonProperty("responseMeanRates")
    public List<MeanRate> getResponseMeanRates() {
        return responseMeanRates;
    }

}
