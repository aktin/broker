<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:b="http://aktin.org/ns/exchange">

<xsl:template match="/">
	<html><body>
			<table border="1">
				<thead>
					<tr>
						<td>ID</td>
						<td>Published</td>
						<td>Closed</td>
						<td colspan="3">Actions</td>
					</tr>
				</thead>
				<tbody>
					<xsl:apply-templates select="/b:request-list/b:request"/>
				</tbody></table>		
	</body></html>
</xsl:template>


<xsl:template match="/b:request-list/b:request">
	<tr class="req" data-id="{@id}">
		<td class="show"><xsl:value-of select="@id"/></td>
		<td><xsl:value-of select="b:published"/></td>
		<td><xsl:value-of select="b:closed"/></td>
		<td class="export"><a href="javascript:exportRequest({@id})">export</a></td>
		<td class="delete"><a href="javascript:deleteRequest({@id})">delete</a></td>
		<td class="show"><a href="request.html#{@id}">details</a></td>
	</tr>
</xsl:template>

</xsl:stylesheet> 