package org.tanzu.mcpclient.document;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class DocumentService {
    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenSplitter;

    public final static String DOCUMENT_ID = "documentId";

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.tokenSplitter = new TokenTextSplitter();
    }

    public void writeToVectorStore(MultipartFile file, String fileId) {
        Resource resource = file.getResource();
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource, PdfDocumentReaderConfig.defaultConfig());

        List<Document> documents = tokenSplitter.split(pdfReader.read());
        for (Document document : documents) {
            document.getMetadata().put(DOCUMENT_ID, fileId);
        }
        vectorStore.write(documents);
    }

}
