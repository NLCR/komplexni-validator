package rzehan.gui;

import rzehan.gui.sample.Config;

import java.io.File;
import java.io.IOException;

/**
 * Created by martin on 2.12.16.
 */
public class DataController {

    private static DataController instance;
    private final Config config;


    private DataController() throws IOException {
        config = new Config(new File("../../resources/main/config.properties"));
    }

    public static DataController instanceOf() throws IOException {
        //TODO: should be initialized with data
        if (instance == null) {
            instance = new DataController();
        }
        return instance;
    }


}
