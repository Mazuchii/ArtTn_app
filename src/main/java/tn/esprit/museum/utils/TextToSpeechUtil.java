package tn.esprit.museum.utils;

import java.io.IOException;

public class TextToSpeechUtil {

    public static void speak(String text) {
        if (text == null || text.isEmpty()) return;
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                String cmd = "powershell -Command \"Add-Type -AssemblyName System.Speech; " +
                        "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$synth.Speak('" + escapeText(text) + "')\"";
                Runtime.getRuntime().exec(cmd);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"say", text});
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"espeak", text});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escapeText(String text) {
        return text.replace("'", "''").replace("\"", "\\\"");
    }
}
