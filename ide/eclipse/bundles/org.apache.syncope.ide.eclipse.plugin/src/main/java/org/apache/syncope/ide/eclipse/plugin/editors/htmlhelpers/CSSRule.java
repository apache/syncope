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
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

public class CSSRule implements IPredicateRule {

    private IToken propToken;
    private IToken valueToken;

    public CSSRule(final IToken propToken, final IToken valueToken) {
        this.propToken = propToken;
        this.valueToken = valueToken;
    }

    private boolean sequenceDetected(final ICharacterScanner scanner, final char[] sequence,
            final boolean eofAllowed) {
        for (int i = 1; i < sequence.length; i++) {
            int c = scanner.read();
            if (c == ICharacterScanner.EOF && eofAllowed) {
                return true;
            } else if (c != sequence[i]) {
                // Non-matching character detected, rewind the scanner back to the start.
                // Do not unread the first character.
                scanner.unread();
                for (int j = i - 1; j > 0; j--) {
                    scanner.unread();
                }
                return false;
            }
        }

        return true;
    }

    private IToken getToken(final ICharacterScanner scanner) {
        int c;
        char[][] delimiters = scanner.getLegalLineDelimiters();
        while ((c = scanner.read()) != ICharacterScanner.EOF) {
            if (c == ':') {
                return propToken;
            } else if (c == ';') {
                return valueToken;
            } else {
                // Check for end of line since it can be used to terminate the pattern.
                for (int i = 0; i < delimiters.length; i++) {
                    if (c == delimiters[i][0] && sequenceDetected(scanner, delimiters[i], true)) {
                        return null;
                    }
                }
            }
        }
        scanner.unread();
        return null;
    }

    public IToken evaluate(final ICharacterScanner scanner, final boolean resume) {
        return doEvaluate(scanner, resume);
    }

    private IToken doEvaluate(final ICharacterScanner scanner, final boolean resume) {
        if (resume) {
            IToken token = getToken(scanner);
            if (token != null) {
                return token;
            }
        } else {

            int c = scanner.read();
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                IToken token = getToken(scanner);
                if (token != null) {
                    return token;
                }
            }
        }

        scanner.unread();
        return Token.UNDEFINED;
    }


    public IToken getSuccessToken() {
        return null;
    }

    public IToken evaluate(final ICharacterScanner scanner) {
        return evaluate(scanner, false);
    }
}
