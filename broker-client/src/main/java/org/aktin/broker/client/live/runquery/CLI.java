package org.aktin.broker.client.live.runquery;




public class CLI  {


	private static void printUsage() {
		System.err.println("Usage: "+CLI.class.getPackageName()+"."+CLI.class.getName()+" [-waitfor=N] -broker=BROKERURI [-auth=CLASS,PARAM] MEDIATYPE1=INPUT1 [...]");
		System.err.println(" -waitfor specifies the number of nodes to wait for. defaults to -1 meaning all nodes currently known.");
		System.err.println(" -broker specifies the broker endpoint URI e.g. http://localhost:8080/broker/");
		System.err.println(" -auth: use authentication class and param. e.g. org.aktin.broker.client2.auth.ApiKeyAuthentication,xxxApiKey123");
		System.err.println(" MEDIATYPE1=INPUT1: request definition to publish. if only one type is specified, INPUT1 can be '-' to read from stdin. Otherwise INPUT1 should indicate a file containing the definition");		
	}

	public static void main(String[] args) {
		printUsage();
		throw new UnsupportedOperationException("Just a stub. Not implemented. Come back later.");
	}
}
