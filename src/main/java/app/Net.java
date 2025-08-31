package app;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;


public final class Net {
    private Net(){}


    public static class GeoResponse { public List<GeoResult> results; }
    public static class GeoResult { public double latitude, longitude; public String name, country, timezone; }
    public static class ForecastResponse { public String timezone; public Hourly hourly; public Daily daily; }


    //to get the data for hourly
    public static class Hourly {
        public List<String> time;
        @SerializedName("temperature_2m") public List<Double> temperature;
        @SerializedName("precipitation_probability") public List<Integer> rainProb;
        @SerializedName("wind_speed_10m") public List<Double> wind;
        @SerializedName("relative_humidity_2m") public List<Integer> humidity;
    }

    //to get the data for daily
    public static class Daily {
        public List<String> time;
        @SerializedName("temperature_2m_max") public List<Double> tmax;
        @SerializedName("temperature_2m_min") public List<Double> tmin;
        @SerializedName("precipitation_probability_max") public List<Integer> rainProbMax;
        @SerializedName("wind_speed_10m_max") public List<Double> windMax;
    }

  //to get the ip address to find the location that we are in currenntly
    public static class IPInfo { public String city; public String country; public double lat; public double lon; }

    public static IPInfo fetchIP() throws Exception {
        String json = get("http://ip-api.com/json");
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        if (!"success".equals(o.get("status").getAsString())) return null;
        IPInfo i = new IPInfo();
        i.city    = o.get("city").getAsString();
        i.country = o.get("country").getAsString();
        i.lat     = o.get("lat").getAsDouble();
        i.lon     = o.get("lon").getAsDouble();
        return i;
    }



    public static String get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }


    private static String getWithUA(String urlStr, String userAgent) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("GET");
        c.setRequestProperty("User-Agent", userAgent);
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

     // we used open mateo api to get the latitude and longitude and find a city
    public static Optional<GeoResult> geocode(String city) throws Exception {
        String q = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String json = get("https://geocoding-api.open-meteo.com/v1/search?count=1&name=" + q);
        GeoResponse resp = new Gson().fromJson(json, GeoResponse.class);
        if (resp != null && resp.results != null && !resp.results.isEmpty()) return Optional.of(resp.results.get(0));
        return Optional.empty();
    }

    // we used open mateo to get the data related to the weather
    public static Optional<ForecastResponse> fetchForecast(double lat, double lon) throws Exception {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.6f&longitude=%.6f" +
                        "&hourly=temperature_2m,precipitation_probability,wind_speed_10m,relative_humidity_2m" +
                        "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max" +
                        "&forecast_days=7&timezone=auto", lat, lon);
        String json = get(url);
        return Optional.ofNullable(new Gson().fromJson(json, ForecastResponse.class));
    }

    // using openstreetmap if open mateo doesnt work
    public static Optional<GeoResult> reverseGeocode(double lat, double lon) throws Exception {
        // clamp/normalize
        if (Double.isNaN(lat) || Double.isNaN(lon)) return Optional.empty();
        lat = Math.max(-90, Math.min(90, lat));
        lon = ((lon + 180) % 360 + 360) % 360 - 180;


        try {
            String url = String.format(Locale.US,
                    "https://geocoding-api.open-meteo.com/v1/reverse?latitude=%.5f&longitude=%.5f&count=1&language=en",
                    lat, lon);
            String json = get(url);
            GeoResponse resp = new Gson().fromJson(json, GeoResponse.class);
            if (resp != null && resp.results != null && !resp.results.isEmpty()) {
                return Optional.of(resp.results.get(0));
            }
        } catch (Exception ignore) { /* fall back */ }


        String url2 = String.format(Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f&zoom=10&addressdetails=1",
                lat, lon);
        String json2 = getWithUA(url2, "Forecastly/1.0 (example@example.com)");
        JsonObject o = JsonParser.parseString(json2).getAsJsonObject();
        if (o == null || !o.has("lat") || !o.has("lon")) return Optional.empty();

        GeoResult r = new GeoResult();
        r.latitude  = o.get("lat").getAsDouble();
        r.longitude = o.get("lon").getAsDouble();

        JsonObject addr = o.has("address") ? o.getAsJsonObject("address") : null;
        String name = pick(addr, "city","town","village","hamlet","county","state","region");
        if (name == null && o.has("name")) name = o.get("name").getAsString();
        if (name == null && o.has("display_name")) name = o.get("display_name").getAsString();
        r.name = name != null ? name : String.format(Locale.US, "Lat %.3f, Lon %.3f", lat, lon);
        r.country = (addr != null && addr.has("country")) ? addr.get("country").getAsString() : null;

        return Optional.of(r);
    }


    private static String pick(JsonObject o, String... keys){
        if (o == null) return null;
        for (String k : keys) if (o.has(k)) return o.get(k).getAsString();
        return null;
    }
}
