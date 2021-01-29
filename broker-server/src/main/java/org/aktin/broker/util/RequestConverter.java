package org.aktin.broker.util;

import java.io.Reader;

public interface RequestConverter {

	String getProducedType();
	String getConsumedType();
	
	Reader transform(Reader input);
}
