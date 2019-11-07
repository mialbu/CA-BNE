package ch.uzh.ifi.ce.cabne.domains.FirstPrice5L;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.BidSampler;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;

import java.util.Iterator;
import java.util.List;


public class FirstPriceLLSampler extends BidSampler<Double, Double> {

	
	public FirstPriceLLSampler(BNESolverContext<Double, Double> context) {
		super(context);
	}

	public Iterator<Sample> conditionalBidIterator(int i, Double v, Double b, List<Strategy<Double, Double>> s) {
		
        int players_nr[] = {0,1,2,3,4};
        int[] opponents_i = new int[players_nr.length - 1];

        for (int inc = 0, k = 0; inc < players_nr.length; inc++) {
            if (inc == i) {
                continue;
            }
            opponents_i[k++] = players_nr[inc];
        }

        Iterator<double[]> rngiter = context.getRng(opponents_i.length).nextVectorIterator(); // Dimension is number of valuations of all players except player i's valuations -> v-i
        // in my work it will almost always be n-1, since every player is naive and has only one valuation (doesn't matter on how many goods, since the player will only place exactly one bid on the paket that contains exactly these goods!)
        // watch out: for the iterations later on, where the one player will deviate from the naive strategy - I'm not sure anymore, maybe it stays the same, since his valuation does not change, only his actual bids will change...

        Strategy<Double, Double> s_opponent1 = s.get(opponents_i[0]);  // s_opponent1 is strategy of 1st opponent player
        Strategy<Double, Double> s_opponent2 = s.get(opponents_i[1]);  // s_opponent2 is strategy of 2nd opponent player
        Strategy<Double, Double> s_opponent3 = s.get(opponents_i[2]);  // s_opponent3 is strategy of 3rd opponent player
        Strategy<Double, Double> s_opponent4 = s.get(opponents_i[3]);  // s_opponent4 is strategy of 4th opponent player

		Iterator<Sample> it = new Iterator<Sample>() {
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Sample next() {	// TODO: rewrite this method for five local bidders on one single item!
				double[] r = rngiter.next();
				Double result[] = new Double[players_nr.length];
				
				result[i] = b;  // vector für bids - 1 pro player, welches resultat wurde gezogen
                // density - wieviel kommts in deinen gezogenen vor vs. in der originalen verteilung

				result[opponents_i[0]] = s_opponent1.getBid(r[0] * s_opponent1.getMaxValue());
                result[opponents_i[1]] = s_opponent2.getBid(r[1] * s_opponent2.getMaxValue());
                result[opponents_i[2]] = s_opponent3.getBid(r[2] * s_opponent3.getMaxValue());
                result[opponents_i[3]] = s_opponent4.getBid(r[3] * s_opponent4.getMaxValue());

				return new Sample(1.0, result); // sample = klasse für  (importance sampling ist wenn verteilung abgeändert wird, so dass mc sample schneller konvergiert)
			}
		};
		return it;
	}
}
