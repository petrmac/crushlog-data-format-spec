package io.cldf.tool.utils;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtils {

  private static final ObjectMapper MAPPER = createObjectMapper(true);
  private static final ObjectMapper COMPACT_MAPPER = createObjectMapper(false);

  private static ObjectMapper createObjectMapper(boolean prettyPrint) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    if (prettyPrint) {
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    return mapper;
  }

  public static String toJson(Object obj, boolean prettyPrint) throws IOException {
    return (prettyPrint ? MAPPER : COMPACT_MAPPER).writeValueAsString(obj);
  }

  public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
    return MAPPER.readValue(json, clazz);
  }
}
