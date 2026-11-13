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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChartTypeTest {

    @Test
    void values() {
        assertEquals(5, ChartType.values().length);
        assertEquals(ChartType.line, ChartType.values()[0]);
        assertEquals(ChartType.bar, ChartType.values()[1]);
        assertEquals(ChartType.pie, ChartType.values()[2]);
        assertEquals(ChartType.doughnut, ChartType.values()[3]);
        assertEquals(ChartType.scatter, ChartType.values()[4]);
    }

    @Test
    void valueOf() {
        assertEquals(ChartType.line, ChartType.valueOf("line"));
        assertEquals(ChartType.bar, ChartType.valueOf("bar"));
        assertEquals(ChartType.pie, ChartType.valueOf("pie"));
        assertEquals(ChartType.doughnut, ChartType.valueOf("doughnut"));
        assertEquals(ChartType.scatter, ChartType.valueOf("scatter"));
    }
}
