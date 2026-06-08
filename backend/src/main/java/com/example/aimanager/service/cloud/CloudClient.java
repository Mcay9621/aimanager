package com.example.aimanager.service.cloud;

import com.example.aimanager.entity.CloudAccount;

import java.util.List;
import java.util.Map;

public interface CloudClient {

    boolean testConnection(CloudAccount account);

    void startInstance(CloudAccount account, String instanceId);

    void stopInstance(CloudAccount account, String instanceId);

    void rebootInstance(CloudAccount account, String instanceId);

    List<Map<String, Object>> describeInstances(CloudAccount account);

    boolean supports(String provider);
}
