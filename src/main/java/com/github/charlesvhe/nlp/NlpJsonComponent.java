package com.github.charlesvhe.nlp;

import java.io.IOException;

import org.springframework.boot.jackson.JsonComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.hankcs.hanlp.seg.common.Term;

@JsonComponent
public class NlpJsonComponent {
	public static class TermSerializer extends JsonSerializer<Term> {
		@Override
		public void serialize(Term value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeStartObject();
			gen.writeStringField("word", value.word);
			gen.writeNumberField("offset", value.offset);
			gen.writeStringField("nature", value.nature.toString());
			gen.writeEndObject();
		}
	}
}
