/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.client.console.chartjs.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scales implements Serializable {

    private static final long serialVersionUID = 9016231209839859089L;

    private final Map<String, Scale> x = new HashMap<>();

    private final Map<String, Scale> y = new HashMap<>();

    public Map<String, Scale> getX() {
        return x;
    }

    public Map<String, Scale> getY() {
        return y;
    }

    public void setX(final Scale scale) {
        x.clear();
        x.put("x", scale);
    }

    public void setY(final Scale scale) {
        y.clear();
        y.put("y", scale);
    }
}
