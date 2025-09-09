package com.sora.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "collection")
public class Collection extends BaseEntity {
    
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    
    @Column(name = "name_key", nullable = false)
    private String nameKey;
    
    @Column(name = "icon_name")
    private String iconName;
    
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    public Collection() {}
    
    public Collection(String code, String nameKey, Integer sortOrder) {
        this.code = code;
        this.nameKey = nameKey;
        this.sortOrder = sortOrder;
        this.isDefault = false;
    }
    
    public Collection(String code, String nameKey, Integer sortOrder, Boolean isDefault) {
        this.code = code;
        this.nameKey = nameKey;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
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
    
    public String getIconName() {
        return iconName;
    }
    
    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}