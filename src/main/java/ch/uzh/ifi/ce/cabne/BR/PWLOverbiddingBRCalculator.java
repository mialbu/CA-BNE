package ch.uzh.ifi.ce.cabne.BR;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.helpers.UtilityHelpers;
import ch.uzh.ifi.ce.cabne.pointwiseBR.Optimizer;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLOverbiddingStrategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class PWLOverbiddingBRCalculator implements BRCalculator<Double, Double[]> {
	private String outputFile;
	BNESolverContext<Double, Double[]> context;
	private ArrayList<int[]> oBundleList;
	private ArrayList<ArrayList<Integer>> oMaxFeasAllocs;
	private ArrayList<Integer> generatedConflicts;


	public PWLOverbiddingBRCalculator(BNESolverContext<Double, Double[]> context, String outputFile) {
		this.context = context;
		this.outputFile = outputFile;
	}

	public void setOBundles(ArrayList<int[]> oBundleList){
		this.oBundleList = new ArrayList<>();
		this.oBundleList = oBundleList;
	}
	
	public void setGeneratedConflicts(ArrayList<Integer> generatedConflicts) {
		this.generatedConflicts = new ArrayList<>();
		this.generatedConflicts = generatedConflicts;
	}

	public void setOMaxFeasAllocs(ArrayList<ArrayList<Integer>> oMaxFeasAllocs) {
		this.oMaxFeasAllocs = new ArrayList<>();
		this.oMaxFeasAllocs = oMaxFeasAllocs;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public Result<Double, Double[]> computeBR(int i, List<Strategy<Double, Double[]>> s) throws IOException {
		int nPoints = Integer.parseInt(context.config.get("gridsize"));
		
		TreeMap<Double, Double[]> pointwiseBRs = new TreeMap<>();
		TreeMap<Double, Double> pointwiseUtility = new TreeMap<>();
		double epsilonAbs = 0.0;
		double epsilonRel = 0.0;

		double maxValue = s.get(i).getMaxValue()[0];
		
		for (int j = 0; j<=nPoints; j++) {
			double v = maxValue * ((double) j) / (nPoints);
			Double[] oldbid = s.get(i).getBid(v);  // oldbid = [bundleBid, oBundleBid]
			Optimizer.Result<Double[]> result = context.optimizer.findBR(i, v, oldbid, s);
//			epsilonAbs = Math.max(epsilonAbs, UtilityHelpers.absoluteLoss(result.oldutility, result.utility));
//			epsilonRel = Math.max(epsilonRel, UtilityHelpers.relativeLoss(result.oldutility, result.utility));

			Double[] newbids = result.bid;
			pointwiseBRs.put(v, newbids);
			pointwiseUtility.put(v, result.utility);
		}

		File brStratFile = new File(outputFile);
		FileWriter fileWriter = new FileWriter(brStratFile, true);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		try {
			bufferedWriter.write(String.valueOf(i));
			bufferedWriter.newLine();
			for (ArrayList<Integer> alloc : oMaxFeasAllocs) {
				for (int plIndex=0; plIndex<alloc.size(); plIndex++) {
					if (plIndex==alloc.size()-1) {
						bufferedWriter.write(alloc.get(plIndex) + " ");
					} else {
						bufferedWriter.write(alloc.get(plIndex) + ",");
					}
				}
			}
			bufferedWriter.newLine();
			bufferedWriter.write("generatedConflicts: ");
			for (int iter=0; iter<generatedConflicts.size(); iter++) {
				bufferedWriter.write(String.valueOf(generatedConflicts.get(iter)));
				if (iter<generatedConflicts.size()-1) {
					bufferedWriter.write(" ");
				}
			}
			bufferedWriter.newLine();
			for (int[] bundle : oBundleList) {
				for (int item=0; item<bundle.length; item++) {
					if (item==bundle.length-1) {
						bufferedWriter.write(bundle[item] + " ");
					} else {
						bufferedWriter.write(bundle[item] + ",");
					}
				}
			}
			bufferedWriter.newLine();
			pointwiseBRs.forEach((valuation, bestR) -> {
				try {
					bufferedWriter.write(
							String.format(Locale.ENGLISH, "%4.3f", valuation) + " "
							+ String.format(Locale.ENGLISH, "%9.8f", pointwiseUtility.get(valuation)) + " "
							+ String.format(Locale.ENGLISH, "%9.8f", bestR[0]) + " "
							+ String.format(Locale.ENGLISH, "%9.8f", bestR[1]) + "  ");
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			bufferedWriter.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		bufferedWriter.close();
		fileWriter.close();
		
		return new Result<>(new UnivariatePWLOverbiddingStrategy(pointwiseBRs), epsilonAbs, epsilonRel);
	}
}
