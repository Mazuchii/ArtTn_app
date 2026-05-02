package tn.esprit.museum.utils;

import java.io.IOException;

public class TextToSpeechUtil {

    /**
     * Convertit un texte en parole (synthèse vocale).
     * Supporte Windows (PowerShell + .NET), macOS (say) et Linux (espeak).
     */
    public static void speak(String text) {
        if (text == null || text.isEmpty()) return;

        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                // Windows : utilise PowerShell + .NET SpeechSynthesizer
                String cmd = "powershell -Command \"Add-Type -AssemblyName System.Speech; " +
                        "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$synth.Speak('" + escapeText(text) + "')\"";
                Runtime.getRuntime().exec(cmd);
            } else if (os.contains("mac")) {
                // macOS : commande 'say'
                Runtime.getRuntime().exec(new String[]{"say", text});
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux : nécessite 'espeak' (installation : sudo apt install espeak)
                Runtime.getRuntime().exec(new String[]{"espeak", text});
            } else {
                System.err.println("Système d'exploitation non supporté pour la synthèse vocale.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String escapeText(String text) {
        return text.replace("'", "''").replace("\"", "\\\"");
    }
}