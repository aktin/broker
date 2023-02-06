package org.aktin.broker.client2.validator;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Validates content to be valid JSON. Needs {@code org.json} library.
 * @author R.W.Majeed
 *
 */
public class JsonValidatorFactory implements RequestValidatorFactory, RequestValidator{
	private Constructor<?> jsonObjectConstructor;
	private Constructor<?> jsonArrayConstructor;
	private Charset defaultCharset;
	
	public JsonValidatorFactory() throws ClassNotFoundException, NoSuchMethodException {
		Class<?> jsonObjectClass = Class.forName("org.json.JSONObject");
		jsonObjectConstructor = jsonObjectClass.getConstructor(String.class);
		
		Class<?> jsonArrayClass = Class.forName("org.json.JSONArray");
		jsonArrayConstructor = jsonArrayClass.getConstructor(String.class);
		
		defaultCharset = StandardCharsets.UTF_8;
		// TODO later version could support loading specific json schema to check against
	}
	
	@Override
	public RequestValidator getValidator(String mediaType) throws IllegalArgumentException {
		
		// TODO check for XML media types, e.g. */*+json[;$]
		return this;
	}

	@Override
	public void validate(InputStream in) throws IOException, ValidationError {

		String str = new String(in.readAllBytes(), defaultCharset);
		try {
            jsonObjectConstructor.newInstance(str);
    		// validation successful. input parsed as JSON array.
    		return;
        } catch (Exception e) {
        	if( e.getClass().getName().equals("org.json.JSONException") ) {
        		// validation failed, try parsing JSON array
        		try {
                    jsonArrayConstructor.newInstance(str);
            		// validation successful. input parsed as JSON array.
            		return;
                } catch (Exception e2) {
                	if( e.getClass().getName().equals("org.json.JSONException") ) {
                		throw new ValidationError(e);
                	}else {
                		throw new IOException("Unexpected error during JSON array validation", e2);
                	}
                }
        	}else {
        		throw new IOException("Unexpected error during JSON validation", e);
        	}
        }
	}

}
