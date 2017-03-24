package org.aktin.broker;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.aktin.broker.db.BrokerBackend;

public abstract class AbstractRequestEndpoint {

	protected abstract RequestTypeManager getTypeManager();
	protected abstract BrokerBackend getBroker();


	public static MediaType removeCharsetInfo(MediaType type){
		// TODO other media type parameters are not preserved (e.g. ;version=1.2, do we need these?
		return new MediaType(type.getType(), type.getSubtype());
	}

	protected Response getRequest(int requestId, List<MediaType> accept) throws SQLException, IOException, NotFoundException{
		MediaType[] available = getTypeManager().createMediaTypes(getBroker().getRequestTypes(requestId));
		if( available.length == 0 ){
			throw new NotFoundException();
		}
		// find acceptable request definition
		RequestConverter rc = getTypeManager().buildConverterChain(accept, Arrays.asList(available));
	
		if( rc == null ){
			// no acceptable response type available
			return Response.notAcceptable(Variant.mediaTypes(available).build()).build();			
		}else{
			Reader def = getBroker().getRequestDefinition(requestId, rc.getConsumedType());
			// transform
			def = rc.transform(def);
			return Response.ok(def, MediaType.valueOf(rc.getProducedType())).build();
		}
	}

}
