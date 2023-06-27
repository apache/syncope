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
package org.apache.syncope.client.console.chartjs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DoughnutAndPieChartData implements Serializable {

    private static final long serialVersionUID = 1L;

    public static class DataSet implements Serializable {

        private static final long serialVersionUID = 1L;

        private final List<Number> data = new ArrayList<>();

        private final List<String> backgroundColor = new ArrayList<>();

        public List<Number> getData() {
            return data;
        }

        public List<String> getBackgroundColor() {
            return backgroundColor;
        }
    }

    private final List<DataSet> datasets = new ArrayList<>();

    private final List<String> labels = new ArrayList<>();

    public List<DataSet> getDatasets() {
        return datasets;
    }

    public List<String> getLabels() {
        return labels;
    }
}
