package org.aktin.broker;

import java.io.Reader;

public interface RequestConverter {

	String getProducedType();
	String getConsumedType();
	
	Reader transform(Reader input);
}
