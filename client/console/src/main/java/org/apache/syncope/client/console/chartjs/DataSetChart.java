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

/**
 * Provides chart data and options.
 *
 * @param <D> the generic type of chart data
 * @param <O> the generic type of chart options
 * @param <S> the generic type of a chart data set
 */
abstract class DataSetChart<
        D extends ChartData<S>, O extends ChartOptions, S extends BaseDataSet>
        extends Chart<O> {

    private static final long serialVersionUID = 999846601210465414L;

    /** The data. */
    protected D data;

    public D getData() {
        return data;
    }

    public void setData(final D data) {
        this.data = data;
    }
}
