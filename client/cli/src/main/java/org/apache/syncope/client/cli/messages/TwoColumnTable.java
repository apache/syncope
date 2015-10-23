
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
package org.apache.syncope.client.cli.messages;

import java.util.Map;

public class TwoColumnTable {

    private static final int TABLE_BORDER = 1;

    private static final int MIN_COLUMN_SPACE = 2;

    private static final int MIN_COLUMN_SIZE = 5;

    private int tableWidth;

    private int firstColumntContentWidth;

    private int secondColumntContentWidth;

    private final String title;

    private final String firstColumnHeader;

    private int firstColumnMaxWidth;

    private final String secondColumnHeader;

    private int secondColumnMaxWidth;

    public TwoColumnTable(final String title,
            final String firstColumnHeader,
            final int firstColumnMaxWidth,
            final String secondColumnHeader,
            final int secondColumnMaxWidth) {

        this.firstColumnHeader = firstColumnHeader.toUpperCase();

        if (firstColumnMaxWidth < MIN_COLUMN_SIZE) {
            this.firstColumnMaxWidth = MIN_COLUMN_SIZE;
        } else {
            this.firstColumnMaxWidth = firstColumnMaxWidth;
        }

        this.secondColumnHeader = secondColumnHeader.toUpperCase();

        if (secondColumnMaxWidth < MIN_COLUMN_SIZE) {
            this.secondColumnMaxWidth = MIN_COLUMN_SIZE;
        } else {
            this.secondColumnMaxWidth = secondColumnMaxWidth;
        }

        tableWidth = (TABLE_BORDER * 3) + (MIN_COLUMN_SPACE * 4) + firstColumnMaxWidth + secondColumnMaxWidth;

        if (title.length() > firstColumnMaxWidth + secondColumnMaxWidth) {
            tableWidth = (TABLE_BORDER * 3) + (MIN_COLUMN_SPACE * 4) + title.length();
        }

        firstColumntContentWidth = (MIN_COLUMN_SPACE * 2) + firstColumnMaxWidth;
        secondColumntContentWidth = (MIN_COLUMN_SPACE * 2) + secondColumnMaxWidth;

        this.title = title.toUpperCase();
    }

    public void printTable(final Map<String, String> value) {

        if (value.isEmpty()) {
            firstColumnMaxWidth = firstColumnHeader.length();
            secondColumnMaxWidth = secondColumnHeader.length();
            firstColumntContentWidth = (MIN_COLUMN_SPACE * 2) + firstColumnMaxWidth;
            secondColumntContentWidth = (MIN_COLUMN_SPACE * 2) + secondColumnMaxWidth;
            tableWidth = (TABLE_BORDER * 3) + (MIN_COLUMN_SPACE * 4) + firstColumnMaxWidth + secondColumnMaxWidth;
            if (title.length() > firstColumnMaxWidth + secondColumnMaxWidth) {
                tableWidth = (TABLE_BORDER * 3) + (MIN_COLUMN_SPACE * 4) + title.length();
            }
        }

        final StringBuilder table = new StringBuilder();

// ################ BORDER-TOP ################
        for (int i = 0; i < tableWidth; i++) {
            table.append("#");
        }
        table.append("\n");
// ################ BORDER-TOP ################

// ################ TABLE-TITLE ################
        table.append("#");
        for (int i = 0; i < titleFirstSpace(title); i++) {
            table.append(" ");
        }
        table.append(title);
        for (int i = 0; i < titleSecondSpace(title); i++) {
            table.append(" ");
        }
        table.append("#");
        table.append("\n");
// ################ TABLE-TITLE ################

// ################ BORDER-TOP ################
        for (int i = 0; i < tableWidth; i++) {
            table.append("#");
        }
        table.append("\n");
// ################ BORDER-TOP ################

// ################ LIST-SPACE ################
        table.append("#");
        for (int i = 0; i < firstColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        for (int i = 0; i < secondColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        table.append("\n");
// ################ LIST-SPACE ################

// ################ HEADER-TITLE ################
        table.append("#");
        for (int i = 0; i < firstColumnFirstSpace(firstColumnHeader); i++) {
            table.append(" ");
        }
        table.append(firstColumnHeader);
        for (int i = 0; i < firstColumnSecondSpace(firstColumnHeader); i++) {
            table.append(" ");
        }
        table.append("#");
        for (int i = 0; i < secondColumnFirstSpace(secondColumnHeader); i++) {
            table.append(" ");
        }
        table.append(secondColumnHeader);
        for (int i = 0; i < secondColumnSecondSpace(secondColumnHeader); i++) {
            table.append(" ");
        }
        table.append("#");
        table.append("\n");
// ################ HEADER-TITLE ################

// ################ LIST-SPACE ################
        table.append("#");
        for (int i = 0; i < firstColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        for (int i = 0; i < secondColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        table.append("\n");
// ################ LIST-SPACE ################

// ################ BORDER-TOP ################
        for (int i = 0; i < tableWidth; i++) {
            table.append("#");
        }
        table.append("\n");
// ################ BORDER-TOP ################

// ################ LIST-SPACE ################
        table.append("#");
        for (int i = 0; i < firstColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        for (int i = 0; i < secondColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        table.append("\n");
// ################ LIST-SPACE ################

// ################ CONTENT ################
        for (final Map.Entry<String, String> entrySet : value.entrySet()) {
            final String firstColumnValue = entrySet.getKey();
            final String secondColumnValue = entrySet.getValue();
            table.append("#");
            for (int i = 0; i < firstColumnFirstSpace(firstColumnValue); i++) {
                table.append(" ");
            }
            table.append(firstColumnValue);
            for (int i = 0; i < firstColumnSecondSpace(firstColumnValue); i++) {
                table.append(" ");
            }
            table.append("#");
            for (int i = 0; i < secondColumnFirstSpace(secondColumnValue); i++) {
                table.append(" ");
            }
            table.append(secondColumnValue);
            for (int i = 0; i < secondColumnSecondSpace(secondColumnValue); i++) {
                table.append(" ");
            }
            table.append("#");
            table.append("\n");
        }
// ################ CONTENT ################

// ################ LIST-SPACE ################
        table.append("#");
        for (int i = 0; i < firstColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        for (int i = 0; i < secondColumntContentWidth; i++) {
            table.append(" ");
        }
        table.append("#");
        table.append("\n");
// ################ LIST-SPACE ################

// ################ BORDER-BOTTOM ################
        for (int i = 0; i < tableWidth; i++) {
            table.append("#");
        }
// ################ BORDER-BOTTOM ################

        System.out.println(table.toString() + "\n");
    }

    private int titleFirstSpace(final String wordToPrint) {
        return ((tableWidth - 2) - (wordToPrint.length())) / 2;
    }

    private int titleSecondSpace(final String wordToPrint) {
        return tableWidth - 2 - (titleFirstSpace(wordToPrint) + wordToPrint.length());
    }

    private int firstColumnFirstSpace(final String wordToPrint) {
        return (MIN_COLUMN_SPACE * 2 + (firstColumnMaxWidth - wordToPrint.length())) / 2;
    }

    private int firstColumnSecondSpace(final String wordToPrint) {
        return firstColumntContentWidth - (firstColumnFirstSpace(wordToPrint) + wordToPrint.length());
    }

    private int secondColumnFirstSpace(final String wordToPrint) {
        return (MIN_COLUMN_SPACE * 2 + (secondColumnMaxWidth - wordToPrint.length())) / 2;
    }

    private int secondColumnSecondSpace(final String wordToPrint) {
        return secondColumntContentWidth - (secondColumnFirstSpace(wordToPrint) + wordToPrint.length());
    }

}
