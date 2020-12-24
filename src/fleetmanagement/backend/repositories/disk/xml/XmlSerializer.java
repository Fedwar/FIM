package fleetmanagement.backend.repositories.disk.xml;

import gsp.util.WrappedException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class XmlSerializer {

    private final JAXBContext context;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;

    public XmlSerializer(Class<?>... serializedClasses) {
        try {
            context = JAXBContext.newInstance(serializedClasses);
            marshaller = context.createMarshaller();
            unmarshaller = context.createUnmarshaller();
        } catch (Exception e) {
            throw new WrappedException(e);
        }
    }

    public Object load(File f) {
        try {
            return context.createUnmarshaller().unmarshal(f);
        } catch (JAXBException e) {
            throw new WrappedException(e);
        }
    }

    public void save(Object o, File f) {
        try (ByteArrayOutputStream data = new ByteArrayOutputStream()) {
            context.createMarshaller().marshal(o, data);
            if (data.size() == 0) {
                throw new JAXBException("Object marshal error");
            }
            try (FileOutputStream fos = new FileOutputStream(f)) {
                data.writeTo(fos);
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        } catch (Exception e) {
            throw new WrappedException(e);
        }

    }
}
