package org.aktin.broker;

import java.io.Reader;
import java.util.List;

import javax.ws.rs.core.MediaType;

/**
 * A request type manager allows verification of request
 * resources against a specified media type as well as
 * conversion to other types.
 * <p>
 * The type conversion is done on demand per request. 
 * The result is transferred to the client and not
 * stored in the database.
 * </p>
 * @author R.W.Majeed
 *
 */
public class RequestTypeManager {

	/**
	 * Get verifier for the specified media type.
	 * Unsupported media types are indicated with
	 * a {@code null} response, in which case the
	 * appropriate error is returned (e.g. unsupported 
	 * media type 415 for POST)
	 */
	// RequestVerifier verifierForType(MediaType type)
	/**
	 * If a GET request with a specified Accept media type
	 * can not be satisfied from the database, a converter is
	 * searched (which is expected to produce that type).
	 * <p>
	 * If a converter is available for the specified media
	 * type, the converter's input type is retrieved and
	 * checked that the input type can be satisfied from
	 * the database or if additional converters need to
	 * be chained.
	 * </p>
	 * @param out produced type
	 * @param in consumed type
	 * @return converter converting between the given types, or {@code null} if not found.
	 */
	RequestConverter converterForType(String out, String in){
		return null;
	}
	
	/**
	 * Build a converter chain which produces an acceptable 
	 * (in order of preference) request and consumes one of
	 * the available resource types. 
	 * <p>
	 * The returned RequestConverter may be a virtual converter 
	 * which invokes multiple chained converters to produce the 
	 * desired result. The chained converter path may be cached
	 * to speed up successive requests.
	 * </p>
	 * @param accept list of accepted types
	 * @param resources list of available resource types
	 * @return converter or {@code null} if not possible.
	 */
	RequestConverter buildConverterChain(List<MediaType> accept, List<MediaType> resources){
		// find out whether physical resource is acceptable
		for( MediaType t : accept ){
			for( MediaType resource : resources ){
				if( t.isCompatible(resource) ){
					return new IdentityTransform(resource);
				}
			}
		}
		// TODO use converters to produce derived resources
		return null;
	}
	public MediaType[] createMediaTypes(List<String> types){
		// convert to MediaType instances
		MediaType[] res = new MediaType[types.size()];
		int index = 0;
		for( String type : types ){
			res[index] = MediaType.valueOf(type);
			index ++;
		}
		return res;
	}
	private static class IdentityTransform implements RequestConverter{
		private MediaType type;
		public IdentityTransform(MediaType type){
			this.type = type;
		}
		@Override
		public String getProducedType() {
			return type.toString();
		}

		@Override
		public String getConsumedType() {
			return type.toString();
		}

		@Override
		public Reader transform(Reader input) {
			return input;
		}
		
	}
}
