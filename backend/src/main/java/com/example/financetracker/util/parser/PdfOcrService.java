package com.example.financetracker.util.parser;

import com.example.financetracker.dto.response.StatementImportPreviewResponse;
import com.example.financetracker.dto.result.StatementParseResult;
import com.example.financetracker.model.entity.BankAccount;
import com.example.financetracker.model.entity.User;
import com.example.financetracker.repository.BankAccountRepository;
import com.example.financetracker.repository.TransactionRepository;
import com.example.financetracker.repository.UserRepository;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class PdfOcrService {
    @Value("${app.media.path}")
    private String mediaPath;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final List<StatementParser> parsers;

    public PdfOcrService(
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            BankAccountRepository bankAccountRepository,
            List<StatementParser> parsers
    ) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.parsers = parsers;
    }

    private User getUser(String email) {
        return this.userRepository.findByUsername(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public StatementImportPreviewResponse previewPDFImport(
            String email,
            Long bankAccountId,
            MultipartFile file
    ) {
        User user = getUser(email);
        BankAccount account = this.bankAccountRepository.getBankAccountByIdAndUser(bankAccountId, user);
        StatementParser parser = parsers.stream()
                .filter(p -> p.supports(account.getBank()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No parser found for bank"));
        String text = extractText(file);
        StatementParseResult parsedInfo = parser.parse(text, account);
        return new StatementImportPreviewResponse(parsedInfo, account.getId());
    }

    public String extractText(MultipartFile file) {
        StringBuilder fullText = new StringBuilder();

        try  {
            // Convert MultipartFile -> temp file
            File tempFile = File.createTempFile("statement-", ".pdf");
            file.transferTo(tempFile);

            // Load PDF
            try (PDDocument document = Loader.loadPDF(tempFile)) {

                PDFRenderer renderer = new PDFRenderer(document);

                Tesseract tesseract = new Tesseract();

                // Docker path
                tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata");

                // Windows path example:
                // tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");

                tesseract.setLanguage("eng");

                for (int page = 0; page < document.getNumberOfPages(); page++) {

                    // Higher DPI = better OCR but slower
                    BufferedImage image =
                            renderer.renderImageWithDPI(page, 300);

                    String pageText = tesseract.doOCR(image);

                    fullText.append(pageText);
                    fullText.append("\n");
                }
            } catch (TesseractException | IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                tempFile.delete();
            }

            return fullText.toString();

        } catch (IOException e) {
            System.err.println("Error reading PDF: " + e.getMessage());
        }
        return null;
    }
}