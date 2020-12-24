package fleetmanagement.webserver;


import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Providers;

import org.junit.*;
import org.mockito.*;

import com.sun.jersey.core.impl.provider.entity.StringProvider;
import com.sun.jersey.multipart.*;


public class MultiPartWriterTest {

	private static final MediaType MULTIPART_MIXED_TYPE = MultiPartMediaTypes.MULTIPART_MIXED_TYPE;
	private MultiPartWriter tested;
	private MultiPart entity;
	@Mock private Providers providers;
	@Mock private MultivaluedMap<String, Object> headers;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		tested = new MultiPartWriterWithFixedBoundary(providers);
		entity = new MultiPart();
		
		when(providers.getMessageBodyWriter(String.class, String.class, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE)).thenReturn(new StringProvider());
	}

	@Test
	public void returnsUnknownSize() throws Exception {
		long result = tested.getSize(entity, MultiPart.class, null, new Annotation[0], MULTIPART_MIXED_TYPE);
		assertEquals(-1, result);
	}
	
	@Test
	public void allowsWritingMultiParts() throws Exception {
		boolean writeable = tested.isWriteable(MultiPart.class, null, new Annotation[0], MULTIPART_MIXED_TYPE);
		assertTrue(writeable);
		
		writeable = tested.isWriteable(String.class, null, new Annotation[0], MULTIPART_MIXED_TYPE);
		assertFalse(writeable);
	}
	
	@Test
	public void writesMultipartsToOutput() throws Exception {				
		addBodyPart("4");
		addBodyPart("123").getHeaders().add("Content-Disposition", "attachment; filename=\"test.txt\"");
		
		Iterator<String> lines = writeToOutput();
		
		assertEquals("--Boundary", lines.next());
		assertEquals("Content-Length: 1", lines.next());
		assertEquals("Content-Type: application/octet-stream", lines.next());
		assertEquals("", lines.next());
		assertEquals("4", lines.next());
		
		assertEquals("--Boundary", lines.next());
		assertEquals("Content-Length: 3", lines.next());
		assertEquals("Content-Disposition: attachment; filename=\"test.txt\"", lines.next());
		assertEquals("Content-Type: application/octet-stream", lines.next());
		assertEquals("", lines.next());
		assertEquals("123", lines.next());

		assertEquals("--Boundary--", lines.next());
		assertFalse(lines.hasNext());
		verify(headers).putSingle("MIME-Version", "1.0");
		verify(headers).putSingle("Content-Type", MultiPartWriterWithFixedBoundary.BOUNDARY_TYPE);
	}
	
	@Test(expected=WebApplicationException.class)
	public void throwsExceptionWhenBodyPartCannotBeWritten() throws Exception {				
		BodyPart part = new BodyPart(new byte[0], MediaType.APPLICATION_OCTET_STREAM_TYPE);
		entity.bodyPart(part);
		
		writeToOutput();
	}
	
	@Test(expected=WebApplicationException.class)
	public void throwsExceptionForMissingBodyPartEntity() throws Exception {				
		BodyPart part = new BodyPart(null, MediaType.APPLICATION_OCTET_STREAM_TYPE);
		entity.bodyPart(part);
		
		writeToOutput();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void throwsExceptionForMissingBodyMediaType() throws Exception {				
		BodyPart part = new BodyPart("123", null);
		entity.bodyPart(part);
		
		writeToOutput();
	}
	
	@Test(expected=WebApplicationException.class)
	public void throwsExceptionWhenNoBodyPartsWereDefined() throws Exception {						
		writeToOutput();
	}
	
	private Iterator<String> writeToOutput() throws IOException, UnsupportedEncodingException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		tested.writeTo(entity, MultiPart.class, null, new Annotation[0], MULTIPART_MIXED_TYPE, headers, out);
		String resultingString = new String(out.toByteArray(), "UTF-8");
		return Arrays.asList(resultingString.split("\r\n")).iterator();
	}

	private BodyPart addBodyPart(String content) {
		BodyPart part = new BodyPart(content, MediaType.APPLICATION_OCTET_STREAM_TYPE);
		part.getHeaders().add("Content-Length", String.valueOf(content.length()));
		entity.bodyPart(part);
		return part;
	}
	
	private static class MultiPartWriterWithFixedBoundary extends MultiPartWriter {
		
		public static final MediaType BOUNDARY_TYPE;
		
		static {
			Map<String, String> params = new HashMap<String, String>();
			params.put("boundary", "Boundary");
			BOUNDARY_TYPE = new MediaType("multipart", "mixed", params);
		}
		
		public MultiPartWriterWithFixedBoundary(Providers providers) {
			super(providers);
		}

		@Override
		protected MediaType getBoundaryMediaType(MediaType mediaType) {
			return BOUNDARY_TYPE;
		}
	}
}
