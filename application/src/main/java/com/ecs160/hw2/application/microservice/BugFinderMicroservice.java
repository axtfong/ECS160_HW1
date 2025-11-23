package com.ecs160.hw2.application.microservice;

import com.ecs160.hw2.application.model.BugIssue;
import com.ecs160.hw2.application.service.OllamaClient;
import com.ecs160.hw2.microservice.Endpoint;
import com.ecs160.hw2.microservice.Microservice;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Microservice B: Bug Finder
 * Provides an endpoint that accepts C file contents and returns a list of JSON Issues.
 */
@Microservice
public class BugFinderMicroservice {
    private OllamaClient ollamaClient;
    private Gson gson;

    public BugFinderMicroservice() {
        this.ollamaClient = new OllamaClient();
        this.gson = new Gson();
    }

    @Endpoint(url = "find_bugs")
    public String handleRequest(String input) {
        try {
            // Parse input - expect JSON with filename and content
            JsonObject inputJson = JsonParser.parseString(input).getAsJsonObject();
            String filename = inputJson.has("filename") ? inputJson.get("filename").getAsString() : "unknown.c";
            String code = inputJson.has("content") ? inputJson.get("content").getAsString() : input;
            
            // Create prompt for Ollama
            String prompt = String.format(
                "Analyze the following C code and identify all bugs. " +
                "Return a JSON array of bug reports. Each bug report should have:\n" +
                "{\n" +
                "  \"bug_type\": \"[type of bug like NullPointerException, MemoryLeak, etc.]\",\n" +
                "  \"line\": [line number where the bug occurs],\n" +
                "  \"description\": \"[brief description of the bug]\",\n" +
                "  \"filename\": \"%s\"\n" +
                "}\n\n" +
                "C Code:\n%s\n\n" +
                "Return only a JSON array of bug objects, no other text.",
                filename, code
            );
            
            // Get response from Ollama
            String response = ollamaClient.generate(prompt);
            
            // Try to parse the response as JSON array
            try {
                // Try to extract JSON array from response
                int arrayStart = response.indexOf("[");
                int arrayEnd = response.lastIndexOf("]") + 1;
                if (arrayStart >= 0 && arrayEnd > arrayStart) {
                    String jsonStr = response.substring(arrayStart, arrayEnd);
                    JsonArray jsonArray = JsonParser.parseString(jsonStr).getAsJsonArray();
                    
                    List<BugIssue> bugs = new ArrayList<>();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        try {
                            JsonObject bugObj = jsonArray.get(i).getAsJsonObject();
                            BugIssue bug = parseBugIssueFromJson(bugObj);
                            bugs.add(bug);
                        } catch (Exception e) {
                            System.err.println("Error parsing bug issue " + i + ": " + e.getMessage());
                            // Skip this issue
                        }
                    }
                    
                    return gson.toJson(bugs);
                }
                
                // If no array found, try to parse as single object
                int objStart = response.indexOf("{");
                int objEnd = response.lastIndexOf("}") + 1;
                if (objStart >= 0 && objEnd > objStart) {
                    String jsonStr = response.substring(objStart, objEnd);
                    try {
                        JsonObject bugObj = JsonParser.parseString(jsonStr).getAsJsonObject();
                        BugIssue bug = parseBugIssueFromJson(bugObj);
                        List<BugIssue> bugs = new ArrayList<>();
                        bugs.add(bug);
                        return gson.toJson(bugs);
                    } catch (Exception e) {
                        System.err.println("Error parsing single bug object: " + e.getMessage());
                    }
                }
                
                // Fallback: return empty array
                return gson.toJson(new ArrayList<>());
            } catch (Exception e) {
                System.err.println("Error parsing Ollama response: " + e.getMessage());
                e.printStackTrace();
                return gson.toJson(new ArrayList<>());
            }
        } catch (Exception e) {
            System.err.println("Error finding bugs: " + e.getMessage());
            e.printStackTrace();
            return gson.toJson(new ArrayList<>());
        }
    }
    
    /**
     * Safely parses BugIssue from JSON, handling "None" values and other edge cases.
     */
    private BugIssue parseBugIssueFromJson(JsonObject json) {
        BugIssue bugIssue = new BugIssue();
        
        // Handle bug_type
        if (json.has("bug_type")) {
            try {
                bugIssue.setBug_type(json.get("bug_type").getAsString());
            } catch (Exception e) {
                bugIssue.setBug_type("Unknown");
            }
        } else {
            bugIssue.setBug_type("Unknown");
        }
        
        // Handle line - convert "None" or invalid values to -1
        if (json.has("line")) {
            try {
                String lineStr = json.get("line").getAsString().trim();
                if (lineStr.equalsIgnoreCase("None") || lineStr.equalsIgnoreCase("null") || 
                    lineStr.isEmpty() || lineStr.equals("-")) {
                    bugIssue.setLine(-1);
                } else {
                    bugIssue.setLine(Integer.parseInt(lineStr));
                }
            } catch (Exception e) {
                try {
                    // Try as integer
                    bugIssue.setLine(json.get("line").getAsInt());
                } catch (Exception ex) {
                    bugIssue.setLine(-1);
                }
            }
        } else {
            bugIssue.setLine(-1);
        }
        
        // Handle description
        if (json.has("description")) {
            try {
                bugIssue.setDescription(json.get("description").getAsString());
            } catch (Exception e) {
                bugIssue.setDescription("");
            }
        } else {
            bugIssue.setDescription("");
        }
        
        // Handle filename
        if (json.has("filename")) {
            try {
                String filename = json.get("filename").getAsString();
                if (filename.equalsIgnoreCase("None") || filename.equalsIgnoreCase("null")) {
                    bugIssue.setFilename("");
                } else {
                    bugIssue.setFilename(filename);
                }
            } catch (Exception e) {
                bugIssue.setFilename("");
            }
        } else {
            bugIssue.setFilename("");
        }
        
        return bugIssue;
    }
}

