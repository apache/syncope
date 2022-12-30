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
package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class BatchPayloadLineReader implements AutoCloseable {

    private static final byte CR = '\r';

    private static final byte LF = '\n';

    private static final int EOF = -1;

    private static final int BUFFER_SIZE = 8192;

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final ReadState readState = new ReadState();

    private final InputStream in;

    private final MediaType multipartMixed;

    private final byte[] buffer = new byte[BUFFER_SIZE];

    private Charset currentCharset = DEFAULT_CHARSET;

    private String currentBoundary = null;

    private int offset = 0;

    private int limit = 0;

    public BatchPayloadLineReader(final InputStream in, final MediaType multipartMixed) {
        this.in = in;
        this.multipartMixed = multipartMixed;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    private boolean isBoundary(final String currentLine) {
        return (currentBoundary + JAXRSService.CRLF).equals(currentLine)
                || (currentBoundary + JAXRSService.DOUBLE_DASH + JAXRSService.CRLF).equals(currentLine);
    }

    private int fillBuffer() throws IOException {
        limit = in.read(buffer, 0, buffer.length);
        offset = 0;

        return limit;
    }

    private String readLine() throws IOException {
        if (limit == EOF) {
            return null;
        }

        ByteBuffer innerBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        // EOF will be considered as line ending
        boolean foundLineEnd = false;

        while (!foundLineEnd) {
            // Is buffer refill required?
            if (limit == offset && fillBuffer() == EOF) {
                foundLineEnd = true;
            }

            if (!foundLineEnd) {
                byte currentChar = buffer[offset++];
                if (!innerBuffer.hasRemaining()) {
                    innerBuffer.flip();
                    ByteBuffer tmp = ByteBuffer.allocate(innerBuffer.limit() * 2);
                    tmp.put(innerBuffer);
                    innerBuffer = tmp;
                }
                innerBuffer.put(currentChar);

                if (currentChar == LF) {
                    foundLineEnd = true;
                } else if (currentChar == CR) {
                    foundLineEnd = true;

                    // Check next byte. Consume \n if available
                    // Is buffer refill required?
                    if (limit == offset) {
                        fillBuffer();
                    }

                    // Check if there is at least one character
                    if (limit != EOF && buffer[offset] == LF) {
                        innerBuffer.put(LF);
                        offset++;
                    }
                }
            }
        }

        if (innerBuffer.position() == 0) {
            return null;
        } else {
            String currentLine = new String(innerBuffer.array(), 0, innerBuffer.position(),
                    readState.isReadBody() ? currentCharset : DEFAULT_CHARSET);

            if (currentLine.startsWith(HttpHeaders.CONTENT_TYPE)) {
                String charsetString = multipartMixed.getParameters().get(MediaType.CHARSET_PARAMETER);
                currentCharset = Optional.ofNullable(charsetString).map(Charset::forName).orElse(DEFAULT_CHARSET);

                currentBoundary = JAXRSService.DOUBLE_DASH + multipartMixed.getParameters().
                        get(RESTHeaders.BOUNDARY_PARAMETER);
            } else if (JAXRSService.CRLF.equals(currentLine)) {
                readState.foundLinebreak();
            } else if (isBoundary(currentLine)) {
                readState.foundBoundary();
            }

            return currentLine;
        }
    }

    public List<BatchPayloadLine> read() throws IOException {
        List<BatchPayloadLine> result = new ArrayList<>();

        String currentLine = readLine();
        if (currentLine != null) {
            currentBoundary = currentLine.trim();
            int counter = 1;
            result.add(new BatchPayloadLine(currentLine, counter++));

            while ((currentLine = readLine()) != null) {
                result.add(new BatchPayloadLine(currentLine, counter++));
            }
        }

        return result;
    }

    /**
     * Read state indicator (whether currently the {@code body} or {@code header} part is read).
     */
    private static class ReadState {

        private int state = 0;

        public void foundLinebreak() {
            state++;
        }

        public void foundBoundary() {
            state = 0;
        }

        public boolean isReadBody() {
            return state >= 2;
        }

        @Override
        public String toString() {
            return String.valueOf(state);
        }
    }
}
