package ch.uzh.ifi.ce.cabne.thesisexamples;

import ch.uzh.ifi.ce.cabne.BR.BRCalculator;
import ch.uzh.ifi.ce.cabne.BR.PWLOverbiddingBRCalculator;
import ch.uzh.ifi.ce.cabne.BR.PWLSimpleBRCalculator;
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
import java.lang.reflect.Array;
import java.util.*;


public class ThesisOverbidding {

	public static void main(String[] args) throws InterruptedException, IOException {
		// Create Context and read Config file for algorithm structure
		BNESolverContext<Double, Double> contextSimple = new BNESolverContext<>();
		BNESolverContext<Double, Double[]> contextOverbidding = new BNESolverContext<>();
		String configfile = args[0];
		String folder = args[1];
		contextSimple.parseConfig(configfile);
		contextOverbidding.parseConfig(configfile);

		// Set number of starting and ending auction instance to
		int startAuction = Integer.parseInt(args[2]);
		int endAuction = Integer.parseInt(args[3]);
		for (int auction = startAuction; auction < endAuction; auction++) {
			// Read log file
			String filename;
			if (auction < 10) {
				filename = "00" + auction;
			} else if (auction < 100) {
				filename = "0" + auction;
			} else {
				filename = String.valueOf(auction);
			}
			String simpleBRFilename = filename + ".simpleBRstrats";
			String simpleBROutputFile = folder + simpleBRFilename;
			String overbiddingBRFilename = filename + ".overbiddingBRStrats";
			String overbiddingBROutputFile = folder + overbiddingBRFilename;

			File logFile = new File(folder + "/" + filename + ".log");
			FileReader fr = new FileReader(logFile);
			BufferedReader br = new BufferedReader(fr);

			int runtime = Integer.parseInt(br.readLine().split(" ")[1]);
			boolean converged = Boolean.parseBoolean(br.readLine().split(" ")[1]);

			if (converged) {
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

//				bundles.forEach((key, value) -> {
//					System.out.print(key + ": ");
//					for (int val : value) {
//						System.out.print(val);
//					}
//					System.out.println();
//				});

				int nrAllocations = Integer.parseInt(br.readLine().split(" ")[1]);
				ArrayList<ArrayList<Integer>> maxFeas = new ArrayList<>();
				for (int alloc=0; alloc<nrAllocations; alloc++) {
					String line = br.readLine();
					ArrayList<Integer> cur = new ArrayList<>();
					for (int b=0; b<line.length(); b++) {
						cur.add(Integer.parseInt(line.split("")[b]));
					}
					maxFeas.add(cur);
				}

				br.close();
				fr.close();

				BundleGenerator bundleGenerator = new BundleGenerator(bundles, maxFeas);
				ArrayList<ArrayList<Integer>> maxFeasibleAllocations = bundleGenerator.getMaxFeasibleAllocations();

				contextSimple.setOptimizer(new PatternSearch<>(contextSimple, new UnivariatePattern()));
				contextOverbidding.setOptimizer(new PatternSearch<>(contextOverbidding, new BoxPattern2D()));

				contextSimple.setIntegrator(new MCIntegrator<>(contextSimple));
				contextOverbidding.setIntegrator(new MCIntegrator<>(contextOverbidding));

				contextSimple.setRng(nrPlayers - 1, new CommonRandomGenerator(nrPlayers - 1));
				contextOverbidding.setRng(nrPlayers - 1, new CommonRandomGenerator(nrPlayers - 1));

				contextSimple.setUpdateRule(new DirectUpdateRule());
				contextOverbidding.setUpdateRule(new DirectUpdateRuleOverbidding());

				contextSimple.setMechanism(new FirstPrice(maxFeasibleAllocations));
				FirstPriceOverbidding oMechanism = new FirstPriceOverbidding(maxFeasibleAllocations, nrPlayers);
				contextOverbidding.setMechanism(oMechanism);

				contextSimple.setSampler(new FirstPriceSampler(contextSimple, nrPlayers));
				contextOverbidding.setSampler(new FirstPriceOverbiddingSampler(contextOverbidding, nrPlayers));
				contextOverbidding.activateConfig("verificationstep");

				Map<Integer, Strategy<Double, Double>> eqSimpleStrats = new HashMap<>();
				Map<Integer, Strategy<Double, Double[]>> eqArrayStrats = new HashMap<>();

				// Read equilibrium strategies from file
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
					double[] placeholderOverbids = new double[nr_steps];
					for (int incr = 0; incr < nr_steps; incr++) {
						steps[incr] = Double.parseDouble(valbids[incr].split(" ")[0]);
						bids[incr] = Double.parseDouble(valbids[incr].split(" ")[1]);
					}
					eqSimpleStrats.put(pl, UnivariatePWLStrategy.setStrategy(steps, bids));
					eqArrayStrats.put(pl, UnivariatePWLOverbiddingStrategy.setInitialStrategy(steps, bids));
				}

				// Run the calculation of simple best response for all bidders
				// Create list of strategies
				List<Strategy<Double, Double>> eqSimpleStratsList = new ArrayList<>();
				for (int i = 0; i < nrPlayers; i++) {
					eqSimpleStratsList.add(eqSimpleStrats.get(i));
				}
				// This list is used for the overbidding part
				List<Strategy<Double, Double[]>> eqArrayStratsList = new ArrayList<>();
				for (int i = 0; i < nrPlayers; i++) {
					eqArrayStratsList.add(eqArrayStrats.get(i));
				}

				contextSimple.activateConfig("outerloop");
				int simpleGridsize = contextSimple.getIntParameter("gridsize");

				File oBrFile = new File(overbiddingBROutputFile);
				FileWriter oFileWriter = new FileWriter(oBrFile, false);
				BufferedWriter oBufferedWriter = new BufferedWriter(oFileWriter);
				oBufferedWriter.write("Overbidding Best Response On Equilibrium");
				oBufferedWriter.newLine();
				oBufferedWriter.close();
				oFileWriter.close();
				// Create an array with all bidders that have interest in at least one item
				ArrayList<Integer> biddersWithValue = new ArrayList<>();
				for (Map.Entry<Integer, int[]> mapEntry : bundles.entrySet()) {
					Integer player = mapEntry.getKey();
					int[] bundle = mapEntry.getValue();
					int sum = 0;
					for (int item : bundle) {
						sum += item;
						if (sum > 0) {
							break;
						}
					}
					if (sum > 0) {
						biddersWithValue.add(player);
					}
				}

				PWLSimpleBRCalculator brcSimple = new PWLSimpleBRCalculator(contextSimple, simpleBROutputFile);
				PWLOverbiddingBRCalculator brcOverbidding = new PWLOverbiddingBRCalculator(contextOverbidding, overbiddingBROutputFile);
				File brStratFile = new File(simpleBROutputFile);
				FileWriter fileWriter = new FileWriter(brStratFile, false);
				BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
				bufferedWriter.write("Simple Best Response On Equilibrium");
				bufferedWriter.newLine();
				bufferedWriter.close();
				fileWriter.close();

				// Run overbidding algorithm for each bidder
				ArrayList<Integer> doneSimpleResults = new ArrayList<>();
				for (int oBidder : biddersWithValue) {

					int occurencesInWinnerAllocations = 0;
					ArrayList<ArrayList<Integer>> checkingAllocs = new ArrayList<>();
					for (ArrayList<Integer> curAlloc : maxFeasibleAllocations) {
						ArrayList<Integer> checkingAlloc = new ArrayList<>();
						for (Integer bidder : curAlloc) {
							if (bidder == oBidder) {
								occurencesInWinnerAllocations++;
							}
							if (curAlloc.contains(oBidder)) {
								if (bidder == oBidder) {
									checkingAlloc.add(6);
								} else {
									checkingAlloc.add(bidder);
								}
							}
						}
						if (curAlloc.contains(oBidder)) {
							Collections.sort(checkingAlloc);
							checkingAllocs.add(checkingAlloc);
						}
					}
					if (occurencesInWinnerAllocations > 1) {
						// Calculate all possible bundles that are possible for overbidding
						HashMap<Integer, int[]> oBundles = bundleGenerator.generateParentBundles(bundles.get(oBidder));
						// This HashMap will map bundles from oBundles to its key (maximal feasible allocations), so that
						// only one best response has to be calculated for each variation of maximal feasible allocation.
						HashMap<ArrayList<ArrayList<Integer>>, ArrayList<int[]>> oMaxFeasibleAllocationsMap = new HashMap<>();

						// Calculate maximal feasible allocations for each overbidding bundle, collect bundles with
						// the exact same maximal feasible allocation in the HashMap under the same key.
						boolean curMaxFeasAdded;
						for (Map.Entry<Integer, int[]> entry : oBundles.entrySet()) {
							int[] oBundle = entry.getValue();
							curMaxFeasAdded = false;

							ArrayList<ArrayList<Integer>> curMaxFeas = bundleGenerator.calculateOverbiddingMaxFeasibleAllocs(oBidder, oBundle);
							for (Map.Entry<ArrayList<ArrayList<Integer>>, ArrayList<int[]>> e : oMaxFeasibleAllocationsMap.entrySet()) {
								ArrayList<ArrayList<Integer>> k = e.getKey();
								ArrayList<int[]> v = e.getValue();

								if (k.containsAll(curMaxFeas) && curMaxFeas.containsAll(k)) {
									ArrayList<int[]> oBundleListSameMaxFeas;
									oBundleListSameMaxFeas = v;
									oBundleListSameMaxFeas.add(oBundle);
									oMaxFeasibleAllocationsMap.put(k, oBundleListSameMaxFeas);
									curMaxFeasAdded = true;
									break;
								}
							}
							if (!curMaxFeasAdded) {
								ArrayList<int[]> listToAdd = new ArrayList<>();
								listToAdd.add(oBundle);
								oMaxFeasibleAllocationsMap.put(curMaxFeas, listToAdd);
							}
						}

						// Calculate the overbidding best response for all possible distinct maximal feasible allocations.
						for (Map.Entry<ArrayList<ArrayList<Integer>>, ArrayList<int[]>> entry : oMaxFeasibleAllocationsMap.entrySet()) {
							ArrayList<ArrayList<Integer>> k = entry.getKey();
							if (k.get(0).size() == 1) {
								if (k.get(0).get(0) == 6) {
									continue;
								}
							}
							if (checkingAllocs.containsAll(k) && k.containsAll(checkingAllocs)) {
								continue;
							}

							// Create a list of the bidders that are additionally in conflict by bidding on the current oBundle.
							// Send it to the mechanism to print it. This is easier to do here than afterwards.
							ArrayList<Integer> companions = new ArrayList<>();
							for (ArrayList<Integer> trueAlloc : maxFeasibleAllocations) {
								if (trueAlloc.contains(oBidder)) {
									for (int winner : trueAlloc) {
										if (winner != oBidder) {
											companions.add(winner);
										}
									}
								}
							}
							ArrayList<Integer> conflictingPartners = new ArrayList<>();
							Collections.sort(companions);
							for (int companion : companions) {
								boolean stillCompanion = false;
								for (ArrayList<Integer> oAlloc : k) {
									for (Integer oCompanion : oAlloc) {
										if (companion == oCompanion) {
											stillCompanion = true;
										}
									}
								}
								if (!stillCompanion) {
									conflictingPartners.add(companion);
								}
							}

							ArrayList<int[]> oBundleList = entry.getValue();

							brcOverbidding.setOBundles(oBundleList);
							brcOverbidding.setOMaxFeasAllocs(k);
							brcOverbidding.setGeneratedConflicts(conflictingPartners);

							oMechanism.setOverbiddingAllocations(k);

							BRCalculator.Result<Double, Double[]> overbiddingResult = brcOverbidding.computeBR(oBidder, eqArrayStratsList);

							if (!doneSimpleResults.contains(oBidder)) {
								// Compute best response on equilibrium for each player
								PWLSimpleBRCalculator.Result<Double, Double> brResult = brcSimple.computeBR(oBidder, eqSimpleStratsList);
//								System.out.println(1);
							}

							doneSimpleResults.add(oBidder);
						}
					}
				}
			}
		}
	}
}
