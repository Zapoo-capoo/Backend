package com.common.storage.service.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class FileSecurityValidator {

    public boolean checkXXE(Path filePath) throws IOException {
        if (containsExternalEntity(filePath)) {
            log.error("-------------- XXE PAYLOAD: {}", filePath.getFileName());
            Files.delete(filePath);
            return true;
        }
        return false;
    }

    private boolean containsExternalEntity(Path filePath) {
        try {
            String content = Files.readString(filePath);
            boolean hasExternalEntity = content.contains("<!ENTITY")
                    && (content.contains("SYSTEM") || content.contains("PUBLIC"));
            if (hasExternalEntity) {
                return true;
            }

            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

            try (InputStream is = Files.newInputStream(filePath)) {
                XMLStreamReader reader = factory.createXMLStreamReader(is);
                while (reader.hasNext()) {
                    reader.next();
                }
                reader.close();
                return false;
            }
        } catch (XMLStreamException e) {
            return e.getMessage() != null && e.getMessage().contains("entity resolution");
        } catch (Exception e) {
            return false;
        }
    }
}
