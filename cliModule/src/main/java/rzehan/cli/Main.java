package rzehan.cli;

import rzehan.shared.SharedClass;

import java.io.File;

/**
 * Created by Martin Řehánek on 27.9.16.
 */
public class Main {
    public static void main(String[] args) {
        //String shared = "TODO";
        String shared = new SharedClass().toString();
        System.out.println("Hello from CLI, shared: " + shared);
    }

}
