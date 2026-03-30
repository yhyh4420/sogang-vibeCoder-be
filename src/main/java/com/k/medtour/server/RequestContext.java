package com.k.medtour.server;

import com.k.medtour.global.auth.UserPrincipal;

import java.util.Map;

public class RequestContext {

    private final String method;
    private final String path;
    private final Map<String, String> pathParams;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final String rawBody;
    private UserPrincipal userPrincipal;

    public RequestContext(String method, String path,
                          Map<String, String> pathParams,
                          Map<String, String> queryParams,
                          Map<String, String> headers,
                          String rawBody) {
        this.method = method;
        this.path = path;
        this.pathParams = pathParams;
        this.queryParams = queryParams;
        this.headers = headers;
        this.rawBody = rawBody;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public String pathParam(String name) {
        String value = pathParams.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Path parameter not found: " + name);
        }
        return value;
    }

    public Long pathParamAsLong(String name) {
        return Long.parseLong(pathParam(name));
    }

    public String queryParam(String name) {
        return queryParams.get(name);
    }

    public String queryParam(String name, String defaultValue) {
        return queryParams.getOrDefault(name, defaultValue);
    }

    public Integer queryParamAsInt(String name, int defaultValue) {
        String val = queryParams.get(name);
        if (val == null || val.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(val);
    }

    public Long queryParamAsLong(String name) {
        String val = queryParams.get(name);
        if (val == null || val.isBlank()) {
            return null;
        }
        return Long.parseLong(val);
    }

    public <T> T body(Class<T> clazz) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new IllegalStateException("Request body is empty");
        }
        return JsonUtil.fromJson(rawBody, clazz);
    }

    public String rawBody() {
        return rawBody;
    }

    public String header(String name) {
        return headers.get(name.toLowerCase());
    }

    public UserPrincipal userPrincipal() {
        return userPrincipal;
    }

    public void setUserPrincipal(UserPrincipal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }
}
