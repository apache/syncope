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
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:wadl="http://wadl.dev.java.net/2009/02"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xalan wadl xs"
                version="1.0">
  
  <xsl:param name="contextPath"/>
  
  <xsl:variable name="namespaces">       
    <xsl:for-each select="/*/namespace::*">
      <namespace prefix="{name()}" url="{.}"/>            
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="namespacePos">       
    <xsl:for-each select="//xs:schema">
      <namespace url="{@targetNamespace}" position="{position()}"/>
    </xsl:for-each>
  </xsl:variable>

  <xsl:template match="/wadl:application">  
    <html lang="en">
      <head>
        <meta charset="utf-8"/>
        <title>          
          <xsl:value-of select="wadl:doc/@title"/>
        </title>

        <link rel="stylesheet" href="{$contextPath}/css/jquery-ui.css"/>
        <link rel="stylesheet" href="{$contextPath}/css/style.css"/>

        <script src="{$contextPath}/js/jquery.js">          
        </script>
        <script src="{$contextPath}/js/jquery-ui.js">          
        </script>
        <script>
          //<![CDATA[
          $(function() {
          //]]>
          <xsl:for-each select="wadl:resources/wadl:resource">
            <xsl:sort select="@path"/>
            <xsl:text>$( "#accordion-</xsl:text>
            <xsl:value-of select="position()"/>
            <xsl:text>" ).accordion({
              collapsible: true,
              heightStyle: "content",
              active: false
              });
            </xsl:text>
            
            <xsl:variable name="parentResourcePath" select="translate(@path, '/{}', '___')"/>
            <xsl:call-template name="dialog-init">
              <xsl:with-param name="resourcePath" select="$parentResourcePath"/>
            </xsl:call-template>
            <xsl:for-each select="wadl:resource">
              <xsl:variable name="childResourcePath" select="translate(@path, '/{}', '___')"/>
              <xsl:call-template name="dialog-init">
                <xsl:with-param name="resourcePath" select="concat($parentResourcePath, $childResourcePath)"/>
              </xsl:call-template>
            </xsl:for-each>
          </xsl:for-each>
          //<![CDATA[
          $( "#tabs" ).tabs().addClass( "ui-tabs-vertical ui-helper-clearfix" );
          $( "#tabs li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
          });
 
          /*
          * hoverIntent | Copyright 2011 Brian Cherne
          * http://cherne.net/brian/resources/jquery.hoverIntent.html
          * modified by the jQuery UI team
          */
          $.event.special.hoverintent = {
          setup: function() {
          $( this ).bind( "mouseover", jQuery.event.special.hoverintent.handler );
          },
          teardown: function() {
          $( this ).unbind( "mouseover", jQuery.event.special.hoverintent.handler );
          },
          handler: function( event ) {
          var currentX, currentY, timeout,
          args = arguments,
          target = $( event.target ),
          previousX = event.pageX,
          previousY = event.pageY;
 
          function track( event ) {
          currentX = event.pageX;
          currentY = event.pageY;
          };
 
          function clear() {
          target
          .unbind( "mousemove", track )
          .unbind( "mouseout", clear );
          clearTimeout( timeout );
          }
 
          function handler() {
          var prop,
          orig = event;
 
          if ( ( Math.abs( previousX - currentX ) +
          Math.abs( previousY - currentY ) ) < 7 ) {
          clear();
 
          event = $.Event( "hoverintent" );
          for ( prop in orig ) {
          if ( !( prop in event ) ) {
          event[ prop ] = orig[ prop ];
          }
          }
          // Prevent accessing the original event since the new event
          // is fired asynchronously and the old event is no longer
          // usable (#6028)
          delete event.originalEvent;
 
          target.trigger( event );
          } else {
          previousX = currentX;
          previousY = currentY;
          timeout = setTimeout( handler, 100 );
          }
          }
 
          timeout = setTimeout( handler, 100 );
          target.bind({
          mousemove: track,
          mouseout: clear
          });
          }
          };
          //]]>
        </script>
      </head>
      <body>
        <h1>
          <xsl:value-of select="wadl:doc/@title"/>
        </h1>        
        
        <h3>Namespaces</h3>                                
        <table>
          <tr>
            <th>Prefix</th>
            <th>URI</th>
            <th>XSD</th>
          </tr>
          <xsl:apply-templates select="wadl:grammars/xs:schema"/>
        </table>
                                                                                              
        <h3>REST resources</h3>                                
        <div id="tabs">
          <ul>
            <xsl:for-each select="wadl:resources/wadl:resource">
              <xsl:sort select="@path"/>
              <li>
                <a href="#tabs-{position()}">
                  <xsl:value-of select="@path"/>
                </a>
              </li>
            </xsl:for-each>
          </ul>
          
          <xsl:apply-templates select="wadl:resources/wadl:resource">
            <xsl:sort select="@path"/>            
          </xsl:apply-templates>
        </div>
      </body>
    </html>       
  </xsl:template>
  
  <xsl:template name="dialog-init">
    <xsl:param name="resourcePath"/>
    
    <xsl:for-each select="wadl:method">
      <xsl:text>$(function() {
        $( "#dialog</xsl:text>
      <xsl:value-of select="$resourcePath"/>_<xsl:value-of select="position()"/>
      <xsl:text>" ).dialog({
        autoOpen: false,
        modal: true,
        height: "auto",
        width: "auto",
        resizable: false
        });
 
        $( "#opener</xsl:text>
      <xsl:value-of select="$resourcePath"/>_<xsl:value-of select="position()"/>
      <xsl:text>" ).click(function() {
        $( "#dialog</xsl:text>
      <xsl:value-of select="$resourcePath"/>_<xsl:value-of select="position()"/>
      <xsl:text>" ).dialog( "open" );
        });
        });
      </xsl:text>
    </xsl:for-each>
  </xsl:template>
  
  <xsl:template match="xs:schema">
    <xsl:variable name="targetNamespace" select="@targetNamespace"/>

    <xsl:variable name="prefix" 
                  select="xalan:nodeset($namespaces)/namespace[@url = $targetNamespace]/@prefix"/>

    <tr>
      <td>
        <xsl:value-of select="$prefix"/>
      </td>
      <td>
        <xsl:value-of select="@targetNamespace"/>
      </td>
      <td>
        <a href="schema_{position()}_{$prefix}.html" 
           onClick="window.open('', 'schema', '', true).focus();" target="schema">
          <xsl:value-of select="$prefix"/>.xsd</a>
      </td>
    </tr>
  </xsl:template>
    
  <xsl:template match="wadl:resource">
    <div id="tabs-{position()}">
      <h2>
        <xsl:value-of select="@path"/>
      </h2>
      
      <xsl:if test="string-length(wadl:doc) &gt; 0">
        <p>
          <xsl:value-of select="wadl:doc/text()" disable-output-escaping="yes"/>
        </p>
      </xsl:if>
            
      <xsl:call-template name="parameters"/>
      
      <xsl:call-template name="methods">
        <xsl:with-param name="resourcePath" select="@path"/>
      </xsl:call-template>
      
      <xsl:variable name="parentPath" select="@path"/>
      <div id="accordion-{position()}">
        <xsl:for-each select="descendant::*[local-name() = 'resource']">
          <xsl:sort select="@path"/>
          <xsl:call-template name="subresource">
            <xsl:with-param name="parentPath" select="$parentPath"/>
          </xsl:call-template>
        </xsl:for-each>
      </div>
    </div>
  </xsl:template>
 
  <xsl:template name="methods">
    <xsl:param name="resourcePath"/>

    <xsl:variable name="escapedPath" select="translate($resourcePath, '/{}', '___')"/>
    <div class="methods">
      <xsl:for-each select="wadl:method">
        <button id="opener{$escapedPath}_{position()}">
          <xsl:value-of select="@name"/>
          <xsl:if test="string-length(@id) &gt; 0">
            <br/>
            (<em>
              <xsl:value-of select="@id"/>
            </em>)
          </xsl:if>
        </button>
        <div id="dialog{$escapedPath}_{position()}" title="{@name} {$resourcePath}">
          <xsl:apply-templates select="."/>
        </div>
      </xsl:for-each>     
    </div>           
  </xsl:template>
  
  <xsl:template name="subresource">
    <xsl:param name="parentPath"/>
    
    <h3>
      <xsl:value-of select="@path"/>
    </h3>
    
    <div>
      <xsl:if test="string-length(wadl:doc) &gt; 0">
        <p>
          <xsl:value-of select="wadl:doc/text()" disable-output-escaping="yes"/>
        </p>
      </xsl:if>

      <xsl:call-template name="parameters"/>

      <xsl:call-template name="methods">
        <xsl:with-param name="resourcePath" select="concat($parentPath, @path)"/>
      </xsl:call-template>
    </div>
  </xsl:template>
 
  <xsl:template match="wadl:method">
    <xsl:if test="string-length(wadl:doc) &gt; 0">
      <p>
        <xsl:value-of select="wadl:doc/text()" disable-output-escaping="yes"/>
      </p>
    </xsl:if>
    
    <xsl:if test="count(wadl:request/@*) + count(wadl:request/*) &gt; 0">
      <xsl:apply-templates select="wadl:request"/>
    </xsl:if>
    <xsl:if test="count(wadl:response/@*) + count(wadl:response/*) &gt; 0">
      <xsl:apply-templates select="wadl:response"/>
    </xsl:if>
  </xsl:template>
  
  <xsl:template match="wadl:request|wadl:response">
    <xsl:call-template name="parameters"/>
    
    <h4>
      R<xsl:value-of select="substring-after(local-name(), 'r')"/>
    </h4>
    
    <xsl:if test="string-length(wadl:doc) &gt; 0">
      <p>
        <xsl:value-of select="wadl:doc/text()" disable-output-escaping="yes"/>
      </p>
    </xsl:if>
    
    <table>
      <xsl:if test="string-length(@status) &gt;0 ">
        <tr>
          <td class="representation-label">Status</td>
          <td>
            <xsl:value-of select="@status"/>
          </td>
        </tr>
      </xsl:if>
      <xsl:if test="count(wadl:representation) &gt; 0">
        <tr>
          <td class="representation-label">Content type</td>
          <td>
            <xsl:if test="count(wadl:representation/@element) &gt; 0">
              <xsl:choose>
                <xsl:when test="starts-with(wadl:representation/@element, 'xs:')">
                  <xsl:value-of select="wadl:representation/@element"/>                  
                </xsl:when>
                <xsl:otherwise>
                  <xsl:variable name="schema-prefix" 
                                select="substring-before(wadl:representation/@element, ':')"/>
                  <xsl:variable name="nsURL" 
                                select="xalan:nodeset($namespaces)/namespace[@prefix = $schema-prefix]/@url"/>
                  <xsl:variable name="schema-position" 
                                select="xalan:nodeset($namespacePos)/namespace[@url = $nsURL]/@position"/>
                  
                  <a href="schema_{$schema-position}_{$schema-prefix}.html#{substring-after(wadl:representation/@element, ':')}"
                     onClick="window.open('', 'schema', '', true).focus();" target="schema">
                    <xsl:value-of select="wadl:representation/@element"/>
                  </a>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
            <xsl:if test="count(wadl:representation/wadl:param) &gt; 0">
              <xsl:value-of select="wadl:representation/wadl:param/@type"/>
            </xsl:if>
            <xsl:if test="count(wadl:representation/wadl:doc) &gt; 0">
              <br/>
              <xsl:value-of select="wadl:representation/wadl:doc/text()" disable-output-escaping="yes"/>
            </xsl:if>            
          </td>
        </tr>
        <tr>
          <td class="representation-label">Media types</td>
          <td>
            <xsl:for-each select="wadl:representation">
              <xsl:value-of select="@mediaType"/>
              <br/>
            </xsl:for-each>
          </td>
        </tr>
      </xsl:if>
    </table>
  </xsl:template>

  <xsl:template name="parameters">
    <xsl:if test="count(wadl:param) &gt; 0">
      <h5>Parameters</h5>
      <table>
        <tr>
          <th>Name</th>
          <th>Description</th>
          <th>Style</th>
          <th>Type</th>
          <th>Default</th>
        </tr>
        <xsl:for-each select="wadl:param">
          <tr>
            <td>
              <xsl:value-of select="@name"/>
            </td>
            <td>
              <xsl:value-of select="wadl:doc/text()" disable-output-escaping="yes"/>
            </td>
            <td>
              <xsl:value-of select="@style"/>
            </td>
            <td>
              <xsl:value-of select="@type"/>
              <xsl:if test="count(wadl:option) &gt; 0">
                <ul>
                  <xsl:for-each select="wadl:option">
                    <li>
                      <xsl:value-of select="@value"/>
                    </li>
                  </xsl:for-each>
                </ul>
              </xsl:if>
            </td>
            <td>
              <xsl:value-of select="@default"/>
            </td>
          </tr>
        </xsl:for-each>
      </table>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
