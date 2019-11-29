package ch.uzh.ifi.ce.cabne.domains.FirstPriceOneItem;


import ch.uzh.ifi.ce.cabne.domains.Mechanism;


public class FirstPrice implements Mechanism<Double, Double> {

	/**
	 * @param i: 	Number of the player the utility is computed for (1 or 2)
	 * @param v: 	Valuation of this player for his pursued item
	 * @param bids:	Array of all bids
	 * @return :	utility of player i according to outcome of auction and his bid
	 */
	@Override
	public double computeUtility(int i, Double v, Double[] bids) { // i = 0 or 1 // v= 0.0-1.0 // bids=[bid 1, bid 2]
		// TODO: 29.11. delete notes at end of project
		// In this mechanism, we assume that the two players bid on one item only,
		// bids is therefore an array of length 2 and contains the bids for this one item per player.

		if (bids[0] > bids[1]) {  // player 1 wins
			if (i == 0) {
				// player i is player 1, he has won the auction and his utility is (v - payment), his payment = his bid
				return v - bids[0];  // bids[0]
			} else { // i is player 2 and has lost, utility = 0
				return 0.0;
			}
        } else if (bids[1] == bids[0]) {
        	// Tie -> 50-50 chance of winning
        	return 0.5*(v - bids[i]);
        } else {  // player 2 wins
        	if (i == 0) {  // if i is player 1 -> utility = 0
        		return 0.0;
        	} else {
		    	return v - bids[1];  // if i is player 2 (winner)
        	}
        }
    }
}
