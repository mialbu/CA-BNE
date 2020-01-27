package ch.uzh.ifi.ce.cabne.thesisexamples;

import java.io.*;
import java.util.*;

public class ResultParser {
    /**
     * Generates a List of all opponents of a player
     *
     * @param player The player considered
     * @param nr_players The number of players in the auction
     * @return A List of all players, except the player considered
     */
    private static ArrayList<Integer> get_opponents(int player, int nr_players) {
        ArrayList<Integer> all_players = new ArrayList<>();
        for (int i = 0; i < nr_players; i++) {
            all_players.add(i);
        }
        //int[] opponents_i = new int[all_players.size() - 1];
        ArrayList<Integer> opponents_i = new ArrayList<>();

        for (int i = 0, k = 0; i < all_players.size(); i++) {
            if (i == player - 1) {
                continue;
            }
            //opponents_i[k++] = all_players.get(i);
            opponents_i.add(all_players.get(i));
        }
        return opponents_i;
    }

    /**
     * Generates bundles for all players that contains each item available by a probability.
     *
     * @param nr_players The number of players in the auction
     * @param nr_items The number of items that are available in the auction
     * @param probability_item_chosen The probability for each item to be in the bundle of a player
     */
    private static HashMap<Integer, int[]> generate_bundles_evenly(int nr_players, int nr_items, double probability_item_chosen) {
        HashMap<Integer, int[]> bundle_dict = new HashMap<>();  // where all bundles are stored

        // create bundle for each player
        for (Integer current_player = 0; current_player < nr_players; current_player++) {
            int[] current_bundle = new int[nr_items];
            // chose each item by the probability probability_item_chosen
            for (int i = 0; i < nr_items; i++) {
                double toss = Math.random();  // toss = random between 0.0 and 1.0
                if (toss < probability_item_chosen) {  // item is chosen
                    current_bundle[i] = 1;
                } else {  // item is not chosen
                    current_bundle[i] = 0;
                }
            }
            bundle_dict.put(current_player, current_bundle);  // Add the bundle to the player
        }

        //bundle_dict.forEach((key, value) -> System.out.println(key + ": " + Arrays.toString(value)));
        return bundle_dict;
    }

    /**
     * Generates all distinct combination of all players (2^n-1 possibilities)
     *
     * @param nr_players The number of players in the auction
     * @return All 2^-1 distinct combinations of all players
     */
    private static ArrayList<ArrayList<Integer>> get_bundle_combinations(int nr_players) {
        // Create Array that contains all players (numerated)
        Integer[] arr = new Integer[nr_players];
        for (int i=0; i < nr_players; i++) {
            arr[i] = i;
        }

        // Set N as # of all possible distinct combinations of all players
        int N = (int) Math.pow(2d, nr_players);
        ArrayList<ArrayList<Integer>> all_bundle_combs = new ArrayList<>();

        // Iterate over all binary codes and for each create an ArrayList
        for (int i = 1; i < N; i++) {
            ArrayList<Integer> currentList = new ArrayList<>();
            String code = Integer.toBinaryString(N | i).substring(1);
            for (int j = 0; j < nr_players; j++) {
                if (code.charAt(j) == '1') {
                    currentList.add(arr[j]);
                }
            }
            all_bundle_combs.add(currentList);
        }
        return all_bundle_combs;
    }

    /**
     * Generates all feasible allocations, whereas no item can be allocated more than once.
     *
     * @param all_combinations All distinct combinations of all players
     * @param bundles The bundles of each player in the auction
     * @return All feasible combinations of players in an allocation
     */
    private static ArrayList<ArrayList<Integer>> get_feasible_allocations(ArrayList<ArrayList<Integer>> all_combinations, HashMap<Integer, int[]> bundles) {
        ArrayList<ArrayList<Integer>> feasible_allocs = new ArrayList<>();

        // all bundles from each player are of the same size
        final int nr_items = bundles.get(0).length;

        // Iterate over all combinations
        for (ArrayList<Integer> comb : all_combinations) {  // list of all combinations of players
            // Each item can only be allocated once
            // (sum of each item over all players in this bundle <= 1) => comb is feasible
            // then this comb is added to feasible_allocs
            int feasible_items = 0;

            for (int item = 0; item < nr_items; item++) {  // 0-4
                int item_interest_count = 0;
                for (Integer player : comb) {  // if the item is of interest for player, then add it to the count
                    item_interest_count += bundles.get(player)[item];  // Get players bundle and add value of current item to the count
                }
                if (item_interest_count > 1) {
                    break;
                } else {
                    feasible_items += 1;
                }
                if (feasible_items == nr_items) {
                    feasible_allocs.add(comb);
                }
            }
        }
        return feasible_allocs;
    }

    /**
     * Generates all maximal allocations, where none of those allocations is a subset of another allocation -> therefore maximal
     *
     * @param feasible_allocs All feasible combinations of players in an allocation
     * @return All combinations of players in an allocation, that are maximal
     */
    private static ArrayList<ArrayList<Integer>> get_max_allocs(ArrayList<ArrayList<Integer>> feasible_allocs) {
        ArrayList<ArrayList<Integer>> max_feasible_allocs = new ArrayList<>(feasible_allocs);

        for (ArrayList<Integer> i : feasible_allocs) {
            for (ArrayList<Integer> j: feasible_allocs) {
                if (i!=j) {
                    if (i.containsAll(j)) {  // then j is a subset of some other allocation and can be ignored!
                        max_feasible_allocs.remove(j);
                    }
                }
            }
        }
        return max_feasible_allocs;
    }

    /**
     * Generates all feasible and maximal allocations, whereas no item can be allocated more than once and
     * there is no player that can be added to a generated allocation and it would still be feasible (hence: maximal)
     *
     * @param bundles The bundles of each player in the auction
     * @return All feasible and maximal combinations of players in an allocation
     */
    private static ArrayList<ArrayList<Integer>> get_max_feasible_allocs_v1(HashMap<Integer, int[]> bundles) {
        int nr_players = bundles.size();
        ArrayList<ArrayList<Integer>> all_combinations = get_bundle_combinations(nr_players);
        ArrayList<ArrayList<Integer>> feasible_allocs = new ArrayList<>();

        // all bundles from each player are of the same size
        int nr_items = bundles.get(0).length;

        // Iterate over all combinations
        for (ArrayList<Integer> comb : all_combinations) {  // list of all combinations of players
            // Each item can only be allocated once
            // (sum of each item over all players in this bundle <= 1) => comb is feasible
            // then this comb is added to feasible_allocs
            int feasible_items = 0;

            for (int item = 0; item < nr_items; item++) {  // 0-4
                int item_interest_count = 0;
                for (Integer player : comb) {  // if the item is of interest for player, then add it to the count
                    item_interest_count += bundles.get(player)[item];  // Get players bundle and add value of current item to the count
                }
                if (item_interest_count > 1) {
                    break;
                } else {
                    feasible_items += 1;
                }
                if (feasible_items == nr_items) {
                    feasible_allocs.add(comb);
                }
            }
        }

        ArrayList<ArrayList<Integer>> max_feasible_allocs = new ArrayList<>(feasible_allocs);

        for (ArrayList<Integer> i : feasible_allocs) {
            for (ArrayList<Integer> j: feasible_allocs) {
                if (i!=j) {
                    if (i.containsAll(j)) {  // then j is a subset of some other allocation and can be ignored!
                        max_feasible_allocs.remove(j);
                    }
                }
            }
        }
        return max_feasible_allocs;
    }

    public enum Status {
        maximal,
        not_processed,
        processed
    }

    private static boolean check_feasible(HashMap<Integer, int[]> bundles, ArrayList<Integer> comb) {
        int nr_items = bundles.get(0).length;
        int feasible_items = 0;

        for (int item = 0; item < nr_items; item++) {
            int item_interest_count = 0;
            for (Integer player : comb) {  // if the item is of interest for player, then add it to the count
                item_interest_count += bundles.get(player)[item];  // Get players bundle and add value of current item to the count
            }
            if (item_interest_count > 1) {
                return false;
            } else {
                feasible_items += 1;
            }
        }
        return feasible_items == nr_items;
    }

    private static ArrayList<ArrayList<Integer>> get_max_feasible_allocs(HashMap<Integer, int[]> bundles) {
        int nr_players = bundles.size();
        ArrayList<ArrayList<Integer>> feasible_allocs = new ArrayList<>();
        ArrayList<ArrayList<Integer>> combs = get_bundle_combinations(nr_players);

        // initialize length map - to iterate through
        HashMap<Integer, ArrayList<ArrayList<Integer>>> length_map = new HashMap<>();
        for (int iter = 1; iter <= nr_players; iter++) {
            length_map.put(iter, new ArrayList<>());
        }

        // initialize status_map - setting all status to not_processed
        HashMap<ArrayList<Integer>, Status> status_map = new HashMap<>();
        for (ArrayList<Integer> comb : combs) {
            status_map.put(comb, Status.not_processed);
            length_map.get(comb.size()).add(comb);
        }

        // iterate over all lengths from largest to lowest
        for (int iter = nr_players; iter > 0; iter--) {
            int length_size = length_map.get(iter).size();
            for (ArrayList<Integer> current_comb : length_map.get(iter)) {
                if (status_map.get(current_comb) == Status.not_processed) {
                    if (check_feasible(bundles, current_comb)) {  // test if feasible | if not, set processed/if feasible, set maximal and set all from its lattice to processed
                        feasible_allocs.add(current_comb);
                        status_map.put(current_comb, Status.maximal);
                        process_downwards_lattice(current_comb, status_map);
                    } else {
                        status_map.put(current_comb, Status.processed);
                    }
                } else {
                    length_size -= 1;
                    if (length_size == 0) {
                        return feasible_allocs;
                    }
                }
            }
        }
        return feasible_allocs;
    }

    // precondition - all allocations are ordered from lowest to highest number (e.g. [4,2] not possible -> must be [2,4])
    private static void process_downwards_lattice(ArrayList<Integer> comb, HashMap<ArrayList<Integer>, Status> map) {
        int sum = comb.size();

        ArrayList<String> lower_ = new ArrayList<>();
        if (sum > 1) {
            lower_ = gen_bins(sum);
        }

        // replace ones with each substring to create children in hypercube
        for (String i : lower_) {
            int incr = 0;
            ArrayList<Integer> current_bundle = new ArrayList<>();
            // iterate over binary code - within: replace 1s with substrings
            for (int pl_in_alloc = 0; pl_in_alloc < sum; pl_in_alloc++) {
                if (Integer.parseInt(String.valueOf(i.charAt(incr))) == 1) {  // iterate 0 to sum-1 - get i.charAt(iterate) -> if 1 then add the iterate-th nr in the comb_code to the current_bundle
                    current_bundle.add(comb.get(incr));
                }
                incr+=1;
            }
            map.put(current_bundle, Status.processed);
        }
    }

    private static ArrayList<String> gen_bins(int size) {
        // Set N as # of all possible distinct combinations of all players
        int N = (int) Math.pow(2d, size);

        // Iterate over all binary codes and for each create an ArrayList
        ArrayList<String> res = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            String code = Integer.toBinaryString(N | i).substring(1);
            res.add(code);
        }
        res.remove(N-1);
        res.remove(0);
        return res;
    }

    /** run this to test the hypercube */
    private static void test_max_feasible_allocs_generation() {
        int nr_players = 5;
        int nr_items = 8;
        double prob = 0.2;

        HashMap<Integer, int[]> bundles = generate_bundles_evenly(nr_players, nr_items, prob);
        bundles.forEach((key, value) -> {
            System.out.println(key + ": " + Arrays.toString(value));
        });

        ArrayList<ArrayList<Integer>> test = get_max_feasible_allocs(bundles);
        System.out.println(test);
    };

    private static void generateFinalStrategies(String folder, int items, int begin, int end) throws IOException {
        if ((items != 8) && (items!=15)) {
            return;
        }
        folder = "../../../../../../../../../../CA-BNE/misc/scripts/" + folder;  // path to the strategy files
        String filename;
        int fileNr;

        int nrOverbiddings = 0;
        int nrCombinatingOverbiddingBidders = 0;
        int currentBidder;
        int lineNumber;
        ArrayList<String> updatedStrats;
        double[][] strategy;
        double[][] oldStrategy;

        for (int index=begin; index<end; index++) {
            fileNr = index;
            if (fileNr < 10) {
                filename = "00" + String.valueOf(fileNr);
//                System.out.println(filename);
            } else if (fileNr < 100) {
                filename = "0" + String.valueOf(fileNr);
            } else {
                filename = String.valueOf(fileNr);
            }
            File oStrats = new File(folder + "/" + filename + ".overbiddingBRStrats");
            FileReader fr = new FileReader(oStrats);
            BufferedReader br = new BufferedReader(fr);
            br.readLine();

            String line;
            currentBidder = 0;
            lineNumber = 0;
            HashMap<Integer, double[][]> strategies = new HashMap<>();
            updatedStrats = new ArrayList<>();
            strategy = new double[1001][4];
            oldStrategy = new double[1001][4];

            ArrayList<Integer> processedBidders = new ArrayList<>();
            ArrayList<Integer> combinationOverbidding = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                lineNumber += 1;
                if ((lineNumber % 5 == 1)) {
                    currentBidder = Integer.parseInt(line);
                }
                if (lineNumber % 5 == 0) {
                    String[] currentStrategy = line.split("  ");
                    strategy = new double[1001][4];
                    for (int i = 0; i < currentStrategy.length; i++) {
                        String[] quadtriple = currentStrategy[i].split(" ");
                        strategy[i][0] = Double.parseDouble(quadtriple[0]);
                        strategy[i][1] = Double.parseDouble(quadtriple[1]);
                        strategy[i][2] = Double.parseDouble(quadtriple[2]);
                        strategy[i][3] = Double.parseDouble(quadtriple[3]);
                    }
                    int nrZeroOBids = 0;
                    for (int j = 0; j < currentStrategy.length; j++) {
                        if (strategy[j][3] == 0.0) {
                            nrZeroOBids += 1;
                        }
                    }
                    if (nrZeroOBids == 1001) {
                        continue;
                    }
                    int overbids = 1001 - nrZeroOBids;
                    // reached by overbidding strategies, null overbidding strategies cut off here
                    if (processedBidders.contains(currentBidder)) {
                        // compare here
                        double[][] newStrategy = new double[1001][4];
                        int nrOld = 0;
                        int nrNew = 0;
                        int nrEqual = 0;
                        for (int it = 0; it < 1001; it++) {
                            if (strategy[it][1] > oldStrategy[it][1]) {
                                newStrategy[it] = strategy[it];
                                nrNew += 1;
                            } else if (strategy[it][1] < oldStrategy[it][1]) {
                                newStrategy[it] = oldStrategy[it];
                                nrOld += 1;
                            } else {
                                newStrategy[it] = strategy[it];
                                nrEqual += 1;
                            }
                        }
                        if ((nrOld > 0) && (nrNew > 0)) {
                            if (!combinationOverbidding.contains(currentBidder)) {
                                combinationOverbidding.add(currentBidder);
                                nrCombinatingOverbiddingBidders += 1;
                            }
                        }
                        strategies.put(currentBidder, newStrategy);
                    } else {
                        processedBidders.add(currentBidder);
                        strategies.put(currentBidder, strategy);
                        oldStrategy = strategies.get(currentBidder);
                        nrOverbiddings += 1;
                    }
                }
            }
            br.close();
            fr.close();

            File oBrFile = new File(folder + "/" + filename + ".finalOverbidStrategies");
            FileWriter oFileWriter = new FileWriter(oBrFile, false);
            BufferedWriter oBufferedWriter = new BufferedWriter(oFileWriter);

            for (Map.Entry<Integer, double[][]> entry : strategies.entrySet()) {
                Integer b = entry.getKey();
                double[][] s = entry.getValue();
                oBufferedWriter.write(b + "  ");
                for (int inc = 0; inc < 1001; inc++) {
                    oBufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", s[inc][0]) + " ");
                    oBufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", s[inc][1]) + " ");
                    oBufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", s[inc][2]) + " ");
                    if (inc == 1000) {
                        oBufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", s[inc][3]));
                    } else {
                        oBufferedWriter.write(String.format(Locale.ENGLISH, "%9.8f", s[inc][3]) + "  ");
                    }
                }
                oBufferedWriter.newLine();
            }

            oBufferedWriter.close();
            oFileWriter.close();
        }
        System.out.println();
        System.out.println("nr of players potentially overbidding: " + nrOverbiddings);
        System.out.println("nr of bidders overbidding with a combination of different equivalent classes: " + nrCombinatingOverbiddingBidders);
    }

    private static void generateFinalSimpleStrategies(String folder, int items, int begin, int end) throws IOException {
        if ((items != 8) && (items!=15)) {
            return;
        }
        folder = "../../../../../../../../../../CA-BNE/misc/scripts/" + folder;  // path to the strategy files
        String filename;
        String line;
        ArrayList<Integer> biddersToConsider = new ArrayList<>();
        HashMap<Integer, String> simpleStrategies = new HashMap<>();
        int fileNr;
        for (int index=begin; index<end; index++) {
            fileNr = index;
            if (fileNr < 10) {
                filename = "00" + String.valueOf(fileNr);
            } else if (fileNr < 100) {
                filename = "0" + String.valueOf(fileNr);
            } else {
                filename = String.valueOf(fileNr);
            }

            biddersToConsider = new ArrayList<>();

            File oStrats = new File(folder + "/" + filename + ".finalOverbidStrategies");
            FileReader fr = new FileReader(oStrats);
            BufferedReader br = new BufferedReader(fr);

            while ((line = br.readLine()) != null) {
                int cBidder = Integer.parseInt(line.split("  ")[0]);
                biddersToConsider.add(cBidder);
            }

            br.close();
            fr.close();

            File file = new File(folder + "/" + filename + ".finalSimpleStrategies");
            FileWriter writer = new FileWriter(file, false);
            BufferedWriter bWriter = new BufferedWriter(writer);
            bWriter.close();
            writer.close();


            if (biddersToConsider.isEmpty()) {
                continue;
            }

            File sFile = new File(folder + "/" + filename + ".simpleBRstrats");
            FileReader fReader = new FileReader(sFile);
            BufferedReader bReader = new BufferedReader(fReader);
            bReader.readLine();
            while ((line = bReader.readLine()) !=null) {
                int bidder = Integer.parseInt(line.split("  ")[0]);
                simpleStrategies.put(bidder, line);
            }
            br.close();
            fr.close();

            File simpleFile = new File(folder + "/" + filename + ".finalSimpleStrategies");
            FileWriter simpleWriter = new FileWriter(simpleFile, false);
            BufferedWriter simpleBufferedWriter = new BufferedWriter(simpleWriter);
            for (int i : biddersToConsider) {
                simpleBufferedWriter.write(simpleStrategies.get(i));
                simpleBufferedWriter.newLine();
            }

            simpleBufferedWriter.close();
            simpleWriter.close();

        }

    }

    private static void countNoneConvergingInstances(String path8items, String path15items) throws IOException {
        int[] notConvergedFiles8 = {0,60,120,180,240,302,308,314,320,380,440,500,560,620,680,740,800,860,910,915,920,925,930,985,992};
        int[] notConvergedFiles15 = {0,34,68,102,136,170,204,238,272,306,340,374,408,442,476,510,544,578,612,646,680,714,748,782,816,850,884,918,952,986};

        String folder = path8items;
        String filename;
        String line;

        int count8 = 0;
        for (int n : notConvergedFiles8){
            filename = String.valueOf(n);
            File sFile = new File(folder + "/" + filename + "notConverged.txt");
            FileReader fReader = new FileReader(sFile);
            BufferedReader bReader = new BufferedReader(fReader);
            while ((line = bReader.readLine()) !=null) {
                if (line.split(" ")[0].equals("allocations")) {
                    count8+=1;
                }
            }
            bReader.close();
            fReader.close();
        }

        folder = path15items;
        int count15=0;
        for (int n : notConvergedFiles15){
            filename = String.valueOf(n);
            File sFile = new File(folder + "/" + filename + "notConverged.txt");
            FileReader fReader = new FileReader(sFile);
            BufferedReader bReader = new BufferedReader(fReader);
            while ((line = bReader.readLine()) !=null) {
                if (line.split(" ")[0].equals("allocations")) {
                    count15+=1;
                }
            }
            bReader.close();
            fReader.close();
        }
        System.out.println("notConverged 8 items : " + count8);
        System.out.println("notConverged 15 items : " + count15);
    }

    private static ArrayList<Double> printAVGMaxRelativeUtilityGain(int items) throws IOException {
        ArrayList<Double> relativeUtilityGains = new ArrayList<>();

        String folder = "../../../../../../../../../../CA-BNE/misc/scripts/15itemsfinal";
        String filename;
        String oLine;
        String sLine;

        String[] oSplit;
        String[] sSplit;

        String[] oQuadriple;
        String[] sTriple;

        double oUtility;
        double sUtility;

        double relativeUtilityGain;

        double maximalRelativeUtilityGain = 0.0;

        double oVal;
        double sVal;

        int countLowerUtility = 0;
        int countHigherUtility = 0;
        int countEqualUtility = 0;

        for (int i=0; i<1000; i++) {
            if (i<10) {
                filename = "00" + i;
            }else if (i<100) {
                filename = "0" + i;
            }else{
                filename = String.valueOf(i);
            }

            File oFile = new File(folder + "/" + filename + ".finalOverbidStrategies");
            FileReader oReader = new FileReader(oFile);
            BufferedReader boReader = new BufferedReader(oReader);
            File sFile = new File(folder + "/" + filename + ".finalSimpleStrategies");
            FileReader sReader = new FileReader(sFile);
            BufferedReader bsReader = new BufferedReader(sReader);
            while ((oLine = boReader.readLine()) != null) {
                sLine = bsReader.readLine();
                maximalRelativeUtilityGain = 0.0;
                if (!oLine.split("  ")[0].equals(sLine.split("  ")[0])) {
                    System.out.println("Invalid comparison of strategies - different players compared");
                } else {
                    oSplit = oLine.split("  ");
                    sSplit = sLine.split("  ");
                    for (int j=1; j<oSplit.length; j++) {
                        oQuadriple = oSplit[j].split(" ");
                        sTriple = sSplit[j].split(" ");
                        oVal = Double.parseDouble(oQuadriple[0]);
                        sVal = Double.parseDouble(sTriple[0]);
                        if (oVal!=sVal) {
                            System.out.println("Invalid valuation");
                        }
                        oUtility = Double.parseDouble(oQuadriple[1]);
                        sUtility = Double.parseDouble(sTriple[1]);
                        if (oUtility<sUtility) {
                            countLowerUtility++;
                        } else if (oUtility>sUtility) {
                            countHigherUtility++;
                        } else {
                            countEqualUtility++;
                        }

                        relativeUtilityGain = oUtility / sUtility;
                        if (relativeUtilityGain > maximalRelativeUtilityGain) {
                            maximalRelativeUtilityGain = relativeUtilityGain;
                        }
                    }
                }
//                System.out.println(maximalRelativeUtilityGain);
                relativeUtilityGains.add(maximalRelativeUtilityGain);
            }

            boReader.close();
            oReader.close();
            bsReader.close();
            sReader.close();
        }
        System.out.println(countLowerUtility);
        System.out.println(countHigherUtility);
        System.out.println(countEqualUtility);
        return relativeUtilityGains;
    }


    public static void main(String[] args) throws IOException {
//        String folder = "testFINAL/";
//        folder = "testff/";
//        folder = "8itemsUsedInThesis/";
//        int items = 15;
//        int begin = 0;
//        int end = 400;
//
//        // Use these methods to create the final Strategies - this is necessary, since there may be multiple deviation classes
//        // We need to combine those into one single strategy by choosing in every of the 1001 values the bid or bids respectively
//        // that has the highest expected utility
//        generateFinalStrategies(folder, items, begin, end);
//        generateFinalSimpleStrategies(folder, items, begin, end);


        // This is used to read all the .txt files where instances of auctions are written to, if they did not converge
        // It counts the amount of instances that were created and did not converge
        countNoneConvergingInstances("../../../../../../../../../../CA-BNE/misc/scripts/8itemsfinal", // path to the instances with 8 items
                "../../../../../../../../../../CA-BNE/misc/scripts/15itemsfinal");  // path to the instances with 15 items


//        ArrayList<Double> maxRelUtilityGains = printAVGMaxRelativeUtilityGain(8);
//        System.out.println("max: " + Collections.max(maxRelUtilityGains));
//        System.out.println();
//        double sum = 0;
//        int cnt = 0;
//        for (Double d : maxRelUtilityGains) {
//            if (d < 1.0) {
////                System.out.println(d);
////                System.out.println();
//                cnt++;
//            }
//            sum += d;
//        }
//        System.out.println();
//        double avg = sum / maxRelUtilityGains.size();
//        System.out.println();
//        System.out.println(avg);
//        generateFinalStrategies(8);

    }
}
