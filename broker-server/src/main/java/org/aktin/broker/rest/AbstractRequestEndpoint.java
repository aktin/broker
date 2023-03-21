package org.aktin.broker.rest;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.util.RequestConverter;
import org.aktin.broker.util.RequestTypeManager;

public abstract class AbstractRequestEndpoint {

	protected abstract RequestTypeManager getTypeManager();
	protected abstract BrokerBackend getBroker();


	/**
	 * Remove charset information from media type.
	 * @param type media type
	 * @return media type without charset info
	 */
	public static MediaType removeCharsetInfo(MediaType type){
		// TODO other media type parameters are not preserved (e.g. ;version=1.2, do we need these?
		return new MediaType(type.getType(), type.getSubtype());
	}

	protected Response getRequest(int requestId, List<MediaType> accept) throws SQLException, IOException, NotFoundException, NotAcceptableException{
		MediaType[] available = getTypeManager().createMediaTypes(getBroker().getRequestTypes(requestId));
		if( available.length == 0 ){
			throw new NotFoundException();
		}
		// find acceptable request definition
		RequestConverter rc = getTypeManager().buildConverterChain(accept, Arrays.asList(available));
	
		if( rc == null ){
			// no acceptable response type available
			throw new NotAcceptableException();
			// could also return Response.notAcceptable(Variant.mediaTypes(available).build()).build();
		}else{
			Reader def = getBroker().getRequestDefinition(requestId, rc.getConsumedType());
			// transform
			def = rc.transform(def);
			// output using UTF-8. The HTTP default charset ISO-8859-1 has some missing characters like e.g Euro sign.
			return Response.ok(def, MediaType.valueOf(rc.getProducedType()).withCharset("UTF-8")).build();
		}
	}

}
