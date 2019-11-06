package ch.uzh.ifi.ce.cabne.domains.FirstPriceMB;


import ch.uzh.ifi.ce.cabne.domains.Mechanism;


public class FirstPrice implements Mechanism<Double, Double> {

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

		Double maxBid = bids[0];
		int nr_maxBids = 1;
		for (int iter=1; iter<bids.length; iter++) {  // loop has to start at 2nd bid, since maxBid set to first bid before
			if (bids[iter] > maxBid) {
				nr_maxBids = 1;
				maxBid = bids[iter];
			} else if (bids[iter] == maxBid) {
				nr_maxBids += 1;
			}
		}

		// bids[i] can maximal be maxBid, given the maxBid calculation above
		if (bids[i] == maxBid) {  // player i wins
			return (v - bids[i])/nr_maxBids ;
		} else return 0.0;
    }
}
