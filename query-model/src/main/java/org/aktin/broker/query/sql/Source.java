package org.aktin.broker.query.sql;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.NONE)
public class Source {
	@XmlAttribute
	String type;
	@XmlAttribute(name="date-format")
	String dateFormat;
	@XmlAttribute(name="command-separator")
	String commandSeparator;
	// XXX use javatypeadapter to serialize to/from string
	@XmlValue
	String value;

	/**
	 * Parse the source code into separate statements.
	 * Placeholder substitution is performed for ${key} if a lookup function is defined.
	 * @param placeholderSubstitution 
	 *  lookup function for placeholder substitution. Set to ${code null} to disable
	 *  placeholder lookup.
	 * @return statement list
	 * @throws SubstitutionError if the substitution failed
	 */
	void splitStatements(Function<String,String> placeholderSubstitution, Consumer<String> handler) throws SubstitutionError{
		String[] lines = value.split("\\r\\n|\\n|\\r");
		StringBuilder stmt = new StringBuilder();
		for( int i=0 ; i<lines.length; i++ ){
			String line = lines[i].trim();
			// check for comment
			if( line.startsWith("--") ){
				// found comment
				continue;
			}
			if( line.endsWith(";") ){
				// end of command
				// append to statement w/o ;
				stmt.append(line.substring(0, line.length()-1));
				if( placeholderSubstitution != null ){
					replacePlaceholders(stmt, placeholderSubstitution);
				}
				handler.accept(stmt.toString());
				stmt = new StringBuilder();
			}else{
				// add the line to the statement
				stmt.append(line);
			}
		}
	}

	void replacePlaceholders(StringBuilder string, Function<String, String> lookup) throws SubstitutionError{
		int s = string.indexOf("${");
		while( s != -1 ){
			int e = string.indexOf("}", s+2);
			if( e == -1 ){
				throw new SubstitutionError("Unterminated ${ at pos "+s+" in "+string);
			}
			String key = string.substring(s+2, e);
			String replacement = lookup.apply(key);
			if( replacement == null ){
				throw new SubstitutionError("No substitution found for ${"+key+"}");
			}
			// replace the placeholder
			string.replace(s, e+1, replacement);
			// adjust changed string length to continue parsing
			e -= 3; // ${} removed
			e -= (key.length() - replacement.length()); // difference of key and replacement
			// find next property
			s = string.indexOf("${", e);
		}
	}
	
}
