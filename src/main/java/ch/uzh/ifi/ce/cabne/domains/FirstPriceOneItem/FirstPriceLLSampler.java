package ch.uzh.ifi.ce.cabne.domains.FirstPriceOneItem;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.BidSampler;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;

import java.util.Iterator;
import java.util.List;


public class FirstPriceLLSampler extends BidSampler<Double, Double> {

	
	public FirstPriceLLSampler(BNESolverContext<Double, Double> context) {
		super(context);
	}

	public Iterator<Sample> conditionalBidIterator(int i, Double v, Double b, List<Strategy<Double, Double>> s) {
		
		final int opponent = (i + 1) % 2; // if i is 0, then opponent is 1, and reverse
		Iterator<double[]> rngiter = context.getRng(1).nextVectorIterator(); // Dimension is number of valuations of all players except player i's valuations -> v-i
		// in my work it will almost always be n-1, since every player is naive and has only one valuation (doesn't matter on how many goods, since the player will only place exactly one bid on the paket that contains exactly these goods!)
		// watch out: for the iterations later on, where the one player will deviate from the naive strategy - I'm not sure anymore, maybe it stays the same, since his valuation does not change, only his actual bids will change...

		Strategy<Double, Double> s_i = s.get(i);  // s_i is strategy of player i
		Strategy<Double, Double> s_opponent = s.get(opponent);  // s_opponent is strategy of opponent player

		Iterator<Sample> it = new Iterator<Sample>() {
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Sample next() {	// TODO: rewrite this method for two local bidders on one single item!
				double[] r = rngiter.next();
				Double result[] = new Double[2];
				
				result[i] = b;  // vector für bids - 1 pro player, welches resultat wurde gezogen density - wieviel kommts in deinen gezogenen vor vs. in der originalen verteilung

				// slocal referred to the local players strategy, eq for sglobal
				result[opponent] = s_opponent.getBid(r[0] * s_opponent.getMaxValue());

				return new Sample(1.0, result); // sample = klasse für  (importance sampling ist wenn verteilung abgeändert wird, so dass mc sample schneller konvergiert)
			}
		};
		return it;
	}
}
