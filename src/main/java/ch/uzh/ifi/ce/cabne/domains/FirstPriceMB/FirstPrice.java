package ch.uzh.ifi.ce.cabne.domains.FirstPriceMB;


import ch.uzh.ifi.ce.cabne.domains.Mechanism;

import java.util.ArrayList;
import java.util.HashMap;


public class FirstPrice implements Mechanism<Double, Double> {

	private final HashMap<Integer,int[]> bundles;  // TODO: remove, since no use in this class ?
	private final ArrayList<ArrayList<Integer>> allocations;

	public FirstPrice(HashMap<Integer, int[]> bundles, ArrayList<ArrayList<Integer>> max_feas_allocs) {
		this.bundles = bundles;
		this.allocations = max_feas_allocs;
	}


	/**
	 * @param i: 	Number of the player the utility is computed for (1 or 2)
	 * @param v: 	Valuation of this player for his pursued item
	 * @param bids:	Array of all bids
	 * @return :	utility of player i according to outcome of auction and his bid
	 */
	@Override
	public double computeUtility(int i, Double v, Double[] bids) { // i = 0 - nr_players // v= 0.0-k //
		// In this mechanism, we assume that the nr_players players bid on nr_items items,
		// bids is therefore an array of length nr_players and contains the bids for the bundle per player.

		// Winner determination - calculates the summed up value of the bids for each allocation
		// -> chooses the one that has the highest sum
		ArrayList<Integer> winner_alloc = new ArrayList<>();
		double winner_alloc_value = 0;
		double current_alloc_value;
		int nr_tiebreaker_allocs = 1;

		for (ArrayList<Integer> current_alloc : this.allocations) {
			current_alloc_value = 0;
			for (Integer currentPlayer : current_alloc) {
				current_alloc_value += bids[currentPlayer.intValue()];
			}
			if (current_alloc_value > winner_alloc_value) {
				winner_alloc_value = current_alloc_value;
				winner_alloc = current_alloc;
				nr_tiebreaker_allocs = 1;
			} else if (current_alloc_value == winner_alloc_value) {
				if (current_alloc.contains(i)) {
					nr_tiebreaker_allocs += 1;
				}
			}
		}

		// if i is in the winner allocation, then the utility of i is its valuation - its calculated price to pay.
		if (winner_alloc.contains(i)) {
			return (v - bids[i]) / nr_tiebreaker_allocs;
		} else {
			return 0.0;
		}
	}
}
