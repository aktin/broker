package org.aktin.broker.query.xml;

import javax.xml.bind.JAXB;

import org.aktin.broker.query.util.XIncludeUnmarshaller;
import org.junit.Assert;
import org.junit.Test;

public class TestRequest {

	public static final QueryRequest getSingleRequest(int requestId, int queryId){
		QueryRequest q = JAXB.unmarshal(XIncludeUnmarshaller.getXIncludeResource("/request.xml"), QueryRequest.class);
		q.id = requestId;
		q.query.id = queryId;
		return q;
	}

	@Test
	public void expectUnmarshalledDocumentComplete(){
		QueryRequest r = getSingleRequest(1, 1);
		Assert.assertNotNull(r.published);
		Assert.assertNotNull(r.deadline);
		Assert.assertNotNull(r.query);
	}

}
