package ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis;


import ch.uzh.ifi.ce.cabne.domains.Mechanism;

import java.util.ArrayList;


public class FirstPriceOverbidding implements Mechanism<Double, Double[]> {

	private final ArrayList<ArrayList<Integer>> allocations;
	private ArrayList<ArrayList<Integer>> allAllocations;
	private final int nrPlayers;

	public FirstPriceOverbidding(ArrayList<ArrayList<Integer>> maxFeasibleAllocations, int nrPlayers) {
		this.allocations = maxFeasibleAllocations;
		this.nrPlayers = nrPlayers;
	}

	public void setOverbiddingAllocations(ArrayList<ArrayList<Integer>> oAllocations) {
		this.allAllocations = new ArrayList<>();
		this.allAllocations.addAll(this.allocations);
		this.allAllocations.addAll(oAllocations);
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
		ArrayList<ArrayList<Integer>> winnerAllocs = new ArrayList<>();
		double winnerAllocValue = 0;
		double curAllocValue;

		// loop to determine the winner allocation
		for (ArrayList<Integer> curAlloc : this.allAllocations) {
			curAllocValue = 0;
			for (Integer playerInAlloc : curAlloc) {
				if (playerInAlloc == nrPlayers) {  // the overbidding bundle bid is in the winner allocation
					curAllocValue += bids[i][1];  // this method is only called by the bidder that overbids!
				} else {
					curAllocValue += bids[playerInAlloc][0];
				}
			}
			if (curAllocValue > winnerAllocValue + 0.00000001) {
				winnerAllocs = new ArrayList<>();
				winnerAllocs.add(curAlloc);
				winnerAllocValue = curAllocValue;
			} else if (curAllocValue > winnerAllocValue - 0.00000001) {
				winnerAllocs.add(curAlloc);
			}
		}

		int nWins = 0;
		int nOWins = 0;
		for (ArrayList<Integer> wAlloc : winnerAllocs) {
			if (wAlloc.contains(i)) {
				nWins++;
			} else if (wAlloc.contains(nrPlayers)) {
				nOWins++;
			}
		}
		double pWins = (double) nWins / winnerAllocs.size();
		double pOWins = (double) nOWins / winnerAllocs.size();

		double utilityWin = v - bids[i][0];
		double utilityOWin = v - bids[i][1];

		return pWins*utilityWin + pOWins*utilityOWin;
	}
}
