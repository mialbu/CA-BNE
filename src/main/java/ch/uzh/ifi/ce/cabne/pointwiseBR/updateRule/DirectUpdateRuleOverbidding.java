package ch.uzh.ifi.ce.cabne.pointwiseBR.updateRule;

public class DirectUpdateRuleOverbidding implements UpdateRule<Double, Double[]> {

	@Override
	public Double[] update(Double v, Double[] oldbid, Double[] newbid, double oldutility, double newutility) {
		return newbid;
	}
}
