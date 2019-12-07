package ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FirstPriceMBWD {
    static final double numericalPrecision = 1e-8;


    public List<int[]> solveWD(HashMap<Integer, int[]> bundles, Double[][] bids) {
        double maxWelfare = -1;
        double[] welfares = new double[bundles.size()];
        for (int j=0; j < bundles.size(); j++) {
            double welfare = Arrays.stream(bundles.get(j)).mapToDouble(i -> bids[i/2][i%2]).sum();
            welfares[j] = welfare;
            maxWelfare = Math.max(maxWelfare, welfare);
        }

        List<int[]> result = new ArrayList<>();
        for (int j=0; j < bundles.size(); j++) {
            if (welfares[j] > maxWelfare - numericalPrecision) {
                result.add(bundles.get(j));
            }
        }
        return result;
    }


    public double computeWelfare(HashMap<Integer, int[]> bundles, Double[][] bids) {
        double maxWelfare = -1;
        for (int j=0; j < bundles.size(); j++) {
            double welfare = Arrays.stream(bundles.get(j)).mapToDouble(i -> bids[i/2][i%2]).sum();
            maxWelfare = Math.max(maxWelfare, welfare);
        }
        return maxWelfare;
    }


    public double[] computeVCG(HashMap<Integer, int[]> bundles, Double[][] bids, int[] allocation) {
        double totalValue = Arrays.stream(allocation).mapToDouble(i -> bids[i/2][i%2]).sum();
        Double[][] bidsClone = bids.clone(); // make sure we own this object
        Double[] zeroBid = new Double[]{0.0, 0.0};

        // Note: this loop doesn't set vcg payments for bidders who win nothing, and in java arrays are initialized to 0
        double[] vcgPayments = new double[bids.length];
        for (int bundleIndex : allocation) {
            int i = bundleIndex/2;
            // set bids of this bidder to 0, then solve subproblem
            bidsClone[i] = zeroBid;
            double valueWithoutI = computeWelfare(bundles, bidsClone);
            bidsClone[i] = bids[i];

            // vcg payment is: (total value of the allocation without i) - (total value of true allocation - value of i)
            vcgPayments[i] = valueWithoutI - (totalValue - bids[i][bundleIndex%2]);
        }

        return vcgPayments;
    }
}
