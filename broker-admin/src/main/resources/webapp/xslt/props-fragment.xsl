<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="/">
	<html><body>
			<table border="1"><tbody>
				<xsl:apply-templates select="/properties/entry"/>
			</tbody></table>		
	</body></html>
</xsl:template>


<xsl:template match="/properties/entry">
	<tr>
		<td><xsl:value-of select="@key"/></td>
		<td align="right"><xsl:value-of select="."/></td>
	</tr>
</xsl:template>

</xsl:stylesheet> 