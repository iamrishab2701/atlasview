package com.atlasview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasViewReportGenerator {
    public static void main(String[] args) {
        try {
            // Step 1: Load JSON data
            File jsonFile = new File("src/main/resources/test-results.json");
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonFile);

            JsonNode tests = rootNode.get("tests");

            // Step 2: Calculate Summary Data
            int totalTests = tests.size();
            int passedTests = 0;
            int failedTests = 0;

            for (JsonNode test : tests) {
                String status = test.get("status").asText();
                if ("PASS".equalsIgnoreCase(status)) {
                    passedTests++;
                } else if ("FAIL".equalsIgnoreCase(status)) {
                    failedTests++;
                }
            }

            int skippedTests = totalTests - (passedTests + failedTests);

            // Step 3: Prepare Thymeleaf Context
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalTests", totalTests);
            summary.put("passedTests", passedTests);
            summary.put("failedTests", failedTests);
            summary.put("skippedTests", skippedTests);

            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("/templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode("HTML");
            resolver.setCharacterEncoding("UTF-8");

            TemplateEngine templateEngine = new TemplateEngine();
            templateEngine.setTemplateResolver(resolver);

            Context context = new Context();
            context.setVariable("tests", mapper.convertValue(tests, Object.class));
            context.setVariable("summary", summary);

            // Step 4: Generate the HTML file
            String htmlContent = templateEngine.process("report", context);

            FileWriter writer = new FileWriter("test-report.html");
            writer.write(htmlContent);
            writer.close();

            System.out.println("HTML report generated successfully!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}