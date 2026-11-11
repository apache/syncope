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
package org.apache.syncope.ext.openfga.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OpenFGAPropertiesTest {

    @Test
    void defaults() {
        OpenFGAProperties props = new OpenFGAProperties();

        assertThat(props.getApiUrl()).isNull();
        assertThat(props.getApiToken()).isNull();
        assertThat(props.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));

        assertThatCode(props::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void gettersAndSetters() {
        OpenFGAProperties props = new OpenFGAProperties();
        props.setApiUrl("http://openfga:8080");
        props.setApiToken("token");
        props.setReadTimeout(Duration.ofSeconds(3));
        props.setConnectTimeout(Duration.ofSeconds(4));

        assertThat(props.getApiUrl()).isEqualTo("http://openfga:8080");
        assertThat(props.getApiToken()).isEqualTo("token");
        assertThat(props.getReadTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(4));

        assertThatCode(props::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void negativeReadTimeoutRejected() {
        OpenFGAProperties props = new OpenFGAProperties();
        props.setReadTimeout(Duration.ofSeconds(-1));

        assertThatThrownBy(props::afterPropertiesSet).
                isInstanceOf(IllegalStateException.class).
                hasMessage("readTimeout must be positive");
    }

    @Test
    void negativeConnectTimeoutRejected() {
        OpenFGAProperties props = new OpenFGAProperties();
        props.setConnectTimeout(Duration.ofSeconds(-1));

        assertThatThrownBy(props::afterPropertiesSet).
                isInstanceOf(IllegalStateException.class).
                hasMessage("connectTimeout must be positive");
    }

    @Test
    void nullTimeoutsAccepted() {
        OpenFGAProperties props = new OpenFGAProperties();
        props.setReadTimeout(null);
        props.setConnectTimeout(null);

        assertThatCode(props::afterPropertiesSet).doesNotThrowAnyException();
    }
}
