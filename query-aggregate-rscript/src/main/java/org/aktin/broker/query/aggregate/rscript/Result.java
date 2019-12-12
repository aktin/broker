package org.aktin.broker.query.aggregate.rscript;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)

public class Result {
	@XmlAttribute
	String type;
	@XmlAttribute
	String file;
	/**
	 * Whether the result file is required to be produced.
	 * Defaults to {@code true}. Set to {@code false} if the
	 * result file is not always generated.
	 */
	@XmlAttribute
	Boolean required;

	protected Result() {
	}
	public Result(String file, String type) {
		this.file = file;
		this.type = type;
	}
	public boolean getRequired() {
		if( required == null ) {
			return true;
		}else {
			return required.booleanValue();
		}
	}
}
