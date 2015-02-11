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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:variable name="delimiter" select="';'"/>
   
  <xsl:template match="reportlet[@class='org.apache.syncope.core.report.StaticReportlet']">
    <xsl:call-template name="header">
      <xsl:with-param name="node" select="configurations/staticAttributes"/>
    </xsl:call-template>
    
    <xsl:call-template name="staticAttributes">
      <xsl:with-param name="header" select="configurations/staticAttributes"/>
    </xsl:call-template>
  </xsl:template>
  
  <xsl:template name="header">
    <xsl:param name="node"/>  
    <xsl:for-each select="$node/*">
      <xsl:text>"</xsl:text>
      <xsl:value-of select="text()"/>
      <xsl:text>"</xsl:text> 
      <xsl:if test="position() != last()">
        <xsl:value-of select="$delimiter"/>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>
    
  <xsl:template name="staticAttributes">
    <xsl:param name="header"/>
    
    <xsl:variable name="attrs" select="."/>
    <xsl:for-each select="$header/*">
      <xsl:variable name="nameAttr" select="text()"/> 
      <xsl:if test="string-length($attrs/*[name(.)=$nameAttr]/text()) &gt; 0 
                      and count($attrs/*[name(.)=$nameAttr]/*/node()) = 0">
        <xsl:variable name="value" select="$attrs/*[name(.)=$nameAttr]/text()"/>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="$value"/>
        <xsl:text>"</xsl:text>
      </xsl:if>
      
      <xsl:if test="string-length($attrs/*[name(.)=$nameAttr]/*/text()) &gt; 0 
                      and count($attrs/*[name(.)=$nameAttr]/*/node()) &gt; 0">
        <xsl:text>"</xsl:text>
        <xsl:for-each select="$attrs/*[name(.)=$nameAttr]/*">
          <xsl:variable name="value" select="text()"/>
          <xsl:text></xsl:text>
          <xsl:value-of select="$value"/>
          <xsl:if test="position() != last()">
            <xsl:value-of select="$delimiter"/>
          </xsl:if>
        </xsl:for-each>
        <xsl:text>"</xsl:text>
      </xsl:if>
      
      <xsl:if test="position() != last()">
        <xsl:value-of select="$delimiter"/>
      </xsl:if>
    
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
