package rzehan.shared.engine;

import java.io.File;
import java.util.List;

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




}
