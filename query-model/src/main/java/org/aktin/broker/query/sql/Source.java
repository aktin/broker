package org.aktin.broker.query.sql;

import java.util.ArrayList;
import java.util.List;

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

	List<String> splitStatements(){
		List<String> commands = new ArrayList<>();
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
				// TODO find and replace placeholders, e.g. ${start.date}, ${end.date}
				commands.add(stmt.toString());
				stmt = new StringBuilder();
			}else{
				// add the line to the statement
				stmt.append(line);
			}
		}
		// additional processing and validation
		return commands;
	}
	
}
