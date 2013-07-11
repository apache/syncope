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

  <xsl:template match="reportlet[@class='org.apache.syncope.core.report.RoleReportlet']">
    <h2>Reportlet: <xsl:value-of select="@name"/></h2>
    <xsl:for-each select="role">
      <h3>Role <xsl:value-of select="@name"/></h3>
      
      <table style="border: 1px solid black;">
        <tr>
          <td>Id:</td>
          <td>
            <xsl:value-of select="@id"/>
          </td>
        </tr>
        <xsl:if test="@roleOwner != 'null'"> <!--!= null test="not(USER/FIRSTNAME)" -->
          <tr>
            <td>Role Owner:</td>
            <td>
              <xsl:value-of select="@roleOwner"/>
            </td>
          </tr>
        </xsl:if>
        <xsl:if test="@userOwner != 'null'">
          <tr>
            <td>User Owner:</td>
            <td>
              <xsl:value-of select="@userOwner"/>
            </td>
          </tr>
        </xsl:if>
        
      </table>

      <xsl:choose>
        <xsl:when test="string-length(attributes/attribute) &gt; 0">
          <xsl:call-template name="attributes">
            <xsl:with-param name="label">Attributes</xsl:with-param>
            <xsl:with-param name="node" select="attributes/attribute"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <h5>THIS ROLE HASN'T ANY ATTRIBUTE</h5>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:choose>
        <xsl:when test="string-length(derivedAttributes/derivedAttribute) &gt; 0">
          <xsl:call-template name="attributes">
            <xsl:with-param name="label">Derived Attributes</xsl:with-param>
            <xsl:with-param name="node" select="derivedAttributes/derivedAttribute"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <h5>THIS ROLE HASN'T ANY DERIVED ATTRIBUTE</h5>
        </xsl:otherwise>
      </xsl:choose>
      <!--</xsl:if>-->
      <xsl:choose>
        <xsl:when test="string-length(virtualAttributes/virtualAttribute) &gt; 0">
          <xsl:call-template name="attributes">
            <xsl:with-param name="label">Virtual Attributes</xsl:with-param>
            <xsl:with-param name="node" select="virtualAttributes/virtualAttribute"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <h5>THIS ROLE HASN'T ANY VIRTUAL ATTRIBUTE</h5>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
        <xsl:when test="entitlements/entitlement">
          <xsl:call-template name="entitlements">
            <xsl:with-param name="label">Entitlements: </xsl:with-param>
            <xsl:with-param name="node" select="entitlements/entitlement"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <h5>THIS ROLE HASN'T ANY ENTITLEMENT</h5>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:choose>
        <xsl:when test="users/user">
          <h4>Users</h4>
          <xsl:for-each select="users/user">
            <h5>User: <xsl:value-of select="@userUsername"/> (Id: <xsl:value-of select="@userId"/>)</h5>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <h5>THIS ROLE HASN'T ANY USER ASSIGNED TO</h5>
        </xsl:otherwise>
      </xsl:choose>
      
      <xsl:call-template name="roleResources">
        <xsl:with-param name="node" select="resources/resource"/>
      </xsl:call-template>
      <hr/>
      
    </xsl:for-each>
  </xsl:template>
 
  <!--entitlement template-->
  <xsl:template name="entitlements">
    <xsl:param name="label"/>
    <xsl:param name="node"/>

    <h4>
      <xsl:value-of select="$label"/>
    </h4>
    
    <table>
      
      <tbody>
        <xsl:for-each select="$node">
          <tr>
            <td>
              <xsl:value-of select="@id"/>
            </td>
          </tr>
        </xsl:for-each>
      </tbody>
    </table>
  </xsl:template>


  <xsl:template name="attributes">
    <xsl:param name="label"/>
    <xsl:param name="node"/>

    <h4>
      <xsl:value-of select="$label"/>
    </h4>
    
    <table>
      <thead>
        <tr>
          <th>Schema name</th>
          <th>Value(s)</th>
        </tr>
      </thead>
      <tbody>
        <xsl:for-each select="$node">
          <xsl:if test="string-length(value/text()) &gt; 0">
            <tr>
              <td>
                <xsl:value-of select="@name"/>
              </td>
              <td>
                <ul>
                  <xsl:for-each select="value">
                    <li>
                      <xsl:value-of select="text()"/>
                    </li>
                  </xsl:for-each>
                </ul>
              </td>
            </tr>
          </xsl:if>
        </xsl:for-each>
      </tbody>
    </table>
  </xsl:template>

  <xsl:template name="roleResources">
    <xsl:param name="node"/>
    
    <h4>Role Resources</h4>
    <ul>
      <xsl:for-each select="$node">
        <li>
          <xsl:value-of select="@name"/>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>
</xsl:stylesheet>