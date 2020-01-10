package ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.BidSampler;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;

import java.util.Iterator;
import java.util.List;


public class FirstPriceSampler extends BidSampler<Double, Double> {

	private final int nr_players;

	public FirstPriceSampler(BNESolverContext<Double, Double> context, int nr_players) {
		super(context);
		this.nr_players = nr_players;
	}


	public Iterator<Sample> conditionalBidIterator(int i, Double v, Double b, List<Strategy<Double, Double>> s) {
        int[] player_nrs = new int[nr_players];
        for (int iter = 0; iter < nr_players; iter++) {
        	player_nrs[iter] = iter;
		}
        int[] opponents_i = new int[player_nrs.length - 1];

        for (int inc = 0, k = 0; inc < player_nrs.length; inc++) {
            if (inc == i) {
                continue;
            }
            opponents_i[k++] = player_nrs[inc];
        }

		// Dimension is number of valuations of all players except player i's valuations -> v-i
		// in my work it will almost always be n-1, since every player is naive and has only one valuation (doesn't matter on how many goods, since the player will only place exactly one bid on the packet that contains exactly these goods!)
		// watch out: for the iterations later on, where the one player will deviate from the naive strategy - I'm not sure anymore, maybe it stays the same, since his valuation does not change, only his actual bids will change...
		Iterator<double[]> rngiter = context.getRng(opponents_i.length).nextVectorIterator();

		Strategy<Double, Double>[] s_opponents = new Strategy[nr_players];

		for (int index = 0; index < opponents_i.length; index++) {
		    s_opponents[index] = s.get(opponents_i[index]);
        }

		double size_bid_area = 1;
		Strategy<Double, Double> current_strategy;
		for (int player : opponents_i) {
			current_strategy = s.get(player);
			if (current_strategy.getMaxValue() > 0.5) {
				size_bid_area = size_bid_area * current_strategy.getMaxValue();
			}
		}
		final double density = 1.0 / size_bid_area;

		Iterator<Sample> it = new Iterator<Sample>() {
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Sample next() {
				double[] r = rngiter.next();
				Double result[] = new Double[player_nrs.length];

				result[i] = b;  // vector für bids - 1 pro player, welches resultat wurde gezogen
                // density - wieviel kommts in deinen gezogenen vor vs. in der originalen verteilung

                for (int index = 0; index < opponents_i.length; index++) {
                    result[opponents_i[index]] = s_opponents[index].getBid(r[index] * s_opponents[index].getMaxValue());
                }

				// TODO: Meeting 6: sample = class for importance sampling (when distribution changes, so that mcsample converges faster)
				return new Sample(density, result);
			}
		};
		return it;
	}
}
