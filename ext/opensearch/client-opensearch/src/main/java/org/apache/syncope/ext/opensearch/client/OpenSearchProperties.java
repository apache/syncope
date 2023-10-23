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
package org.apache.syncope.ext.opensearch.client;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("opensearch")
public class OpenSearchProperties {

    private List<String> hosts = new ArrayList<>();

    private int indexMaxResultWindow = 10000;

    private String numberOfShards = "1";

    private String numberOfReplicas = "1";

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(final List<String> hosts) {
        this.hosts = hosts;
    }

    public int getIndexMaxResultWindow() {
        return indexMaxResultWindow;
    }

    public void setIndexMaxResultWindow(final int indexMaxResultWindow) {
        this.indexMaxResultWindow = indexMaxResultWindow;
    }

    public String getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(final String numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public String getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(final String numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }
}
