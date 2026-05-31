package com.capstone.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * JsonDataReader — Reads test data from JSON files.
 *
 * WHY EXTERNAL JSON FOR TEST DATA (not hardcoded):
 * 1. Separation of concerns: test logic in .java, test data in .json
 * 2. Non-developers (BAs, manual testers) can update test data without touching Java
 * 3. Data-driven testing: same test method runs with multiple datasets via @DataProvider
 * 4. No recompilation needed when test data changes
 * 5. Capstone requirement: "no hardcoding values, use helpers JSON, Excel etc."
 *
 * WHY JACKSON:
 * ObjectMapper is thread-safe after configuration — safe to share as a static
 * instance across parallel tests. Most widely used JSON library in enterprise Java.
 */
public class JsonDataReader {

    private static final Logger log = LoggerFactory.getLogger(JsonDataReader.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String DEFAULT_FILE = "testdata/testdata.json";

    // Cached root node — loaded once, reused across all tests
    private static JsonNode rootNode;

    private JsonDataReader() {}

    private static synchronized JsonNode getRoot() {
        if (rootNode == null) {
            try (InputStream is = JsonDataReader.class.getClassLoader()
                    .getResourceAsStream(DEFAULT_FILE)) {
                if (is == null) throw new RuntimeException("testdata.json not found on classpath");
                rootNode = mapper.readTree(is);
                log.info("Test data loaded from: {}", DEFAULT_FILE);
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse testdata.json", e);
            }
        }
        return rootNode;
    }

    /**
     * Reads nested value using dot-notation path.
     * e.g. get("notes.home_note.title") → "Buy Groceries"
     */
    public static String get(String jsonPath) {
        JsonNode node = getRoot();
        for (String key : jsonPath.split("\\.")) {
            node = node.get(key);
            if (node == null) throw new RuntimeException("Path not found in testdata.json: " + jsonPath);
        }
        return node.asText();
    }

    // ===== CONVENIENCE METHODS — used by test classes =====

    public static String getUserEmail(String userKey) {
        return get("users." + userKey + ".email");
    }

    public static String getUserPassword(String userKey) {
        return get("users." + userKey + ".password");
    }

    public static String getUserName(String userKey) {
        return get("users." + userKey + ".name");
    }

    public static String getNoteTitle(String noteKey) {
        return get("notes." + noteKey + ".title");
    }

    public static String getNoteDescription(String noteKey) {
        return get("notes." + noteKey + ".description");
    }

    public static String getNoteCategory(String noteKey) {
        return get("notes." + noteKey + ".category");
    }

    // Alias used by some classes
    public static String getTestData(String jsonPath) {
        return get(jsonPath);
    }

    /**
     * DataProvider for NotesUITests — returns all note combinations.
     * Returns Object[][] used directly by TestNG @DataProvider.
     */
    public static Object[][] getAllNotesDataProvider() {
        JsonNode notes = getRoot().get("notes");
        return new Object[][] {
            {
                notes.get("home_note").get("category").asText(),
                notes.get("home_note").get("title").asText(),
                notes.get("home_note").get("description").asText()
            },
            {
                notes.get("work_note").get("category").asText(),
                notes.get("work_note").get("title").asText(),
                notes.get("work_note").get("description").asText()
            },
            {
                notes.get("personal_note").get("category").asText(),
                notes.get("personal_note").get("title").asText(),
                notes.get("personal_note").get("description").asText()
            }
        };
    }

    // Alias kept for backward compatibility with older test references
    public static Object[][] getNoteDataProvider() {
        return getAllNotesDataProvider();
    }
}
