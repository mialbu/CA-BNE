package ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis;


import ch.uzh.ifi.ce.cabne.domains.Mechanism;

import java.util.ArrayList;


public class FirstPrice implements Mechanism<Double, Double> {

	private final ArrayList<ArrayList<Integer>> allocations;

	public FirstPrice(ArrayList<ArrayList<Integer>> max_feas_allocs) {
		this.allocations = max_feas_allocs;
	}

	/**
	 * Computes the utility for a player
	 *
	 * @param i: 	Number of the player the utility is computed for
	 * @param v: 	Valuation of this player for his pursued item
	 * @param bids:	Array of all bids
	 * @return :	utility of player i according to outcome of auction and his bid
	 */
	@Override
	public double computeUtility(int i, Double v, Double[] bids) { // (i = 0 - nr_players // v= 0.0-k)
		// Winner determination - calculates the summed up value of the bids for each allocation
		// (first price chooses the one that has the highest sum)
		ArrayList<ArrayList<Integer>> winnerAllocs = new ArrayList<>();
		double winnerAllocValue = -1.0;
		double curAllocValue;

		for (ArrayList<Integer> curAlloc : this.allocations) {
			curAllocValue = 0;
			for (Integer playerInAlloc : curAlloc) {
				curAllocValue += bids[playerInAlloc];
			}
			if (curAllocValue > winnerAllocValue) {
				winnerAllocs = new ArrayList<>();
				winnerAllocs.add(curAlloc);
				winnerAllocValue = curAllocValue;
			} else if (curAllocValue > winnerAllocValue - 0.00000001) {
				winnerAllocs.add(curAlloc);
				winnerAllocValue = curAllocValue;
			}
		}

		int nWins = 0;
		for (ArrayList<Integer> wAlloc : winnerAllocs) {
			if (wAlloc.contains(i)) {
				nWins++;
			}
		}

		double pWins = (double) nWins / winnerAllocs.size();

		double utilityWin = v - bids[i];

		return pWins * utilityWin + (Math.pow(10, -6) * bids[i]); // or minus to get the lower bids more often - avoid noise if possible
		// noises may come from bid spaces in which the bidder is indifferent - random chosen - noisy
		// set regulating term to favor lower or higher bids...
	}
}
