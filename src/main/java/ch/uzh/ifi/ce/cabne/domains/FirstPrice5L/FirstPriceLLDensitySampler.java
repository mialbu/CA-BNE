package ch.uzh.ifi.ce.cabne.domains.FirstPrice5L;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.BidSampler;
import ch.uzh.ifi.ce.cabne.helpers.distributions.DensityFunction;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;

import java.util.Iterator;
import java.util.List;


public class FirstPriceLLDensitySampler extends BidSampler<Double, Double> {
    private DensityFunction densityFunction;

    public FirstPriceLLDensitySampler(BNESolverContext<Double, Double> context, DensityFunction densityFunction) {
        super(context);
        this.densityFunction = densityFunction;
    }

    public Iterator<Sample> conditionalBidIterator(int i, Double v, Double b, List<Strategy<Double, Double>> s) {

        final int opponent = (i + 1) % 2; // if i is 0, then opponent is 1, and reverse
        Iterator<double[]> rngiter = context.getRng(2).nextVectorIterator(); // Dimension is number of valuations of all players except player i's valuations -> v-i
        // in my work it will almost always be n-1, since every player is naive and has only one valuation (doesn't matter on how many goods, since the player will only place exactly one bid on the paket that contains exactly these goods!)
        // watch out: for the iterations later on, where the one player will deviate from the naive strategy - I'm not sure anymore, maybe it stays the same, since his valuation does not change, only his actual bids will change...

        Strategy<Double, Double> s_i = s.get(i);  // s_i is strategy of player i
        Strategy<Double, Double> s_opponent = s.get(opponent);  // s_opponent is strategy of opponent player

        Iterator<Sample> it = new Iterator<Sample>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Sample next() {	// TODO: rewrite this method for two local bidders on one single item!
                double[] r = rngiter.next();
                Double result[] = new Double[2];

                result[i] = b;  // vector für bids - 1 pro player, welches resultat wurde gezogen density - wieviel kommts in deinen gezogenen vor vs. in der originalen verteilung

                // slocal referred to the local players strategy, eq for sglobal
                result[opponent] = s_i.getBid(r[0] * s_i.getMaxValue());

                return new Sample(1.0, result); // sample = klasse für  (importance sampling ist wenn verteilung abgeändert wird, so dass mc sample schneller konvergiert)
            }
        };
        return it;


        /*final int localopponent = (i + 1) % 2;
        Iterator<double[]> rngiter = context.getRng(2).nextVectorIterator();
        Strategy<Double, Double> slocal = s.get(localopponent);
        UnivariatePWLStrategy sglobal = (UnivariatePWLStrategy) s.get(2);

        Iterator<Sample> it = new Iterator<Sample>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Sample next() {
                double[] r = rngiter.next();
                Double result[] = new Double[3];

                result[i] = b;
                result[localopponent] = slocal.getBid(r[0] * slocal.getMaxValue());

                double maxglobalbid = Math.min(b + result[localopponent], sglobal.getBid(sglobal.getMaxValue()));
                double maxglobalvalue = sglobal.invert(maxglobalbid);
                double density = maxglobalvalue / sglobal.getMaxValue() / slocal.getMaxValue() / sglobal.getMaxValue();

                result[2] = sglobal.getBid(r[1] * maxglobalvalue);

                return new Sample(density, result);
            }
        };
        return it;*/
    }
}
