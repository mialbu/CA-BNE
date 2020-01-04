package ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis;


import ch.uzh.ifi.ce.cabne.domains.Mechanism;

import java.util.ArrayList;


public class FirstPriceOverbidding implements Mechanism<Double, Double[]> {

	private final ArrayList<ArrayList<Integer>> allocations;

	public FirstPriceOverbidding(ArrayList<ArrayList<Integer>> maxFeasibleAllocations) {
		this.allocations = maxFeasibleAllocations;
	}

	public FirstPriceOverbidding(ArrayList<ArrayList<Integer>> maxFeasibleAllocations, ArrayList<ArrayList<Integer>> overbiddingMaxFeasibleAllocations) {
		this.allocations = maxFeasibleAllocations;
		this.allocations.addAll(overbiddingMaxFeasibleAllocations);
	}

	/**
	 * Computes the utility for a player
	 *
	 * @param i: 	Number of the player the utility is computed for
	 * @param v: 	Valuation of this player for his pursued item
	 * @param bids:	Array of all bids
	 * @return :	utility of player i according to outcome of auction and his bid
	 */
	public double computeUtility(int i, Double v, Double[][] bids) {  // i=player v=valuation
		// player i is the bidder with two bids, all other bidders have only one bid on one bundle

		// Winner determination - calculates the summed up value of the bids for each allocation
		// (first price chooses the one that has the highest sum)
		ArrayList<Integer> winner_alloc = new ArrayList<>();
		double winner_alloc_value = 0;
		double current_alloc_value;
		int nr_tiebreaker_allocs = 1;

		for (ArrayList<Integer> current_alloc : this.allocations) {
			current_alloc_value = 0;
			for (Integer currentPlayer : current_alloc) {
				current_alloc_value += bids[currentPlayer.intValue()][0];  //todo: bids[][]
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


		// if i is in the winner allocation, then the utility of i is its valuation minus its calculated price to pay.
		if (winner_alloc.contains(i)) {
			return (v - bids[i][0]) / nr_tiebreaker_allocs; //todo: bids[][]
		} else {
			return 0.0;
		}
	}
}
