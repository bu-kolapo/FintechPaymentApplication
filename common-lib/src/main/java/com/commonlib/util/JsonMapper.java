package com.commonlib.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonMapper {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public String toJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }

    public <T> T fromJson(String json, Class<T> clazz) {
        try { return mapper.readValue(json, clazz); }
        catch (JsonProcessingException e) { throw new RuntimeException(e); }
    }
}

