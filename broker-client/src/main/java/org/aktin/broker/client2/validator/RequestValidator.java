package org.aktin.broker.client2.validator;

import java.io.IOException;
import java.io.InputStream;

public interface RequestValidator {
	void validate(InputStream in) throws ValidationError, IOException;
}
