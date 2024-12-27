package com.atlasview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.VerticalAlignment;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.data.general.DefaultPieDataset;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
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

            // Step 3: Prepare Thymeleaf Context
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalTests", totalTests);
            summary.put("passedTests", passedTests);
            summary.put("failedTests", failedTests);
            summary.put("skippedTests", totalTests - (passedTests + failedTests));

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

            // Generate the PDF report
            generatePdfReport(tests);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generatePdfReport(JsonNode tests) throws IOException {
        // Set the path for the PDF file
        String pdfPath = "test-report.pdf";

        // Create a PDF writer and document
        PdfWriter writer = new PdfWriter(pdfPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Create a bold font for the title
        PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // Add a title
        document.add(new Paragraph("Test Report").setFont(boldFont).setFontSize(18));

        // Create a summary table
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

        // Generate pie chart
        String chartPath = generatePieChart(passedTests, failedTests, skippedTests);

        // Add pie chart to the PDF
        ImageData chartData = ImageDataFactory.create(chartPath);
        Image chartImage = new Image(chartData);
        chartImage.setWidth(400);  // Adjust the size of the chart
        document.add(chartImage);

        // Add a summary table
        Table summaryTable = new Table(4);
        summaryTable.addCell("Total Tests");
        summaryTable.addCell("Passed");
        summaryTable.addCell("Failed");
        summaryTable.addCell("Skipped");
        summaryTable.addCell(String.valueOf(totalTests));
        summaryTable.addCell(String.valueOf(passedTests));
        summaryTable.addCell(String.valueOf(failedTests));
        summaryTable.addCell(String.valueOf(skippedTests));
        document.add(summaryTable);

        // Add detailed test results
        document.add(new Paragraph("\nTest Results:").setFont(boldFont));
        Table detailsTable = new Table(4);
        detailsTable.addCell("Test Name");
        detailsTable.addCell("Status");
        detailsTable.addCell("Duration");
        detailsTable.addCell("Error (if any)");

        for (JsonNode test : tests) {
            detailsTable.addCell(test.get("name").asText());
            detailsTable.addCell(test.get("status").asText());
            detailsTable.addCell(test.get("duration").asText());
            detailsTable.addCell(test.has("error") ? test.get("error").asText() : "N/A");
        }

        document.add(detailsTable);

        // Close the document
        document.close();

        System.out.println("PDF report generated at: " + pdfPath);
    }

    private static String generatePieChart(int passed, int failed, int skipped) throws IOException {
        // Create dataset
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Passed", passed);
        dataset.setValue("Failed", failed);
        dataset.setValue("Skipped", skipped);

        // Create the chart
        JFreeChart chart = ChartFactory.createPieChart(
                "Test Results Overview",  // Chart title
                dataset,                  // Dataset
                true,                     // Include legend
                true,                     // Include tooltips
                false                     // Exclude URLs
        );

        // Customize the plot
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);  // Set background to white
        plot.setOutlinePaint(null);            // Remove border outline

        // Set modern colors for each section
        plot.setSectionPaint("Passed", new Color(102, 204, 0));  // Green
        plot.setSectionPaint("Failed", new Color(255, 51, 51));  // Red
        plot.setSectionPaint("Skipped", new Color(255, 204, 0)); // Yellow

        // Customize the legend
        chart.getLegend().setPosition(RectangleEdge.BOTTOM);
        chart.getLegend().setHorizontalAlignment(HorizontalAlignment.CENTER);

        // Customize title font
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setVerticalAlignment(VerticalAlignment.TOP);
        chart.getTitle().setHorizontalAlignment(HorizontalAlignment.CENTER);

        // Save the chart as an image
        String chartPath = "test-results-chart.png";
        ChartUtils.saveChartAsPNG(new File(chartPath), chart, 600, 400);

        return chartPath;  // Return the file path to the generated chart
    }
}
