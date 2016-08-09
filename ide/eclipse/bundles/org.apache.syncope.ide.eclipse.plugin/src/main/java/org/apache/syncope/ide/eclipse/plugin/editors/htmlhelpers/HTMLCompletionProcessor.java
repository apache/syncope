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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class HTMLCompletionProcessor extends HTMLTemplateAssistProcessor {

    private int offset;
    private boolean xhtmlMode = false;
    private char[] chars = {};
    private boolean assistCloseTag = true;
    protected String[] getLastWord(final String text) {
        StringBuilder sb = new StringBuilder();
        Stack<String> stack = new Stack<String>();
        String word    = "";
        String prevTag = "";
        String lastTag = "";
        String attr    = "";
        String temp1   = ""; // temporary
        String temp2   = ""; // temporary
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // skip scriptlet
            if (c == '<' && text.length() > i + 1 && text.charAt(i + 1) == '%') {
                i = text.indexOf("%>", i + 2);
                if (i == -1) {
                    i = text.length();
                }
                continue;
            }
            // skip XML declaration
            if (c == '<' && text.length() > i + 1 && text.charAt(i + 1) == '?') {
                i = text.indexOf("?>", i + 2);
                if (i == -1) {
                    i = text.length();
                }
                continue;
            }

            if (isDelimiter(c)) {
                temp1 = sb.toString();

                // skip whitespaces in the attribute value
                if (temp1.length() > 1
                        && ((temp1.startsWith("\"") && !temp1.endsWith("\"") && c != '"')
                        || (temp1.startsWith("'") && !temp1.endsWith("'") && c != '\''))) {
                    sb.append(c);
                    continue;
                }
                if (temp1.length() == 1 && ((temp1.equals("\"") || (temp1.equals("'"))))) {
                    sb.append(c);
                    continue;
                }

                if (!temp1.equals("")) {
                    temp2 = temp1;
                    if (temp2.endsWith("=") && !prevTag.equals("") && !temp2.equals("=")) {
                        attr = temp2.substring(0, temp2.length() - 1);
                    }
                }
                if (temp1.startsWith("<") && !temp1.startsWith("</") && !temp1.startsWith("<!")) {
                    prevTag = temp1.substring(1);
                    if (!temp1.endsWith("/")) {
                        stack.push(prevTag);
                    }
                } else if (temp1.startsWith("</") && stack.size() != 0) {
                    stack.pop();
                } else if ((!temp1.startsWith("\"") && !temp1.startsWith("'"))
                        && temp1.endsWith("/") && stack.size() != 0) {
                    stack.pop();
                }
                sb.setLength(0);

                if (c == '<') {
                    sb.append(c);
                } else if (c == '"' || c == '\'') {
                    if (temp1.startsWith("\"") || temp1.startsWith("'")) {
                        sb.append(temp1);
                    }
                    sb.append(c);
                } else if (c == '>') {
                    prevTag = "";
                    attr    = "";
                }
            } else {
                if (c == '=' && !prevTag.equals("")) {
                    attr = temp2.trim();
                }
                temp1 = sb.toString();
                if (temp1.length() > 1
                    && (temp1.startsWith("\"") && temp1.endsWith("\""))
                    || (temp1.startsWith("'") && temp1.endsWith("'"))) {
                    sb.setLength(0);
                }
                sb.append(c);
            }
        }

        if (stack.size() != 0) {
            lastTag = (String) stack.pop();
        }
        // Hmm... it's not perfect...
        if (attr.endsWith("=")) {
            attr = attr.substring(0, attr.length() - 1);
        }
        word = sb.toString();

        return new String[]{word, prevTag, lastTag, attr};
    }

    //Tests a character is delimiter or not delimiter.
    protected boolean isDelimiter(final char c) {
        return (c == ' ' || c == '(' || c == ')' || c == ',' //|| c == '.'
                || c == ';' || c == '\n' || c == '\r' || c == '\t' || c == '+'
                || c == '>' || c == '<' || c == '*' || c == '^' //|| c == '{'
                //|| c == '}'
                || c == '[' || c == ']' || c == '"' || c == '\'');
    }

    protected List<TagInfo> getTagList() {
        return TagDefinition.getTagInfoAsList();
    }

    protected TagInfo getTagInfo(final String name) {
        List<TagInfo> tagList = TagDefinition.getTagInfoAsList();
        for (int i = 0; i < tagList.size(); i++) {
            TagInfo info = (TagInfo) tagList.get(i);
            if (info.getTagName().equals(name)) {
                return info;
            }
        }

        return null;
    }

    public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer,
            final int documentOffset) {

        String   text = viewer.getDocument().get().substring(0, documentOffset);
        String[] dim  = getLastWord(text);
        String   word = dim[0].toLowerCase();
        String   prev = dim[1].toLowerCase();
        String   last = dim[2];

        this.offset = documentOffset;

        List<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
        List<TagInfo> tagList = getTagList();

        // attribute value
        if (word.startsWith("<") && !word.equals("</")) {

            TagInfo parent = getTagInfo(last);
            //tagList = new ArrayList < TagInfo>();
            if (parent != null) {
                String[] childNames = parent.getChildTagNames();
                for (int i = 0; i < childNames.length; i++) {
                    tagList.add(getTagInfo(childNames[i]));
                }

            }
            for (int i = 0; i < tagList.size(); i++) {
                TagInfo tagInfo = (TagInfo) tagList.get(i);
                if (tagInfo instanceof TextInfo) {
                    TextInfo textInfo = (TextInfo) tagInfo;
                    if ((textInfo.getText().toLowerCase()).indexOf(word) == 0) {
                        list.add(new CompletionProposal(
                                textInfo.getText(), documentOffset - word.length(),
                                word.length(), textInfo.getPosition()));
                    }
                    continue;
                }
                String tagName = tagInfo.getTagName();
                if (("<" + tagInfo.getTagName().toLowerCase()).indexOf(word) == 0) {
                    String assistKeyword = tagName;
                    int position = 0;
                    // required attributes
                    AttributeInfo[] requierAttrs = tagInfo.getRequiredAttributeInfo();
                    for (int j = 0; j < requierAttrs.length; j++) {
                        assistKeyword = assistKeyword + " " + requierAttrs[j].getAttributeName();
                        if (requierAttrs[j].hasValue()) {
                            assistKeyword = assistKeyword + "=\"\"";
                            if (j == 0) {
                                position = tagName.length() + requierAttrs[j].getAttributeName().length() + 3;
                            }
                        }
                    }
                    if (tagInfo.hasBody()) {
                        assistKeyword = assistKeyword + ">";
                        if (true) {
                            if (position == 0) {
                                position = assistKeyword.length();
                            }
                            assistKeyword = assistKeyword + "</" + tagName + ">";
                        }
                    } else {
                        if (tagInfo.isEmptyTag() && !xhtmlMode) {
                            assistKeyword += ">";
                        } else {
                            assistKeyword += "/>";
                        }
                    }
                    if (position == 0) {
                        position = assistKeyword.length();
                    }
                    try {
                        list.add(new CompletionProposal(
                                assistKeyword, documentOffset - word.length() + 1,
                                word.length() - 1, position));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            // attribute
        } else if (!prev.equals("")) {
            String tagName = prev;
            TagInfo tagInfo = getTagInfo(tagName);
            if (tagInfo != null) {
                AttributeInfo[] attrList = tagInfo.getAttributeInfo();
                for (int j = 0; j < attrList.length; j++) {
                    if (attrList[j].getAttributeName().toLowerCase().indexOf(word) == 0) {
                        String assistKeyword = null;
                        int position = 0;
                        if (attrList[j].hasValue()) {
                            assistKeyword = attrList[j].getAttributeName() + "=\"\"";
                            position = 2;
                        } else {
                            assistKeyword = attrList[j].getAttributeName();
                            position = 0;
                        }
                        list.add(new CompletionProposal(
                                assistKeyword, documentOffset - word.length(), word.length(),
                                attrList[j].getAttributeName().length() + position));
                    }
                }
            }
            // close tag
        } else if (!last.equals("")) {
            TagInfo info = getTagInfo(last);
            if (info == null || xhtmlMode || info.hasBody() || !info.isEmptyTag()) {
                String assistKeyword = "</" + last + ">";
                int length = 0;
                if (word.equals("</")) {
                    length = 2;
                }
                String contentBefore = viewer.getDocument().get().substring(0, documentOffset - length);
                if (contentBefore.endsWith("\t")) {
                    list.add(new CompletionProposal(
                            assistKeyword, documentOffset - (length + 1), length + 1,
                            assistKeyword.length()));
                } else {
                    list.add(new CompletionProposal(
                            assistKeyword, documentOffset - length, length,
                            assistKeyword.length()));
                }
            }
        }

        sortCompilationProposal(list);

        ICompletionProposal[] templates = super.computeCompletionProposals(viewer, documentOffset);
        for (int i = 0; i < templates.length; i++) {
            list.add(templates[i]);
        }

        ICompletionProposal[] prop = list.toArray(new ICompletionProposal[list.size()]);
        return prop;
    }

    @Override public IContextInformation[] computeContextInformation(final ITextViewer viewer,
            final int documentOffset) {
        ContextInformation[] info = new ContextInformation[0];
        return info;
    }

    @Override public char[] getCompletionProposalAutoActivationCharacters() {
        return chars;
    }

    @Override public char[] getContextInformationAutoActivationCharacters() {
        return chars;
    }

    @Override public IContextInformationValidator getContextInformationValidator() {
        return new ContextInformationValidator(this);
    }

    @Override public String getErrorMessage() {
        return "Error";
    }

    public static void sortCompilationProposal(final List<ICompletionProposal> prop) {
        Collections.sort(prop, new Comparator<ICompletionProposal>() {
            public int compare(final ICompletionProposal o1, final ICompletionProposal o2) {
                return o1.getDisplayString().compareTo(o2.getDisplayString());
            }
        });
    }

    public void setAutoAssistChars(final char[] chars) {
        if (chars != null) {
            this.chars = chars;
        }
    }

    public void setAssistCloseTag(final boolean assistCloseTag) {
        this.assistCloseTag = assistCloseTag;
    }
}
