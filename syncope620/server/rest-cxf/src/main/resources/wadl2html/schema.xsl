<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
                xmlns:wadl="http://wadl.dev.java.net/2009/02"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="wadl xs"
                version="1.0">
  
  <xsl:strip-space elements="*"/>

  <xsl:param name="contextPath"/>
  <xsl:param name="schema-position"/>  
  <xsl:param name="schema-prefix"/>  
  
  <xsl:template match="/wadl:application">  
    <html lang="en">
      <head>
        <meta charset="utf-8"/>
        <title>          
          <xsl:value-of select="wadl:doc/@title"/>
        </title>
                  
        <link rel="stylesheet" href="{$contextPath}/webjars/highlightjs/${highlightjs.version}/styles/default.min.css"/>
        
        <script src="{$contextPath}/webjars/highlightjs/${highlightjs.version}/highlight.min.js">           
        </script>
        <script>
          hljs.initHighlightingOnLoad();
        </script>
      </head>
      <body>        
        <pre>
          <code class="xml">
            <xsl:apply-templates select="//xs:schema[position() = $schema-position]" mode="verb"/>
          </code>
        </pre>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="*|@*" mode="verb">
    <xsl:variable name="node-type">
      <xsl:call-template name="node-type"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$node-type='element'">
        <xsl:for-each select="ancestor::node()">
          <xsl:if test="position() &gt; 3">
            <xsl:text>&#160;&#160;</xsl:text>
          </xsl:if>
        </xsl:for-each>

        <xsl:choose>
          <xsl:when test="name() = 'xs:complexType' or name() = 'xs:simpleType'">
            <a name="int_{@name}">
              <xsl:text>&lt;</xsl:text>
              <xsl:value-of select="name()"/>
            </a>          
          </xsl:when>
          <xsl:when test="name() = 'xs:element'">
            <a name="{@name}">
              <xsl:text>&lt;</xsl:text>
              <xsl:value-of select="name()"/>
            </a>          
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>&lt;</xsl:text>
            <xsl:value-of select="name()"/>
          </xsl:otherwise>
        </xsl:choose>
        
        <xsl:apply-templates select="@*" mode="verb"/>        

        <xsl:choose>
          <xsl:when test="count(descendant::node()) = 0">
            <xsl:text>/&gt;&#10;</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:text>&gt;&#10;</xsl:text>
            
            <xsl:apply-templates mode="verb"/>
                
            <xsl:for-each select="ancestor::node()">
              <xsl:if test="position() &gt; 3">
                <xsl:text>&#160;&#160;</xsl:text>
              </xsl:if>
            </xsl:for-each>

            <xsl:text>&lt;/</xsl:text>
            <xsl:value-of select="name()"/>
            <xsl:text>&gt;&#10;</xsl:text>
          </xsl:otherwise>
        </xsl:choose>                        
      </xsl:when>
      <xsl:when test="$node-type='text'">
        <xsl:value-of select="self::text()"/>
      </xsl:when>
      <xsl:when test="$node-type='attribute'">
        <xsl:text> </xsl:text>
        <xsl:value-of select="name()"/>
        <xsl:text>="</xsl:text>
        <xsl:choose>
          <xsl:when test="contains(., ':') and not(starts-with(., 'xs:'))">
            <a>
              <xsl:variable name="current" select="."/>
              <xsl:attribute name="href">
                <xsl:choose>
                  <xsl:when test="name() = 'ref'">#<xsl:value-of select="substring-after($current, ':')"/></xsl:when>
                  <xsl:otherwise>#int_<xsl:value-of select="substring-after($current, ':')"/></xsl:otherwise>
                </xsl:choose>                
              </xsl:attribute>
              <xsl:value-of select="$current"/>                          
            </a>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="."/>            
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>"</xsl:text>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="node-type">
    <xsl:param name="node" select="."/>
    <xsl:apply-templates mode="nodetype" select="$node"/>
  </xsl:template>
  <xsl:template mode="nodetype" match="*">element</xsl:template>
  <xsl:template mode="nodetype" match="@*">attribute</xsl:template>
  <xsl:template mode="nodetype" match="text()">text</xsl:template>
</xsl:stylesheet>
