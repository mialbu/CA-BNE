package ch.uzh.ifi.ce.cabne.domains.FirstPriceMB;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.BidSampler;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class FirstPriceMBSampler extends BidSampler<Double, Double> {

	
	public FirstPriceMBSampler(BNESolverContext<Double, Double> context) {
		super(context);
	}


	public Iterator<Sample> conditionalBidIterator(int i, Double v, Double b, List<Strategy<Double, Double>> s) {
		
        int[] players_nr = {0,1,2,3,4};
        int nr_players = 10;
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

		Strategy<Double, Double>[] s_opponents = new Strategy[nr_players];

		// s_opponent[1] is strategy of 2nd opponent player
		for (int index = 0; index < opponents_i.length; index++) {
		    s_opponents[index] = s.get(opponents_i[index]);
        }

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

                for (int index = 0; index < opponents_i.length; index++) {
                    result[opponents_i[index]] = s_opponents[index].getBid(r[index] * s_opponents[index].getMaxValue());
                }

                double size_bid_area = 0;
                Strategy<Double, Double> current_strategy;
                for (int player : players_nr) {
                    current_strategy = s.get(player);
                    size_bid_area += current_strategy.getMaxValue();
                }

                double density = 1.0 / size_bid_area;

				return new Sample(density, result); // sample = klasse für  (importance sampling ist wenn verteilung abgeändert wird, so dass mc sample schneller konvergiert)
			}
		};
		return it;
	}
}
