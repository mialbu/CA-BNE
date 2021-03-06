package ch.uzh.ifi.ce.cabne.thesisexamples;

		import ch.uzh.ifi.ce.cabne.BR.AdaptivePWLBRCalculator;
		import ch.uzh.ifi.ce.cabne.BR.PWLBRCalculator;
		import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithm;
		import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithmCallback;
		import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
		import ch.uzh.ifi.ce.cabne.bundelgenerator.BundleGenerator;
		import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPrice;
		import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPriceSampler;
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
		String pre = args[1];

		// Define number of players and items and probability of an item getting chosen for interest for a player
		int nrPlayers = Integer.parseInt(args[2]);
		int nrItems = Integer.parseInt(args[3]);
		double prob_interest = 1.0 - Math.pow(0.01, 1.0/nrItems);

		int startRun = Integer.parseInt(args[4]);
		int endRun = Integer.parseInt(args[5]);
		int nrOfRuns = endRun - startRun;

		String folder_output = pre + "/";

		for (int run_nr = startRun; run_nr < endRun; run_nr++) {
				long startTime = System.nanoTime();

				// Generate name of file for logs and final strategies
				String filename;
				if (run_nr < 10) {
					filename = "00" + (run_nr);
				} else if (run_nr < 100) {
					filename = "0" + run_nr;
				} else {
					filename = String.valueOf(run_nr);
				}

				// Create Context And Read Config file for algorithm structure
				BNESolverContext<Double, Double> context = new BNESolverContext<>();
				String configfile = args[0];
				context.parseConfig(configfile);

			boolean hasConverged = false;
			while (!hasConverged) {
				// Initialize bundles
				BundleGenerator bundleGenerator = new BundleGenerator(nrPlayers, nrItems, prob_interest);

				HashMap<Integer, int[]> bundles = bundleGenerator.getBundles();
				bundles.forEach((key, value) -> {
					System.out.print(key + " ");
					for (String item : Arrays.toString(value).split(" ")) {
						if (item.startsWith("[")) {
							System.out.print(item.substring(1));
						} else if (item.endsWith("]")) {
							System.out.print(item.substring(0, 1));
						} else {
							System.out.print(item);
						}
					}
					System.out.println();
				});
				System.out.println();

				// Calculate all maximal and feasible allocations
				ArrayList<ArrayList<Integer>> max_feasible_allocations = bundleGenerator.getMaxFeasibleAllocations();

				// Initialize all algorithm pieces (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
				context.setOptimizer(new PatternSearch<>(context, new UnivariatePattern()));

				// Set Integrator used for calculation of expected utility
				context.setIntegrator(new MCIntegrator<>(context));

				// Set a random number generator (at this auction each player only bids on his one bundle)
				context.setRng(nrPlayers - 1, new CommonRandomGenerator(nrPlayers - 1));

				// Set an update rule for pointwise best response calculation
				context.setUpdateRule(new UnivariateDampenedUpdateRule(0.2, 0.6, 0.5 / context.getDoubleParameter("epsilon"), true));

				// Set best response calculator (piecewise linear best response calculator)
				context.setBRC(new AdaptivePWLBRCalculator(context));

				// choose best response calculator for outer loop
				context.setOuterBRC(new PWLBRCalculator(context));

				// Set verifier to verify the result in the verification step
				context.setVerifier(new BoundingVerifier1D(context));


				// Initialize auction settings
				// Choose mechanism that is used for price calculation
				context.setMechanism(new FirstPrice(max_feasible_allocations));

				// Set mechanism sampler (FPMBSampler uses BidIterator)
				context.setSampler(new FirstPriceSampler(context, nrPlayers));

				// Create a BNEAlgorithm instance with number of bidders and configuration
				BNEAlgorithm<Double, Double> bneAlgo = new BNEAlgorithm<>(nrPlayers, context);

				// Add the initial strategy for the first local bidder to the BNEAlgorithm
				// lower is always 0.0 / upper is k (nr of items that are of interest)
				bundles.forEach((key, value) -> {
					double max_value = 0;
					for (int val : value) {
						max_value += val;
					}
					bneAlgo.setInitialStrategy(key, UnivariatePWLStrategy.makeTruthful(0.0, max_value));
					if (max_value == 0) {
						bneAlgo.makeBidderNonUpdating(key);
					}
				});

				File stratsFile = new File(folder_output + "/" + filename + ".strats");
				FileWriter fr_strats = new FileWriter(stratsFile, false);
				BufferedWriter br_strats = new BufferedWriter(fr_strats);

				// Create Callback to print out players strategies after each iteration
				BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> {
					try {
						br_strats.write(String.format(Locale.ENGLISH, "%10.9f", epsilon));
						br_strats.newLine();

						bundles.forEach((key, value) -> {
							// Print out strategy
							StringBuilder builder = new StringBuilder();
							builder.append(String.format(Locale.ENGLISH, "%2d", key)).append("  ");

							// Cast strategy to UnivariatePWLStrategy to get access to underlying data structure
							UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) strategies.get(key);
							for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {
								builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getKey()));
								builder.append(" ");
								builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getValue()));
								builder.append("  ");
							}

							try {
								br_strats.write(builder.toString());
								br_strats.newLine();
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
					} catch (IOException e) {
						e.printStackTrace();
					}
				};
				bneAlgo.setCallback(callback);

				// Run the bne algorithm
				BNEAlgorithm.Result<Double, Double> result;
				result = bneAlgo.run();

				br_strats.close();
				fr_strats.close();

				long endTime = System.nanoTime();
				long totalTime = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);

				// Write information about this run to a .log file
				File logFile = new File(folder_output + "/" + filename + ".log");
				FileWriter fr = new FileWriter(logFile, false);
				BufferedWriter br = new BufferedWriter(fr);

				br.write("runtime " + totalTime + "\n");
				if (Double.isInfinite(result.epsilon)) {
					br.write("converged False" + "\n");
				} else {
					br.write("converged True" + "\n");
				}
				br.write("epsilon " + result.epsilon + "\n");
				br.write("players " + nrPlayers + "\n");
				br.write("items " + nrItems + "\n");
				br.write("probability " + prob_interest + "\n");

				bundles.forEach((key, value) -> {
					try {
						br.write(key + " ");
						for (String curItem : Arrays.toString(value).split(" ")) {
							if (curItem.startsWith("[")) {
								br.write(curItem.substring(1));
							} else if (curItem.endsWith("]")) {
								br.write(curItem.substring(0, 1));
							} else {
								br.write(curItem);
							}
						}
						br.newLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

				try {
					br.write("allocations " + max_feasible_allocations.size());
					br.newLine();
					for (ArrayList<Integer> allocs : max_feasible_allocations) {
						for (Integer alloc : allocs) {
							br.write(String.valueOf(alloc));
						}
						br.newLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (Double.isFinite(result.epsilon)) {
					hasConverged = true;
				} else {
					File notFile = new File(folder_output + "/" + args[4] + "notConverged.txt");
					FileWriter notfr = new FileWriter(notFile, true);
					BufferedWriter notbr = new BufferedWriter(notfr);
					bundles.forEach((key, value) -> {
						try {
							notbr.write(key + " ");
							for (String curItem : Arrays.toString(value).split(" ")) {
								if (curItem.startsWith("[")) {
									notbr.write(curItem.substring(1));
								} else if (curItem.endsWith("]")) {
									notbr.write(curItem.substring(0, 1));
								} else {
									notbr.write(curItem);
								}
							}
							notbr.newLine();
						} catch (IOException e) {
							e.printStackTrace();
						}
					});

					try {
						notbr.write("allocations " + max_feasible_allocations.size());
						notbr.newLine();
						for (ArrayList<Integer> allocs : max_feasible_allocations) {
							for (Integer alloc : allocs) {
								notbr.write(String.valueOf(alloc));
							}
							notbr.newLine();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					notbr.newLine();
					notbr.close();
					notfr.close();
				}

				br.close();
				fr.close();
			}
		}
	}
}
