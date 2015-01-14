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
                version="1.0">

  <xsl:variable name="delimiter" select="';'"/>
  
  <xsl:template match="reportlet[@class='org.apache.syncope.core.report.RoleReportlet']">
    
    <xsl:call-template name="header">
      <xsl:with-param name="node" select="configurations/roleAttributes"/>
    </xsl:call-template>
    <xsl:for-each select="role">
      <xsl:call-template name="roleAttributes">
        <xsl:with-param name="header" select="../configurations/roleAttributes"/>
        <xsl:with-param name="attrs" select="."/>
      </xsl:call-template>
      <xsl:text>&#10;</xsl:text>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template name="header">
    <xsl:param name="node"/>  
    <xsl:for-each select="$node/*">
      <xsl:value-of select="text()"/>   
      <xsl:if test="position() != last()">
        <xsl:value-of select="$delimiter"/>
      </xsl:if>
    </xsl:for-each>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>
    
  <xsl:template name="roleAttributes">
    <xsl:param name="header"/>
    <xsl:param name="attrs"/>
    
    <xsl:for-each select="$header/*">
      <xsl:variable name="nameAttr" select="text()"/>
      
      <xsl:choose> 
        <xsl:when test="string-length($attrs/@*[name()=$nameAttr]) &gt; 0">
          <xsl:variable name="roleAttr" select="$attrs/@*[name()=$nameAttr]"/>
          <xsl:text>"</xsl:text>
          <xsl:value-of select="$roleAttr/."/>
          <xsl:text>"</xsl:text>
        </xsl:when>
        <xsl:when test="name($attrs/*[name(.)=$nameAttr]/*[name(.)='entitlement']) 
                        and count($attrs/*[name(.)=$nameAttr]/node()) &gt; 0">
          <xsl:text>"</xsl:text>       
          <xsl:for-each select="$attrs/*/entitlement">
            <xsl:variable name="value" select="@id"/>
            <xsl:value-of select="$value"/>
            <xsl:if test="position() != last()">
              <xsl:value-of select="$delimiter"/>
            </xsl:if>
          </xsl:for-each>
          <xsl:text>"</xsl:text>
        </xsl:when>
        <xsl:when test="name($attrs/*[name(.)=$nameAttr]/*[name(.)='resource']) 
                        and count($attrs/*[name(.)=$nameAttr]/node()) &gt; 0">
          <xsl:text>"</xsl:text>       
          <xsl:for-each select="$attrs/*/resource">
            <xsl:variable name="value" select="@name"/>
            <xsl:value-of select="$value"/>
            <xsl:if test="position() != last()">
              <xsl:value-of select="$delimiter"/>
            </xsl:if>
          </xsl:for-each>
          <xsl:text>"</xsl:text>
        </xsl:when>
        <xsl:when test="name($attrs/*[name(.)=$nameAttr]/*[name(.)='user']) 
                        and count($attrs/*[name(.)=$nameAttr]/node()) &gt; 0">
          <xsl:text>"</xsl:text>       
          <xsl:for-each select="$attrs/*/user">
            <xsl:variable name="value" select="@userUsername"/>
            <xsl:value-of select="$value"/>
            <xsl:if test="position() != last()">
              <xsl:value-of select="$delimiter"/>
            </xsl:if>
          </xsl:for-each>
          <xsl:text>"</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>"</xsl:text>
          <xsl:if test="string-length($attrs/*/*[@name=$nameAttr]/value/text()) &gt; 0"> 
            <xsl:variable name="value" select="$attrs/*/*[@name=$nameAttr]/value/text()"/>
            <xsl:value-of select="$value"/>
          </xsl:if>
          <xsl:text>"</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:if test="position() != last()">
        <xsl:value-of select="$delimiter"/>
      </xsl:if>
    
    </xsl:for-each>
  </xsl:template>
  
</xsl:stylesheet>

