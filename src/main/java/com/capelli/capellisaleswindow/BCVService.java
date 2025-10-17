
package com.capelli.capellisaleswindow;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONObject;

public class BCVService {

    // URL de una API pública que refleja los datos del BCV
    private static final String API_URL = "https://pydolarvenezuela.p.rapidapi.com/api/v1/dollar/bcv";
    
    // NOTA: Esta API requiere una clave. Puedes obtener una gratis en RapidAPI.
    // O usar una alternativa que no lo requiera. Por simplicidad, usaremos una que no lo necesita.
    // Cambiemos a una URL más directa que no requiere autenticación:
    private static final String DIRECT_API_URL = "https://api.exchangedyn.com/markets/quotes/usdves/bcv";


    public static double getBCVRate() {
        try {
            URL url = new URL(DIRECT_API_URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Error en la API: Código de respuesta " + responseCode);
                return 0.0;
            }

            StringBuilder inline = new StringBuilder();
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNext()) {
                inline.append(scanner.nextLine());
            }
            scanner.close();

            JSONObject data_obj = new JSONObject(inline.toString());
            
            JSONObject sources = data_obj.getJSONObject("sources");
            JSONObject bcv = sources.getJSONObject("BCV");
            double rate = bcv.getDouble("quote");

            return rate;

        } catch (IOException | org.json.JSONException e) {
            e.printStackTrace();
            return 0.0;
        }
    }
}