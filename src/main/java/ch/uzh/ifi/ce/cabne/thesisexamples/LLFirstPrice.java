package ch.uzh.ifi.ce.cabne.thesisexamples;

import ch.uzh.ifi.ce.cabne.BR.PWLBRCalculator;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithm;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithmCallback;
import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceOneItem.FirstPrice;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceOneItem.FirstPriceLLSampler;
import ch.uzh.ifi.ce.cabne.integration.MCIntegrator;
import ch.uzh.ifi.ce.cabne.pointwiseBR.PatternSearch;
import ch.uzh.ifi.ce.cabne.pointwiseBR.UnivariatePattern;
import ch.uzh.ifi.ce.cabne.pointwiseBR.updateRule.UnivariateDampenedUpdateRule;
import ch.uzh.ifi.ce.cabne.randomsampling.CommonRandomGenerator;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;
import ch.uzh.ifi.ce.cabne.verification.BoundingVerifier1D;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;


public class LLFirstPrice {
	
	public static void main(String[] args) throws InterruptedException, IOException {

        /********************************
         * Create Context And Read Config   1/4
         * ******************************/
		// create context and read configuration file for algorithm structure
		BNESolverContext<Double, Double> context = new BNESolverContext<>();  // create SolverContext (settings information about computation that will be done)
		String configfile = args[0];  // get information about how the iteration processes are structured
		context.parseConfig(configfile);  // read config file infos

        /*********************************
         * Initialize All Algorithm Pieces  2/4
         * *******************************/
        // choose Best Response algorithm
        // (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
		context.setOptimizer(new PatternSearch<>(context, new UnivariatePattern()));

        // MCIntegrator - used for calculation of expected utility. Always MC used!
        context.setIntegrator(new MCIntegrator<>(context));

        // choose a random number generator for TODO: figure out what for exactly and describe it here
        // alle rng setzen die gebraucht werden, in 1dim ein..
        // wird für valuation gebraucht (belief ist gegeben) aber der effektive wert wird von diesen rng gezogen
        // dim davon ist wie viele zahlen braucht man um v-i darzustellen -> nicht v von allen sondern v ohne i
		context.setRng(1, new CommonRandomGenerator(1));

		// choose an update rule for pointwise best response calculation
		context.setUpdateRule(new UnivariateDampenedUpdateRule(0.1, 0.3, 0.5 / context.getDoubleParameter("epsilon"), true));
	// wMax kleiner - kleinere updates macht alles smoother !!! einfach smoother updaten, dann konvergiert es besser, da sonst zu grosse schritte gemacht werden und überdas ziel hinausschiessen und deshalb folgefehler weitergenommen werden...
	// in vallback könnte an strategy geschraubt werden, dass ecken als fehler beachtet werden.. diese ausbessern -- nur wenn sonst nichts geht
		// choose a best response calculator (here set to adaptive pointwise linear best response
		context.setBRC(new PWLBRCalculator(context));  // adaptive -> Punkte werden gewichtet verteilt und nicht linear. da wo es mehr knicke hat, hat es mehr punkte

        // choose best response calculator for outer loop -> verification step ignore - höchstens klasse tauschen 8 solange in 1dim gleiche benutzen - wenn in 2dim andere klassen
		context.setOuterBRC(new PWLBRCalculator(context));

		// used to verify the actual result in the verification step
		context.setVerifier(new BoundingVerifier1D(context));

		/****************************
		* Instantiate Auction Setting   3/4
		* ***************************/
		
		// choose mechanism that is used for price calculation TODO: Write new Mechanism method (1. for 2 players bidding on one item - LL)!!
		context.setMechanism(new FirstPrice());
		// set sampler
		context.setSampler(new FirstPriceLLSampler(context));  // FPLLSampler uses BidIterator TODO: See what has to be changed for LL bids

        // create a BNEAlgorithm Instance with 2 bidders and configuration
		BNEAlgorithm<Double, Double> bneAlgo = new BNEAlgorithm<>(2, context); // TODO: Create instance with 2 bidders
		
		// add the initial strategy for the first local bidder to the BNEAlgorithm TODO: verify first player strategy to bid his exact valuation
		bneAlgo.setInitialStrategy(0, UnivariatePWLStrategy.makeTruthful(0.0, 1.0)); //TODO: call constructor with initial strategy to start from expected final equilibrium!!!!!!!!!!! todo now!
		// add the initial strategy for the global bidder to the BNEAlgorithm // TODO: delete this bidder
		// bneAlgo.setInitialStrategy(2, UnivariatePWLStrategy.makeTruthful(0.0, 2.0)); // Third (global) bidder
        // add information about second local bidder - since symmetric - will set update strategy to False and generate same Output as the bidder it was symmetrically adjusted
		//bneAlgo.setInitialStrategy(1, UnivariatePWLStrategy.makeTruthful(0.0, 1.0)); // TODO: verify second player strategy to bid his exact valuation
        bneAlgo.makeBidderSymmetric(1, 0);

        /**********************************************************************
         * Create Callback to print out players strategies after each iteration     4/4
         * ********************************************************************/

        // create callback that prints out first local player's strategy after each iteration
        BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> {
            // print out strategy
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(Locale.ENGLISH,"%2d", iteration));
            builder.append(String.format(Locale.ENGLISH," %7.6f  ", epsilon));

            // cast s to UnivariatePWLStrategy to get access to underlying data structure.
            UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) strategies.get(0);
            for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {  //map entry is value bid boundle
                builder.append(String.format(Locale.ENGLISH, "%7.6f",e.getKey()));
                builder.append(" ");
                builder.append(String.format(Locale.ENGLISH,"%7.6f",e.getValue()));
                builder.append("  ");
            }

            // alternatively, just sample the strategy on a regular grid.

			/*for (int i=0; i<=100; i++) {
				double v = strategies.get(0).getMaxValue() * i / ((double) 100);
				builder.append(String.format(Locale.ENGLISH,"%7.6f",v));
				builder.append(" ");
				builder.append(String.format(Locale.ENGLISH,"%7.6f",strategies.get(0).getBid(v)));
				builder.append("  ");
			}*/

            System.out.println(builder.toString());

		/*// create callback that prints out the local and global players' strategies after each iteration, and also forces the strategies to be monotone
		BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> { // args come from bnealgo - func einfach für convenience
            // print out strategy
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%2d", iteration));    // TODO: check if this is the printout
            builder.append(String.format(" %7.6f  ", epsilon)); // TODO: same as above

            int ngridpoints = 1000; // TODO: figure out where this number is defined and what exactly it represents
            for (int i=0; i<=ngridpoints/2; i++) {  // first half of ngridpoints
                double v = strategies.get(1).getMaxValue() * i / ngridpoints; // für alle llG
                builder.append(String.format("%5.4f",v));
                builder.append(" ");
                builder.append(" ");
                builder.append(String.format("%5.4f", strategies.get(0).getBid(v)));
                builder.append(" ");
                builder.append(String.format("%5.4f", strategies.get(1).getBid(v)));
                builder.append("  ");
            }
            for (int i=ngridpoints/2; i<=ngridpoints; i++) {  // second half of ngridpoints // nur für global, da dieser ja bis 2 geht :)
                double v = strategies.get(1).getMaxValue() * i / ngridpoints;
                builder.append(String.format("%5.4f",v));
                builder.append(" ");
                builder.append("0.0000");
                builder.append(" ");
                builder.append(String.format("%5.4f", strategies.get(1).getBid(v)));
                builder.append("  ");
            }
            System.out.println(builder.toString());  // prints builder here*/

            // make the strategy monotone, so it can be inverted for importance sampling // because of importance sampling done, hängt davon ab ob invertierbar, bid muss invertiert werden damit auf resultat' - kann ich ignorieren!
            ///// raus bis
            //// bis hier!
        };
		bneAlgo.setCallback(callback);
		
		BNEAlgorithm.Result<Double, Double> result;
		result = bneAlgo.run();  // TODO: Check what steps follow after all this setup above was made in the first step!
    }
}
