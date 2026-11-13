package me.yeonjae.ovlo.adapter.out.persistence;

import tools.jackson.databind.json.JsonMapper;

final class NoticeJson {
    private static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();
    private NoticeJson() {}
    static String write(Object value) { return MAPPER.writeValueAsString(value); }
    static <T> T read(String json, Class<T> type) { return json == null ? null : MAPPER.readValue(json, type); }
}
