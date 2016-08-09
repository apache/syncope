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

import java.util.ResourceBundle;

import org.apache.syncope.ide.eclipse.plugin.editors.TemplateEditor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AutoIndentAction extends TextEditorAction {

    public AutoIndentAction(final ResourceBundle bundle, final String prefix, final ITextEditor editor) {
        super(bundle, prefix, editor);
        update();
    }

    @Override
    public void run() {
        String content = getCurrentEditorContent();
        content = formatContent(content);
        ITextEditor ite = getHTMLEditor();
        IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
        doc.set(content);
    }

    private String formatContent(final String contentarg) {
        String content =  contentarg;
        Document htmlDoc = Jsoup.parse(content);
        content = htmlDoc.html();
        return content;
    }

    public String getCurrentEditorContent() {
        ITextEditor ite = getHTMLEditor();
        IDocument doc = ite.getDocumentProvider().getDocument(ite.getEditorInput());
        return doc.get();
    }

    protected ITextEditor getHTMLEditor() {
        final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .getActiveEditor();
        if (!(editor instanceof TemplateEditor)) {
            return null;
        }
        TemplateEditor te = (TemplateEditor) editor;
        return (ITextEditor) (te.getActiveHTMLEditor());
    }

    @Override
    public void update() {
        setEnabled(true);
    }
}
