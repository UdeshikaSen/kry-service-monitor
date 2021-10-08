package com.kry.servicepoller.entities;

import java.net.URL;
import java.util.Date;

public class Service {
    private int id;
    private String name;
    private URL url;
    private Date createdDate;
    private ServiceStatus currentStatus;
    
    public Service() {
    }
    
    public Service(int id, String name, URL url, Date createdDate, ServiceStatus currentStatus) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.createdDate = createdDate;
        this.currentStatus = currentStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public ServiceStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ServiceStatus currentStatus) {
        this.currentStatus = currentStatus;
    }
    
    @Override
    public String toString() {
        return "Service{" + "name='" + name + "'" + ", url=" + url + '}';
    }
}
