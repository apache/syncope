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
package org.apache.syncope.client.cli.commands.question;

import javax.xml.ws.WebServiceException;
import org.apache.syncope.client.cli.Input;
import org.apache.syncope.common.lib.SyncopeClientException;

public class QuestionReadByUser extends AbstractQuestionCommand {

    private static final String READ_HELP_MESSAGE = "question --read-by-user {USERNAME}";

    private final Input input;

    public QuestionReadByUser(final Input input) {
        this.input = input;
    }

    public void read() {
        if (input.getParameters().length == 1) {
            try {
                questionResultManager.printQuestion(questionSyncopeOperations.readByUser(input.firstParameter()));
            } catch (final SyncopeClientException | WebServiceException ex) {
                if (ex.getMessage().startsWith("NotFound")) {
                    questionResultManager.notFoundError("Security question", input.firstParameter());
                } else {
                    questionResultManager.generic("Error: " + ex.getMessage());
                }
            } catch (final NumberFormatException ex) {
                questionResultManager.numberFormatException("security question", input.firstParameter());
            }
        } else {
            questionResultManager.commandOptionError(READ_HELP_MESSAGE);
        }
    }

}
