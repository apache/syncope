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
package org.apache.syncope.ide.eclipse.plugin.editors.htmlhelpers;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;

public class DocTypeRule extends MultiLineRule {

    public DocTypeRule(final IToken token) {
        super("<!DOCTYPE", ">", token);
    }

    protected boolean endSequenceDetected(final ICharacterScanner scanner) {
        int c;
        boolean quoted = false;
        char[][] delimiters = scanner.getLegalLineDelimiters();
        boolean previousWasEscapeCharacter = false;
        while ((c = scanner.read()) != ICharacterScanner.EOF) {
            if (c == fEscapeCharacter) {
                // Skip the escaped character.
                scanner.read();
            } else if (c == '[') {
                quoted = true;
            } else if (c == ']') {
                quoted = false;
            } else if (fEndSequence.length > 0 && c == fEndSequence[0]
                    && !quoted && sequenceDetected(scanner, fEndSequence, true)) {
                return true;
            } else if (fBreaksOnEOL) {
                // Check for end of line since it can be used to terminate the pattern.
                for (int i = 0; i < delimiters.length; i++) {
                    if (c == delimiters[i][0] && sequenceDetected(scanner, delimiters[i], true)) {
                        if (!fEscapeContinuesLine || !previousWasEscapeCharacter) {
                            return true;
                        }
                    }
                }
            }
            previousWasEscapeCharacter = (c == fEscapeCharacter);
        }
        if (fBreaksOnEOF) {
            return true;
        }
        scanner.unread();
        return false;
    }
}
