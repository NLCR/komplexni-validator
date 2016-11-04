package rzehan.shared.engine;

import rzehan.shared.engine.exceptions.HashComputationException;
import rzehan.shared.engine.exceptions.InvalidPathException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by martin on 21.10.16.
 */
public class Utils {

    /**
     * Suported types are Integer, String, File, List<String>, List<File>
     *
     * @param type
     * @param object
     * @return
     */
    @Deprecated
    public static boolean instanceOf(String type, Object object) {
        if (type.equals("string")) {
            return object instanceof String;
        } else if (type.equals("integer")) {
            return object instanceof Integer;
        } else if (type.equals("file")) {
            return object instanceof File;
        } else if (type.equals("list")) {
            return object instanceof List;
        } else if (type.equals("string_list")) {
            //tohle neni uplne korektni
            if (object instanceof List) {
                List list = (List) object;
                if (list.isEmpty()) {
                    return true;
                    //Tohle by fungovat melo, List<X> i List<Y> pouzivaji stejny runtime objekt.
                    // Pretypovani projde a protoze je prazdny, problem nenastane:
                    /*List<String> sl = new ArrayList<String>();
                    List l = (List) sl;
                    List<Integer> il = (List<Integer>) l;*/
                } else {
                    Object firstItem = list.get(0);
                    return firstItem instanceof String;
                    //Tohle nemusi nutne vzdy fungovat. Je na producentovi, aby nepouzival obecny List, jinak muze nastat tohle:
                    /*List l = new ArrayList();
                    l.add("string");
                    l.add(Integer.valueOf(0));
                    List<Integer> il = (List<Integer>) l;
                    for(Integer i : il){
                        //ClassCastException here
                    }*/
                }
            } else {
                return false;
            }
        } else if (type.equals("file_list")) {
            if (object instanceof List) {
                List list = (List) object;
                if (list.isEmpty()) {
                    return true;
                } else {
                    Object firstItem = list.get(0);
                    return firstItem instanceof File;
                }
            } else {
                return false;
            }
        } else {
            throw new RuntimeException("Neznámý typ " + type);
        }
    }

    public static File buildAbsoluteFile(File parentDir, String filePath) throws InvalidPathException {
        //prevod do "zakladni formy", tj. zacinajici rovnou nazvem souboru/adresare
        //tj. "neco", "\neco", "/neco", "./neco", ".\neco" -> "neco"
        if (filePath.startsWith("./") || filePath.startsWith(".\\")) {
            filePath = filePath.substring(2, filePath.length());
        } else if (filePath.startsWith("/") || filePath.startsWith("\\")) {
            filePath = filePath.substring(1, filePath.length());
        }
        // tenhle tvar nesmi ale zacinat na tecky ani lomitka
        if (filePath.matches("^[\\./\\\\]+.*")) {
            System.out.println(filePath);
            throw new InvalidPathException(filePath);
        }
        String[] segments = filePath.split("[\\\\/]");
        File file = new File(parentDir, buildPathFromSegments(segments));
        return file.getAbsoluteFile();
    }

    private static String buildPathFromSegments(String[] segments) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i != 0) {//prvni soubor/adresar by nemel zacinat oddelovacem
                builder.append(File.separatorChar);
            }
            builder.append(segments[i]);
        }
        return builder.toString();
    }

    public static String computeHash(File file) throws HashComputationException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
            fis.close();
            return md5.toUpperCase();
        } catch (Exception e) {
            throw new HashComputationException(e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns longest common substrings (dynamic programming)
     * https://en.wikipedia.org/wiki/Longest_common_substring_problem
     *
     * @param s
     * @param t
     * @return
     */
    public static Set<String> findLongestCommonSubstrings(String s, String t) {
        int[][] table = new int[s.length()][t.length()];
        int longest = 0;
        Set<String> result = new HashSet<>();

        for (int i = 0; i < s.length(); i++) {
            for (int j = 0; j < t.length(); j++) {
                if (s.charAt(i) != t.charAt(j)) {
                    continue;
                }

                table[i][j] = (i == 0 || j == 0) ? 1
                        : 1 + table[i - 1][j - 1];
                if (table[i][j] > longest) {
                    longest = table[i][j];
                    result.clear();
                }
                if (table[i][j] == longest) {
                    result.add(s.substring(i - longest + 1, i + 1));
                }
            }
        }
        return result;
    }

    public static int getLongestCommonSubstringLength(String s, String t) {
        Set<String> longestCommonSubstrings = findLongestCommonSubstrings(s, t);
        if (longestCommonSubstrings.isEmpty()) {
            return 0;
        } else {
            return longestCommonSubstrings.iterator().next().length();
        }
    }


}
