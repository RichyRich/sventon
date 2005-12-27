<%
/*
 * ====================================================================
 * Copyright (c) 2005 Sventon Project. All rights reserved.
 *
 * This software is licensed as described in the file LICENSE, which
 * you should have received as part of this distribution. The terms
 * are also available at http://sventon.berlios.de.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
%>
<%@ page import="de.berlios.sventon.svnsupport.LogEntryActionType"%>
<%@ page import="org.tmatesoft.svn.core.SVNLogEntryPath"%>
<%@ page import="org.tmatesoft.svn.core.SVNLogEntry"%>
<%@ page import="java.util.*"%>
<%@ page session="false"%>
<%@ include file="/WEB-INF/jspf/sventonbar.jsp"%>
<spring:hasBindErrors name="command"><c:set var="hasErrors" scope="page" value="true"/></spring:hasBindErrors>

<table class="sventonTopTable" border="0">
  <form name="searchForm" action="search.svn" method="get" onsubmit="return doSearch(searchForm);">
    <tr>
      <c:choose>
        <c:when test="${!empty numrevision}">
          <td class="sventonHeadlines">
            Revision: ${command.revision} (${numrevision})
          </td>
        </c:when>
        <c:otherwise>
          <td class="sventonHeadlines" style="color: #ff0000">
            Revision: ${command.revision}
          </td>
        </c:otherwise>
      </c:choose>

      <td align="right" style="white-space: nowrap;">Search current directory and below <input type="text" name="sventonSearchString" class="sventonSearchField" value=""/><input type="submit" value="go!"/><input type="hidden" name="startDir" value="${command.pathPart}"/></td>
    </tr>
    <tr>
      <td><a href="javascript:toggleElementVisibility('latestCommitInfoDiv'); changeHideShowDisplay('latestCommitLink');">[<span id="latestCommitLink">show</span> latest commit info]</a></td>
    </tr>
    <tr>
      <td style="white-space: nowrap;">
        <%@ include file="/WEB-INF/jspf/latestcommit.jsp"%>
      </td>
    </tr>
    <c:set var="command" value="${command}"/>
    <jsp:useBean id="command" type="de.berlios.sventon.command.SVNBaseCommand" />
    <tr>
      <td class="sventonHeadlines" colspan="2">
       Repository path:<br/><a href="repobrowser.svn?path=/&revision=${command.revision}">
        ${url} <% if (!"".equals(command.getMountPoint(false))) { %>/ <%= command.getMountPoint(true) %><% } %></a> /
        <c:forTokens items="${command.pathNoLeaf}" delims="/" var="pathSegment">
          <c:set var="accuPath" scope="page" value="${accuPath}${pathSegment}/"/>
          <c:choose>
            <c:when test="${hasErrors}">
              ${pathSegment}
            </c:when>
            <c:otherwise>
          <a href="repobrowser.svn?path=/${accuPath}&revision=${command.revision}">${pathSegment}</a>
        </c:otherwise>
          </c:choose>
           /
        </c:forTokens>
        ${command.target}
      </td>
    </tr>
    <!-- Needed by ASVNTC -->
    <input type="hidden" name="path" value="${command.path}${entry.name}"/>
    <input type="hidden" name="revision" value="${command.revision}"/>
  </form>
</table>

<spring:hasBindErrors name="command">
  <table class="sventonSpringErrorMessageTable">
    <tr><td><font color="#FF0000"><spring:message code="${errors.globalError.code}" text="${errors.globalError.defaultMessage}"/></font></td></tr>
  </table>
</spring:hasBindErrors>

<form name="gotoForm" method="post" action="repobrowser.svn">
<table class="sventonRepositoryFunctionsTable">
<tr>
<td><font color="#FF0000"><spring:bind path="command.revision">${status.errorMessage}</spring:bind></font></td>
<td><font color="#FF0000"><spring:bind path="command.path">${status.errorMessage}</spring:bind></font></td>
</tr>
 <tr>
 <td>Go to revision</td><td colspan="2">Go to path <% if (!"".equals(command.getMountPoint(false))) { %>(from: <%= command.getMountPoint(false) %>)<% } %></td>
 </tr>
<tr>
<td><spring:bind path="command.revision"><input class="sventonRevision" type="text" name="revision" value="${status.value}"/></spring:bind></td>
<td><spring:bind path="command.pathPart"><input class="sventonGoTo" id="goToPath" type="text" name="path" value="${status.value}" /></spring:bind></td>
<td><input class="sventonGoToSubmit" type="submit" value="go to"/></td>
<td><input class="sventonFlattenSubmit" type="button" value="flatten dirs" onclick="javascript: return doFlatten();"/></td>

</tr>
</table>
</form>

