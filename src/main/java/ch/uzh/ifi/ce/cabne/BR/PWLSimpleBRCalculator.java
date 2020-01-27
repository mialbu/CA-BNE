package ch.uzh.ifi.ce.cabne.BR;

import ch.uzh.ifi.ce.cabne.algorithm.BNESolverContext;
import ch.uzh.ifi.ce.cabne.helpers.UtilityHelpers;
import ch.uzh.ifi.ce.cabne.pointwiseBR.Optimizer;
import ch.uzh.ifi.ce.cabne.strategy.Strategy;
import ch.uzh.ifi.ce.cabne.strategy.UnivariatePWLStrategy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class PWLSimpleBRCalculator implements BRCalculator<Double, Double> {
	BNESolverContext<Double, Double> context;
	String outputFile;

	public PWLSimpleBRCalculator(BNESolverContext<Double, Double> context, String outputFile) {
		this.context = context;
		this.outputFile = outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public Result<Double, Double> computeBR(int i, List<Strategy<Double, Double>> s) throws IOException {
		int nPoints = 1000;//Integer.parseInt(context.config.get("gridsize"));

		TreeMap<Double, Double> pointwiseBRs = new TreeMap<>();
		TreeMap<Double, Double> pointwiseUtility = new TreeMap<>();
		double epsilonAbs = 0.0;
		double epsilonRel = 0.0;

		double maxValue = s.get(i).getMaxValue();

		for (int j = 0; j<=nPoints; j++) {
			double v = maxValue * ((double) j) / (nPoints);
			Double oldbid = s.get(i).getBid(v);
			Optimizer.Result<Double> result = context.optimizer.findBR(i, v, oldbid, s);

			Double newbid = context.updateRule.update(v, oldbid, result.bid, result.oldutility, result.utility);
			pointwiseBRs.put(v,  newbid);
			pointwiseUtility.put(v, result.utility);
		}

		File brStratFile = new File(outputFile);
		FileWriter fileWriter = new FileWriter(brStratFile, true);
		BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

		bufferedWriter.write(i + "  ");
		for (Map.Entry<Double, Double> entry : pointwiseBRs.entrySet()) {
			Double key = entry.getKey();
			Double value = entry.getValue();
			bufferedWriter.write(String.format(Locale.ENGLISH, "%5.4f", key));
			bufferedWriter.write(" ");
			bufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", pointwiseUtility.get(key)));
			bufferedWriter.write(" ");
			bufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", value));
			bufferedWriter.write("  ");
		}

		bufferedWriter.newLine();

		bufferedWriter.close();
		fileWriter.close();


		return new Result<Double, Double>(new UnivariatePWLStrategy(pointwiseBRs), epsilonAbs, epsilonRel);
	}

}
