package ch.uzh.ifi.ce.cabne.thesisexamples;

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
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class ThesisOverbidding {

	public static void main(String[] args) throws InterruptedException, IOException {
		long startTime = System.nanoTime();

		// Create Context And Read Config file for algorithm structure
		BNESolverContext<Double, Double> context = new BNESolverContext<>();
		String configfile = args[0];
		context.parseConfig(configfile);

		// Read log file
		String folder_file = "misc/scripts/5-4-4/";
		String filename = "03";
		String testfilename = filename + "test";
		File logFile = new File(folder_file + "/" + filename + ".log");
		FileReader fr = new FileReader(logFile);
		BufferedReader br = new BufferedReader(fr);

		// runtime
		int runtime = Integer.parseInt(br.readLine().split(" ")[1]);
		System.out.println(runtime);

		// converged (boolean)
		boolean converged = Boolean.parseBoolean(br.readLine().split(" ")[1]);
		System.out.println(converged);

		if (!converged) {
			System.out.println("##### Auction did not converge! #####");
		} else {
			// epsilon
			Float eps = Float.parseFloat(br.readLine().split(" ")[1]);
//			System.out.println(eps);

			// number of players
			int nr_players = Integer.parseInt(br.readLine().split(" ")[1]);
//			System.out.println(nr_players);

			// number of items
			int nr_items = Integer.parseInt(br.readLine().split(" ")[1]);
//			System.out.println(nr_items);

			// probability
			Float prob = Float.parseFloat(br.readLine().split(" ")[1]);
//			System.out.println(prob);

			// bundles
			HashMap<Integer, int[]> bundles = new HashMap<>();
			for (int pls = 0; pls < nr_players; pls++) {
				String currentLine = br.readLine();
				Integer curPl = Integer.parseInt(currentLine.split(" ")[0]);
				int[] curIts = new int[nr_items];
				for (int its = 0; its < nr_items; its++) {
					curIts[its] = Integer.parseInt(currentLine.split(" ")[1].split(",")[its]);
				}
				bundles.put(curPl, curIts);
			}

			// TODO 15dez: add player with parent bundle of current player

			bundles.forEach((key, value) -> {
				System.out.print(key + ": ");
				for (int item : value) {
					System.out.print(item);
				}
				System.out.println();
			});

			// Calculate all maximal and feasible allocations
			BundleGenerator bundleGenerator = new BundleGenerator(bundles);
			ArrayList<ArrayList<Integer>> max_feasible_allocations = bundleGenerator.get_max_feasible_allocs();
			max_feasible_allocations.forEach(System.out::println);

			// Initialize all algorithm pieces (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
			context.setOptimizer(new PatternSearch<>(context, new UnivariatePattern()));

			// Set Integrator used for calculation of expected utility
			context.setIntegrator(new MCIntegrator<>(context));

			// TODO 15dez: with additional bundle -> dim = nr_players?
			// Set a random number generator (at this auction each player only bids on his one bundle)
			context.setRng(nr_players - 1, new CommonRandomGenerator(nr_players - 1));

			// Set an update rule for pointwise best response calculation
			context.setUpdateRule(new UnivariateDampenedUpdateRule(0.2, 0.6, 0.5 / context.getDoubleParameter("epsilon"), true));

			// Set best response calculator (piecewise linear best response calculator)
			context.setBRC(new PWLBRCalculator(context));

			// choose best response calculator for outer loop
			context.setOuterBRC(new PWLBRCalculator(context));

			// Set verifier to verify the result in the verification step
			context.setVerifier(new BoundingVerifier1D(context));

			// TODO: for each additional possible bundle a bidder may bid on
			//  -> calc feas allocs and set everything about the auction up and run a two-dimensional best response optimization
			//  -> Trying to find out if the expected utility of this new BR is higher than the one before.
			// TODO: Do I have to calculate the expected utility of the former auction

			// Initialize auction settings
			// Choose mechanism that is used for price calculation
			context.setMechanism(new FirstPrice(max_feasible_allocations));

			// Set mechanism sampler (FPMBSampler uses BidIterator)
			context.setSampler(new FirstPriceMBSampler(context, nr_players));

			// Create a BNEAlgorithm instance with number of bidders and configuration
			BNEAlgorithm<Double, Double> bneAlgo = new BNEAlgorithm<>(nr_players, context);

			// TODO 15dez: for each player read line with strategy (here just one row to test)
			File stratFile = new File(folder_file + "/" + filename + ".strats");
			FileReader sr = new FileReader(stratFile);
			BufferedReader sbr = new BufferedReader(sr);

			sbr.readLine();

			for (int pl = 0; pl < nr_players; pl++) {
				String line = sbr.readLine();
				int currentPlayer = Integer.parseInt(String.valueOf(line.split("  ")[0].charAt(1)));
				System.out.println(currentPlayer);
//				System.out.println("#" + line.split("  ")[0].charAt(1) + "#");
				String[] valbids = line.split("  ");
				valbids = ArrayUtils.remove(valbids, 0);
				int nr_steps = valbids.length;
				double[] steps = new double[nr_steps];
				double[] bids = new double[nr_steps];
				for (int incr = 0; incr < nr_steps; incr++) {
					steps[incr] = Double.parseDouble(valbids[incr].split(" ")[0]);
					bids[incr] = Double.parseDouble(valbids[incr].split(" ")[1]);
				}
//			for (int i= 0; i<steps.length;i++) {
//				System.out.print(steps[i]);
//				System.out.print(": ");
//				System.out.println(bids[i]);
//			}

				// Add the initial strategy for the first local bidder to the BNEAlgorithm
				// lower is always 0.0 / upper is k (nr of items that are of interest)
				// TODO 10dez: set initial strategy to equilibrium strategy read from .strats file
				// steps.length == bids.length -> otherwise it does not work
				bneAlgo.setInitialStrategy(currentPlayer, UnivariatePWLStrategy.setStrategy(steps, bids));
			}
				//TODO:####################################################################################################
				File stratsFile = new File(folder_file + "/" + testfilename + ".strats");
				FileWriter fr_strats = new FileWriter(stratsFile, false);
				BufferedWriter br_strats = new BufferedWriter(fr_strats);

				// Create Callback to print out players strategies after each iteration
				BNEAlgorithmCallback<Double, Double> callback = (iteration, type, strategies, epsilon) -> {
					try {
						br_strats.write(String.format(Locale.ENGLISH, "%10.9f", epsilon));
						br_strats.newLine();
//					System.out.println(iteration + " " + type + " " + String.format(Locale.ENGLISH, "%10.9f", epsilon));
						System.out.println(String.format(Locale.ENGLISH, "%10.9f", epsilon));

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
//						System.out.println(builder.toString());
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
				// TODO:#####################################################################################################
			long endTime = System.nanoTime();
			long totalTime = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);

			// Write information about this run to a .log file
			File testLogFile = new File(folder_file + "/" + testfilename + ".log");
			FileWriter testfr = new FileWriter(testLogFile, false);
			BufferedWriter testbr = new BufferedWriter(testfr);

			testbr.write("runtime " + totalTime + "\n");
			if (Double.isInfinite(result.epsilon)) {
				testbr.write("converged False" + "\n");
			} else {
				testbr.write("converged True" + "\n");
			}
			testbr.write("epsilon " + result.epsilon + "\n");
			testbr.write("players " + nr_players + "\n");
			testbr.write("items " + nr_items + "\n");

			bundles.forEach((key, value) -> {
				try {
					testbr.write(key + " ");
					for (String item : Arrays.toString(value).split(" ")) {
						if (item.startsWith("[")) {
							testbr.write(item.substring(1));
						} else if (item.endsWith("]")) {
							testbr.write(item.substring(0, 1));
						} else {
							testbr.write(item);
						}
					}
					testbr.newLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

//			bundles.forEach((key, value) -> {
//				try {
//					br.write("bundle" + key.toString() + " ");
//					br .write(Arrays.toString(value) + "\n");
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			});

			br.close();
			fr.close();
			}



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
	}
}
