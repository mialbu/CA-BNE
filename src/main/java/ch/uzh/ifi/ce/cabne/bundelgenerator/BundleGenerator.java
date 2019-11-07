package ch.uzh.ifi.ce.cabne.bundelgenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class BundleGenerator {

    private final HashMap<Integer,int[]> bundles;
    private final ArrayList<ArrayList<Integer>> max_feasible_allocs;

    public BundleGenerator(int nr_players, int nr_items, double probability_items) {
        HashMap<Integer, int[]> bundles = generate_bundles_evenly(nr_players,nr_items,probability_items);
        ArrayList<ArrayList<Integer>> max_feasible_allocs = get_max_feasible_allocs(bundles);
        this.bundles = bundles;
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
     * @param player The player considered
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
     * @param nr_players The number of players in the auction
     * @param nr_items The number of items that are available in the auction
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
        for (int i=0; i < nr_players; i++) {
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
    public ArrayList<ArrayList<Integer>> get_max_feasible_allocs(HashMap<Integer, int[]> bundles) {
        int nr_players = bundles.size();
        ArrayList<ArrayList<Integer>> all_combinations = get_bundle_combinations(nr_players);
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
}
