package org.aktin.broker.client2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import javax.xml.bind.JAXB;
import javax.xml.transform.TransformerException;

import org.aktin.broker.client.BrokerAdmin;
import org.aktin.broker.client.ResponseWithMetadata;
import org.aktin.broker.xml.NodeList;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.RequestTargetNodes;
import org.aktin.broker.xml.ResultInfo;
import org.aktin.broker.xml.ResultList;
import org.aktin.broker.xml.util.Util;
import org.w3c.dom.Node;


public class BrokerAdmin2 extends AbstractBrokerClient<AdminNotificationListener> implements BrokerAdmin{
	public BrokerAdmin2(URI endpointURI) {
		super();
		setEndpoint(endpointURI);
	}

	public <T> HttpResponse<T> getRequestDefinition(int requestId, String mediaType, BodyHandler<T> handler) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId)
				.header(ACCEPT_HEADER, mediaType)
				.GET().build();
		return sendRequest(req, handler);
		// 		return getRequestDefinition(getQueryURI(id), mediaType);

	}
	@Override
	protected URI getQueryBaseURI() {
		return resolveBrokerURI("request/");
	}

	@Override
	public Reader getRequestDefinition(int requestId, String mediaType) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId)
				.header(ACCEPT_HEADER, mediaType)
				.GET().build();
		// TODO use streaming instead of string (memory)
		HttpResponse<String> resp = sendRequest(req, BodyHandlers.ofString());
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}
		return new StringReader(resp.body());

	}

	@Override
	public Reader getRequestNodeMessage(int requestId, int nodeId) throws IOException {
		HttpResponse<String> resp = getRequestNodeMessage(requestId, nodeId, BodyHandlers.ofString());
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			// TODO more useful exception
			throw new FileNotFoundException();
		}else {
//			return Utils.contentReaderForInputStream(resp.body(), resp.headers().firstValue(CONTENT_TYPE_HEADER).orElse(null), StandardCharsets.ISO_8859_1);
			return new StringReader(resp.body());
		}
	}
	
	public <T> HttpResponse<T> getRequestNodeMessage(int requestId, int nodeId, BodyHandler<T> handler) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId+"/status/"+nodeId).GET().build();
		return sendRequest(req, handler);
	}

	private HttpRequest createNodeResourceRequest(int nodeId, String resourceId) throws IOException {
		return createBrokerRequest("node/"+nodeId+"/"+URLEncoder.encode(resourceId,"UTF-8")).GET().build();
	}
	public <T> HttpResponse<T> getNodeResourceResponse(int nodeId, String resourceId, BodyHandler<T> handler) throws IOException {
		HttpRequest req = createNodeResourceRequest(nodeId, resourceId);
		return sendRequest(req, handler);
	}

	@Override
	public ResponseWithMetadata getNodeResource(int nodeId, String resourceId) throws IOException {
		HttpResponse<InputStream> resp = getNodeResourceResponse(nodeId, resourceId, BodyHandlers.ofInputStream());
		return wrapResource(resp, resourceId);
	}

	@Override
	public <T> T getNodeResourceJAXB(int nodeId, String resourceId, Class<T> type) throws IOException {
		try( InputStream in = getNodeResourceResponse(nodeId, resourceId, BodyHandlers.ofInputStream()).body() ){
			return JAXB.unmarshal(in, type);
		}
	}

	@Override
	public Properties getNodeProperties(int nodeId, String resourceId) throws IOException {
		try( InputStream in = getNodeResourceResponse(nodeId, resourceId, BodyHandlers.ofInputStream()).body() ){
			Properties p = new Properties();
			p.loadFromXML(in);
			return p;
		}
	}

	@Override
	public String getNodeString(int nodeId, String resourceId) throws IOException {
		HttpResponse<String> resp = getNodeResourceResponse(nodeId, resourceId, BodyHandlers.ofString());
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}else {
			return resp.body();
		}
	}

	@Override
	public int createRequest() throws IOException {
		HttpRequest req = createBrokerRequest("request")
				.POST(BodyPublishers.noBody()).build();
		return sendAndExpectRequestCreated(req);
	}

//	@Override
//	public int createRequest(String contentType, OutputWriter writer) throws IOException {
//		throw new UnsupportedOperationException();
//	}

	private int getQueryId(URI uri){
		return Integer.parseInt(getQueryBaseURI().relativize(uri).getPath());
	}

	private int sendAndExpectRequestCreated(HttpRequest req) throws IOException {
		HttpResponse<Void> resp = sendRequest(req, BodyHandlers.discarding());
		
		if( resp.statusCode() != HTTP_STATUS_201_CREATED ) {
			throw new IOException("Unexpected response status "+resp.statusCode());
		}
		String location = resp.headers().firstValue(LOCATION_HEADER).orElse(null);
		if( location == null ) {
			throw new IOException("No location in response headers");
		}
		
		try {
			return getQueryId(new URI(location));
		} catch (URISyntaxException e) {
			throw new IOException("Invalid location URI in response headers: "+location);
		}
	}
	public int createRequest(String contentType, BodyPublisher publisher) throws IOException {
		HttpRequest req = createBrokerRequest("request")
				.header(CONTENT_TYPE_HEADER, contentType)
				.POST(publisher).build();
		return sendAndExpectRequestCreated(req);
	}

//	@Override
//	public int createRequest(String contentType, InputStream content) throws IOException {
//		return createRequest(contentType, BodyPublishers.ofInputStream(singleSupplier(content)));
//	}

	@Override
	public int createRequest(String contentType, Node content) throws IOException {
		StringWriter w = new StringWriter();
		
		try {
			Util.writeDOM(content, w, "UTF-8");
		} catch (TransformerException e) {
			throw new IOException("Failed to serialize DOM to string");
		}
		
		return createRequest(MEDIATYPE_APPLICATION_XML_UTF8, w.toString());
	}

	@Override
	public int createRequest(String contentType, String content) throws IOException {
		return createRequest(contentType, BodyPublishers.ofString(content));
	}

	@Override
	public void deleteRequest(int requestId) throws IOException {
		// delete request from broker
		HttpRequest req = createBrokerRequest("request/"+requestId).DELETE().build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
		// delete results from aggregator
		req = createAggregatorRequest("request/"+requestId+"/result")
				.DELETE()
				.build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public void publishRequest(int requestId) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId+"/publish").POST(BodyPublishers.noBody()).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public void closeRequest(int requestId) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId+"/close").POST(BodyPublishers.noBody()).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

//	@Override
//	public void putRequestDefinition(int requestId, String contentType, OutputWriter writer) throws IOException {
//		throw new UnsupportedOperationException();
//	}

	public void putRequestDefinition(int requestId, String contentType, BodyPublisher publisher) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId)
				.header(CONTENT_TYPE_HEADER, contentType)
				.PUT(publisher).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public void putRequestDefinition(int requestId, String contentType, String content) throws IOException {
		putRequestDefinition(requestId, contentType, BodyPublishers.ofString(content));
	}

	@Override
	public List<org.aktin.broker.xml.Node> listNodes() throws IOException {
		HttpRequest req = createBrokerRequest("node").GET().build();
		NodeList nl = sendAndExpectJaxb(req, NodeList.class);
		if( nl.getNodes() == null ){
			return Collections.emptyList();
		}else{
			return nl.getNodes();
		}
	}

	@Override
	public org.aktin.broker.xml.Node getNode(int nodeId) throws IOException {
		HttpRequest req = createBrokerRequest("node/"+nodeId).GET().build();
		return sendAndExpectJaxb(req, org.aktin.broker.xml.Node.class);
	}

	@Override
	public List<RequestInfo> listAllRequests() throws IOException {
		HttpRequest req = createBrokerRequest("request").GET().build();
		RequestList resp = sendAndExpectJaxb(req, RequestList.class);
		return postprocessRequestList(resp);
	}

	public List<RequestInfo> listRequestsFiltered(String mediaType, String predicate) throws IOException {
		String urispec = "request/filtered?type="+URLEncoder.encode(mediaType, StandardCharsets.UTF_8)+"&predicate="+URLEncoder.encode(predicate,StandardCharsets.UTF_8);
		
		HttpRequest req = createBrokerRequest(urispec).GET().build();
		
		RequestList resp = sendAndExpectJaxb(req, RequestList.class);
		return postprocessRequestList(resp);
	}

	@Override
	public RequestInfo getRequestInfo(int requestId) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId).method("OPTIONS", BodyPublishers.noBody()).build();
		return sendAndExpectJaxb(req, RequestInfo.class);
	}

	@Override
	public List<RequestStatusInfo> listRequestStatus(int requestId) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId+"/status")
				.header(ACCEPT_HEADER, "application/xml")
				.GET().build();
		RequestStatusList list = sendAndExpectJaxb(req, RequestStatusList.class);
		if( list == null || list.getStatusList() == null ) {
			return Collections.emptyList();
		}else {
			return list.getStatusList();
		}
	}

	@Override
	public List<ResultInfo> listResults(int requestId) throws IOException {
		HttpRequest req = createAggregatorRequest("request/"+requestId+"/result")
				.header(ACCEPT_HEADER, "application/xml")
				.GET().build();
		ResultList list = sendAndExpectJaxb(req, ResultList.class);
		if( list == null || list.getResults() == null ) {
			return Collections.emptyList();
		}else {
			return list.getResults();
		}
	}

	@Override
	public String getResultString(int requestId, int nodeId) throws IOException {
		return getResult(requestId, nodeId, BodyHandlers.ofString()).body();
	}
	public <T> HttpResponse<T> getResult(int requestId, int nodeId, BodyHandler<T> handler) throws IOException {
		HttpRequest.Builder rb = createAggregatorRequest("request/"+requestId+"/result/"+nodeId).GET();
		return sendRequest(rb.build(), handler);
	}

	@Override
	public ResponseWithMetadata getResult(int requestId, int nodeId) throws IOException {
		HttpResponse<InputStream> resp = getResult(requestId, nodeId, BodyHandlers.ofInputStream());
		return wrapResource(resp, requestId+"_result_"+nodeId);
	}
    
    public String getAggregatedResultUUID(int requestId) throws IOException {
        HttpRequest req = createBrokerRequest("export/request-bundle/" + requestId)
                .POST(BodyPublishers.noBody()).build();
        HttpResponse<String> response = sendRequest(req, BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new IOException("Unexpected response code " + response.statusCode() + " instead of expected 200");
        return response.body();
    }
    
    public <T> HttpResponse<T> getAggregatedResult(String uuid, BodyHandler<T> handler) throws IOException {
        HttpRequest req = createBrokerRequest("download/" + uuid).GET().build();
        return sendRequest(req, handler);
    }
    
    @Override
    public ResponseWithMetadata getAggregatedResult(String uuid) throws IOException {
        HttpResponse<InputStream> response = getAggregatedResult(uuid, BodyHandlers.ofInputStream());
        String filename = getFileNameFromHeaders(response.headers());
        return wrapResource(response, filename);
    }
    
    private String getFileNameFromHeaders(HttpHeaders headers) {
        Optional<String> dispositionHeader = headers.firstValue("Content-Disposition");
        if (dispositionHeader.isPresent()) {
            String disposition = dispositionHeader.get();
            int startIdx = disposition.indexOf("filename=\"");
            if (startIdx != -1) {
                int endIdx = disposition.indexOf("\"", startIdx + 10); // 10 is the length of 'filename="'
                if (endIdx != -1) {
                    return disposition.substring(startIdx + 10, endIdx);
                }
            }
        }
        return null;
    }

	@Override
	public int[] getRequestTargetNodes(int requestId) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId+"/nodes")
				.header(ACCEPT_HEADER, "application/xml")
				.GET().build();
		RequestTargetNodes list = sendAndExpectJaxb(req, RequestTargetNodes.class);
		// not-found === no target restriction === return null
		if( list == null ) {
			return null;
		}else {
			return list.getNodes();
		}
	}

	@Override
	public void setRequestTargetNodes(int requestId, int[] nodes) throws IOException {
		RequestTargetNodes tn = new RequestTargetNodes(nodes);
		HttpRequest req = putJAXB(createBrokerRequest("request/"+requestId+"/nodes"), tn).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public void clearRequestTargetNodes(int requestId) throws IOException {
		HttpRequest req = createBrokerRequest("request/"+requestId+"/nodes")
				.DELETE().build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}
	@Override
	public String getWebsocketPath() {
		return "websocket";
	}

	@Override
	protected void onWebsocketText(String text) {
		String[] args = text.split(" ", 4);
			switch( args[0] ) {
			case "created": // request created (requestId)
				for( AdminNotificationListener listener : listeners )
				listener.onRequestCreated(Integer.valueOf(args[1]));
				break;
			case "published": // request published (requestId)
				for( AdminNotificationListener listener : listeners )
				listener.onRequestPublished(Integer.valueOf(args[1]));
				break;
			case "closed": // request closed (requestId)
				for( AdminNotificationListener listener : listeners )
				listener.onRequestClosed(Integer.valueOf(args[1]));
				break;
			case "status": // request status reported by node (requestId, nodeId, status)
				for( AdminNotificationListener listener : listeners )
				listener.onRequestStatusUpdate(Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3]);
				break;
			case "result": // request result uploaded by node (requestId, nodeId, mediaType)
				for( AdminNotificationListener listener : listeners )
				listener.onRequestResultUpdate(Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3]);
				break;
			case "resource": // resource changed by node (nodeId, resourceId)
				for( AdminNotificationListener listener : listeners )
				listener.onResourceUpdate(Integer.valueOf(args[1]), args[2]);
				break;
			case "pong": // ping response
				for( AdminNotificationListener listener : listeners )
				listener.onPong(args[1]);
				break;
			default:
				// ignoring unsupported websocket command
				// TODO log warning
			}
	}

}
