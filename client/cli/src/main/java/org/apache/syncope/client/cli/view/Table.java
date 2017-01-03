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
package org.apache.syncope.client.cli.view;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public final class Table {

    private static final String TABLE_TITLE_FORMAT = "#  %s  #%n";

    private final String title;

    private final List<String> headers;

    private final Set<List<String>> values;

    private int columnsNumber;

    private Object[] tmpValuesArray;

    private String tableContentFormat;

    private int[] columnsSize;

    private int tableWidth;

    private String border = "";

    private Table(
            final String title,
            final List<String> headers,
            final Set<List<String>> values) {
        this.title = title;
        this.headers = headers;
        this.values = values;
    }

    public void print() {
        System.out.println("");
        columnsNumber = headers.size();
        tmpValuesArray = new String[columnsNumber];

        buildTableContentFormat();
        initializeColumnSize();
        countTableWidth();

        printBorder();
        printTitle();
        printBorder();
        printHeaders();
        printBorder();
        printeContent();
        printBorder();
        System.out.println("");
    }

    private void buildTableContentFormat() {
        final StringBuilder tableContentFormatBuilder = new StringBuilder("#");
        for (int s = 0; s < columnsNumber; s++) {
            tableContentFormatBuilder.append("  %s  #");
        }
        tableContentFormatBuilder.append("\n");
        tableContentFormat = tableContentFormatBuilder.toString();
    }

    private void initializeColumnSize() {
        columnsSize = new int[columnsNumber];
        for (int j = 0; j < columnsSize.length; j++) {
            columnsSize[j] = 0;
        }

        for (int i = 0; i < columnsSize.length; i++) {
            if (headers.get(i).length() > columnsSize[i]) {
                columnsSize[i] = headers.get(i).length();
            }
        }

        for (final List<String> value : values) {
            for (int j = 0; j < columnsSize.length; j++) {
                if (value.get(j) != null && value.get(j).length() > columnsSize[j]) {
                    columnsSize[j] = value.get(j).length();
                }
            }
        }
    }

    private void countTableWidth() {
        int maxColumnValueSum = 0;
        for (int j = 0; j < columnsSize.length; j++) {
            maxColumnValueSum = maxColumnValueSum + columnsSize[j];
        }

        tableWidth = maxColumnValueSum + (columnsNumber * (2 + 2)) + columnsNumber + 1;
    }

    private void printBorder() {
        if (border.isEmpty()) {
            final StringBuilder borderBuilder = new StringBuilder();
            for (int j = 0; j < tableWidth; j++) {
                borderBuilder.append("#");
            }
            border = borderBuilder.toString();
        }

        System.out.println(border);
    }

    private void printTitle() {
        System.out.format(TABLE_TITLE_FORMAT, StringUtils.center(" ", tableWidth - 6));
        System.out.format(TABLE_TITLE_FORMAT, StringUtils.center(title.toUpperCase(), tableWidth - 6));
        System.out.format(TABLE_TITLE_FORMAT, StringUtils.center(" ", tableWidth - 6));
    }

    private void printHeaders() {
        printColumnSpace();

        for (int h = 0; h < columnsNumber; h++) {
            tmpValuesArray[h] = StringUtils.center(headers.get(h).toUpperCase(), columnsSize[h]);
        }

        System.out.format(tableContentFormat, tmpValuesArray);

        printColumnSpace();
    }

    private void printeContent() {
        printColumnSpace();

        for (final List<String> value : values) {
            for (int j = 0; j < columnsNumber; j++) {
                if (value.get(j) == null) {
                    tmpValuesArray[j] = StringUtils.center("null", columnsSize[j]);
                } else {
                    tmpValuesArray[j] = StringUtils.center(value.get(j), columnsSize[j]);
                }
            }
            System.out.format(tableContentFormat, tmpValuesArray);
        }

        printColumnSpace();
    }

    private void printColumnSpace() {
        for (int h = 0; h < columnsNumber; h++) {
            tmpValuesArray[h] = StringUtils.center(" ", columnsSize[h]);
        }

        System.out.format(tableContentFormat, tmpValuesArray);
    }

    public static class TableBuilder {

        private final List<String> headers = new ArrayList<>();

        private final Set<List<String>> values = new LinkedHashSet<>();

        private final String title;

        public TableBuilder(final String title) {
            this.title = title;
        }

        public TableBuilder header(final String header) {
            headers.add(header);
            return this;
        }

        public TableBuilder rowValues(final List<String> row) {
            values.add(row);
            return this;
        }

        public Table build() {
            return new Table(title, headers, values);
        }
    }
}
