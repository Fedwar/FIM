package fleetmanagement.frontend.webserver;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Locale;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.MessageBodyWriter;

import com.sun.jersey.core.provider.AbstractMessageReaderWriterProvider;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.controllers.Templates;

public class ModelAndViewWriter implements MessageBodyWriter<ModelAndView<?>> {
	
	private final UserSession request;

	public ModelAndViewWriter(UserSession request) {
		this.request = request;
	}

	@Override
	public long getSize(ModelAndView<?> toSerialize, Class<?> type, Type genericType, Annotation[] annotationts, MediaType mediaType) {
		return -1L;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotationts, MediaType mediaType) {
		return type == ModelAndView.class;
	}

	@Override
	public void writeTo(ModelAndView<?> toSerialize, Class<?> type, Type genericType, Annotation[] annotationts, MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream out) throws IOException, WebApplicationException {
		String html = serializePage(toSerialize);		
		addOptionalHtmlContentType(headers);
		AbstractMessageReaderWriterProvider.writeToAsString(html, out, mediaType);
	}

	private void addOptionalHtmlContentType(MultivaluedMap<String, Object> headers) {
		if (headers.containsKey("Content-Type"))
			headers.putSingle("Content-Type", MediaType.TEXT_HTML);
	}

	private String serializePage(ModelAndView<?> toSerialize) {
		Locale l = request.getLocale();
		SecurityContext sc = request.getSecurityContext();
		String result = Templates.renderPage(toSerialize.page, l, sc, toSerialize.viewmodel, request.getUsername());
		return result;
	}
}
