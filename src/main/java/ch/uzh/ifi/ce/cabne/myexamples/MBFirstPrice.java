package ch.uzh.ifi.ce.cabne.myexamples;

import ch.uzh.ifi.ce.cabne.BR.PWLBRCalculator;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithm;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithmCallback;
import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceMB.FirstPrice;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceMB.FirstPriceMBSampler;
import ch.uzh.ifi.ce.cabne.bundelgenerator.BundleGenerator;
import ch.uzh.ifi.ce.cabne.integration.MCIntegrator;
import ch.uzh.ifi.ce.cabne.pointwiseBR.PatternSearch;
import ch.uzh.ifi.ce.cabne.pointwiseBR.UnivariatePattern;
import ch.uzh.ifi.ce.cabne.pointwiseBR.updateRule.UnivariateDampenedUpdateRule;
import ch.uzh.ifi.ce.cabne.randomsampling.CommonRandomGenerator;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;
import ch.uzh.ifi.ce.cabne.verification.BoundingVerifier1D;

import java.io.IOException;
import java.util.*;


public class MBFirstPrice {

	public static void main(String[] args) throws InterruptedException, IOException {
		int nr_runs = 100;

        /********************************
         * Create Context And Read Config   1/4
         * ******************************/
		// create context and read configuration file for algorithm structure
		BNESolverContext<Double, Double> context = new BNESolverContext<>();  // create SolverContext (settings information about computation that will be done)
		String configfile = args[0];  // get information about how the iteration processes are structured
		context.parseConfig(configfile);  // read config file infos

		// Define number of players and items and probability of an item getting chosen for interest for a player
		int nr_players = 10;
		int nr_items = 6;
		double prob_interest = 0.3;
		System.out.println("nr_players " + nr_players);
		System.out.println("nr_items " + nr_items);
		System.out.println("prob_interest " + prob_interest);

		// Instantiate BundleGenerator
		BundleGenerator bundleGenerator = new BundleGenerator(nr_players, nr_items, prob_interest);
		HashMap<Integer, int[]> bundles = bundleGenerator.get_bundles();
		bundles.forEach((key, value) -> {
			System.out.println(key + " " + Arrays.toString(value));
		});
		// Calculate all maximal and feasible allocations
		ArrayList<ArrayList<Integer>> max_feasible_allocations = bundleGenerator.get_max_feasible_allocs();


        /*********************************
         * Initialize All Algorithm Pieces  2/4
         * *******************************/
        // choose Best Response algorithm
        // (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
		context.setOptimizer(new PatternSearch<>(context, new UnivariatePattern()));  // multivariate cross pattern für step nach verification.

        // MCIntegrator - used for calculation of expected utility. Always MC used!
        context.setIntegrator(new MCIntegrator<>(context));

        // choose a random number generator
        // alle rng setzen die gebraucht werden, in 1dim ein..
        // wird für valuation gebraucht (belief ist gegeben) aber der effektive wert wird von diesen rng gezogen
        // dim davon ist wie viele zahlen braucht man um v-i darzustellen -> nicht v von allen sondern v ohne i
		context.setRng(nr_players-1, new CommonRandomGenerator(nr_players-1));  // Bei diesen Auktionen bietet jeder Player nur einen Betrag für sein jeweiliges Bundle.

		// choose an update rule for pointwise best response calculation
		context.setUpdateRule(new UnivariateDampenedUpdateRule(0.15, 0.35, 0.5 / context.getDoubleParameter("epsilon"), true));
		// wMax kleiner - kleinere updates macht alles smoother !!! einfach smoother updaten, dann konvergiert es besser, da sonst zu grosse schritte gemacht werden und überdas ziel hinausschiessen und deshalb folgefehler weitergenommen werden...
		// in vallback könnte an strategy geschraubt werden, dass ecken als fehler beachtet werden.. diese ausbessern -- nur wenn sonst nichts geht
		// choose a best response calculator (here set to adaptive pointwise linear best response
		// ws 0.15, 0.35, 0.5
		context.setBRC(new PWLBRCalculator(context));  // adaptive -> Punkte werden gewichtet verteilt und nicht linear. da wo es mehr knicke hat, hat es mehr punkte

        // choose best response calculator for outer loop -> verification step ignore - höchstens klasse tauschen 8 solange in 1dim gleiche benutzen - wenn in 2dim andere klassen
		context.setOuterBRC(new PWLBRCalculator(context));

		// used to verify the actual result in the verification step
		context.setVerifier(new BoundingVerifier1D(context));


		/****************************
		* Instantiate Auction Setting   3/4
		* ***************************/
		
		// choose mechanism that is used for price calculation
		context.setMechanism(new FirstPrice(bundles, max_feasible_allocations));  // Call constructor of FirstPrice Mechanism with arg bundles  TODO: FirstPriceOverbidding(bundles, max_f, overbidbundle, playernr)
		// set sampler
		context.setSampler(new FirstPriceMBSampler(context, nr_players));  // FPMBSampler uses BidIterator

        // create a BNEAlgorithm Instance with nr_players bidders and configuration
		BNEAlgorithm<Double, Double> bneAlgo = new BNEAlgorithm<>(nr_players, context);
		
		// add the initial strategy for the first local bidder to the BNEAlgorithm // lower is always 0.0 // upper is k (nr of items that are of interest)

		bundles.forEach((key, value) -> {
			double max_value = 0;
			for (int val : value) {
				max_value += val;
			}
			bneAlgo.setInitialStrategy(key, UnivariatePWLStrategy.makeTruthful(0.0, max_value));
		});


        /**********************************************************************
         * Create Callback to print out players strategies after each iteration     4/4
         * ********************************************************************/

        // create callback that prints out first local player's strategy after each iteration
        BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> {
        	System.out.println(iteration  + " " + type);
        	bundles.forEach((key, value) -> {
				// print out strategy
				StringBuilder builder = new StringBuilder();
				builder.append(String.format(Locale.ENGLISH,"%2d", key));
				builder.append(String.format(Locale.ENGLISH," %7.6f  ", epsilon));

				// cast s to UnivariatePWLStrategy to get access to underlying data structure.
				UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) strategies.get(key);
				for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {  //map entry is value bid boundle
					builder.append(String.format(Locale.ENGLISH, "%7.6f",e.getKey()));
					builder.append(" ");
					builder.append(String.format(Locale.ENGLISH,"%7.6f",e.getValue()));
					builder.append("  ");
				}
				System.out.println(builder.toString());
			});
        	System.out.println();
        };
		bneAlgo.setCallback(callback);

		BNEAlgorithm.Result<Double, Double> result;
		result = bneAlgo.run();
    }
}
