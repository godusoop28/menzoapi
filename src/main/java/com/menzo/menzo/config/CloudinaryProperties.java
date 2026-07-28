package com.menzo.menzo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "menzo.cloudinary")
public class CloudinaryProperties {

    /** Formato completo: cloudinary://<api_key>:<api_secret>@<cloud_name> */
    private String url = "";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
