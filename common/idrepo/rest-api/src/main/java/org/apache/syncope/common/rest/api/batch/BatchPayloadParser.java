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
package org.apache.syncope.common.rest.api.batch;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BatchPayloadParser {

    private static final Logger LOG = LoggerFactory.getLogger(BatchPayloadParser.class);

    private static final Pattern PATTERN_LAST_CRLF = Pattern.compile("(.*)\\r\\n\\s*", Pattern.DOTALL);

    private static final Pattern PATTERN_HEADER_LINE = Pattern.compile("((?:\\w|[!#$%\\&'*+\\-.^`|~])+):\\s?(.*)\\s*");

    private static final Pattern PATTERN_BLANK_LINE = Pattern.compile("\\s*\r?\n\\s*");

    private static final String[] HTTP_METHODS = {
        HttpMethod.DELETE,
        HttpMethod.PATCH,
        HttpMethod.POST,
        HttpMethod.PUT
    };

    private static BatchPayloadLine removeEndingCRLF(final BatchPayloadLine line) {
        Matcher matcher = PATTERN_LAST_CRLF.matcher(line.toString());
        return matcher.matches()
                ? new BatchPayloadLine(matcher.group(1), line.getLineNumber())
                : line;
    }

    private static void removeEndingCRLFFromList(final List<BatchPayloadLine> lines) {
        if (!lines.isEmpty()) {
            BatchPayloadLine lastLine = lines.removeLast();
            lines.add(removeEndingCRLF(lastLine));
        }
    }

    private static List<List<BatchPayloadLine>> split(final List<BatchPayloadLine> lines, final String boundary) {
        List<List<BatchPayloadLine>> messageParts = new ArrayList<>();
        List<BatchPayloadLine> currentPart = new ArrayList<>();
        boolean isEndReached = false;

        String quotedBoundary = Pattern.quote(boundary);
        Pattern boundaryDelimiterPattern = Pattern.compile("--" + quotedBoundary + "--\\s*");
        Pattern boundaryPattern = Pattern.compile("--" + quotedBoundary + "\\s*");

        for (BatchPayloadLine line : lines) {
            if (boundaryDelimiterPattern.matcher(line.toString()).matches()) {
                removeEndingCRLFFromList(currentPart);
                messageParts.add(currentPart);
                isEndReached = true;
            } else if (boundaryPattern.matcher(line.toString()).matches()) {
                removeEndingCRLFFromList(currentPart);
                messageParts.add(currentPart);
                currentPart = new ArrayList<>();
            } else {
                currentPart.add(line);
            }

            if (isEndReached) {
                break;
            }
        }

        // Remove preamble
        if (!messageParts.isEmpty()) {
            messageParts.removeFirst();
        }

        if (!isEndReached) {
            int lineNumber = lines.isEmpty() ? 0 : lines.getFirst().getLineNumber();
            throw new IllegalArgumentException("Missing close boundary delimiter around line " + lineNumber);
        }

        return messageParts;
    }

    private static void consumeHeaders(final List<BatchPayloadLine> bodyPart, final BatchItem item) {
        Map<String, List<Object>> headers = new HashMap<>();

        boolean isHeader = true;
        for (Iterator<BatchPayloadLine> itor = bodyPart.iterator(); itor.hasNext() && isHeader;) {
            BatchPayloadLine currentLine = itor.next();

            Matcher headerMatcher = PATTERN_HEADER_LINE.matcher(currentLine.toString());
            if (headerMatcher.matches() && headerMatcher.groupCount() == 2) {
                itor.remove();
            } else {
                isHeader = false;
            }
        }
        consumeBlankLine(bodyPart);

        isHeader = true;
        for (Iterator<BatchPayloadLine> itor = bodyPart.iterator(); itor.hasNext() && isHeader;) {
            BatchPayloadLine currentLine = itor.next();

            if (currentLine.toString().contains("HTTP/1.1")) {
                itor.remove();

                if (ArrayUtils.contains(HTTP_METHODS, StringUtils.substringBefore(currentLine.toString(), " "))
                        && item instanceof BatchRequestItem) {

                    BatchRequestItem bri = BatchRequestItem.class.cast(item);
                    String[] parts = currentLine.toString().split(" ");
                    bri.setMethod(parts[0]);
                    String[] target = parts[1].split("\\?");
                    bri.setRequestURI(target[0]);
                    if (target.length > 1) {
                        bri.setQueryString(target[1]);
                    }
                } else if (item instanceof BatchResponseItem) {
                    BatchResponseItem bri = BatchResponseItem.class.cast(item);
                    try {
                        bri.setStatus(Integer.parseInt(StringUtils.substringBefore(
                                StringUtils.substringAfter(currentLine.toString(), " "), " ").trim()));
                    } catch (NumberFormatException e) {
                        LOG.error("Invalid value found in response for HTTP status", e);
                    }
                }
            } else {
                Matcher headerMatcher = PATTERN_HEADER_LINE.matcher(currentLine.toString());
                if (headerMatcher.matches() && headerMatcher.groupCount() == 2) {
                    itor.remove();

                    String headerName = headerMatcher.group(1).trim();
                    String headerValue = headerMatcher.group(2).trim();

                    List<Object> header = headers.get(headerName);
                    if (header == null) {
                        header = new ArrayList<>();
                        headers.put(headerName, header);
                    }
                    header.addAll(Stream.of(headerValue.split(",")).map(String::trim).toList());
                } else {
                    isHeader = false;
                }
            }
        }
        consumeBlankLine(bodyPart);

        item.setHeaders(headers);
    }

    private static void consumeBlankLine(final List<BatchPayloadLine> bodyPart) {
        if (!bodyPart.isEmpty() && PATTERN_BLANK_LINE.matcher(bodyPart.getFirst().toString()).matches()) {
            bodyPart.removeFirst();
        }
    }

    public static <T extends BatchItem> List<T> parse(
            final InputStream in,
            final MediaType multipartMixed,
            final T template) throws IOException {

        List<BatchPayloadLine> lines;
        try (BatchPayloadLineReader lineReader = new BatchPayloadLineReader(in, multipartMixed)) {
            lines = lineReader.read();
        }

        return split(lines, multipartMixed.getParameters().get(RESTHeaders.BOUNDARY_PARAMETER)).stream().
                map(bodyPart -> {
                    LOG.debug("Body part:\n{}", bodyPart);

                    T item = SerializationUtils.clone(template);
                    consumeHeaders(bodyPart, item);
                    item.setContent(bodyPart.stream().map(BatchPayloadLine::toString).collect(Collectors.joining()));

                    return item;
                }).toList();
    }

    private BatchPayloadParser() {
        // private constructor for static utility class
    }
}
