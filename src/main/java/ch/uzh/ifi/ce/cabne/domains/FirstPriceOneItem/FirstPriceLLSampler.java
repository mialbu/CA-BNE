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
		if (i == 2) {
			Iterator<double[]> rngiter = context.getRng(2).nextVectorIterator();
			UnivariatePWLStrategy slocal0 = (UnivariatePWLStrategy) s.get(0);
			UnivariatePWLStrategy slocal1 = (UnivariatePWLStrategy) s.get(1);
			
			Iterator<Sample> it = new Iterator<Sample>() {
				@Override
				public boolean hasNext() {
					return true;
				}

				@Override
				public Sample next() {
					double[] r = rngiter.next();
					Double result[] = new Double[3];
					
					// bids of local players
					double density = 1.0 / slocal0.getMaxValue() / slocal1.getMaxValue();

					//double maxlocalbid0 = Math.min(b, slocal0.getBid(slocal0.getMaxValue()));
					//double maxlocalvalue0 = slocal0.invert(maxlocalbid0);
					double maxlocalvalue0 = slocal0.getMaxValue();
					density *= maxlocalvalue0 / slocal0.getMaxValue();
					result[0] = slocal0.getBid(r[0] * maxlocalvalue0);

					double maxlocalbid1 = Math.max(0.0, Math.min(b - result[0], slocal1.getBid(slocal1.getMaxValue())));
					double maxlocalvalue1 = slocal1.invert(maxlocalbid1);
					density *= maxlocalvalue1 / slocal1.getMaxValue();
					result[1] = slocal1.getBid(r[1] * maxlocalvalue1);
					 
					result[2] = b;

					return new Sample(density, result);
				}
			};
			return it;
		}
		
		final int opponent = (i + 1) % 2; // if i is 0, then opponent is 1, and reverse
		Iterator<double[]> rngiter = context.getRng(2).nextVectorIterator(); // Dimension is number of valuations of all players except player i's valuations -> v-i
		// in my work it will almost always be n-1, since every player is naive and has only one valuation (doesn't matter on how many goods, since the player will only place exactly one bid on the paket that contains exactly these goods!)
		// watch out: for the iterations later on, where the one player will deviate from the naive strategy - I'm not sure anymore, maybe it stays the same, since his valuation does not change, only his actual bids will change...

		Strategy<Double, Double> s1 = s.get(0);  // s1 is strategy of player 1
		Strategy<Double, Double> s2 = s.get(1);  // s2 is strategy of player 2
		
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
				result[opponent] = slocal.getBid(r[0] * slocal.getMaxValue());
				
				double maxglobalbid = Math.min(b + result[opponent], sglobal.getBid(sglobal.getMaxValue()));
				double maxglobalvalue = sglobal.invert(maxglobalbid);
				double density = maxglobalvalue / sglobal.getMaxValue() / slocal.getMaxValue() / sglobal.getMaxValue();
				
				result[2] = sglobal.getBid(r[1] * maxglobalvalue);
				
				return new Sample(density, result); // sample = klasse für  (importance sampling ist wenn verteilung abgeändert wird, so dass mc sample schneller konvergiert)
			}
		};
		return it;
	}
}
