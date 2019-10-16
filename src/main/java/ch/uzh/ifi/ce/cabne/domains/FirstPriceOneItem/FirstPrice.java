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
	public double computeUtility(int i, Double v, Double[] bids) {
		// In this mechanism, we assume that the two players bid on one item only,
		// bids is therefore an array of length 2 and contains the bids for this one item per player.
				
		if (bids[0] > bids[1]) {
        	// player 1 wins
			if (i == 1) {
				return v - bids[1];
			} else {
				return 0.0;
			}
        } else if (bids[1] == bids[0]) {
        	// tie: 50-50 chance of winning
        	return 0.5*(v - bids[i]);
        } else {
        	if (i == 1) {
        		return 0.0;
        	} else {
		    	return v - bids[i];
        	}
        }
    }
}
