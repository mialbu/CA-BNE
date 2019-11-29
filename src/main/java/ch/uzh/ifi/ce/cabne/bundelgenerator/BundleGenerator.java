package ch.uzh.ifi.ce.cabne.bundelgenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class BundleGenerator {

    private final HashMap<Integer, int[]> bundles;
    private final ArrayList<ArrayList<Integer>> max_feasible_allocs;

    public BundleGenerator(int nr_players, int nr_items, double probability_items) {
        HashMap<Integer, int[]> bundles = generate_bundles_evenly(nr_players, nr_items, probability_items);
        this.bundles = bundles;
        ArrayList<ArrayList<Integer>> max_feasible_allocs = calculate_max_feasible_allocs();
        this.max_feasible_allocs = max_feasible_allocs;
    }

    public HashMap<Integer, int[]> get_bundles() {
        return this.bundles;
    }

    public ArrayList<ArrayList<Integer>> get_max_feasible_allocs() {
        return this.max_feasible_allocs;
    }


    /**
     * Generates a List of all opponents of a player
     *
     * @param player     The player considered
     * @param nr_players The number of players in the auction
     * @return A List of all players, except the player considered
     */
    public static ArrayList<Integer> get_opponents(int player, int nr_players) {
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
     * @param nr_players              The number of players in the auction
     * @param nr_items                The number of items that are available in the auction
     * @param probability_item_chosen The probability for each item to be in the bundle of a player
     */
    public HashMap<Integer, int[]> generate_bundles_evenly(int nr_players, int nr_items, double probability_item_chosen) {
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
        for (int i = 0; i < nr_players; i++) {
            arr[i] = i;
        }

        // Set N as # of all possible distinct combinations of all players
        int N = (int) Math.pow(2d, nr_players);
        ArrayList<ArrayList<Integer>> all_bundle_combs = new ArrayList<ArrayList<Integer>>();

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
     * Generates all feasible and maximal allocations, whereas no item can be allocated more than once and
     * there is no player that can be added to a generated allocation and it would still be feasible (hence: maximal)
     *
     * @param bundles The bundles of each player in the auction
     * @return All feasible and maximal combinations of players in an allocation
     */
//    public ArrayList<ArrayList<Integer>> get_max_feasible_allocs(HashMap<Integer, int[]> bundles) {
//        int nr_players = bundles.size();
//        ArrayList<ArrayList<Integer>> all_combinations = get_bundle_combinations(nr_players);
//        ArrayList<ArrayList<Integer>> feasible_allocs = new ArrayList<>();
//
//        // all bundles from each player are of the same size
//        final int nr_items = bundles.get(0).length;
//
//        // Iterate over all combinations
//        for (ArrayList<Integer> comb : all_combinations) {  // list of all combinations of players
//            // Each item can only be allocated once
//            // (sum of each item over all players in this bundle <= 1) => comb is feasible
//            // then this comb is added to feasible_allocs
//            int feasible_items = 0;
//
//            for (int item = 0; item < nr_items; item++) {  // 0-4
//                int item_interest_count = 0;
//                for (Integer player : comb) {  // if the item is of interest for player, then add it to the count
//                    item_interest_count += bundles.get(player)[item];  // Get players bundle and add value of current item to the count
//                }
//                if (item_interest_count > 1) {
//                    break;
//                } else {
//                    feasible_items += 1;
//                }
//                if (feasible_items == nr_items) {
//                    feasible_allocs.add(comb);
//                }
//            }
//        }
//
//        ArrayList<ArrayList<Integer>> max_feasible_allocs = new ArrayList<>(feasible_allocs);
//
//        for (ArrayList<Integer> i : feasible_allocs) {
//            for (ArrayList<Integer> j : feasible_allocs) {
//                if (i != j) {
//                    if (i.containsAll(j)) {  // then j is a subset of some other allocation and can be ignored!
//                        max_feasible_allocs.remove(j);
//                    }
//                }
//            }
//        }
//        return max_feasible_allocs;
//    }

    public enum Status {
        maximal,
        not_processed,
        processed
    }

    private boolean check_feasible(ArrayList<Integer> comb) {
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

    private ArrayList<ArrayList<Integer>> calculate_max_feasible_allocs() {
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
                    if (check_feasible(current_comb)) {  // test if feasible | if not, set processed/if feasible, set maximal and set all from its lattice to processed
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
    private static void process_downwards_lattice(ArrayList<Integer> comb, HashMap<ArrayList<Integer>, Status> map) {  // TODO: take input [2,4,5] - get [2],[4],[5],[2,4],[2,5],[4,5]
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
                incr += 1;
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
        res.remove(N - 1);
        res.remove(0);
        return res;
    }
}
