package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "country")
public class Country extends BaseEntity {
    
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    
    @Column(name = "name_key", nullable = false)
    private String nameKey;
    
    @Column(name = "latitude", nullable = false)
    private Double latitude;
    
    @Column(name = "longitude", nullable = false)
    private Double longitude;
    
    @Column(name = "timezone")
    private String timezone;
    
    public Country() {}
    
    public Country(String code, String nameKey, Double latitude, Double longitude) {
        this.code = code;
        this.nameKey = nameKey;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getNameKey() {
        return nameKey;
    }
    
    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public String getTimezone() {
        return timezone;
    }
    
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}