package com.coniungo.app.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.OffsetDateTime;

public class GsonConfig {

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .create();
    }
}