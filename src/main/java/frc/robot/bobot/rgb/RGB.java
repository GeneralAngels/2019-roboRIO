package frc.robot.bobot.rgb;

import edu.wpi.first.wpilibj.SerialPort;
import frc.robot.bobot.Subsystem;

import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class RGB extends Subsystem {
    // Already OCed To 75Hz. Dont overclock higher!
    // Base clock: 20Hz
    private static final int REFRESH_RATE = 75;
    private int length = 1, divider = 1;
    private Timer timer;
    private SerialPort serial;
    private Pattern pattern;

    public RGB(int length, int divider) {
        if (length > 0)
            this.length = length;
        if (divider > 0)
            this.divider = divider;
        try {
            this.serial = new SerialPort(9600, SerialPort.Port.kUSB);
        } catch (Exception ignored) {
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                loop();
            }
        }, 0, 1000 / REFRESH_RATE);
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    private void loop() {
        if (pattern != null) {
            send(pattern.color(length));
            pattern.next(length);
        }
    }

    private void send(Color color) {
        try {
            byte[] packet = new byte[3];
            // RGB
//            packet[0] = (byte) (color.getRed() / divider);
//            packet[1] = (byte) (color.getGreen() / divider);
//            packet[2] = (byte) (color.getBlue() / divider);
            // BGR
            if (color != null) {
                packet[0] = (byte) (color.getBlue() / divider);
                packet[1] = (byte) (color.getGreen() / divider);
                packet[2] = (byte) (color.getRed() / divider);
            } else {
                packet[0] = (byte) -1;
                packet[1] = (byte) -1;
                packet[2] = (byte) -1;
            }
            serial.write(packet, 3);
        } catch (Exception ignored) {

        }
    }

    public interface Pattern {
        Color color(int length);

        void next(int length);
    }
}
