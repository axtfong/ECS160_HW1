package com.ecs160.hw2.application;

import com.ecs160.hw2.application.microservice.BugFinderMicroservice;
import com.ecs160.hw2.application.microservice.IssueComparatorMicroservice;
import com.ecs160.hw2.application.microservice.IssueSummarizerMicroservice;
import com.ecs160.hw2.application.service.OllamaClient;
import com.google.gson.JsonObject;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for microservices with mocked LLM responses.
 */
public class MicroserviceTest {

    @Test
    public void testIssueSummarizerMicroservice() throws Exception {
        // Mock OllamaClient
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "{\"bug_type\":\"NullPointerException\",\"line\":42,\"description\":\"Possible null dereference\",\"filename\":\"test.java\"}";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        // Create microservice with mocked client
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);
        assertNotNull(service);

        // Test with mock response
        String testInput = "{\"title\":\"Bug in code\",\"description\":\"Null pointer exception\"}";
            String result = service.handleRequest(testInput);
            assertNotNull(result);
        assertTrue(result.contains("bug_type"));
        assertTrue(result.contains("NullPointerException"));
    }

    @Test
    public void testBugFinderMicroservice() throws Exception {
        // Mock OllamaClient
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"MemoryLeak\",\"line\":10,\"description\":\"Memory not freed\",\"filename\":\"test.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        // Create microservice with mocked client
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        assertNotNull(service);

        String testInput = "{\"filename\":\"test.c\",\"content\":\"int main() { return 0; }\"}";
            String result = service.handleRequest(testInput);
            assertNotNull(result);
        assertTrue(result.contains("bug_type") || result.equals("[]"));
    }

    @Test
    public void testIssueComparatorMicroservice() throws Exception {
        // Mock OllamaClient
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"NullPointerException\",\"line\":42,\"description\":\"Common bug\",\"filename\":\"test.java\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        // Create microservice with mocked client
        IssueComparatorMicroservice service = new IssueComparatorMicroservice(mockClient);
        assertNotNull(service);

        JsonObject input = new JsonObject();
        input.add("list1", new com.google.gson.JsonArray());
        input.add("list2", new com.google.gson.JsonArray());
        
        String result = service.handleRequest(input.toString());
        assertNotNull(result);
        // Result should be a JSON array (even if empty)
        assertTrue(result.startsWith("[") && result.endsWith("]"));
    }
    
    @Test
    public void testBugFinderDetectsMemoryLeak() throws Exception {
        // Test that BugFinder can detect memory leaks
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"MemoryLeak\",\"line\":15,\"description\":\"Memory allocated but not freed\",\"filename\":\"memory.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "void test() { char* ptr = malloc(100); /* missing free(ptr) */ }";
        String testInput = "{\"filename\":\"memory.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect memory leak", result.contains("MemoryLeak") || result.contains("memory"));
    }
    
    @Test
    public void testBugFinderDetectsNullPointer() throws Exception {
        // Test that BugFinder can detect null pointer dereferences
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"NullPointerException\",\"line\":20,\"description\":\"Possible null pointer dereference\",\"filename\":\"null.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "void test(int* ptr) { int x = *ptr; /* ptr might be NULL */ }";
        String testInput = "{\"filename\":\"null.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect null pointer", result.contains("NullPointer") || result.contains("null"));
    }
    
    @Test
    public void testBugFinderDetectsBufferOverflow() throws Exception {
        // Test that BugFinder can detect buffer overflows
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"BufferOverflow\",\"line\":10,\"description\":\"Array index out of bounds\",\"filename\":\"buffer.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "void test() { int arr[10]; arr[20] = 5; /* buffer overflow */ }";
        String testInput = "{\"filename\":\"buffer.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect buffer overflow", result.contains("Buffer") || result.contains("overflow") || result.contains("bounds"));
    }
    
    @Test
    public void testBugFinderDetectsMultipleBugs() throws Exception {
        // Test that BugFinder can detect multiple bugs in one file
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[" +
            "{\"bug_type\":\"MemoryLeak\",\"line\":5,\"description\":\"Memory leak\",\"filename\":\"multi.c\"}," +
            "{\"bug_type\":\"NullPointerException\",\"line\":10,\"description\":\"Null pointer\",\"filename\":\"multi.c\"}" +
            "]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "void test1() { malloc(100); } void test2(int* p) { *p = 5; }";
        String testInput = "{\"filename\":\"multi.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        // Should contain multiple bugs
        assertTrue("Should detect multiple bugs", result.contains("MemoryLeak") || result.contains("NullPointer"));
    }
    
    @Test
    public void testIssueSummarizerExtractsBugType() throws Exception {
        // Test that IssueSummarizer properly extracts bug type from GitHub issue
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "{\"bug_type\":\"SegmentationFault\",\"line\":42,\"description\":\"Segmentation fault occurs\",\"filename\":\"crash.c\"}";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);
        String testInput = "{\"title\":\"Crash in application\",\"description\":\"Application crashes with segmentation fault\",\"body\":\"The app crashes when accessing invalid memory\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should extract bug type", result.contains("bug_type"));
        assertTrue("Should contain segmentation fault info", result.contains("Segmentation") || result.contains("crash"));
    }
    
    @Test
    public void testIssueSummarizerHandlesMissingFields() throws Exception {
        // Test that IssueSummarizer handles GitHub issues with missing fields
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "{\"bug_type\":\"Unknown\",\"line\":-1,\"description\":\"Issue description\",\"filename\":\"\"}";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);
        String testInput = "{\"title\":\"Bug report\"}"; // Missing description and body
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should handle missing fields gracefully", result.contains("bug_type"));
    }
    
    @Test
    public void testIssueComparatorFindsCommonBugs() throws Exception {
        // Test that IssueComparator can find common bugs between two lists
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"MemoryLeak\",\"line\":15,\"description\":\"Memory leak in function\",\"filename\":\"leak.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        IssueComparatorMicroservice service = new IssueComparatorMicroservice(mockClient);
        
        // Create two lists with one common bug
        com.google.gson.JsonArray list1 = new com.google.gson.JsonArray();
        JsonObject bug1 = new JsonObject();
        bug1.addProperty("bug_type", "MemoryLeak");
        bug1.addProperty("line", 15);
        bug1.addProperty("description", "Memory leak in function");
        bug1.addProperty("filename", "leak.c");
        list1.add(bug1);
        
        com.google.gson.JsonArray list2 = new com.google.gson.JsonArray();
        JsonObject bug2 = new JsonObject();
        bug2.addProperty("bug_type", "MemoryLeak");
        bug2.addProperty("line", 15);
        bug2.addProperty("description", "Memory not freed");
        bug2.addProperty("filename", "leak.c");
        list2.add(bug2);
        
        JsonObject input = new JsonObject();
        input.add("list1", list1);
        input.add("list2", list2);
        
        String result = service.handleRequest(input.toString());
        assertNotNull(result);
        assertTrue("Should find common bugs", result.startsWith("[") && result.endsWith("]"));
    }
    
    @Test
    public void testBugFinderHandlesMalformedOllamaResponse() throws Exception {
        // Test that BugFinder handles malformed JSON from Ollama gracefully
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        // Simulate malformed response with extra text
        String mockResponse = "Here is the JSON: [{\"bug_type\":\"Test\",\"line\":1,\"description\":\"Test bug\",\"filename\":\"test.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testInput = "{\"filename\":\"test.c\",\"content\":\"int main() { return 0; }\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        // Should still parse the JSON array from the response
        assertTrue("Should handle malformed response", result.startsWith("[") && result.endsWith("]"));
    }
    
    @Test
    public void testBugFinderHandlesEmptyCode() throws Exception {
        // Test that BugFinder handles empty code gracefully
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[]"; // No bugs found
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testInput = "{\"filename\":\"empty.c\",\"content\":\"\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertEquals("Should return empty array for empty code", "[]", result);
    }
    
    @Test
    public void testOllamaPromptFormatting() throws Exception {
        // Test that prompts sent to Ollama are properly formatted
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String[] capturedPrompt = new String[1];
        
        when(mockClient.generate(Mockito.anyString())).thenAnswer(invocation -> {
            capturedPrompt[0] = invocation.getArgument(0);
            return "[{\"bug_type\":\"Test\",\"line\":1,\"description\":\"Test\",\"filename\":\"test.c\"}]";
        });
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testInput = "{\"filename\":\"test.c\",\"content\":\"int x = 5;\"}";
        service.handleRequest(testInput);
        
        assertNotNull("Prompt should be captured", capturedPrompt[0]);
        assertTrue("Prompt should mention C code", capturedPrompt[0].contains("C code") || capturedPrompt[0].contains("C Code"));
        assertTrue("Prompt should mention bug detection", capturedPrompt[0].contains("bug") || capturedPrompt[0].contains("Bug"));
        assertTrue("Prompt should include code", capturedPrompt[0].contains("int x"));
    }
    
    @Test
    public void testIssueSummarizerPromptFormatting() throws Exception {
        // Test that IssueSummarizer formats prompts correctly
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String[] capturedPrompt = new String[1];
        
        when(mockClient.generate(Mockito.anyString())).thenAnswer(invocation -> {
            capturedPrompt[0] = invocation.getArgument(0);
            return "{\"bug_type\":\"Test\",\"line\":1,\"description\":\"Test\",\"filename\":\"test.c\"}";
        });
        
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);
        String testInput = "{\"title\":\"Bug\",\"description\":\"Test bug\"}";
        service.handleRequest(testInput);
        
        assertNotNull("Prompt should be captured", capturedPrompt[0]);
        assertTrue("Prompt should mention GitHub issue", capturedPrompt[0].contains("issue") || capturedPrompt[0].contains("Issue"));
        assertTrue("Prompt should mention bug report", capturedPrompt[0].contains("bug") || capturedPrompt[0].contains("Bug"));
    }
    
    @Test
    public void testBugFinderDetectsUseAfterFree() throws Exception {
        // Test detection of use-after-free bugs
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"UseAfterFree\",\"line\":25,\"description\":\"Using memory after it has been freed\",\"filename\":\"useafterfree.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "void test() { int* ptr = malloc(sizeof(int)); free(ptr); *ptr = 5; }";
        String testInput = "{\"filename\":\"useafterfree.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect use-after-free", result.contains("UseAfterFree") || result.contains("use") || result.contains("free"));
    }
    
    @Test
    public void testBugFinderDetectsIntegerOverflow() throws Exception {
        // Test detection of integer overflow bugs
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"IntegerOverflow\",\"line\":8,\"description\":\"Integer overflow in arithmetic operation\",\"filename\":\"overflow.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "int test() { int max = INT_MAX; return max + 1; }";
        String testInput = "{\"filename\":\"overflow.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect integer overflow", result.contains("Overflow") || result.contains("overflow") || result.contains("Integer"));
    }
    
    @Test
    public void testBugFinderDetectsUninitializedVariable() throws Exception {
        // Test detection of uninitialized variable usage
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"UninitializedVariable\",\"line\":5,\"description\":\"Variable used before initialization\",\"filename\":\"uninit.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "void test() { int x; int y = x + 1; }";
        String testInput = "{\"filename\":\"uninit.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect uninitialized variable", result.contains("Uninitialized") || result.contains("uninitialized") || result.contains("Variable"));
    }
    
    @Test
    public void testBugFinderHandlesInvalidJsonResponse() throws Exception {
        // Test that BugFinder handles completely invalid JSON from Ollama
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "This is not JSON at all";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testInput = "{\"filename\":\"test.c\",\"content\":\"int main() { return 0; }\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        // Should return empty array when JSON parsing fails
        assertEquals("Should return empty array for invalid JSON", "[]", result);
    }
    
    @Test
    public void testBugFinderHandlesOllamaException() throws Exception {
        // Test that BugFinder handles exceptions from Ollama gracefully
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        when(mockClient.generate(anyString())).thenThrow(new java.io.IOException("Connection refused"));
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testInput = "{\"filename\":\"test.c\",\"content\":\"int main() { return 0; }\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        // Should return empty array when Ollama throws exception
        assertEquals("Should return empty array on Ollama exception", "[]", result);
    }
    
    @Test
    public void testIssueSummarizerHandlesOllamaException() throws Exception {
        // Test that IssueSummarizer handles Ollama exceptions gracefully
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        when(mockClient.generate(anyString())).thenThrow(new java.io.IOException("Connection refused"));
        
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);
        String testInput = "{\"title\":\"Bug\",\"description\":\"Test bug\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        // Should return error response with bug_type "Error"
        assertTrue("Should return error response", result.contains("Error") || result.contains("error"));
    }
    
    @Test
    public void testIssueComparatorHandlesNoCommonBugs() throws Exception {
        // Test that IssueComparator returns empty array when no common bugs exist
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[]"; // No common bugs
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        IssueComparatorMicroservice service = new IssueComparatorMicroservice(mockClient);
        
        com.google.gson.JsonArray list1 = new com.google.gson.JsonArray();
        JsonObject bug1 = new JsonObject();
        bug1.addProperty("bug_type", "MemoryLeak");
        bug1.addProperty("line", 10);
        bug1.addProperty("description", "Memory leak");
        bug1.addProperty("filename", "file1.c");
        list1.add(bug1);
        
        com.google.gson.JsonArray list2 = new com.google.gson.JsonArray();
        JsonObject bug2 = new JsonObject();
        bug2.addProperty("bug_type", "NullPointer");
        bug2.addProperty("line", 20);
        bug2.addProperty("description", "Null pointer");
        bug2.addProperty("filename", "file2.c");
        list2.add(bug2);
        
        JsonObject input = new JsonObject();
        input.add("list1", list1);
        input.add("list2", list2);
        
            String result = service.handleRequest(input.toString());
            assertNotNull(result);
        assertTrue("Should return array (may be empty)", result.startsWith("[") && result.endsWith("]"));
    }
    
    @Test
    public void testIssueSummarizerUsesBodyWhenDescriptionMissing() throws Exception {
        // Test that IssueSummarizer uses body field when description is missing
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "{\"bug_type\":\"Test\",\"line\":1,\"description\":\"Test from body\",\"filename\":\"test.c\"}";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        IssueSummarizerMicroservice service = new IssueSummarizerMicroservice(mockClient);
        String testInput = "{\"title\":\"Bug\",\"body\":\"This is the body text\"}"; // No description field
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should use body when description missing", result.contains("bug_type"));
    }
    
    @Test
    public void testBugFinderDetectsDivisionByZero() throws Exception {
        // Test detection of division by zero bugs
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String mockResponse = "[{\"bug_type\":\"DivisionByZero\",\"line\":12,\"description\":\"Division by zero error\",\"filename\":\"divide.c\"}]";
        when(mockClient.generate(anyString())).thenReturn(mockResponse);
        
        BugFinderMicroservice service = new BugFinderMicroservice(mockClient);
        String testCode = "int test(int x) { return 100 / x; } // x might be 0";
        String testInput = "{\"filename\":\"divide.c\",\"content\":\"" + testCode.replace("\"", "\\\"") + "\"}";
        
        String result = service.handleRequest(testInput);
        assertNotNull(result);
        assertTrue("Should detect division by zero", result.contains("Division") || result.contains("Zero") || result.contains("divide"));
    }
    
    @Test
    public void testIssueComparatorPromptFormatting() throws Exception {
        // Test that IssueComparator formats prompts correctly
        OllamaClient mockClient = Mockito.mock(OllamaClient.class);
        String[] capturedPrompt = new String[1];
        
        when(mockClient.generate(Mockito.anyString())).thenAnswer(invocation -> {
            capturedPrompt[0] = invocation.getArgument(0);
            return "[]";
        });
        
        IssueComparatorMicroservice service = new IssueComparatorMicroservice(mockClient);
        
        com.google.gson.JsonArray list1 = new com.google.gson.JsonArray();
        com.google.gson.JsonArray list2 = new com.google.gson.JsonArray();
        
        JsonObject input = new JsonObject();
        input.add("list1", list1);
        input.add("list2", list2);
        
        service.handleRequest(input.toString());
        
        assertNotNull("Prompt should be captured", capturedPrompt[0]);
        assertTrue("Prompt should mention comparing lists", capturedPrompt[0].contains("Compare") || capturedPrompt[0].contains("compare"));
        assertTrue("Prompt should mention common bugs", capturedPrompt[0].contains("common") || capturedPrompt[0].contains("same") || capturedPrompt[0].contains("similar"));
    }
}

