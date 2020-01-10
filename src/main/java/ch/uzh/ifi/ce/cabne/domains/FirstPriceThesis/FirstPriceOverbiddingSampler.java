package ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.BidSampler;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;

import java.util.Iterator;
import java.util.List;


public class FirstPriceOverbiddingSampler extends BidSampler<Double, Double[]> {

	private final int nrPlayers;

	public FirstPriceOverbiddingSampler(BNESolverContext<Double, Double[]> context, int nr_players) {
		super(context);
		this.nrPlayers = nr_players;
	}

	@Override
	public Iterator<Sample> conditionalBidIterator(int i, Double v, Double[] b, List<Strategy<Double, Double[]>> s) {
		int[] playerNrs = new int[nrPlayers];
		for (int iter = 0; iter < nrPlayers; iter++) {
			playerNrs[iter] = iter;
		}
		int[] opponents_i = new int[playerNrs.length - 1];

		for (int inc = 0, k = 0; inc < playerNrs.length; inc++) {
			if (inc == i) {
				continue;
			}
			opponents_i[k++] = playerNrs[inc];
		}

		// Dimension is number of valuations of all players except player i's valuations -> v-i
		// in my work it will almost always be n-1, since every player is naive and has only one valuation (doesn't matter on how many goods, since the player will only place exactly one bid on the packet that contains exactly these goods!)
		// watch out: for the iterations later on, where the one player will deviate from the naive strategy - I'm not sure anymore, maybe it stays the same, since his valuation does not change, only his actual bids will change...
		Iterator<double[]> rngiter = context.getRng(opponents_i.length).nextVectorIterator();

		Strategy<Double, Double[]>[] s_opponents = new Strategy[nrPlayers];

		for (int index = 0; index < opponents_i.length; index++) {
			s_opponents[index] = s.get(opponents_i[index]);
		}

		double size_bid_area = 1;
		Strategy<Double, Double[]> current_strategy;
		for (int player : opponents_i) {
			current_strategy = s.get(player);
			if (current_strategy.getMaxValue()[0] > 0.5) {
				size_bid_area = size_bid_area * current_strategy.getMaxValue()[0];
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
				Double result[][] = new Double[playerNrs.length][2];

				result[i][0] = b[0];
				result[i][1] = b[1];

				for (int index = 0; index < opponents_i.length; index++) {
					result[opponents_i[index]] = s_opponents[index].getBid(r[index] * s_opponents[index].getMaxValue()[0]);
				}

				return new Sample(density, result);
			}
		};
		return it;
	}
}
