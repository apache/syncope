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
package org.apache.syncope.common.lib;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class LogOutputStreamTest {

    private Logger logger;

    private LogOutputStream stream;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
        stream = new LogOutputStream(logger);
    }

    @Test
    void writeLines() throws Exception {
        stream.write("first\nsecond\n".getBytes(StandardCharsets.UTF_8));

        verify(logger).debug("first");
        verify(logger).debug("second");
    }

    @Test
    void writeCrLf() throws Exception {
        stream.write("first\r\nsecond\r\n".getBytes(StandardCharsets.UTF_8));

        verify(logger).debug("first");
        verify(logger).debug("second");
    }

    @Test
    void writeWithOffsetAndLength() {
        byte[] bytes = "xxfirst\n".getBytes(StandardCharsets.UTF_8);

        stream.write(bytes, 2, bytes.length - 2);

        verify(logger).debug("first");
    }

    @Test
    void flushPartialLine() throws Exception {
        stream.write("partial".getBytes(StandardCharsets.UTF_8));
        verifyNoInteractions(logger);

        stream.flush();

        verify(logger).debug("partial");
    }

    @Test
    void flushWithoutContent() {
        stream.flush();

        verifyNoInteractions(logger);
    }

    @Test
    void closePartialLine() throws Exception {
        stream.write("partial".getBytes(StandardCharsets.UTF_8));
        verifyNoInteractions(logger);

        stream.close();

        verify(logger).debug("partial");
    }

    @Test
    void writeSingleCharacters() {
        stream.write('a');
        stream.write('\n');

        verify(logger).debug("a");
    }
}
