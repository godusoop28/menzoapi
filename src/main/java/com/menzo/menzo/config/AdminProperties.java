package com.menzo.menzo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "menzo.admin")
public class AdminProperties {

    private String masterEmail = "emy.rodriguezc28@gmail.com";

    public String getMasterEmail() {
        return masterEmail;
    }

    public void setMasterEmail(String masterEmail) {
        this.masterEmail = masterEmail;
    }
}
