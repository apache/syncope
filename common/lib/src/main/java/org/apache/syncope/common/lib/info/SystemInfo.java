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
package org.apache.syncope.common.lib.info;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Queue;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "systemInfo")
@XmlType
public class SystemInfo extends AbstractBaseBean {

    private static final long serialVersionUID = -352727968865892499L;

    private String hostname;

    private String os;

    private String jvm;

    private int availableProcessors;

    private long startTime;

    private final CircularFifoQueue<LoadInstant> load = new CircularFifoQueue<>(10);

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public String getOs() {
        return os;
    }

    public void setOs(final String os) {
        this.os = os;
    }

    public String getJvm() {
        return jvm;
    }

    public void setJvm(final String jvm) {
        this.jvm = jvm;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setAvailableProcessors(final int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(final long startTime) {
        this.startTime = startTime;
    }

    @XmlElementWrapper(name = "load")
    @XmlElement(name = "instant")
    @JsonProperty("load")
    public Queue<LoadInstant> getLoad() {
        return load;
    }

    @XmlRootElement(name = "loadInstant")
    @XmlType
    public static class LoadInstant extends AbstractBaseBean {

        private static final long serialVersionUID = 1700788373758716478L;

        private long uptime;

        private double systemLoadAverage;

        private long totalMemory;

        private long freeMemory;

        private long maxMemory;

        public double getSystemLoadAverage() {
            return systemLoadAverage;
        }

        public void setSystemLoadAverage(final double systemLoadAverage) {
            this.systemLoadAverage = systemLoadAverage;
        }

        public long getUptime() {
            return uptime;
        }

        public void setUptime(final long uptime) {
            this.uptime = uptime;
        }

        public long getTotalMemory() {
            return totalMemory;
        }

        public void setTotalMemory(final long totalMemory) {
            this.totalMemory = totalMemory;
        }

        public long getFreeMemory() {
            return freeMemory;
        }

        public void setFreeMemory(final long freeMemory) {
            this.freeMemory = freeMemory;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public void setMaxMemory(final long maxMemory) {
            this.maxMemory = maxMemory;
        }
    }

}
