<%@page contentType="application/xml;charset=UTF-8" pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><jsp:useBean id="export" scope="request" type="java.lang.String"
/><c:out value="${export}" escapeXml="false"/>
