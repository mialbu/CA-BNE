package ch.uzh.ifi.ce.cabne.myexamples;

		import ch.uzh.ifi.ce.cabne.BR.PWLBRCalculator;
		import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithm;
		import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithmCallback;
		import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
		import ch.uzh.ifi.ce.cabne.bundelgenerator.BundleGenerator;
		import ch.uzh.ifi.ce.cabne.domains.FirstPriceMB.FirstPrice;
		import ch.uzh.ifi.ce.cabne.domains.FirstPriceMB.FirstPriceMBSampler;
		import ch.uzh.ifi.ce.cabne.integration.MCIntegrator;
		import ch.uzh.ifi.ce.cabne.pointwiseBR.PatternSearch;
		import ch.uzh.ifi.ce.cabne.pointwiseBR.UnivariatePattern;
		import ch.uzh.ifi.ce.cabne.pointwiseBR.updateRule.UnivariateDampenedUpdateRule;
		import ch.uzh.ifi.ce.cabne.randomsampling.CommonRandomGenerator;
		import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;
		import ch.uzh.ifi.ce.cabne.verification.BoundingVerifier1D;

		import java.io.BufferedWriter;
		import java.io.File;
		import java.io.FileWriter;
		import java.io.IOException;
		import java.util.*;
		import java.util.concurrent.TimeUnit;


public class ThesisFirstPrice {

	public static void main(String[] args) throws InterruptedException, IOException {

		int nrOfRuns = 100;

		for (int run_nr = 0; run_nr < nrOfRuns; run_nr++) {
			long startTime = System.nanoTime();

			// Create Context And Read Config file for algorithm structure
			BNESolverContext<Double, Double> context = new BNESolverContext<>();
			String configfile = args[0];
			context.parseConfig(configfile);

			// Define number of players and items and probability of an item getting chosen for interest for a player
			int nr_players = 7;
			int nr_items = 8;
			double prob_interest = 0.4;
			String folder_output = "pass3";

			// Initialize bundles
			BundleGenerator bundleGenerator = new BundleGenerator(nr_players, nr_items, prob_interest);
			HashMap<Integer, int[]> bundles = bundleGenerator.get_bundles();
			bundles.forEach((key, value) -> {
				System.out.println(key + " " + Arrays.toString(value));
			});

			// Calculate all maximal and feasible allocations
			ArrayList<ArrayList<Integer>> max_feasible_allocations = bundleGenerator.get_max_feasible_allocs();

			// Initialize all algorithm pieces (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
			context.setOptimizer(new PatternSearch<>(context, new UnivariatePattern()));

			// Set Integrator used for calculation of expected utility
			context.setIntegrator(new MCIntegrator<>(context));

			// Set a random number generator (at this auction each player only bids on his one bundle)
			context.setRng(nr_players - 1, new CommonRandomGenerator(nr_players - 1));

			// Set an update rule for pointwise best response calculation
			context.setUpdateRule(new UnivariateDampenedUpdateRule(0.2, 0.6, 0.5 / context.getDoubleParameter("epsilon"), true));

			// Set best response calculator (piecewise linear best response calculator)
			context.setBRC(new PWLBRCalculator(context));

			// choose best response calculator for outer loop  // TODO: -> ignore verification step
			context.setOuterBRC(new PWLBRCalculator(context));

			// Set verifier to verify the result in the verification step
			context.setVerifier(new BoundingVerifier1D(context));


			// Initialize auction settings
			// Choose mechanism that is used for price calculation
			context.setMechanism(new FirstPrice(max_feasible_allocations));

			// Set mechanism sampler (FPMBSampler uses BidIterator)
			context.setSampler(new FirstPriceMBSampler(context, nr_players));

			// Create a BNEAlgorithm instance with number of bidders and configuration
			BNEAlgorithm<Double, Double> bneAlgo = new BNEAlgorithm<>(nr_players, context);

			// Add the initial strategy for the first local bidder to the BNEAlgorithm
			// lower is always 0.0 / upper is k (nr of items that are of interest)
			bundles.forEach((key, value) -> {
				double max_value = 0;
				for (int val : value) {
					max_value += val;
				}
				bneAlgo.setInitialStrategy(key, UnivariatePWLStrategy.makeTruthful(0.0, max_value));
			});

			// Generate name of file for logs and final strategies
			String filename;
			if (run_nr < 10) {
				filename = "0" + run_nr;
			} else if (run_nr < 100) {
				filename = "" + run_nr;
			} else {
				filename = String.valueOf(run_nr);
			}

			// Create Callback to print out players strategies after each iteration
			BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> {
				System.out.println(iteration + " " + type);

				bundles.forEach((key, value) -> {
					// Print out strategy
					StringBuilder builder = new StringBuilder();
					builder.append(String.format(Locale.ENGLISH, "%2d", key));
					builder.append(String.format(Locale.ENGLISH, " %7.6f  ", epsilon));

					// Cast strategy to UnivariatePWLStrategy to get access to underlying data structure
					UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) strategies.get(key);
					for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {
						builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getKey()));
						builder.append(" ");
						builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getValue()));
						builder.append("  ");
					}
					System.out.println(builder.toString());
				});
				System.out.println();
			};
			bneAlgo.setCallback(callback);

			// Run the bne algorithm
			BNEAlgorithm.Result<Double, Double> result;
			result = bneAlgo.run();

			// Write the final strategy to a .strats file
			File stratsFile = new File(folder_output + "/" + filename + ".strats");
			FileWriter fr_strats = new FileWriter(stratsFile, true);
			BufferedWriter br_strats = new BufferedWriter(fr_strats);
			for (int inc = 0; inc <= result.equilibriumStrategies.size()-1; inc++) {
				UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) result.equilibriumStrategies.get(inc);
				StringBuilder builder = new StringBuilder();
				builder.append(String.format(Locale.ENGLISH, "%2d", inc));
				builder.append(" ");

				for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {
					builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getKey()));
					builder.append(" ");
					builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getValue()));
					builder.append("  ");
				}
				System.out.println(builder.toString());
				br_strats.write(builder.toString() + "\n");
			}
			br_strats.close();
			fr_strats.close();

			long endTime   = System.nanoTime();
			long totalTime = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);

			// Write information about this run to a .log file
			File logFile = new File( folder_output + "/" + filename + ".log");
			FileWriter fr = new FileWriter(logFile, true);
			BufferedWriter br = new BufferedWriter(fr);

			br.write("runtime " + totalTime + "\n");
			if (Double.isInfinite(result.epsilon)) {
                br.write("converged False" + "\n");
            } else {
                br.write("converged True" + "\n");
            }
			br.write("epsilon " + result.epsilon + "\n");
			br.write("players " + nr_players  + "\n");
			br.write("items " + nr_items  + "\n");
			br.write("probability " + prob_interest  + "\n");

			bundles.forEach((key, value) -> {
				try {
					br.write("bundle" + key.toString() + " ");
					br .write(Arrays.toString(value) + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

			br.close();
			fr.close();

			// TODO: 22.11.
			// FirstPriceOverbidding(bundles, max_f, overbidbundle, playernr)

			// TODO: 22.11.
			// setOptimizer -> multivariate cross pattern für step nach verification.
		}
	}
}
