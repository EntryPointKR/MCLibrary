package kr.rvs.mclibrary.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.rvs.mclibrary.bukkit.gson.BukkitTypeAdapterFactory;
import kr.rvs.mclibrary.bukkit.gson.MaterialAndDataAdapter;
import kr.rvs.mclibrary.bukkit.item.MaterialAndData;

import java.lang.reflect.Type;

/**
 * Created by Junhyeong Lim on 2017-08-18.
 */
public class GsonManager {
    private final GsonBuilder builder;

    public GsonManager(GsonBuilder builder) {
        this.builder = builder;
    }

    public GsonManager() {
        this(new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapterFactory(new BukkitTypeAdapterFactory())
                .registerTypeAdapter(MaterialAndData.class, new MaterialAndDataAdapter()));
    }

    public Gson getGson() {
        return builder.create();
    }

    public void registerTypeAdapter(Type type, Object typeAdapter) {
        builder.registerTypeAdapter(type, typeAdapter);
    }

    public GsonBuilder getBuilder() {
        return builder;
    }
}