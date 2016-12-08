package rzehan.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;

import java.util.Random;

/**
 * Created by martin on 2.12.16.
 */
public class InitScreenController {

    @FXML
    Button btnTest;


    @FXML
    ProgressIndicator fdmfProgress;

    @FXML
    ImageView fdmfOkImg;

    @FXML
    ImageView fdmfErrorImg;

    @FXML
    Label fdmfErrorText;


    @FXML
    ProgressIndicator kakaduProgress;

    @FXML
    ImageView kakaduOkImg;

    @FXML
    ImageView kakaduErrorImg;

    @FXML
    Label kakaduErrorText;


    public void test(ActionEvent actionEvent) {
        init();
        btnTest.setDisable(true);
        checkFdmf();
    }

    private void checkFdmf() {
        fdmfProgress.setVisible(true);
        //btnTest.setDisable(true);
       /* fdmfOkImg.setVisible(false);
        fdmfErrorImg.setVisible(false);
        fdmfProgress.setVisible(true);
        fdmfErrorText.setVisible(false);*/

        //http://fxexperience.com/2011/07/worker-threading-in-javafx-2-0/
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        fdmfProgress.setVisible(false);
                        Random rand = new Random();
                        if (rand.nextBoolean()) {
                            fdmfOkImg.setVisible(true);
                            fdmfErrorImg.setVisible(false);
                        } else {
                            fdmfOkImg.setVisible(false);
                            fdmfErrorImg.setVisible(true);
                            fdmfErrorText.setVisible(true);
                        }
                        //btnTest.setDisable(false);
                        checkKakadu();
                    }
                });
            }
        }).start();
    }

    private void checkKakadu() {
        //btnTest.setDisable(true);
        kakaduProgress.setVisible(true);

      /*  kakaduOkImg.setVisible(false);
        kakaduErrorImg.setVisible(false);
        kakaduProgress.setVisible(true);
        kakaduErrorText.setVisible(false);*/

        //http://fxexperience.com/2011/07/worker-threading-in-javafx-2-0/
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        kakaduProgress.setVisible(false);
                        Random rand = new Random();
                        if (rand.nextBoolean()) {
                            kakaduOkImg.setVisible(true);
                            kakaduErrorImg.setVisible(false);
                        } else {
                            kakaduOkImg.setVisible(false);
                            kakaduErrorImg.setVisible(true);
                            kakaduErrorText.setVisible(true);
                        }
                        btnTest.setDisable(false);
                        //checkKakadu();
                    }
                });
            }
        }).start();
    }

    private void init(){
        fdmfOkImg.setVisible(false);
        fdmfErrorImg.setVisible(false);
        fdmfProgress.setVisible(false);
        fdmfErrorText.setVisible(false);

        kakaduOkImg.setVisible(false);
        kakaduErrorImg.setVisible(false);
        kakaduProgress.setVisible(false);
        kakaduErrorText.setVisible(false);
    }


}
