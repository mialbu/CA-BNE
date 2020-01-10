package ch.uzh.ifi.ce.cabne.strategy;

import java.util.SortedMap;
import java.util.TreeMap;

// single-dimensional piecewise linear strategy

public class UnivariatePWLOverbiddingStrategy implements Strategy<Double, Double[]> {
	double[] values;
	double[][] bids;
	SortedMap<Double, Double[]> data;
	int n;
	boolean isAscending;
	Double[] maxValue = new Double[2];

	public UnivariatePWLOverbiddingStrategy(SortedMap<Double, Double[]> intervals) {
		// don't use a TreeMap or anything similar for looking up the intervals.
		// After construction, we prefer a fast static data structure, i.e. a good old sorted array.
		// the map used to initialize is kept around so it can be recovered
		data = intervals;
		
		n = intervals.size();
		values = new double[n+2];
		bids = new double[n+2][2];
		int i = 0;
		for (double key : intervals.keySet()) {
			i++;
			values[i] = key;
			bids[i][0] = intervals.get(key)[0];
			bids[i][1] = intervals.get(key)[1];
		}
		values[0] = -1.0;
		values[n+1] = Double.MAX_VALUE;
		bids[0][0] = bids[1][0];
		bids[0][1] = bids[1][1];
		bids[n+1][0] = bids[n][0];
		bids[n+1][1] = bids[n][1];

		maxValue[0] = values[n];
		maxValue[1] = values[n];
	}

	public static UnivariatePWLOverbiddingStrategy setStrategy(double[] values, double[] bids) {
		SortedMap<Double, Double[]> intervals = new TreeMap<>();

		for (int j = 0; j < values.length; j++) {
			Double[] currentBids = new Double[2];
			currentBids[0] = bids[j*2];
			currentBids[1] = bids[j*2+1];
			intervals.put(values[j], currentBids);
		}
		return new UnivariatePWLOverbiddingStrategy(intervals);  // intervals maps key=value to value=double[2] (bids)
	}
	
	public static UnivariatePWLOverbiddingStrategy setInitialStrategy(double[] values, double[] bids) {
		SortedMap<Double, Double[]> intervals = new TreeMap<>();

		for (int j = 0; j < values.length; j++) {
			Double[] currentBids = new Double[2];
			currentBids[0] = bids[j];
			currentBids[1] = 0.0; //then compare to when it is set to 0.0 initially
			intervals.put(values[j], currentBids);
		}
		return new UnivariatePWLOverbiddingStrategy(intervals);
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

        double weight = (value - floor) / (ceiling - floor);

		Double[] bid = new Double[2];
		bid[0] = weight * bids[lo][0] + (1-weight) * bids[hi][0];
		bid[1] = weight * bids[lo][1] + (1-weight) * bids[hi][1];
		return bid;
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
	public Double[] getMaxValue() {
		return maxValue;
	}
}
