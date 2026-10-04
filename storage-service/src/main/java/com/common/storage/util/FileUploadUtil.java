package com.common.storage.util;

import com.common.storage.constant.SUPPORT_FILE_TYPE;
import com.common.storage.exception.InvalidFileTypeException;
import com.common.storage.exception.MaxUploadSizeExceedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class FileUploadUtil {
    @Value("#{${limit-size-in-mb.image:20} * 1024 * 1024}")
    private long MAX_IMAGE_SIZE; // 20MB

    @Value("#{${limit-size-in-mb.audio:5} * 1024 * 1024}")
    private long MAX_AUDIO_SIZE; // 5MB

    @Value("#{${limit-size-in-mb.office:5} * 1024 * 1024}")
    private long MAX_OFFICE_SIZE; // 5MB

    @Value("#{${limit-size-in-mb.video:100} * 1024 * 1024}")
    private long MAX_VIDEO_SIZE; // 100MB

    public String validateUploadingFile(MultipartFile file)
            throws MaxUploadSizeExceedException, IOException {
        String fileType = this.detectFileType(file);
        log.info("Try to upload file with type: {}", fileType);

        this.validateSupportedFileType(fileType);
        this.validateFileSize(file, fileType);

        return fileType;
    }

    private void validateSupportedFileType(String fileType) {
        if (!SUPPORT_FILE_TYPE.isSupported(fileType)) {
            throw new InvalidFileTypeException("The file type is not supported.");
        }
    }
    /////////////////////////////////////////////////////////


    private static void addOffsetsToStops(NodeList stops) {
        for (int i = 0; i < stops.getLength(); i++) {
            Element stop = (Element) stops.item(i);
            if (!stop.hasAttribute("offset")) {
                stop.setAttribute("offset", "0");
            }
        }
    }

    private static String addMissingOffsets(String svgContent) throws Exception {
        Document document = parseSvgContentToDocument(svgContent);
        if (document != null) {
            NodeList gradients = document.getElementsByTagName("linearGradient");
            for (int i = 0; i < gradients.getLength(); i++) {
                Element gradient = (Element) gradients.item(i);
                NodeList stops = gradient.getElementsByTagName("stop");
                addOffsetsToStops(stops);
            }
            gradients = document.getElementsByTagName("radialGradient");
            for (int i = 0; i < gradients.getLength(); i++) {
                Element gradient = (Element) gradients.item(i);
                NodeList stops = gradient.getElementsByTagName("stop");
                addOffsetsToStops(stops);
            }
            return documentToString(document);
        }
        return svgContent;
    }

    private static Document parseSvgContentToDocument(String svgContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(svgContent));
        return builder.parse(is);
    }

    public static String documentToString(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }



    private String detectFileType(MultipartFile file) throws IOException {
        return new Tika().detect(file.getBytes());
    }



    private void validateFileSize(MultipartFile file, String fileType)
            throws MaxUploadSizeExceedException {
        long fileSize = file.getSize();

        if (this.isImage(fileType)) {
            this.validateMaxSize(fileSize, MAX_IMAGE_SIZE,
                    "Image file size should not exceed: %d MB");
            return;
        }

        if (this.isAudio(fileType)) {
            this.validateMaxSize(fileSize, MAX_AUDIO_SIZE,
                    "Audio file size should not exceed: %d MB");
            return;
        }

        if (this.isVideo(fileType)) {
            this.validateMaxSize(fileSize, MAX_VIDEO_SIZE,
                    "Video file size should not exceed: %d MB");
            return;
        }

        if (this.isOfficeDocument(fileType)) {
            this.validateMaxSize(fileSize, MAX_OFFICE_SIZE,
                    "Office file size should not exceed: %d MB");
            return;
        }

        throw new InvalidFileTypeException("The file type is not supported.");
    }

    private void validateMaxSize(long actualSize, long maxSize, String messageTemplate)
            throws MaxUploadSizeExceedException {
        if (actualSize > maxSize) {
            log.error("Failed to upload large file: {}", actualSize);
            throw new MaxUploadSizeExceedException(
                    String.format(messageTemplate, maxSize / 1024 / 1024)
            );
        }
    }

    private boolean isImage(String fileType) {
        return fileType.startsWith("image/");
    }

    private boolean isAudio(String fileType) {
        return fileType.startsWith("audio/");
    }

    private boolean isVideo(String fileType) {
        return fileType.startsWith("video/")
                || "application/x-matroska".equals(fileType);
    }

    private boolean isOfficeDocument(String fileType) {
        return fileType.startsWith("application/")
                || fileType.startsWith("text/");
    }

    public BufferedImage handleBufferedImage(java.io.File imageFile) throws Exception {
        String type = Files.probeContentType(imageFile.toPath());
        if (!type.equals("image/svg+xml")) {
            return ImageIO.read(imageFile);
        }
        PNGTranscoder t = new PNGTranscoder();
        String svgContent = Files.readString(Path.of(imageFile.getPath()), StandardCharsets.UTF_8);
        String svgContentWithOffsets = addMissingOffsets(svgContent);
        TranscoderInput input = new TranscoderInput(new StringReader(svgContentWithOffsets));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);
        t.transcode(input, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        return ImageIO.read(inputStream);
    }
}
