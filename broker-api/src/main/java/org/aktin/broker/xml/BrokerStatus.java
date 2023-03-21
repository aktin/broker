package org.aktin.broker.xml;

import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

@XmlRootElement(name="broker-status")
@XmlAccessorType(XmlAccessType.FIELD)
public class BrokerStatus {

	Instant timestamp;
	Period uptime;
	/**
	 * Version info for latest software modules for the nodes (e.g. broker
	 * client). The first element of this list should be "broker-api"
	 * and contain the API version used by the server.
	 */
	@XmlElementWrapper(name="node-software")
	@XmlElement(name="module")
	@Getter
	List<SoftwareModule> software;

	
	protected BrokerStatus(){
	}

	public static BrokerStatus create(){
		BrokerStatus b = new BrokerStatus();
		b.timestamp = Instant.now();
		// TODO calculate uptime
		b.software = new ArrayList<>();
		b.software.add(SoftwareModule.BROKER_API);
		return b;
	}

	@Override
	public String toString(){
		return "BrokerStatus[timestamp="+timestamp.toString()+", api-version="+software.get(0).version+"]";
	}
}
