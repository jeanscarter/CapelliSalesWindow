package com.capelli.capellisaleswindow;

import com.capelli.config.AppConfig;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;

/**
 * Servicio para obtener la tasa de cambio del BCV.
 */
public class BCVService {

    private static final Logger LOGGER = Logger.getLogger(BCVService.class.getName());
    
    /**
     * Obtiene la tasa de cambio actual del BCV.
     * @return La tasa de cambio o la tasa por defecto si hay error
     */
    public static double getBCVRate() {
        try {
            String apiUrl = AppConfig.getBcvApiUrl();
            int timeout = AppConfig.getBcvTimeoutSeconds() * 1000; // Convertir a milisegundos
            
            LOGGER.info("Consultando tasa BCV desde: " + apiUrl);
            
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                LOGGER.warning("API BCV respondió con código: " + responseCode);
                return getDefaultRate();
            }

            StringBuilder inline = new StringBuilder();
            try (Scanner scanner = new Scanner(url.openStream())) {
                while (scanner.hasNext()) {
                    inline.append(scanner.nextLine());
                }
            }

            JSONObject data_obj = new JSONObject(inline.toString());
            JSONObject sources = data_obj.getJSONObject("sources");
            JSONObject bcv = sources.getJSONObject("BCV");
            double rate = bcv.getDouble("quote");
            
            LOGGER.info("Tasa BCV obtenida exitosamente: " + rate);
            return rate;

        } catch (IOException | org.json.JSONException e) {
            LOGGER.log(Level.WARNING, "Error al obtener tasa BCV, usando tasa por defecto", e);
            return getDefaultRate();
        }
    }
    
    /**
     * Obtiene la tasa por defecto configurada.
     * @return La tasa por defecto
     */
    public static double getDefaultRate() {
        double defaultRate = AppConfig.getDefaultBcvRate();
        LOGGER.info("Usando tasa BCV por defecto: " + defaultRate);
        return defaultRate;
    }
    
    /**
     * Intenta obtener la tasa desde la API, pero no lanza excepción.
     * @return La tasa obtenida o la tasa por defecto
     */
    public static double getBCVRateSafe() {
        try {
            return getBCVRate();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error inesperado al obtener tasa BCV", e);
            return getDefaultRate();
        }
    }
}