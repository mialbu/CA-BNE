package ch.uzh.ifi.ce.cabne.thesisexamples;

import ch.uzh.ifi.ce.cabne.BR.BRCalculator;
import ch.uzh.ifi.ce.cabne.BR.PWLOverbiddingBRCalculator;
import ch.uzh.ifi.ce.cabne.BR.PWLSimpleBRCalculator;
import ch.uzh.ifi.ce.cabne.algorithm.BNEAlgorithmCallback;
import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.bundelgenerator.BundleGenerator;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPrice;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPriceOverbidding;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPriceOverbiddingSampler;
import ch.uzh.ifi.ce.cabne.domains.FirstPriceThesis.FirstPriceSampler;
import ch.uzh.ifi.ce.cabne.integration.MCIntegrator;
import ch.uzh.ifi.ce.cabne.pointwiseBR.BoxPattern2D;
import ch.uzh.ifi.ce.cabne.pointwiseBR.PatternSearch;
import ch.uzh.ifi.ce.cabne.pointwiseBR.UnivariatePattern;
import ch.uzh.ifi.ce.cabne.pointwiseBR.updateRule.DirectUpdateRule;
import ch.uzh.ifi.ce.cabne.pointwiseBR.updateRule.DirectUpdateRuleOverbidding;
import ch.uzh.ifi.ce.cabne.randomsampling.CommonRandomGenerator;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLOverbiddingStrategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;
import org.apache.commons.lang3.ArrayUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class ThesisOverbidding {

	public static void main(String[] args) throws InterruptedException, IOException {
		long startTime = System.nanoTime();

		// Create Context And Read Config file for algorithm structure
		BNESolverContext<Double, Double> contextSimple = new BNESolverContext<>();
		BNESolverContext<Double, Double[]> contextOverbidding = new BNESolverContext<>();
		String configfile = args[0];
		String simple = args[1];
		String overbidding = args[2];
		contextSimple.parseConfig(configfile);
		contextOverbidding.parseConfig(configfile);

		// FILE READ
		// Read log file
		String folder = "misc/scripts/jan02start/";
		String filename = "000";
		String simpleBRFilename = filename + ".simpleBRstrats";
		String simpleBROutputFile = folder + simpleBRFilename;
		String overbiddingBRFilename = filename + ".overbiddingStrats";
		String overbiddingBROutputFile = folder + overbiddingBRFilename;

		File logFile = new File(folder + "/" + filename + ".log");
		FileReader fr = new FileReader(logFile);
		BufferedReader br = new BufferedReader(fr);

		int runtime = Integer.parseInt(br.readLine().split(" ")[1]);
		boolean converged = Boolean.parseBoolean(br.readLine().split(" ")[1]);

		if (!converged) {
			System.out.println("##### Auction did not converge! #####");
			//continue;
		} else {
			Float eps = Float.parseFloat(br.readLine().split(" ")[1]);
			int nrPlayers = Integer.parseInt(br.readLine().split(" ")[1]);
			int nrItems = Integer.parseInt(br.readLine().split(" ")[1]);
			Float prob = Float.parseFloat(br.readLine().split(" ")[1]);

			HashMap<Integer, int[]> bundles = new HashMap<>();
			for (int pls = 0; pls < nrPlayers; pls++) {
				String currentLine = br.readLine();
				Integer curPl = Integer.parseInt(currentLine.split(" ")[0]);
				int[] curIts = new int[nrItems];
				for (int its = 0; its < nrItems; its++) {
					curIts[its] = Integer.parseInt(currentLine.split(" ")[1].split(",")[its]);
				}
				bundles.put(curPl, curIts);
			}

			bundles.forEach((key, value) -> {
				System.out.print(key + ": ");
				for (int val : value) {
					System.out.print(val);
				}
				System.out.println();
			});

			br.close();
			fr.close();

			BundleGenerator bundleGenerator = new BundleGenerator(bundles);
			ArrayList<ArrayList<Integer>> maxFeasibleAllocations = bundleGenerator.getMaxFeasibleAllocations();

			contextSimple.setOptimizer(new PatternSearch<>(contextSimple, new UnivariatePattern()));
			contextOverbidding.setOptimizer(new PatternSearch<>(contextOverbidding, new BoxPattern2D()));

			contextSimple.setIntegrator(new MCIntegrator<>(contextSimple));
			contextOverbidding.setIntegrator(new MCIntegrator<>(contextOverbidding));

			contextSimple.setRng(nrPlayers - 1, new CommonRandomGenerator(nrPlayers - 1));
			contextOverbidding.setRng(nrPlayers - 1, new CommonRandomGenerator(nrPlayers - 1));  // todo: what rng?

			contextSimple.setUpdateRule(new DirectUpdateRule());
			contextOverbidding.setUpdateRule(new DirectUpdateRuleOverbidding());

//			contextSimple.setBRC(new PWLSimpleBRCalculator(contextSimple, ""));
//			contextSimple.setVerifier(new BoundingVerifier1D(contextSimple));
//			contextOverbidding.setVerifier(new BoundingVerifier1D(contextOverbidding));

			contextSimple.setMechanism(new FirstPrice(maxFeasibleAllocations));
			contextOverbidding.setMechanism(new FirstPriceOverbidding(maxFeasibleAllocations));

			contextSimple.setSampler(new FirstPriceSampler(contextSimple, nrPlayers));
			contextOverbidding.setSampler(new FirstPriceOverbiddingSampler(contextOverbidding, nrPlayers));

//			contextOverbidding.setBRC(new PWLOverbiddingBRCalculator(contextOverbidding, "a.testingob182"));  // outputfile will be set for each auction

			Map<Integer, Strategy<Double, Double>> eqSimpleStrats = new HashMap<>();
			Map<Integer, Strategy<Double, Double[]>> eqArrayStrats = new HashMap<>();
//			boolean[] updateBidder = new boolean[nrPlayers];

			// READ EQULIBRIUM STRATEGIES
			File stratFile = new File(folder + "/" + filename + ".eqStrats");
			FileReader sr = new FileReader(stratFile);
			BufferedReader sbr = new BufferedReader(sr);
			sbr.readLine();

			for (int pl = 0; pl < nrPlayers; pl++) {
				String line = sbr.readLine();
				int currentPlayer = Integer.parseInt(String.valueOf(line.split("  ")[0].charAt(1)));
				String[] valbids = line.split("  ");
				valbids = ArrayUtils.remove(valbids, 0);
				int nr_steps = valbids.length;
				double[] steps = new double[nr_steps];
				double[] bids = new double[nr_steps];
				for (int incr = 0; incr < nr_steps; incr++) {
					steps[incr] = Double.parseDouble(valbids[incr].split(" ")[0]);
					bids[incr] = Double.parseDouble(valbids[incr].split(" ")[1]);
				}
				eqSimpleStrats.put(pl, UnivariatePWLStrategy.setStrategy(steps, bids));
				eqArrayStrats.put(pl, UnivariatePWLOverbiddingStrategy.setStrategy(false, steps, bids));  // todo: check how overbidding=false occurs -> is it correct?
			}

			File brStratFile = new File(simpleBROutputFile);
			FileWriter fileWriter = new FileWriter(brStratFile, false);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			bufferedWriter.write("Simple Best Response On Equilibrium");
			bufferedWriter.newLine();
			bufferedWriter.close();
			fileWriter.close();

			// RUN CALCULATION OF SIMPLE BEST RESPONSE FOR ALL BIDDERS
			// create list of strategies
			List<Strategy<Double, Double>> eqSimpleStratsList = new ArrayList<>();
			for (int i = 0; i < nrPlayers; i++) {
				eqSimpleStratsList.add(eqSimpleStrats.get(i));  //get(canonicalBidders[i])
			}
			List<Strategy<Double, Double[]>> eqArrayStratsList = new ArrayList<>();
			for (int i = 0; i < nrPlayers; i++) {
				eqArrayStratsList.add(eqArrayStrats.get(i));  //get(canonicalBidders[i])
			}

			contextSimple.activateConfig("verificationstep");
//			contextSimple.advanceRngs();
			int gridsize = contextSimple.getIntParameter("gridsize");

			if (simple.equals("1")) {
				PWLSimpleBRCalculator brcSimple = new PWLSimpleBRCalculator(contextSimple, simpleBROutputFile);

				// compute best response for overbidding player
				for (int curB = 0; curB < nrPlayers; curB++) {
					PWLSimpleBRCalculator.Result<Double, Double> brResult = brcSimple.computeBR(curB, eqSimpleStratsList);
					//double highestEpsilon = result.epsilonAbs;
				}
			}

			if (overbidding.equals("1")) {
				// RUN OVERBIDDING OF PLAYINGBIDDER for each overbidding bundle - write out overbidding bundle and utility and on the second line the strategy with 2-dim bids
				int overbiddingBidder = 2;

				// Calculate all maximal and feasible allocations
				HashMap<Integer, int[]> oBundles = new HashMap<>();
				oBundles = bundleGenerator.generateParentBundles(bundles.get(overbiddingBidder));

				// key: maximal feasible allocations - value: overbidding bundles with the same maxFeasAllocs
				// now the best response only has to be calculated for each entry in this hashmap instead of all parentBundles!
				HashMap<ArrayList<ArrayList<Integer>>, ArrayList<int[]>> oMaxFeasibleAllocations = new HashMap<>();

				boolean curMaxFeasAdded;
				for (Map.Entry<Integer, int[]> entry : oBundles.entrySet()) {
					Integer key = entry.getKey();
					int[] value = entry.getValue();

					ArrayList<ArrayList<Integer>> curMaxFeas = bundleGenerator.calculateOverbiddingMaxFeasibleAllocs(overbiddingBidder, value);
					curMaxFeasAdded = false;
					for (Map.Entry<ArrayList<ArrayList<Integer>>, ArrayList<int[]>> e : oMaxFeasibleAllocations.entrySet()) {
						ArrayList<ArrayList<Integer>> k = e.getKey();
						ArrayList<int[]> v = e.getValue();

						if (k.containsAll(curMaxFeas) && curMaxFeas.containsAll(k)) {
							ArrayList<int[]> cBundles = new ArrayList<>();
							cBundles = v;
							cBundles.add(value);
							oMaxFeasibleAllocations.put(k, cBundles);
							curMaxFeasAdded = true;
							break;
						}
					}
					if (!curMaxFeasAdded) {
						ArrayList<int[]> listToAdd = new ArrayList<>();
						listToAdd.add(value);
						oMaxFeasibleAllocations.put(curMaxFeas, listToAdd);
					}
				}
				System.out.println(1);

//				oMaxFeasibleAllocations.forEach((k, v) -> {
//
//				});





				// foreach obundle calculate omaxfeasallocs
				// save the obundle and save the omaxfeasallocs
				// if for any future obundle, the same omaxfeasallocs have already been calculated
				// add this bundle to the list of the recent bundle
				// for those two bundles, the best response has only once to be calculated!

//				HashMap<Integer, ArrayList<ArrayList<Integer>>> oMaxFeasibleAllocations = new HashMap<>();
//				HashMap<Integer, ArrayList<int[]>> oBundleMap = new HashMap<>();
				// generate all parent bundles and add them to this map




				int oMaxCount = 0;
//				for (Map.Entry<Integer, int[]> entry : oBundles.entrySet()) {
//					Integer k = entry.getKey();
//					int[] v = entry.getValue();
//
//					// for each bundle calculate maxfeas
//					ArrayList<ArrayList<Integer>> curMaxFeas = bundleGenerator.calculateOverbiddingMaxFeasibleAllocs(overbiddingBidder, v);

//					if (oMaxFeasibleAllocations.containsValue(curMaxFeas)) {
//
//					} else {
//						// add this alloc to the omaxfeasMap
//						oMaxFeasibleAllocations.put(oMaxCount, curMaxFeas);
//						oBundleSet.put(oMaxCount, v);
//						oMaxCount += 1;
//					}
//
//					oMaxFeasibleAllocations.put(0, curMaxFeas);
//				}
//
//				// for each key in omaxfeasallocs run the br
//				oMaxFeasibleAllocations.forEach((key, value) -> {
//				});
//
//
//
//				oMaxFeasibleAllocations =
//						bundleGenerator.calculateOverbiddingMaxFeasibleAllocs(overbiddingBidder, overbiddingBundle);
//
//				HashMap<Integer, ArrayList<ArrayList<Integer>>> oAllocs = new HashMap<>();
//				boolean t = true;
//
//				if (t) {
//					oAllocs.put(0, oMaxFeasibleAllocations);
//				}
//				for (int oRun=0; oRun<oAllocs.size(); oRun++) {
//					overbiddingBundle = oAllocs.get(oRun).get(0);
//				}








				// Initialize all algorithm pieces (PatternSearch, UnivariateBrentSearch, UnivariateGridSearch)
				contextOverbidding.setOptimizer(new PatternSearch<>(contextOverbidding, new BoxPattern2D()));
				// TODO 15dez: with additional bundle -> dim = nrPlayers?
				// Set a random number generator (at this auction each player only bids on his one bundle)
				contextOverbidding.setRng(nrPlayers, new CommonRandomGenerator(nrPlayers));

				// TODO: for each additional possible bundle a bidder may bid on
				//  -> calc feas allocs and set everything about the auction up and run a two-dimensional best response optimization
				//  -> Trying to find out if the expected utility of this new BR is higher than the one before.

				contextOverbidding.activateConfig("verificationstep");
				gridsize = contextOverbidding.getIntParameter("gridsize");
				//context.advanceRNgs();
				PWLOverbiddingBRCalculator brcOverbidding = new PWLOverbiddingBRCalculator(contextOverbidding, overbiddingBROutputFile);

				// compute best response for overbidding player
				BRCalculator.Result<Double, Double[]> overbiddingResult = brcOverbidding.computeBR(overbiddingBidder, eqArrayStratsList);
				Strategy<Double, Double[]> overbiddingBR = overbiddingResult.br;

				long endTime = System.nanoTime();
				long totalTime = TimeUnit.NANOSECONDS.toSeconds(endTime - startTime);
			}
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
