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
package org.apache.syncope.client.console.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SRAStatistics implements Serializable {

    private static final long serialVersionUID = 25070367703725L;

    public static class Measurement implements Serializable {

        private static final long serialVersionUID = 24933964529045L;

        private String statistic;

        private Float value;

        public String getStatistic() {
            return statistic;
        }

        public void setStatistic(final String statistic) {
            this.statistic = statistic;
        }

        public Float getValue() {
            return value;
        }

        public void setValue(final Float value) {
            this.value = value;
        }
    }

    public static class Tag implements Serializable {

        private static final long serialVersionUID = 25010610267446L;

        private String tag;

        private final List<String> values = new ArrayList<>();

        public String getTag() {
            return tag;
        }

        public void setTag(final String tag) {
            this.tag = tag;
        }

        public List<String> getValues() {
            return values;
        }
    }

    private String name;

    private String description;

    private String baseUnit;

    private final List<Measurement> measurements = new ArrayList<>();

    private final List<Tag> availableTags = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(final String baseUnit) {
        this.baseUnit = baseUnit;
    }

    @JsonIgnore
    public Optional<Float> getMeasurement(final String statistic) {
        return measurements.stream().filter(m -> statistic.equals(m.getStatistic())).
                findFirst().map(Measurement::getValue);
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public List<Tag> getAvailableTags() {
        return availableTags;
    }
}
