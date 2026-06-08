package com.example.aimanager.service.cloud;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.ecs.model.v20140526.*;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.example.aimanager.entity.CloudAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AliyunCloudClient implements CloudClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunCloudClient.class);

    @Override
    public boolean supports(String provider) {
        return "aliyun".equals(provider);
    }

    @Override
    public List<Map<String, Object>> describeInstances(CloudAccount account) {
        List<Map<String, Object>> instances = describeInstancesInRegion(account, account.getRegion());
        log.info("阿里云[{}] 默认区域[{}] 找到 {} 个实例", account.getAliasName(), account.getRegion(), instances.size());
        return instances;
    }

    private List<Map<String, Object>> describeInstancesInRegion(CloudAccount account, String region) {
        if (region == null || region.isEmpty()) return Collections.emptyList();
        if (!hasValidCredentials(account)) {
            log.warn("阿里云[{}] 凭证不完整，跳过", account.getAliasName());
            return Collections.emptyList();
        }
        try {
            IAcsClient client = createClient(account, region);

            DescribeInstancesRequest req = new DescribeInstancesRequest();
            req.setPageSize(50);
            req.setSysRegionId(region);

            DescribeInstancesResponse resp = client.getAcsResponse(req);
            List<Map<String, Object>> instances = new ArrayList<>();

            for (DescribeInstancesResponse.Instance instance : resp.getInstances()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", instance.getInstanceId());
                item.put("name", instance.getInstanceName());
                item.put("provider", "aliyun");
                item.put("type", "ecs");
                item.put("status", mapStatus(instance.getStatus()));
                item.put("cpu", String.valueOf(instance.getCpu()));
                item.put("memory", String.valueOf(instance.getMemory()));
                if (instance.getPublicIpAddress() != null && !instance.getPublicIpAddress().isEmpty()) {
                    item.put("publicIp", instance.getPublicIpAddress().get(0));
                } else {
                    item.put("publicIp", instance.getEipAddress() != null ? instance.getEipAddress().getIpAddress() : "");
                }
                if (instance.getVpcAttributes() != null && instance.getVpcAttributes().getPrivateIpAddress() != null
                        && !instance.getVpcAttributes().getPrivateIpAddress().isEmpty()) {
                    item.put("privateIp", instance.getVpcAttributes().getPrivateIpAddress().get(0));
                } else {
                    item.put("privateIp", "");
                }
                item.put("region", region);
                item.put("accountAlias", account.getAliasName());
                item.put("osName", instance.getOSName());
                item.put("createTime", String.valueOf(instance.getCreationTime()));
                item.put("expireTime", String.valueOf(instance.getExpiredTime()));
                instances.add(item);
            }
            return instances;

        } catch (ClientException e) {
            log.warn("阿里云[{}] DescribeInstances 失败: {}", region, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void startInstance(CloudAccount account, String instanceId) {
        if (!hasValidCredentials(account)) throw new RuntimeException("凭证不完整");
        try {
            IAcsClient client = createClient(account, null);
            StartInstanceRequest req = new StartInstanceRequest();
            req.setInstanceId(instanceId);
            client.getAcsResponse(req);
        } catch (ClientException e) {
            throw new RuntimeException("启动实例失败: " + e.getMessage());
        }
    }

    @Override
    public void stopInstance(CloudAccount account, String instanceId) {
        if (!hasValidCredentials(account)) throw new RuntimeException("凭证不完整");
        try {
            IAcsClient client = createClient(account, null);
            StopInstanceRequest req = new StopInstanceRequest();
            req.setInstanceId(instanceId);
            req.setForceStop(false);
            client.getAcsResponse(req);
        } catch (ClientException e) {
            throw new RuntimeException("停止实例失败: " + e.getMessage());
        }
    }

    @Override
    public void rebootInstance(CloudAccount account, String instanceId) {
        if (!hasValidCredentials(account)) throw new RuntimeException("凭证不完整");
        try {
            IAcsClient client = createClient(account, null);
            RebootInstanceRequest req = new RebootInstanceRequest();
            req.setInstanceId(instanceId);
            req.setForceStop(false);
            client.getAcsResponse(req);
        } catch (ClientException e) {
            throw new RuntimeException("重启实例失败: " + e.getMessage());
        }
    }

    private IAcsClient createClient(CloudAccount account, String region) {
        DefaultProfile profile = DefaultProfile.getProfile(region != null ? region : account.getRegion(),
                account.getAccessKey(), account.getAccessSecret());
        com.aliyuncs.http.HttpClientConfig clientConfig = com.aliyuncs.http.HttpClientConfig.getDefault();
        clientConfig.setConnectionTimeoutMillis(5000);
        clientConfig.setReadTimeoutMillis(15000);
        profile.setHttpClientConfig(clientConfig);
        return new DefaultAcsClient(profile);
    }

    @Override
    public boolean testConnection(CloudAccount account) {
        if (!hasValidCredentials(account)) return false;
        try {
            IAcsClient client = createClient(account, null);
            DescribeRegionsRequest req = new DescribeRegionsRequest();
            client.getAcsResponse(req);
            return true;
        } catch (ClientException e) {
            return false;
        }
    }

    private boolean hasValidCredentials(CloudAccount account) {
        return account.getAccessKey() != null && !account.getAccessKey().isEmpty()
                && account.getAccessSecret() != null && !account.getAccessSecret().isEmpty();
    }

    private String mapStatus(String status) {
        if (status == null) return "error";
        return switch (status) {
            case "Running" -> "running";
            case "Stopped" -> "stopped";
            case "Starting" -> "running";
            case "Stopping" -> "stopped";
            default -> "error";
        };
    }
}
