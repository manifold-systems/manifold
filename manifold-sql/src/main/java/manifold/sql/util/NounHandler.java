package manifold.sql.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by klu on 6/17/2015.
 * This class is used for singularizing and converting names to things that look like java class names.
 */
public class NounHandler {
  private String input;
  private HashMap<String, String> specialPlurals = new HashMap<>();

  public NounHandler(){
    input = "";
    exceptionalPlurals();
  }

  public NounHandler(String in) {
    input = in;
    exceptionalPlurals();
  }

  /**
   * Changes the string that this instance is currently holding.
   * @param in The new string
   */
  public void changeWord(String in) {
    input = in;
  }

  public static void main(String[] args) throws IOException {
    //Testing
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;
    input = br.readLine();
    NounHandler n = new NounHandler(input);
    System.out.println(n.getSingular());


  }

  /**
   * Gets the final noun in a compoundName (ie compoundName goes to Name)
   *
   * @param words List of 'Words' in the initial string
   * @return the final noun in the name
   */
  private String getFinalWord(String[] words) {
    return words[words.length - 1];
  }

  private boolean isConsonant(char c) {
    return !Character.toString(c).matches("[aeiou]");
  }

  /**
   * Gets all the distinct words in a compoundName
   *
   * @param word Initial word
   * @return an array of all the nouns/words
   */
  private String[] getWords(String word) {
    String[] finalSplit = new String[1];
    String[] initialSplit = word.split("[^A-Za-z0-9']");
    ArrayList<String> currentList = new ArrayList<>(Arrays.asList(initialSplit));
    finalSplit = currentList.toArray(finalSplit);
    return finalSplit;
  }

  private static String[] getWordsStatic(String word) {
    String[] initialSplit = word.split("[^A-Za-z0-9']");
    ArrayList<String> currentList = new ArrayList<>(Arrays.asList(initialSplit));

    /*splits on capital letters and numbers*/
    ArrayList<String[]> nextList = new ArrayList<String[]>();
    for(int i = 0; i<currentList.size();i++){
      String[] next = currentList.get(i).split("(?=[A-Z0-9])|(?<=[A-Z0-9])");
      nextList.add(next);
    }
    ArrayList<String> finalList = new ArrayList<>();
    for(String[] ss: nextList){
      for(String s: ss){
        finalList.add(s);
      }
    }
    return finalList.toArray(new String[finalList.size()]);
  }

  private String singularize(String word) {
    char[] chars = word.toCharArray();
    char last = chars[chars.length - 1];
    String current = word;
    try {
      Integer.parseInt(current);
      return current;
    }
    catch (Exception e){

    }

    if(current.length() == 1){
      return current.equals("s")?"":current;
    }
    for (Map.Entry<String, String> entry : specialPlurals.entrySet()) {
      String regexptest = "[A-Za-z]*" + entry.getKey();
      if (current.matches(regexptest)) {
        current = current.substring(0, current.length() - entry.getKey().length());
        current += entry.getValue();
      }
    }

    if (current.equals(word)) {
      if (last == 's' || last == 'S') {
        current = current.substring(0, chars.length - 1);
      }
      try {
        last = chars[chars.length - 2];
        if (last == 'e') {
          char prev = chars[chars.length - 3];
          char p2 = chars[chars.length-4];
          if (prev == 'x' || prev == 's' || (prev == 'h' && (p2 == 's' || p2 == 'c'))) {
            current = current.substring(0, current.length() - 1);
          }
          if (prev == 'i') {
            current = current.substring(0, current.length() - 2);
            current += 'y';
          }
          if (prev == 'v') {
            current = current.substring(0, current.length() - 1);
            current += 'f';
            if (chars[chars.length - 4] == 'i') {
              current += 'e';
            }
          }
        }
        if (last == '\'') {
          current = current.substring(0, current.length() - 1);
        }
      } catch (ArrayIndexOutOfBoundsException e){

      }
    }
    return current;
  }

  private void exceptionalPlurals() {
    /*Handles really weird plural problems, populates specialPlurals*/
    specialPlurals.put("feet", "foot");
    specialPlurals.put("geese", "goose");
    specialPlurals.put("lice", "louse");
    specialPlurals.put("mice", "mouse");
    specialPlurals.put("teeth", "tooth");
    specialPlurals.put("men", "man");
    specialPlurals.put("children", "child");
    specialPlurals.put("brethren", "brother");
    specialPlurals.put("oxen", "ox");
    specialPlurals.put("safes", "safe");
    specialPlurals.put("indices", "index");
    specialPlurals.put("matrices", "matrix");
    specialPlurals.put("vertices", "vertex");
    specialPlurals.put("axes", "axis");
    specialPlurals.put("geneses", "genesis");
    specialPlurals.put("nemeses", "nemesis");
    specialPlurals.put("crises", "crisis");
    specialPlurals.put("series", "series");
    specialPlurals.put("species", "species");
    specialPlurals.put("memoranda", "memorandum");
    specialPlurals.put("millenia", "millenium");
    specialPlurals.put("spectra", "spectrum");
    specialPlurals.put("alumni", "alumnus");
    specialPlurals.put("foci", "focus");
    specialPlurals.put("genera", "genus");
    specialPlurals.put("radii", "radius");
    specialPlurals.put("succubi", "succubus");
    specialPlurals.put("syllabi", "syllabus");
    specialPlurals.put("octopi", "octopus");
    specialPlurals.put("automata", "automaton");
    specialPlurals.put("criteria", "criterion");
    specialPlurals.put("phenomena", "phenomenon");
    specialPlurals.put("polyhedra", "polyhedron");
    specialPlurals.put("seraphim", "seraph");
    specialPlurals.put("aves", "ave");
    specialPlurals.put("beehives", "beehive");
    specialPlurals.put("captives", "captive");
    specialPlurals.put("oves", "ove");
    specialPlurals.put("curves", "curve");
    specialPlurals.put("kai", "TheCreator!");
    specialPlurals.put("eaves", "eave");
    specialPlurals.put("eves", "eve");
    specialPlurals.put("evolves", "evolve");
    specialPlurals.put("ives", "ive");
    specialPlurals.put("solves", "solve");
    specialPlurals.put("swerves", "swerve");
    specialPlurals.put("twelves", "twelve");
    specialPlurals.put("valves", "valve");
    specialPlurals.put("people", "person");
    specialPlurals.put("news", "news");
    specialPlurals.put("uises", "uise");
    specialPlurals.put("ases", "ase");
    specialPlurals.put("eses", "ese");
    specialPlurals.put("auses", "ause");
    specialPlurals.put("ouses", "ouse");
    specialPlurals.put("corpses", "corpse");
    specialPlurals.put("eases", "ease");
    specialPlurals.put("urses", "urse");
    specialPlurals.put("orses", "orse");
    specialPlurals.put("enses", "ense");
    specialPlurals.put("uses", "uses");
    specialPlurals.put("oses", "ose");
    specialPlurals.put("erses", "erse");

  }


  /**
   * Given a database table name, returns an equivalent Java class name.
   * <p>
   *   Specifically, given a string, we call a lexical unit any single numerical character or any set of letters separated
   *   by a capital or a non letter character. Then this method takes all lexical units, puts them together in order not
   *   separated, capitalizes all but the first, and creates a singular form for the last.
   * </p>
   * @return A classname equivalent.
   */
  public String getSingular() {
    String[] strings = getWords(input);
    String finalword = getFinalWord(strings);
    String finalout = singularize(finalword);
    if (strings.length == 1) {
      return finalout;
    }
    String output = "";
    output += Character.toUpperCase(strings[0].charAt(0)) + (strings[0].length() > 1 ? strings[0].substring(1) : "");
    for (int i = 1; i < strings.length - 1; i++) {
      if(!strings[i].equals("")) {
        String nextpart = Character.toUpperCase(strings[i].charAt(0)) + (strings[i].length() > 1 ? strings[i].substring(1) : "");
        output += nextpart;
      }
    }
    output += Character.toUpperCase(finalout.charAt(0))+finalout.substring(1);
    return output;
  }

  /**
   * Changes casing to camel casing.
   * <p>
   *  Specifically, given a string, we call a lexical unit any single numerical character or any set of letters separated
   *  by a capital or a non letter character. Then this method takes all lexical units, puts them together in order not
   *  separated, and capitalizes all but the first.
   * </p>
   * @param s a string to modify
   * @return a camel cased version of the string
   */
  public static String getCamelCased(String s) {
    if(s.equals("")){
      return s;
    }
    String[] strings = getWordsStatic(s);
    String finalword = strings[strings.length-1];
    if (strings.length == 1) {
      return finalword;
    }
    String output = "";
    output += strings[0].equals("")?"":Character.toLowerCase(strings[0].charAt(0)) + (strings[0].length() > 1 ? strings[0].substring(1) : "");
    for (int i = 1; i < strings.length - 1; i++) {
      if(!strings[i].equals("")) {
        String nextpart = Character.toUpperCase(strings[i].charAt(0)) + (strings[i].length() > 1 ? strings[i].substring(1) : "");
        output += nextpart;
      }
    }
    output += finalword.equals("")?"":(Character.toUpperCase(finalword.charAt(0))+finalword.substring(1));
    return output;
  }

  /**
   * Adds an exception to the singularizer. Only the base form is needed (ie between fighters
   * and firefighters, only adding fighters, fighter is necessary).
   * Please only use this method if the singularizer is incorrectly singularizing; the list of
   * exception words is already populated.
   * @param plural plural form
   * @param singular singular form
   */
  public void addException(String plural, String singular){
    specialPlurals.put(plural, singular);
  }
}
