package com.example.financetracker.util.parser;

import com.example.financetracker.dto.request.CreateTransactionRequest;
import com.example.financetracker.dto.response.StatementImportPreviewResponse;
import com.example.financetracker.dto.response.TransactionResponse;
import com.example.financetracker.dto.result.StatementParseResult;
import com.example.financetracker.model.entity.Bank;
import com.example.financetracker.model.entity.BankAccount;
import com.example.financetracker.model.entity.Transactions;
import com.example.financetracker.model.types.TransactionSource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChaseStatementParser implements StatementParser {

    @Override
    public boolean supports(Bank bank) {
        return bank != null && "Chase".equalsIgnoreCase(String.valueOf(bank.getName()));
    }

    @Override
    public StatementParseResult parse(String text, BankAccount bankAccount) {

        String[] lines = text.split("\\R");

        Pattern statementPeriodPattern = Pattern.compile(
                "([A-Za-z]+\\s\\d{1,2},\\s\\d{4})\\s+through\\s+([A-Za-z]+\\s\\d{1,2},\\s\\d{4})"
        );

        Pattern transactionPattern = Pattern.compile(
                "^(\\d{2}/\\d{2})\\s+(.+?)\\s+(-?[\\d,]+\\.\\d{2})\\s+(-?[\\d,]+\\.\\d{2})$"
        );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

        LocalDate startDate = null;
        LocalDate endDate = null;

        List<CreateTransactionRequest> responseList = new ArrayList<>();

        List<String> statementLines = new ArrayList<>();

        for (String line : lines) {
            Matcher statementMatcher = statementPeriodPattern.matcher(line);
            if (statementMatcher.find()) {
                String startDateStr = statementMatcher.group(1);
                String endDateStr = statementMatcher.group(2);

                startDate = LocalDate.parse(startDateStr, formatter);
                endDate = LocalDate.parse(endDateStr, formatter);
            }

            Matcher transactionMatcher = transactionPattern.matcher(line);
            if (transactionMatcher.matches()) {
                String postDateStr = transactionMatcher.group(1);     // 03/23
                String middle = transactionMatcher.group(2);          // Card Purchase With Pin 03/22 Old Navy...
                BigDecimal amount = new BigDecimal(transactionMatcher.group(3).replace(",", ""));
                BigDecimal balance = new BigDecimal(transactionMatcher.group(4).replace(",", ""));

                String merchant = extractMerchant(middle);

                int year = getYearForTransaction(postDateStr, startDate, endDate);

                LocalDate transactionDate = LocalDate.parse(
                        postDateStr + "/" + year,
                        DateTimeFormatter.ofPattern("MM/dd/yyyy")
                );

                statementLines.add(line);
                CreateTransactionRequest transaction = new CreateTransactionRequest(
                        amount,
                        middle,
                        merchant,
                        transactionDate.atStartOfDay(),
                        null,
                        bankAccount.getId()
                );

                responseList.add(transaction);
            } else if (line.matches("^\\d{2}/\\d{2}.*")) {
                System.out.println("POSSIBLE MISSED TRANSACTION: " + line);
                statementLines.add(line);
            }

        }
        for (String line : statementLines) {
            sendOllama(createPrompt(startDate, endDate, line));
        }

        return new StatementParseResult(startDate, endDate, responseList);
    }

    private void sendOllama(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("model", "llama3.2:3b");
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ollama:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response body: " + response.body());
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readTree(response.body());

            String responseJson = rootNode.get("response").asString();

            JsonNode responseNode = mapper.readTree(responseJson);

            String merchantResponse = responseNode.get("merchant").asString();

            String descriptionResponse = responseNode.get("description").asString();

            String categoryResponse = responseNode.get("transactionCategory").asString();

            System.out.println("Merchant Response: " + merchantResponse);
            System.out.println("Description Response: " + descriptionResponse);
            System.out.println("Category Response: " + categoryResponse);

        } catch (IOException |  InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String createPrompt(LocalDate startDate, LocalDate endDate, String statement) {

        return """
                You are a JSON extraction engine.
                
                Return ONLY valid JSON.
                Do not explain.
                Do not write code.
                Do not use markdown.
                Do not include ```json.
                Do not include Python, Java, or SQL.
                For the merchant try and extract just the merchants name not any of location data or identifying numbers.
                You MUST return all 3 fields:
                description
                merchant
                transactionCategory
                
                Convert this single bank transaction line into this JSON shape:
                
                {
                  "description": "",
                  "merchant": "",
                  "transactionCategory" : ""
                }
                
                Statement period: %s through %s
                
                Transaction line:
                %s
                """.formatted(startDate, endDate, statement);
    }

    private String extractMerchant(String middle) {
        middle = middle.replaceFirst(
                "^(Card Purchase With Pin|Card Purchase|Recurring Card Purchase|Purchase With Pin)\\s+",
                ""
        );

        // Remove everything before the second MM/dd if OCR messed up the transaction type
        middle = middle.replaceFirst("^.*?\\d{2}/\\d{2}\\s+", "");

        middle = middle.replaceFirst("\\s+Card(?:\\s+\\d{4})?$", "");

        return middle.trim();
    }

    private int getYearForTransaction(
            String mmdd,
            LocalDate statementStartDate,
            LocalDate statementEndDate
    ) {
        int month = Integer.parseInt(mmdd.substring(0, 2));

        if (statementStartDate.getYear() == statementEndDate.getYear()) {
            return statementStartDate.getYear();
        }

        // Handles statements crossing Dec -> Jan
        if (month >= statementStartDate.getMonthValue()) {
            return statementStartDate.getYear();
        }

        return statementEndDate.getYear();
    }
}
