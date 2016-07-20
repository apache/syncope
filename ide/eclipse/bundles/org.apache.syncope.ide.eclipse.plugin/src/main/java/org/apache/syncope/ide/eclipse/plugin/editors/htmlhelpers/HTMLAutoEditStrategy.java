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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;

public class HTMLAutoEditStrategy extends DefaultIndentLineAutoEditStrategy {

    private String charset = System.getProperty("file.encoding");
    protected boolean enable;

    public HTMLAutoEditStrategy() {
        this.enable = true;
    }

    public void setEnabled(final boolean enable) {
        this.enable = enable;
    }

    public void setFile(final IFile file) {
        try {
            this.charset = file.getCharset();
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public void customizeDocumentCommand(final IDocument d, final DocumentCommand c) {
        if (enable) {
            try {
                if ("-".equals(c.text) && c.offset >= 3 && d.get(c.offset - 3, 3).equals("<!-")) {
                    c.text = "-  -->";
                    c.shiftsCaret = false;
                    c.caretOffset = c.offset + 2;
                    c.doit = false;
                    return;
                }
                if ("[".equals(c.text) && c.offset >= 2 && d.get(c.offset - 2, 2).equals("<!")) {
                    c.text = "[CDATA[]]>";
                    c.shiftsCaret = false;
                    c.caretOffset = c.offset + 7;
                    c.doit = false;
                    return;
                }
                if ("l".equals(c.text) && c.offset >= 4 && d.get(c.offset - 4, 4).equals("<?xm")) {
                    c.text = "l version = \"1.0\" encoding = \"" + charset + "\"?>";
                    return;
                }
            } catch (final BadLocationException e) {
                e.printStackTrace();
            }
        }
        super.customizeDocumentCommand(d, c);
    }

}
