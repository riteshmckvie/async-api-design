package com.ritesh.async.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("null")
public class CsvUploadController {

    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    @PostMapping("/upload/csv")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a CSV file to upload.");
        }

        if (!isCsvValid(file)) {
            return ResponseEntity.badRequest().body("Invalid CSV file format or data.");
        }

        String transactionId = "TXN-" + System.currentTimeMillis();
        System.out.println(transactionId);

        //1. Start asynchronous processing and return a CompletableFuture
        CompletableFuture<Void> processingFuture = CompletableFuture.runAsync(() -> {
            asyncProcessCsv(file, transactionId);
        });

        //2. Return a 202 (Accepted) response with the transaction ID
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transactionId);
    }



    private boolean isCsvValid(MultipartFile file) {
        // Add your CSV validation logic here
        // Return true if the CSV is valid; otherwise, return false
        return true;
    }


    private void asyncProcessCsv(MultipartFile file, String transactionId) {
        CompletableFuture<Void> processingFuture = CompletableFuture.runAsync(() -> {
            // Your CSV processing logic here
            try (CSVReader csvReader = new CSVReader(
                    new InputStreamReader(file.getInputStream()))) {

                // Process CSV rows here
                // ...
            	try {
            		  Thread.sleep(1000);
            		} catch (InterruptedException e) {
            		  Thread.currentThread().interrupt();
            		}
                int totalRows = 2;
				// Send progress updates via SSE
                for (int i = 1; i <= totalRows ; i++) {
                    String progressMessage = "Processing row " + i;
                    sendProgressUpdate(transactionId, progressMessage);
                }

                // Send completion message via SSE
                sendCompletionMessage(transactionId, "CSV processing completed.");
            } catch (Exception e) {
                // Handle exceptions during processing
                sendErrorMessage(transactionId, "Error during processing: " + e.getMessage());
            } finally {
                sseEmitters.remove(transactionId);
            }
        });

        // Handle any exceptions that occur during processing
        processingFuture.exceptionally(ex -> {
            sendErrorMessage(transactionId, "Error during processing: " + ex.getMessage());
            return null;
        });
    }


    @GetMapping("/sse/{transactionId}")
    public SseEmitter getSseEmitter(@PathVariable String transactionId) {
        SseEmitter sseEmitter = new SseEmitter();
    	System.out.println("inside getSseEmitter "+transactionId);

        sseEmitters.put(transactionId, sseEmitter);
        return sseEmitter;
    }


    private void sendProgressUpdate(String transactionId, String message) {
        SseEmitter sseEmitter = sseEmitters.get(transactionId);
        if (sseEmitter != null) {
        	System.out.println("inside sendProgressUpdate "+transactionId);
            try {
                sseEmitter.send(SseEmitter.event().name("progress").data(message));
            } catch (IOException e) {
                // Handle exceptions when sending SSE updates
                e.printStackTrace();
            }
        }
    }


    private void sendCompletionMessage(String transactionId, String message) {
        SseEmitter sseEmitter = sseEmitters.get(transactionId);

        if (sseEmitter != null) {
        	System.out.println("inside sendCompletionMessage "+transactionId);
            try {
                sseEmitter.send(SseEmitter.event().name("complete").data(message));
                sseEmitter.complete(); // Close the SSE connection
            } catch (IOException e) {
                // Handle exceptions when sending SSE updates
                e.printStackTrace();
            }
        }
    }


    private void sendErrorMessage(String transactionId, String message) {
        SseEmitter sseEmitter = sseEmitters.get(transactionId);

        if (sseEmitter != null) {
        	System.out.println("inside sendErrorMessage "+transactionId);
            try {
                sseEmitter.send(SseEmitter.event().name("error").data(message));
                sseEmitter.completeWithError(new RuntimeException(message)); // Complete with an error
            } catch (IOException e) {
                // Handle exceptions when sending SSE updates
                e.printStackTrace();
            }
        }
    }
}