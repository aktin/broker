package org.aktin.broker.xml.query;

import java.time.Period;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Abstract query schedule
 * @author R.W.Majeed
 *
 */
@XmlSeeAlso({SingleExecution.class, RepeatedExecution.class})
public abstract class QuerySchedule {

	/**
	 * Duration for the queried data, relative to the request reference date
	 * {@link QueryRequest#referenceDate}. Usually negative: e.g. -D1M for previous month.
	 */
	@XmlElement(required=true)
	public Period duration;

}
