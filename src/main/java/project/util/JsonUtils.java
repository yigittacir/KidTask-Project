package project.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public class JsonUtils {

    // LocalDate ayarlı Gson
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .create();

    // --- GENERIC SAVE (KAYDETME) ---
    public static <T> void save(String filePath, List<T> data) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            // Uyarıyı düzeltmek için printStackTrace yerine bunu kullanıyoruz
            System.err.println("Dosya kaydetme hatası: " + e.getMessage());
        }
    }

    // --- GENERIC LOAD (OKUMA) ---
    public static <T> List<T> load(String filePath, Type type) {
        File file = new File(filePath);
        if (!file.exists()) return new ArrayList<>();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            System.err.println("Dosya okuma hatası: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // --- Type helper ---
    public static <T> Type listOf(Class<T> cls) {
        return TypeToken.getParameterized(List.class, cls).getType();
    }

    // --- LocalDate Adapter ---
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(date.toString());
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDate.parse(json.getAsString());
        }
    }
}