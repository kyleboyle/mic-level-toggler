package com.kyleboyle;

import com.jautoitx.Keyboard;
import com.jautoitx.Win;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MicTrigger {

    private static final String WIN_TITLE = "key toggle on mic level";

    private static final int DEFAULT_TRIGGER_LEVEL = 30;
    private static final int DEFAULT_TOGGLE_TIMEOUT = 20;
    private static final String DEFAULT_KEYSTROKE = "k";
    private static final String DEFAULT_WINDOW_FILTERS = "Counter-Strike\npubg";
    private static Set<String> windowFilterSet;

    private static JFrame frame;
    private static volatile int triggerLevelValue = DEFAULT_TRIGGER_LEVEL;
    private static volatile int toggleTimeoutValue = DEFAULT_TOGGLE_TIMEOUT;
    private static volatile String keystroke = DEFAULT_KEYSTROKE;

    private static ImageIcon activeIcon = new ImageIcon(
            Toolkit.getDefaultToolkit().getImage(MicTrigger.class.getResource("/icon.png"))
                    .getScaledInstance(21, 18, Image.SCALE_DEFAULT)
    );

    private static void createAndShowGUI() throws LineUnavailableException {
        //Create and set up the window.
        frame = new JFrame(WIN_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JPanel main = new JPanel();

        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.getContentPane().add(main);
        main.setLayout(new SpringLayout());

        JLabel volumeLabel = new JLabel("0");
        volumeLabel.setHorizontalTextPosition(JLabel.LEFT);
        JSpinner triggerLevel = new JSpinner();
        ((SpinnerNumberModel) triggerLevel.getModel()).setMinimum(0);
        triggerLevel.getModel().setValue(DEFAULT_TRIGGER_LEVEL);
        triggerLevel.addChangeListener(e -> triggerLevelValue = Integer.parseInt(triggerLevel.getValue().toString()));

        main.add(new JLabel("current mic level:"));
        main.add(volumeLabel);
        volumeLabel.setPreferredSize(new Dimension(30, 20));

        main.add(new JLabel("trigger level:"));
        main.add(triggerLevel);

        main.add(new JLabel("toggle keystroke"));
        TextField keystrokeField = new TextField(DEFAULT_KEYSTROKE);
        main.add(keystrokeField);
        keystrokeField.addActionListener(a -> {
            keystroke = keystrokeField.getText();
        });

        JSpinner toggleTimeout = new JSpinner();
        ((SpinnerNumberModel) toggleTimeout.getModel()).setMinimum(0);
        toggleTimeout.getModel().setValue(DEFAULT_TOGGLE_TIMEOUT);
        toggleTimeout.addChangeListener(e -> {
            toggleTimeoutValue = Integer.parseInt(toggleTimeout.getValue().toString());
        });

        main.add(new JLabel("toggle timeout ms"));
        main.add(toggleTimeout);

        main.add(new JLabel("window title filters"));

        JTextArea windowFilters = new JTextArea(4, 20);
        windowFilters.setText(DEFAULT_WINDOW_FILTERS);
        setWindowFilters(DEFAULT_WINDOW_FILTERS);

        windowFilters.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }

            @Override
            public void focusLost(FocusEvent e) {
                setWindowFilters(windowFilters.getText());
            }
        });

        main.add(new JScrollPane(windowFilters));

        JToggleButton enabler = new JToggleButton("Disable", true);
        main.add(enabler);
        main.add(new JLabel());

        SpringUtilities.makeCompactGrid(main,
                6, 2,
                3, 3,  //initX, initY
                3, 10); //xPad, yPad

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage(MicTrigger.class.getResource("/icon2.png")));
        frame.pack();
        frame.setVisible(true);

        long[] updateCounter = new long[1];
        LevelMonitor levelMon = new LevelMonitor(l ->
        {
            updateCounter[0]++;

            if (updateCounter[0] % 2 == 0) {
                volumeLabel.setText(l.toString());
            }
            processLevel(l, volumeLabel);
        });
        levelMon.start();


        enabler.addActionListener(e -> {
            System.out.println(e.getActionCommand());
            if ("Disable".equals(e.getActionCommand())) {
                levelMon.stop();
                enabler.setActionCommand("Enable");
                enabler.setText("Enable");
            } else {
                try {
                    levelMon.start();
                    enabler.setActionCommand("Disable");
                    enabler.setText("Disable");
                } catch (LineUnavailableException e1) {
                    throw new RuntimeException(e1);
                }
            }
        });


        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                levelMon.stop();
                System.exit(0);
            }
        });

    }


    private static volatile boolean isPressed = false;
    private static long toggleExpireTime = 0;

    private static void processLevel(Integer micLevel, JLabel decorateComponent) {
        if (micLevel >= triggerLevelValue) {
            toggleExpireTime = System.currentTimeMillis() + toggleTimeoutValue;
            if (!isPressed) {
                String activeWindowTitle = Win.getActiveTitle();

                if (WIN_TITLE.equals(Win.getActiveTitle())) {
                    return;
                }
                boolean shouldTriggerForActiveWindow = false;
                for (String filter : windowFilterSet) {
                    if (activeWindowTitle.contains(filter)) {
                        shouldTriggerForActiveWindow = true;
                    }
                }
                if (!shouldTriggerForActiveWindow) {
                    return;
                }

                System.out.println("on");
                Keyboard.send("{" + keystroke + " down}");
                //decorateComponent.setBorder(BorderFactory.createLineBorder(Color.red));
                decorateComponent.setIcon(activeIcon);
                isPressed = true;
            }
        } else if (isPressed && System.currentTimeMillis() >= toggleExpireTime) {
            Keyboard.send("{" + keystroke + " up}");
            //decorateComponent.setBorder(BorderFactory.createEmptyBorder());
            decorateComponent.setIcon(null);
            isPressed = false;
            System.out.println("off");
        }
    }

    private static void setWindowFilters(String filters) {
        String[] lines = filters.split("[\\n\\r]+");
        System.out.println("new filters " + Arrays.toString(lines));
        windowFilterSet = new HashSet<>(Arrays.asList(lines));
    }

    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Unrecoverable error: " + e.toString(), "ERROR", JOptionPane.WARNING_MESSAGE);
                System.exit(1);
            }
        });
    }


    private static void audioLevel(JLabel level) throws LineUnavailableException {


    }

}
