package ch.uzh.ifi.ce.cabne.strategy;

import java.util.SortedMap;
import java.util.TreeMap;

// single-dimensional piecewise linear strategy

public class UnivariatePWLOverbiddingStrategy implements Strategy<Double, Double[]> {
	double[] values, bids;
	SortedMap<Double, Double[]> data;
	int n;
	boolean isAscending;
	double maxValue;
	boolean overbidding;

	public UnivariatePWLOverbiddingStrategy(boolean overbids, SortedMap<Double, Double[]> intervals) {
		// don't use a TreeMap or anything similar for looking up the intervals.
		// After construction, we prefer a fast static data structure, i.e. a good old sorted array.
		// the map used to initialize is kept around so it can be recovered
		data = intervals;
		overbidding = overbids;
		
		n = intervals.size();
		values = new double[n+2];
		int i = 0;
		values[0] = -1.0;
		if (overbidding) {
			bids = new double[2*n+2];
			for (double key : intervals.keySet()) {
				i++;
				values[i] = key;
				bids[i*2-1] = intervals.get(key)[0];
				bids[i*2] = intervals.get(key)[1];
			}
			values[n+1] = Double.MAX_VALUE;
			bids[2*n+1] = bids[n];
		} else {
			bids = new double[n+2];
			for (double key : intervals.keySet()) {
				i++;
				values[i] = key;
				bids[i] = intervals.get(key)[0];
			}
			values[n+1] = Double.MAX_VALUE;
			bids[n+1] = bids[n];
		}
		bids[0] = bids[1];

		
		isAscending = true;
		if (overbidding) {
			for (i=1; i<2*n+1; i+=2) {
				if (bids[i+2] < bids[i]) {
					isAscending = false;
					break;
				}
			}
		} else {
			for (i=0; i<n+1; i++) {
				if (bids[i+1] < bids[i]) {
					isAscending = false;
					break;
				}
			}
		}

		if (overbidding) {
			maxValue = values[2*n];
		} else {
			maxValue = values[n];
		}
	}
	
	public static UnivariatePWLOverbiddingStrategy setStrategy(boolean overbidding, double[] values, double[] bids) {
		SortedMap<Double, Double[]> intervals = new TreeMap<>();
		if (bids.length > values.length) {
			for (int j = 0; j < values.length; j+=2) {
				Double[] currentBids = new Double[2];
				currentBids[0] = bids[j];
				currentBids[1] = bids[j+1];
				intervals.put(values[j], currentBids);
			}
		} else {
			for (int j = 0; j < values.length; j++) {
				Double[] currentBid = new Double[1];
				currentBid[0] = bids[j];
				intervals.put(values[j], currentBid);
			}
		}
		return new UnivariatePWLOverbiddingStrategy(overbidding, intervals);
	}

	public Double[] getBid(Double value) {
		// binary search
		int lo = 0, hi=n+1;
		while (lo + 1 < hi) {
			int middle = (lo + hi)/2;
			if (values[middle] <= value) {
				lo = middle;
			} else {
				hi = middle;
			}
		}
				
		double floor = values[lo];
		double ceiling = values[hi];

//		if (n==2) {
//			Double[] val = new Double[1];
//			val[0] = value;
//			return val;
//		}

        double weight = (value - floor) / (ceiling - floor);
        //return weight * bids[hi] + (1 - weight) * bids[lo];
		if (overbidding) {
			Double[] overbids = new Double[2];
			overbids[0] = bids[2*lo-1] + weight * (bids[2*hi-1] - bids[2*lo-1]);  // linear between lo and hi
			overbids[1] = bids[2*lo] + weight * (bids[2*hi] - bids[2*lo]);
			return overbids;
		} else {
			Double[] bidarray = new Double[1];
			bidarray[0] = bids[lo] + weight * (bids[hi] - bids[lo]);
			return bidarray;
		}
	}
	
	public Double invert(Double bid) {
		if (!isAscending) {
			throw new RuntimeException("Can't invert nonmonotonic strategy.");
		}
		
		// binary search
		int lo = 0, hi=n+1;
		while (lo + 1 < hi) {
			int middle = (lo + hi)/2;
			if (bids[middle] <= bid) {
				lo = middle;
			} else {
				hi = middle;
			}
		}
				
		double floor = values[lo];
		double ceiling = values[hi];
		
        double weight = (bid - floor) / (ceiling - floor);
        return weight * values[hi] + (1 - weight) * values[lo];        
	}
	
	@Override
    public String toString() {
        return "PiecewiseLinearStrategy{" +
                "values=" + values + " bids=" + bids +
                '}';
    }

	public SortedMap<Double, Double[]> getData() {
		return data;
	}

	@Override
	public Double getMaxValue() {
		return maxValue;
	}
}
