package ch.uzh.ifi.ce.cabne.myexamples;

import ch.uzh.ifi.ce.cabne.BR.PWLBRCalculator;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithm;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithmCallback;
import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.bundelgenerator.BundleGenerator;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPrice;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPriceMBSampler;
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


public class ThesisOverbidding {

	public static void main(String[] args) throws InterruptedException, IOException {

		// TODO: mb 6.12.
		//  -> Read log file
		//  -> check if converged
		//  -> if converged: (do while next step exists)
		//  	-> read strategy file
		//  	-> read first line split(" ")[0] = number of players
		//  	-> then each entry consists of reading one line (iteration and current epsilon)
		//  	-> and reading a line for each players current strategy
		//  	-> when strategies of one iteration are read
		//  		-> plot current strategies -> update plot
		//  			-> delete current plot content
		//  			-> plot current strategies
		//  -> else: continue



//		int nrOfRuns = 10;
//		System.out.println("Total runs: " + nrOfRuns);
//
//		// Define number of players and items and probability of an item getting chosen for interest for a player
//		int nr_players = 4;
//		int nr_items = 4;
//		double prob_interest = 0.3;
//		String folder_output = "misc/scripts/9-6-3/";
////		String folder_output = "../pass1/9-6-3/";
//
//		for (int run_nr = 0; run_nr < nrOfRuns; run_nr++) {
//			System.out.println("Starting run: " + run_nr);
//			long startTime = System.nanoTime();
//
//			// Generate name of file for logs and final strategies
//			String filename;
//			if (run_nr < 10) {
//				filename = "0" + run_nr;
//			} else if (run_nr < 100) {
//				filename = "" + run_nr;
//			} else {
//				filename = String.valueOf(run_nr);
//			}
//
//			// Create Context And Read Config file for algorithm structure
//			BNESolverContext<Double, Double> context = new BNESolverContext<>();
//			String configfile = args[0];
//			context.parseConfig(configfile);
//
//			// Initialize bundles
//			BundleGenerator bundleGenerator = new BundleGenerator(nr_players, nr_items, prob_interest);
//
//			// Check if there is any interest in any item
////			boolean hasAnyInterest = bundleGenerator.hasAnyInterest();
////			if (!hasAnyInterest) {
////			long endTime   = System.nanoTime();
////			long totalTime = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);
////
////			// Write information about this run to a .log file
////			File logFile = new File( folder_output + "/" + filename + ".log");
////			FileWriter fr = new FileWriter(logFile, true);
////			BufferedWriter br = new BufferedWriter(fr);
////			br.write("runtime " + totalTime + "\n");
////			br.write("converged False" + "\n");
////			br.write("epsilon Infinity\n");
////			br.write("players " + nr_players  + "\n");
////			br.write("items " + nr_items  + "\n");
////			br.write("probability " + prob_interest  + "\n");
////			br.write("0 allEmpty");
////			continue;
////			}
//
//			HashMap<Integer, int[]> bundles = bundleGenerator.get_bundles();
////			bundles.forEach((key, value) -> {
////				System.out.print(key + " ");
////				for (String item : Arrays.toString(value).split(" ")) {
////					if (item.startsWith("[")) {
////						System.out.print(item.substring(1));
////					} else if (item.endsWith("]")) {
////						System.out.print(item.substring(0,1));
////					} else {
////						System.out.print(item);
////					}
////				}
////				System.out.println();
////			});
////			System.out.println();
//
//			// Calculate all maximal and feasible allocations
//			ArrayList<ArrayList<Integer>> max_feasible_allocations = bundleGenerator.get_max_feasible_allocs();
//
//			// Initialize all algorithm pieces (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
//			context.setOptimizer(new PatternSearch<>(context, new UnivariatePattern()));
//
//			// Set Integrator used for calculation of expected utility
//			context.setIntegrator(new MCIntegrator<>(context));
//
//			// Set a random number generator (at this auction each player only bids on his one bundle)
//			context.setRng(nr_players - 1, new CommonRandomGenerator(nr_players - 1));
//
//			// Set an update rule for pointwise best response calculation
//			context.setUpdateRule(new UnivariateDampenedUpdateRule(0.2, 0.6, 0.5 / context.getDoubleParameter("epsilon"), true));
//
//			// Set best response calculator (piecewise linear best response calculator)
//			context.setBRC(new PWLBRCalculator(context));
//
//			// choose best response calculator for outer loop  // TODO: -> ignore verification step
//			context.setOuterBRC(new PWLBRCalculator(context));
//
//			// Set verifier to verify the result in the verification step
//			context.setVerifier(new BoundingVerifier1D(context));
//
//
//			// Initialize auction settings
//			// Choose mechanism that is used for price calculation
//			context.setMechanism(new FirstPrice(max_feasible_allocations));
//
//			// Set mechanism sampler (FPMBSampler uses BidIterator)
//			context.setSampler(new FirstPriceMBSampler(context, nr_players));
//
//			// Create a BNEAlgorithm instance with number of bidders and configuration
//			BNEAlgorithm<Double, Double> bneAlgo = new BNEAlgorithm<>(nr_players, context);
//
//			// Add the initial strategy for the first local bidder to the BNEAlgorithm
//			// lower is always 0.0 / upper is k (nr of items that are of interest)
//			bundles.forEach((key, value) -> {
//				double max_value = 0;
//				for (int val : value) {
//					max_value += val;
//				} // TODO: 1.12. do not create a strategy for players with no interest at all, since the eps is then set to 0.0 always
//				// TODO: did not figure out why until now...  -- fixed on 4.12. by cutting 0-multiplication in MCIntegrator
//				bneAlgo.setInitialStrategy(key, UnivariatePWLStrategy.makeTruthful(0.0, max_value));
//				if (max_value == 0) {
//					bneAlgo.makeBidderNonUpdating(key);
//				}
//			});
//
//			File stratsFile = new File(folder_output + "/" + filename + ".strats");
//			FileWriter fr_strats0 = new FileWriter(stratsFile, true);
//			BufferedWriter br_strats0 = new BufferedWriter(fr_strats0);
//			try {
//				br_strats0.write(nr_players);
//				br_strats0.newLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			br_strats0.close();
//			fr_strats0.close();
//
//			FileWriter fr_strats = new FileWriter(stratsFile, true);
//			BufferedWriter br_strats = new BufferedWriter(fr_strats);
//
//
//			// Create Callback to print out players strategies after each iteration
//			BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> {
////				try {
////					br_strats.write(String.format(Locale.ENGLISH, "%10.9f", epsilon));
////					br_strats.newLine();
////					System.out.println(iteration + " " + type + " " + String.format(Locale.ENGLISH, "%10.9f", epsilon));
//					System.out.println(String.format(Locale.ENGLISH, "%10.9f", epsilon));
//
//					bundles.forEach((key, value) -> {
//						// Print out strategy
//						StringBuilder builder = new StringBuilder();
//						builder.append(String.format(Locale.ENGLISH, "%2d", key)).append("  ");
//
//						// Cast strategy to UnivariatePWLStrategy to get access to underlying data structure
//						UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) strategies.get(key);
//						for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {
//							builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getKey()));
//							builder.append(" ");
//							builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getValue()));
//							builder.append("  "); }
//
//						try {
//							br_strats.write(builder.toString());
//							br_strats.newLine();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
////						System.out.println(builder.toString());
//					});
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
//			};
//			bneAlgo.setCallback(callback);
//
//			// Run the bne algorithm
//			BNEAlgorithm.Result<Double, Double> result;
//			result = bneAlgo.run();
//
//			br_strats.close();
//			fr_strats.close();
//
////			// Write the final strategy to a .strats file
////			File stratsFile = new File(folder_output + "/" + filename + ".strats");
////			FileWriter fr_strats = new FileWriter(stratsFile, true);
////			BufferedWriter br_strats = new BufferedWriter(fr_strats);
////			for (int inc = 0; inc <= result.equilibriumStrategies.size()-1; inc++) {
////				UnivariatePWLStrategy sPWL = (UnivariatePWLStrategy) result.equilibriumStrategies.get(inc);
////				StringBuilder builder = new StringBuilder();
////				builder.append(String.format(Locale.ENGLISH, "%2d", inc));
////				builder.append(" ");
////
////				for (Map.Entry<Double, Double> e : sPWL.getData().entrySet()) {
////					builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getKey()));
////					builder.append(" ");
////					builder.append(String.format(Locale.ENGLISH, "%7.6f", e.getValue()));
////					builder.append("  ");
////				}
////				System.out.println(builder.toString());
////				br_strats.write(builder.toString() + "\n");
////			}
////			br_strats.close();
////			fr_strats.close();
//
//			long endTime   = System.nanoTime();
//			long totalTime = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);
//
//			// Write information about this run to a .log file
//			File logFile = new File( folder_output + "/" + filename + ".log");
//			FileWriter fr = new FileWriter(logFile, true);
//			BufferedWriter br = new BufferedWriter(fr);
//
//			br.write("runtime " + totalTime + "\n");
//			if (Double.isInfinite(result.epsilon)) {
//                br.write("converged False" + "\n");
//            } else {
//                br.write("converged True" + "\n");
//            }
//			br.write("epsilon " + result.epsilon + "\n");
//			br.write("players " + nr_players  + "\n");
//			br.write("items " + nr_items  + "\n");
//			br.write("probability " + prob_interest  + "\n");
//
//			bundles.forEach((key, value) -> {
//				try {
//					br.write(key + " ");
//					for (String item : Arrays.toString(value).split(" ")) {
//						if (item.startsWith("[")) {
//							br.write(item.substring(1));
//						} else if (item.endsWith("]")) {
//							br.write(item.substring(0, 1));
//						} else {
//							br.write(item);
//						}
//					}
//					br.newLine();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			});
//
////			bundles.forEach((key, value) -> {
////				try {
////					br.write("bundle" + key.toString() + " ");
////					br .write(Arrays.toString(value) + "\n");
////				} catch (IOException e) {
////					e.printStackTrace();
////				}
////			});
//
//			br.close();
//			fr.close();
//
//			// TODO: 22.11.
//			// FirstPriceOverbidding(bundles, max_f, overbidbundle, playernr)
//
//			// TODO: 22.11.
//			// setOptimizer -> multivariate cross pattern für step nach verification.
//		}
	}
}
